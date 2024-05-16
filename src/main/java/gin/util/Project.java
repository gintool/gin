package gin.util;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import gin.test.UnitTest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.shared.invoker.*;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.gradle.tooling.*;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.idea.IdeaContentRoot;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.idea.IdeaSourceDirectory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.pmw.tinylog.Logger;

import javax.annotation.Nullable;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handy class for analysing Maven and Gradle projects.
 */
public class Project implements Serializable {

    @Serial
    private static final long serialVersionUID = -7683615011351128680L;

    private static final String DEFAULT_MAVEN_HOME = File.separator + "usr" + File.separator + "local" + File.separator;

    private static final boolean DEBUG = false;
    private final File projectDir;
    private final String projectName;
    private final List<File> moduleDirs = new LinkedList<>();
    private final List<File> mainSourceDirs = new LinkedList<>();
    private final List<File> testSourceDirs = new LinkedList<>();
    private final List<File> mainResourceDirs = new LinkedList<>();
    private final List<File> testResourceDirs = new LinkedList<>();
    private final List<File> mainClassDirs = new LinkedList<>();
    private final List<File> testClassDirs = new LinkedList<>();
    private File mavenHome = new File(DEFAULT_MAVEN_HOME);
    private String gradleVersion = "7.6";
    private BuildType buildType;

    /**
     * Builds a project object. You must call {@link #setUp()} or {@link #setUp(String, String)} before using it to capture the project's structure.
     *
     * @param directory the Project's directory
     * @param name      the Project's name
     */
    public Project(File directory, String name) {
        projectDir = directory.getAbsoluteFile();
        projectName = name;
        detectBuildType();
    }

    protected static String getMethodSignature(File srcDir, String methodName, String className, int lineNumber) {

        String pathToSource = className.replace(".", File.separator) + ".java";
        File sourceFile = new File(srcDir, pathToSource);

        if (!sourceFile.exists()) {
            return null;
        }

        CompilationUnit unit = null;
        try {
            unit = StaticJavaParser.parse(sourceFile);
        } catch (FileNotFoundException e) {
            Logger.error("Cannot find source file: " + sourceFile);
            System.exit(-1);
        }

        // Get all methods in the compilation unit
        List<MethodDeclaration> nodes = unit.getChildNodesByType(MethodDeclaration.class);

        for (MethodDeclaration m : nodes) {

            String name = m.getNameAsString();

            int start = m.getRange().get().begin.line;
            int end = m.getRange().get().end.line;

            if (name.equals(methodName) && (start <= lineNumber) && (lineNumber <= end)) {

                String methodSignature;
                methodSignature = m.getDeclarationAsString(false, false, false);

                // Strip out return type if provided
                String prefix = methodSignature.substring(0, methodSignature.indexOf("("));
                if (prefix.contains(" ")) {
                    int lastIndexOf = prefix.trim().lastIndexOf(" ");
                    methodSignature = methodSignature.substring(lastIndexOf + 1).trim();
                }

                // Remove all spaces
                methodSignature = methodSignature.replaceAll("\\s", "");

                return className + "." + methodSignature;

            }

        }

        return null;

    }

    public void setUp() {
        detectDirs();
    }

    public void setUp(@Nullable String gradleVersion, @Nullable String mavenHome) {
        if (!StringUtils.isBlank(gradleVersion)) {
            this.setGradleVersion(gradleVersion);
        }
        if (!StringUtils.isBlank(mavenHome)) {
            this.setMavenHome(FileUtils.getFile(mavenHome));
        }
        this.setUp();
    }

    public List<File> getMainSourceDirs() {
        return mainSourceDirs;
    }

    public List<File> getTestSourceDirs() {
        return testSourceDirs;
    }

    public List<File> getMainResourceDirs() {
        return mainResourceDirs;
    }

    public List<File> getTestResourceDirs() {
        return testResourceDirs;
    }

    public List<File> getMainClassDirs() {
        return mainClassDirs;
    }

    public List<File> getTestClassDirs() {
        return testClassDirs;
    }

    public File getProjectDir() {
        return projectDir;
    }

    // Getters/Setters
    public File getMavenHome() {
        return mavenHome;
    }

    public void setMavenHome(File mavenHome) {
        this.mavenHome = mavenHome;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getGradleVersion() {
        return gradleVersion;
    }

    public void setGradleVersion(String gradleVersion) {
        this.gradleVersion = gradleVersion;
    }

    public BuildType getBuildType() {
        return buildType;
    }

    // Calculate classpath for project
    public String classpath() {
        List<String> classDirNames = new LinkedList<>();
        for (File classDir : this.allClassDirs()) {
            classDirNames.add(classDir.getAbsolutePath());
        }
        String[] classDirNamesArray = classDirNames.toArray(new String[0]);
        if (isMavenProject()) {
            return String.join(File.pathSeparator, classDirNamesArray) + getDependenciesClasspath();
        } else {
            return String.join(File.pathSeparator, classDirNamesArray);
        }
    }

    // Return a list of all class directories for project
    public List<File> allClassDirs() {
        List<File> allClassDirs = new LinkedList<>(mainClassDirs);
        allClassDirs.addAll(testClassDirs);
        return allClassDirs;
    }

    // Return a list of all source directories for project
    public List<File> allSourceDirs() {
        List<File> allSourceDirs = new LinkedList<>(mainSourceDirs);
        allSourceDirs.addAll(testSourceDirs);
        return allSourceDirs;
    }

    // Return a list of all resource directories for project
    public List<File> allResourceDirs() {
        List<File> allResourceDirs = new LinkedList<>(mainResourceDirs);
        allResourceDirs.addAll(testResourceDirs);
        return allResourceDirs;
    }

    // Module directories, including at the top level
    public List<File> getModuleDirs() {
        return moduleDirs;
    }

    // Find a given source file within the project by classname
    public File findSourceFile(String className) {

        String pathToSource = className.replace(".", File.separator) + ".java";

        for (File dir : mainSourceDirs) {
            File sourceFile = new File(dir, pathToSource);
            if (sourceFile.exists()) {
                return sourceFile;
            }
        }

        return null;

    }

    // Check for build file to determine Maven/Gradle
    private void detectBuildType() {

        File mavenPomFile = new File(projectDir, "pom.xml");
        File gradleBuildFile = new File(projectDir, "build.gradle");

        if (mavenPomFile.exists()) {
            buildType = BuildType.MAVEN;
        } else if (gradleBuildFile.exists()) {
            buildType = BuildType.GRADLE;
        } else {
            Logger.error("Build file not found in: " + projectDir.getAbsolutePath());
            System.exit(-1);
        }

    }

    // Find source directories
    private void detectDirs() {

        if (isGradleProject()) {
            detectDirsGradle();
        } else if (isMavenProject()) {
            detectDirsMaven();
        }

    }

    public boolean isGradleProject() {
        return buildType == BuildType.GRADLE;
    }

    public boolean isMavenProject() {
        return buildType == BuildType.MAVEN;
    }

    private void detectDirsGradle() {

        GradleConnector connector = GradleConnector.newConnector().forProjectDirectory(projectDir);
        if (gradleVersion != null) {
            connector = connector.useGradleVersion(gradleVersion);
        }

        // Source Directories
        ProjectConnection connection = connector.connect();
        IdeaProject project = connection.getModel(IdeaProject.class);
        GradleProject gradleProject = connection.getModel(GradleProject.class);

        for (IdeaModule module : project.getModules()) {

            File outputDir = module.getCompilerOutput().getOutputDir();
            if (outputDir != null) {
                File classDir = new File(outputDir, "classes");
                File javaDir = new File(classDir, "java");
                File mainDir = new File(javaDir, "main");
                File testDir = new File(javaDir, "test");
                this.mainClassDirs.add(mainDir);
                this.testClassDirs.add(testDir);
            }

            for (IdeaContentRoot root : module.getContentRoots()) {

                File moduleDir = root.getRootDirectory();
                this.moduleDirs.add(moduleDir);

                File buildDir = new File(root.getRootDirectory(), "build");
                File classDir = new File(buildDir, "classes");
                File javaDir = new File(classDir, "java");
                File mainDir = new File(javaDir, "main");
                File testDir = new File(javaDir, "test");
                this.mainClassDirs.add(mainDir);
                this.testClassDirs.add(testDir);

                for (IdeaSourceDirectory dir : root.getSourceDirectories()) {
                    this.mainSourceDirs.add(dir.getDirectory());
                }

                for (IdeaSourceDirectory dir : root.getResourceDirectories()) {
                    this.mainResourceDirs.add(dir.getDirectory());
                }

                for (IdeaSourceDirectory dir : root.getTestDirectories()) {
                    this.testSourceDirs.add(dir.getDirectory());
                }

                for (IdeaSourceDirectory dir : root.getTestResourceDirectories()) {
                    this.testResourceDirs.add(dir.getDirectory());
                }
            }

        }

        // Class directories (build output)
        File buildDir = gradleProject.getBuildDirectory();
        File classDir = new File(buildDir, "classes");
        File javaDir = new File(classDir, "java");
        File mainDir = new File(javaDir, "main");
        File testDir = new File(javaDir, "test");
        this.mainClassDirs.add(mainDir);
        this.testClassDirs.add(testDir);

        connection.close();

    }

    // Find Class directories
    private void detectDirsMaven() {
        addDirMaven(projectDir);
    }

    private void addDirMaven(File dir) {

        this.moduleDirs.add(dir);

        MavenXpp3Reader reader = new MavenXpp3Reader();

        File pomFile = new File(dir, "pom.xml");
        Model model = null;

        try {
            model = reader.read(new FileReader(pomFile));
            model.setPomFile(pomFile);
        } catch (IOException | XmlPullParserException e) {
            Logger.error("Error creating maven model from pom.xml");
            Logger.error(e);
            System.exit(-1);
        }

        Build build = model.getBuild();

        String source = null;

        if (build != null) {
            source = model.getBuild().getSourceDirectory();
        }

        if (source == null) {
            source = "src" + File.separator + "main" + File.separator + "java";
        }

        File sourceDir = new File(dir, source);
        if (sourceDir.exists()) {
            this.mainSourceDirs.add(sourceDir);
        }

        String test = null;
        if (build != null) {
            test = model.getBuild().getTestSourceDirectory();
        }
        if (test == null) {
            test = "src" + File.separator + "test" + File.separator + "java";
        }
        File testDir = new File(dir, test);
        if (testDir.exists()) {
            this.testSourceDirs.add(testDir);
        }

        String output = null;
        if (build != null) {
            output = model.getBuild().getOutputDirectory();
        }
        if (output == null) {
            output = "target" + File.separator + "classes";
        }
        File mainClassDir = new File(dir, output);
        this.mainClassDirs.add(mainClassDir);

        String outputTest = null;
        if (build != null) {
            outputTest = model.getBuild().getTestOutputDirectory();
        }
        if (outputTest == null) {
            outputTest = "target" + File.separator + "test-classes";
        }
        File testClassDir = new File(dir, outputTest);
        this.testClassDirs.add(testClassDir);

        // Now any modules
        for (String module : model.getModules()) {
            File subdir = new File(dir, module);
            this.addDirMaven(subdir);
        }


    }

    public String getDependenciesClasspath() {

        StringBuilder dependencies = new StringBuilder();
        try {
            InvocationRequest request = new DefaultInvocationRequest();

            File pomFile = new File(projectDir, "pom.xml");
            request.setPomFile(pomFile);

            request.setGoals(Collections.singletonList("org.apache.maven.plugins:maven-dependency-plugin:3.1.1:list"));

            File depOutput = Files.createTempFile("gin-" + projectName + "-dependencies", ".txt").toFile();

            Properties properties = new Properties();
            properties.setProperty("outputFile", depOutput.getCanonicalPath());
            properties.setProperty("appendOutput", "true");
            properties.setProperty("outputAbsoluteArtifactFilename", "true");
            request.setProperties(properties);

            Invoker invoker = new DefaultInvoker();
            invoker.setMavenHome(mavenHome);

            InvocationResult result = null;

            // Extremely detailed debug output.
            if (DEBUG) {
                request.setErrorHandler(Logger::info);
            }

            request.setOutputHandler(line -> {
            });

            try {
                result = invoker.execute(request);
            } catch (MavenInvocationException e) {
                Logger.error(e, "Error invoking maven.");
                System.exit(-1);
            }

            if (result.getExitCode() != 0) {
                Logger.error("Invocation of Maven gave non-zero return code:" + result.getExitCode());
                System.exit(-1);
            }

            List<String> output;
            Path path = depOutput.toPath();
            output = Files.readAllLines(path);
            Files.deleteIfExists(depOutput.toPath());

            if (!output.isEmpty()) {
                for (String jar : output) {
                    Pattern pattern = Pattern.compile("(?:compile|:runtime|:test|:provided):(.*\\.jar)(.*)");
                    Matcher matcher = pattern.matcher(jar);
                    if (matcher.find()) {
                        dependencies.append(File.pathSeparator).append(matcher.group(1));
                    }
                }
            }
        } catch (IOException ex) {
            Logger.error(ex, "Error reading dependencies classpath file");
            System.exit(-1);
        }
        return dependencies.toString();
    }

    // Get the names of all unit tests in the project
    public void runAllUnitTests(String task, String mavenProfile, String[] buildToolArgs) {

        if (isMavenProject()) {
            runAllUnitTestsMaven(task, mavenProfile, new Properties(), buildToolArgs);
        } else {
            runAllUnitTestsGradle(new Properties(), buildToolArgs);
        }

    }

    public void runAllUnitTestsWithProperties(String task, String mavenProfile, Properties properties, String[] buildToolArgs) {

        if (isMavenProject()) {
            runAllUnitTestsMaven(task, mavenProfile, properties, buildToolArgs);
        } else {
            runAllUnitTestsGradle(properties, buildToolArgs);
        }

    }

    public Set<UnitTest> parseTestReports() {
        if (isMavenProject()) {
            return parseMavenTestReport();
        } else {
            return parseGradleTestReport();
        }
    }

    // Maven
    private void runAllUnitTestsMaven(String task, String profile, Properties properties, String[] mavenArgs) {

        InvocationRequest request = new DefaultInvocationRequest();

        File pomFile = new File(projectDir, "pom.xml");
        request.setPomFile(pomFile);

        if (!profile.isEmpty()) {
            request.setProfiles(Collections.singletonList(profile));
        }

        request.setGoals(Collections.singletonList(task));

        if (properties.getProperty("THREADS") != null) {
            request.setThreads(properties.getProperty("THREADS"));
            properties.remove("THREADS");
        }
//Logger.debug("ARGLINE:" + request.getProperties().getProperty("argLine"));   
        request.setProperties(properties);
        Logger.info("Running MVN project with properties: ");
        for (String property : properties.stringPropertyNames()) {
            Logger.info(property + "=" + properties.getProperty(property));
        }
        
        for (String mavenArg : mavenArgs) {
        	request.addArg(mavenArg);
        }

        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(mavenHome);

        InvocationResult result = null;

        // Extremely detailed debug output.
        if (DEBUG) {
            request.setErrorHandler(Logger::info);
        }

        try {
            result = invoker.execute(request);
        } catch (MavenInvocationException e) {
            Logger.error("Error invoking maven.");
            Logger.trace(e);
            System.exit(-1);
        }

        if (result.getExitCode() != 0) {
            Logger.error("Invocation of Maven gave non-zero return code:" + result.getExitCode());
            Logger.error(result.getExecutionException());
            System.exit(-1);
        }


    }

    // Gradle
    private void runAllUnitTestsGradle(Properties properties, String[] gradleArgs) {

        GradleConnector connector = GradleConnector.newConnector().forProjectDirectory(projectDir);

        if (gradleVersion != null) {
            connector.useGradleVersion(gradleVersion);
        }

        ProjectConnection connection = connector.connect();

        TestLauncher launcher = connection.newTestLauncher();

        launcher.addArguments(gradleArgs);        
        
        launcher = launcher.withJvmTestClasses("*");
        if (properties.containsKey("argLine")) {
            Logger.info("Running Gradle profile with argument line: " + properties.getProperty("argLine"));
            Map<String, String> variables = new HashMap<>();
            variables.put("JAVA_TOOL_OPTIONS", properties.getProperty("argLine"));
            launcher.setEnvironmentVariables(variables);
        }


        try {
            launcher.run();
        } catch (TestExecutionException exception) {
            if (exception.getCause().toString().contains("Test failed")) {
                Logger.warn("One or more tests failed running test suite");
                Logger.warn("Message: " + exception.getMessage());
                Logger.warn("Cause: " + exception.getCause());
                Logger.warn("These tests will be omitted by parser when reading tests reports.");
            } else {
                Logger.error("TestExecutionException from \"gradle test\" command when running test suite.");
                Logger.error("Message: " + exception.getMessage());
                Logger.error("Cause: " + exception.getCause());
            }
        } finally {
            connection.close();
        }

    }

    // Gradle - parse test report to get list of tests
    private Set<UnitTest> parseGradleTestReport() {

        Set<UnitTest> tests = new HashSet<>();

        for (File moduleDir : this.getModuleDirs()) {
            Set<UnitTest> moduleTests = parseGradleTestReportForModule(moduleDir);
            tests.addAll(moduleTests);
        }

        return tests;

    }

    protected Set<UnitTest> parseGradleTestReportForModule(File moduleDir) {

        //String moduleName = moduleDir.getAbsolutePath();
        Path relativeModulePath = projectDir.toPath().relativize(moduleDir.toPath());
        String moduleName = relativeModulePath.toString();

        Set<UnitTest> tests = new HashSet<>();

        File buildDir = new File(moduleDir, "build");
        File reportsDir = new File(buildDir, "reports");
        File testsDir = new File(reportsDir, "tests");
        File testDir = new File(testsDir, "test");
        File classesDir = new File(testDir, "classes");
        File[] classesFiles = classesDir.listFiles();

        if (classesFiles != null) {

            for (File classFile : classesFiles) {

                // Get classname from filename
                // Format: package.Class[$InternalClass].html

                String className = StringUtils.substringBefore(classFile.getName(), ".html");
                String innerClassName = "";

                if (className.contains("$")) {
                    innerClassName = StringUtils.substringAfter(className, "$");
                    className = StringUtils.substringBefore(className, "$");
                }

                Document doc = null;

                try {
                    doc = Jsoup.parse(classFile, "UTF-8", "");
                } catch (IOException e) {
                    Logger.error("Error opening test report file: " + classFile.getAbsolutePath());
                    System.exit(-1);
                }

                // Test method names are in the third table in the document
                // Tables have a header and then the test name is the first item in each row.
                Elements tables = doc.getElementsByTag("table");
                Element testTable = tables.get(2);
                Elements testRows = testTable.getElementsByTag("tr");
                boolean header = true;
                for (Element testRow : testRows) {
                    if (header) {
                        header = false;
                    } else {

                        Elements rowEntries = testRow.getElementsByTag("td");
                        Element methodNameEntry = rowEntries.first();
                        String testMethodName = methodNameEntry.text();
                        UnitTest test;
                        if (innerClassName.isEmpty()) {
                            test = new UnitTest(className, testMethodName, moduleName);
                        } else {
                            test = new UnitTest(className + '$' + innerClassName, testMethodName, moduleName);
                        }

                        boolean success = rowEntries.get(2).text().equals("passed");
                        if (!success) {
                            Logger.warn("Excluding ignored or failed test case: " + test);
                        } else {
                            tests.add(test);
                        }
                    }
                }

            }

        }

        return tests;

    }

    // Maven - parse test report to get list of tests
    private Set<UnitTest> parseMavenTestReport() {

        Set<UnitTest> tests = new HashSet<>();

        for (File moduleDir : this.getModuleDirs()) {
            Set<UnitTest> moduleTests = parseMavenTestReportForModule(moduleDir);
            tests.addAll(moduleTests);
        }

        return tests;

    }

    // Maven - parse test reports to get list of tests
    private Set<UnitTest> parseMavenTestReportForModule(File moduleDir) {

        Path relativeModulePath = projectDir.toPath().relativize(moduleDir.toPath());
        String moduleName = relativeModulePath.toString();

        Set<UnitTest> tests = new HashSet<>();

        File targetDir = new File(moduleDir, "target");
        File surefireReportsDir = new File(targetDir, "surefire-reports");

        FilenameFilter filter = (dir, name) -> name.endsWith(".xml");

        String[] reportFilenames = surefireReportsDir.list(filter);

        String innerClassName = "";  // Currently, we don't support inner classes in maven

        if (reportFilenames != null) {


            for (String filename : reportFilenames) {

                File reportFile = new File(surefireReportsDir, filename);

                Document doc = null;

                try {
                    doc = Jsoup.parse(reportFile, "UTF-8", "");
                } catch (IOException e) {
                    Logger.error("Error opening maven surefire test report file: " + reportFile.getAbsolutePath());
                    System.exit(-1);
                }

                Elements testCases = doc.getElementsByTag("testcase");
                for (Element testCase : testCases) {

                    String className = testCase.attr("classname");
                    String methodName = testCase.attr("name");

                    // Special case: sometimes parameter notes (e.g. seeds) added by Spring etc.
                    if (methodName.contains(" ")) {
                        methodName = methodName.split("\\s")[0];
                    }

                    Elements skipped = testCase.getElementsByTag("skipped");
                    Elements failure = testCase.getElementsByTag("failure");

                    UnitTest test = new UnitTest(className, methodName, moduleName);

                    if (!skipped.isEmpty()) {
                        Logger.warn("Test skipped so excluded by profiler: " + test);
                    } else if (!failure.isEmpty()) {
                        Logger.warn("Test case failed, excluded by profiler: " + test);
                    } else {
                        tests.add(test);
                    }
                }

            }

        }

        return tests;

    }

    public void runUnitTest(UnitTest test, String args, String task, String mavenProfile, String[] buildToolArgs) throws
            FailedToExecuteTestException {

        if (isMavenProject()) {
            runUnitTestMaven(test, args, task, mavenProfile, buildToolArgs);
        } else {
            runUnitTestGradle(test, args, buildToolArgs);
        }

    }

    public void runUnitTestGradle(UnitTest test, String args, String[] gradleArgs) {

        File connectionDir = projectDir;

        if (!test.getModuleName().isEmpty()) {
            connectionDir = new File(test.getModuleName());
        }

        GradleConnector connector = GradleConnector.newConnector().forProjectDirectory(connectionDir);

        if (gradleVersion != null) {
            connector.useGradleVersion(gradleVersion);
        }

        ProjectConnection connection = connector.connect();

        TestLauncher testLauncher = connection.newTestLauncher();

        testLauncher.addArguments(gradleArgs);
        
        Map<String, String> variables = new HashMap<>();
        variables.put("JAVA_TOOL_OPTIONS", args);

        // Workaround for inner classes, see https://github.com/gradle/gradle/issues/5763
        if (test.getInnerClassName().isEmpty()) {
            testLauncher = testLauncher.withJvmTestClasses(test.getTopClassName());
        } else {
            testLauncher = testLauncher.withJvmTestClasses(test.getTopClassName() + "*");
        }

        testLauncher = testLauncher.withJvmTestMethods(test.getTopClassName(), test.getMethodName());

        testLauncher.setEnvironmentVariables(variables);

        try {
            testLauncher.run();
        } catch (TestExecutionException | BuildException exception) {
            Logger.error(exception.getClass().getSimpleName() + " from gradle test launcher");
            Logger.error("Message: " + exception.getMessage());
            Logger.error("Cause: " + exception.getCause());
            exception.printStackTrace();
            System.exit(-1);
        }

        connection.close();

    }


    public void runUnitTestMaven(UnitTest test, String args, String taskName, String profile, String[] mavenArgs)
            throws FailedToExecuteTestException {

        // Maven requires a # separating class and method, with no parentheses
        String testClassName = test.getFullClassName();
        String methodName = test.getMethodName();
        String testName = testClassName + "#" + methodName;

        InvocationRequest request = new DefaultInvocationRequest();

        File pomFile = new File(projectDir, "pom.xml");
        request.setPomFile(pomFile);

        if (!profile.isEmpty()) {
            request.setProfiles(Collections.singletonList(profile));
        }
        
        for (String mavenArg : mavenArgs) {
        	request.addArg(mavenArg);
        }

        request.setGoals(Collections.singletonList(taskName));

        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(mavenHome);
                
        Properties properties = new Properties();
        request.setProperties(properties);
//Logger.debug("ARGLINE:" + request.getProperties().getProperty("argLine"));        
//        properties.setProperty("argLine", "@{argLine} " + args);
	properties.setProperty("argLine", args);
        properties.setProperty("test", testName);

        if (!test.getModuleName().isEmpty()) {
            List<String> moduleList = new LinkedList<>();
            moduleList.add(test.getModuleName());
            request.setProjects(moduleList);
        }

        InvocationResult result = null;

        try {
            result = invoker.execute(request);
        } catch (MavenInvocationException e) {
            Logger.error("Error invoking maven.");
            Logger.trace(e);
            System.exit(-1);
        }

        if (result.getExitCode() != 0) {
            Logger.error("Error running tests: " + test);
            throw new FailedToExecuteTestException(BuildType.MAVEN, "Non-zero return code:" + result.getExitCode(), test);
        }

    }


    public Set<String> allMainClasses() {

        Set<String> mainClasses = new HashSet<>();

        for (File classDir : this.mainClassDirs) {
            Set<String> classes = listOfClassesInDir(classDir);
            mainClasses.addAll(classes);
        }

        return mainClasses;

    }


    public Set<String> allTestClasses() {

        Set<String> testClasses = new HashSet<>();

        for (File classDir : this.testClassDirs) {
            Set<String> classes = listOfClassesInDir(classDir);
            testClasses.addAll(classes);
        }

        return testClasses;

    }

    private Set<String> listOfClassesInDir(File dir) {

        URLClassLoader urlClassLoader = null;
        Set<String> classNames = new HashSet<>();

        try {
            urlClassLoader = new URLClassLoader(new URL[]{dir.toURI().toURL()}, null);
        } catch (MalformedURLException e) {
            Logger.error("Could not read directory to find classes");
            Logger.error("Directory was; " + dir.getAbsolutePath());
            System.exit(-1);
        }

        ClassPath guavaClassPathUtility = null;
        try {
            guavaClassPathUtility = ClassPath.from(urlClassLoader);
        } catch (IOException e) {
            Logger.error("Error reading directory using Guava ClassPath Utility to find classes");
            Logger.error("Directory was: " + dir.getAbsolutePath());
            System.exit(-1);
        }

        ImmutableSet<ClassPath.ClassInfo> classes = guavaClassPathUtility.getAllClasses();

        for (ClassPath.ClassInfo classInfo : classes) {
            classNames.add(classInfo.getName());
        }

        return classNames;

    }

    public String getMethodSignature(String method, int lineNumber) {

        String methodName = method.substring(method.lastIndexOf('.') + 1).trim();
        String className = method.substring(0, method.lastIndexOf('.'));

        String signature = null;
        for (File srcDir : this.allSourceDirs()) {
            signature = getMethodSignature(srcDir, methodName, className, lineNumber);
            if (signature != null) {
                break;
            }
        }

        if (signature == null) {
            Logger.warn("Could not find source for method: " + method + " line: " + lineNumber);
        }

        return signature;

    }

    public String toString() {
        return this.projectName;
    }

    public enum BuildType {
        GRADLE, MAVEN

    }

}


