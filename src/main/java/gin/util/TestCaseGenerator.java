package gin.util;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.pmw.tinylog.Logger;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.LogOutputStream;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

/**
 * EvoSuite test generator.
 * <p>
 * Generates unit tests for Maven and Gradle projects.
 * Runs generated tests for Maven projects.
 */
public class TestCaseGenerator implements Serializable {

    @Serial
    private static final long serialVersionUID = -2311248061581562068L;
    // below settings to get deterministic results according to: https://github.com/EvoSuite/evosuite/issues/48
    private static final String p_functional_mocking = "0";
    private static final String p_reflection_on_private = "0";
    private static final String stopping_condition = "MaxStatements";
    // to allow running with gin utilities
    private static final boolean use_separate_classloader = false;
    private static final boolean filter_assertions = true;
    private final Project project;
    private final String projectCP; // classpath to be supplied to EvoSuite
    @Argument(alias = "d", description = "Project directory: required", required = true)
    protected File projectDir;
    @Argument(alias = "p", description = "Project name: required", required = true)
    protected String projectName;
    @Argument(alias = "h", description = "Path to maven bin directory e.g. /usr/local/")
    protected File mavenHome = new File("/usr/local/");  // default on OS X
    @Argument(alias = "v", description = "Set Gradle version")
    protected String gradleVersion;
    @Argument(alias = "evosuiteCP", description = "Path to evosuite jar, set to testgeneration/evosuite-1.0.6.jar by default")
    protected File evosuiteCP = new File("testgeneration/evosuite-1.0.6.jar");
    @Argument(alias = "classNames", description = "List of classes for which to generate tests")
    protected String[] classNames;
    @Argument(alias = "projectTarget", description = "Directory for project class files or jar file of the project; ignored if classNames parameter is set")
    protected File projectTarget;
    @Argument(alias = "outputDir", description = "Output directory for generated tests; for maven projects the pom file will be updated automatically")
    protected File outputDir;
    @Argument(alias = "removeTests", description = "Remove existing tests from outputDir, set to projectDir/src/java/test if outputDir not specified")
    protected boolean removeTests = false;

    // EvoSuite parameters
    @Argument(alias = "genearateTests", description = "Generate tests for classNames or projectTarget")
    protected boolean generateTests = false;
    @Argument(alias = "test", description = "Run all tests in outputDir, set to projectDir/src/test/java if outputDir not specified")
    protected boolean test = false;
    @Argument(alias = "classNumber", description = "Number of classes to generate EvoSuite tests for, used for debugging purposes")
    protected Integer classNumber = 0;
    @Argument(alias = "seed", description = "Random seed for test case generation, set to 88 by default")
    protected String seed = "88"; // random seed, need this to get deterministic results
    @Argument(alias = "maxStatements", description = "Search budget for test case generation, set to 50000 statements by default")
    protected String search_budget = "50000"; // search budget for MaxStatements stopping condition
    private boolean checkIfRewrite = false;
    // to supress some evosuite warnings
    //private static final String sandbox_mode = "OFF";
    // time saving
    //private static final boolean minimize = false;
    //private static final int assertion_minimization_fallback_time = 0;

    public TestCaseGenerator(String[] args) {

        Args.parseOrExit(this, args);

        // Set up project and class names for which to generate tests

        this.project = new Project(projectDir, projectName);

        if (this.gradleVersion != null) {
            project.setGradleVersion(this.gradleVersion);
        }
        if (this.mavenHome != null) {
            project.setMavenHome(this.mavenHome);
        }
        project.setUp();
        this.projectCP = project.classpath();

        // Set up output directory for tests

        if (outputDir == null) {
            setOutputDir();
        } else {
            checkIfRewrite = true;
        }

        if (removeTests) {

            Logger.info("Tests will be removed from " + outputDir.getAbsolutePath());
            try {
                cleanOutputDir(outputDir);
            } catch (IOException e) {
                Logger.error("IO Exception encountered while cleaning the test output directory: " + e);
                System.exit(-1);
            }
            Logger.info("Successfully removed tests from " + outputDir.getAbsolutePath());

        }

        if (generateTests) {

            if (!evosuiteCP.isFile()) {

                Logger.info("Path to evosuite jar is required for test case generation, can be found in gin/testgeneration");
                System.exit(0);

            }

            if ((classNames == null) && (projectTarget == null)) {
                Logger.info("classNames or projectTarget (i.e. jar or class directory) parameter needs to be set. Exiting.");
                System.exit(0);
            }

            runAllUnitTests();

            if (classNames == null && projectTarget != null) {
                classNames = gatherAllClasses();
            }

            // for debugging purposes
            if (classNumber > 0 && classNames != null) {
                classNames = Arrays.copyOfRange(classNames, 0, classNumber);
            }

            if (classNames != null) {
                generate(classNames);
            } else {
                Logger.error("Tests not generated as no class names were found.");
                System.exit(-1);
            }
        }

        if (test) {

            Logger.info("Running all tests from " + outputDir.getAbsolutePath());
            runAllTests();
        }

    }

    public static void main(String[] args) {

        new TestCaseGenerator(args);

    }

    private void setOutputDir() {

        File srcDir = new File(projectDir, "src");
        File testDir = new File(srcDir, "test");
        File javaDir = new File(testDir, "java");

        if (javaDir.isDirectory()) {

            this.outputDir = javaDir;

        } else {

            Logger.error(javaDir + " is not a directory. Please specify the output directory using the outputDir parameter. Please note tests are run using mvn/gradle test, so manual copying of tests to the appropriate directories might be needed before tests can be run with Gin.");
            System.exit(-1);

        }

    }

    protected void cleanOutputDir(File folder) throws IOException {

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    file.delete();
                } else if (file.isDirectory()) {
                    cleanOutputDir(file);
                }
            }
        }

    }


    private String[] gatherAllClasses() {

        List<String> allClasses = new ArrayList<>();

        String[] cmd = {"java", "-jar", evosuiteCP.getAbsolutePath()
                , "-listClasses"
                , "-target"
                , projectTarget.getAbsolutePath()
        };

        try {

            new ProcessExecutor().command(cmd)
                    .redirectOutput(new LogOutputStream() {
                        @Override
                        protected void processLine(String line) {
                            allClasses.add(line);
                        }
                    })
                    .execute();

        } catch (IOException e) {
            Logger.error("IO Exception encountered when listing project classes using EvoSuite: " + e);
        } catch (InterruptedException e) {
            Logger.error("Interrupted Exception encountered when listing project classes using EvoSuite: " + e);
        } catch (TimeoutException e) {
            Logger.error("Timeout Exception encountered when listing project classes using EvoSuite: " + e);
        }

        allClasses.removeIf(Objects::isNull);

        return allClasses.toArray(new String[0]);

    }

    protected void generate(String[] classNames) {

        String[] cmd = {"java", "-jar", evosuiteCP.getAbsolutePath()
                , "-projectCP", projectCP
                , "-class", "ClassName"
                , "-seed", seed
                , "-Dtest_dir=" + outputDir.getAbsolutePath()
                //, "-Dreport_dir=" + projectDir.getAbsolutePath() + File.separator + report_dir
                , "-Dp_functional_mocking=" + p_functional_mocking
                , "-Dp_reflection_on_private=" + p_reflection_on_private
                , "-Dstopping_condition=" + stopping_condition
                , "-Dsearch_budget=" + search_budget
                , "-Duse_separate_classloader=" + use_separate_classloader
                , "-Dfilter_assertions=" + filter_assertions
                //, "-Dsandbox_mode=" + sandbox_mode
                //, "-Dminimize=" + minimize
                //, "-Dassertion_minimization_fallback_time=" + assertion_minimization_fallback_time
        };

        for (String className : classNames) {

            //if new File(projectCPclassName

            cmd[6] = className;

            try {

                new ProcessExecutor().command(cmd)
                        .redirectOutput(Slf4jStream.ofCaller().asInfo())
                        .redirectError(Slf4jStream.ofCaller().asInfo())
                        .destroyOnExit() // Destroy the process when VM exits
                        .execute();

            } catch (IOException e) {
                Logger.error("IO Exception encountered when generating EvoSuite tests: " + e);
            } catch (InterruptedException e) {
                Logger.error("Interrupted Exception encountered when generating EvoSuite tests: " + e);
            } catch (TimeoutException e) {
                Logger.error("Timeout Exception encountered when generating EvoSuite tests: " + e);
            }

        }

    }

    private void runAllTests() {

        if (project.getBuildType() == Project.BuildType.MAVEN) {
            updatePom();
        } else if (project.getBuildType() == Project.BuildType.GRADLE) {
            Logger.warn("Make sure to add evosuite-standalone-runtime dependency to the project in order to be able to run the generated EvoSuite tests. Afterwards you can run these from within the project with 'gradle test'.");
            System.exit(0);
            //addInitScript (todo: add init.gradle to projectDir, invoke init script from Project)
        } else {
            Logger.warn("Project type not supported.");
            System.exit(0);
        }

        runAllUnitTests();

    }


    // sets test source directory and adds/removes evosuite-standalone-runtime dependency
    // returns original testSourceDirectory
    private void updatePom() {

        Logger.info("Updating the pom file..");

        Model model = null;
        MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        try {
            File pomfile = new File(projectDir.toString() + File.separator + "pom.xml");
            FileInputStream reader = new FileInputStream(pomfile);
            model = mavenreader.read(reader);
            model.setPomFile(pomfile);
            reader.close();
        } catch (FileNotFoundException e) {
            Logger.error("File not found error while reading pom.");
            Logger.trace(e);
        } catch (IOException e) {
            Logger.error("IO error while reading pom.");
            Logger.trace(e);
        } catch (XmlPullParserException e) {
            Logger.error("XmlPullParserException while reading pom.");
            Logger.trace(e);
        }

        if (model != null) {
            Build mbuild = model.getBuild();
            if (checkIfRewrite) {
                mbuild.setTestSourceDirectory(outputDir.getPath());
                model.setBuild(mbuild);
            }

            Dependency dependency = new Dependency();
            dependency.setGroupId("org.evosuite");
            dependency.setArtifactId("evosuite-standalone-runtime");
            dependency.setVersion("1.0.6");

            boolean check = false;
            for (Dependency dep : model.getDependencies()) {
                if (dep.getGroupId().equals(dependency.getGroupId()) && dep.getArtifactId().equals(dependency.getArtifactId())) {
                    check = true;
                    break;
                }
            }

            if (!check) {
                model.addDependency(dependency);
            }

            MavenXpp3Writer mavenwriter = new MavenXpp3Writer();
            try {
                FileOutputStream writer = new FileOutputStream(new File(projectDir.toString(), File.separator + "pom.xml"));
                mavenwriter.write(writer, model);
                writer.close();
            } catch (FileNotFoundException e) {
                Logger.error("File not found error while writing pom.");
                Logger.trace(e);
            } catch (IOException e) {
                Logger.error("IO error while writing pom.");
                Logger.trace(e);
            }
        }

    }


    // needs evosuite-standalone-runtime dependency declared in the project
    protected void runAllUnitTests() {

        if (project != null) {
            project.runAllUnitTests("clean test", "", new String[0]);
        } else {
            Logger.error("Cannot run tests on a null project.");
        }

    }

}
