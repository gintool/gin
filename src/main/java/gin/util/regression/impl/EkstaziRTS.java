package gin.util.regression.impl;

import com.google.common.base.Functions;
import gin.test.UnitTest;
import gin.util.RTSProfiler;
import gin.util.regression.RTSStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.ekstazi.Config;
import org.ekstazi.Names;
import org.ekstazi.agent.EkstaziAgent;
import org.ekstazi.data.RegData;
import org.ekstazi.data.Storer;
import org.ekstazi.util.Types;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static org.ekstazi.data.DependencyAnalyzer.CLASS_EXT;

/**
 * This class works as an off-line integration of Ekstazi with Gin during
 * profiling. The dependency information is collected during the test phase of
 * the project, and then the dependency information is used to select test cases
 * for the target classes.
 *
 * @author Giovani
 * @see <a href="http://ekstazi.org/">Ekstazi</a>
 * @see RTSProfiler
 */
public class EkstaziRTS extends RTSStrategy {

    @Serial
    private static final long serialVersionUID = 2828162141766700048L;

    /**
     * The directory containing Ekstazi's dependency information.
     */
    protected File ekstaziDir;

    /**
     * The dependency files reader and writer.
     */
    protected Storer storer;

    /**
     * Constructs the object with the directory of the project as a base
     * directory for Ekstazi to store information, i.e., the {@code .ekstazi}
     * directory will be located inside the project's directory.
     * <p>
     * If this method is called, then parallel execution will most definitely
     * fail. Use {@link #EkstaziRTS()} for using Ekstazi with a temporary
     * directory and enabling parallelisation.
     *
     * @param projectRootDir the root directory of the project under improvement
     */
    public EkstaziRTS(File projectRootDir) {
        this.ekstaziDir = FileUtils.getFile(projectRootDir, Names.EKSTAZI_ROOT_DIR_NAME);
        this.storer = Config.createStorer();
    }

    /**
     * Constructs the object with the directory of the project as a base
     * directory for Ekstazi to store information, i.e., the {@code .ekstazi}
     * directory will be located inside the project's directory.
     * <p>
     * If this method is called, then parallel execution will most definitely
     * fail. Use {@link #EkstaziRTS()} for using Ekstazi with a temporary
     * directory and enabling parallelisation.
     *
     * @param projectRootDir the root directory of the project under improvement
     */
    public EkstaziRTS(String projectRootDir) {
        this(FileUtils.getFile(projectRootDir));
    }

    /**
     * Constructs the object and stores Ekstazi's files into the system's
     * {@code temp} directory. Gin will try to remove this directory when the
     * JVM exits.
     */
    public EkstaziRTS() {
        try {
            final File tempDir = Files.createTempDirectory("gin").toFile();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> FileUtils.deleteQuietly(tempDir)));

            this.ekstaziDir = FileUtils.getFile(tempDir, Names.EKSTAZI_ROOT_DIR_NAME);
            FileUtils.forceMkdir(this.ekstaziDir);
            this.storer = Config.createStorer();
        } catch (IOException ex) {
            Logger.error(ex, "Could not create temporary directory for Ekstazi.");
            System.exit(-1);
        }
    }

    @Override
    public String getArgumentLine() {
        try {
            // Finds the path to the jar containing Ekstazi's agent
            String agentJarPath = new File(Types.extractJarURL(EkstaziAgent.class).toURI().getSchemeSpecificPart()).getAbsolutePath();
            // Returns the argument line to inject the agent in the execution of
            // Maven or Gradle. This will force the re-execution of all tests to
            // always collect dependency information.
            return "-javaagent:" + agentJarPath + "=mode=junit,force.all=true,root.dir=" + this.ekstaziDir.getCanonicalPath();
        } catch (IOException | URISyntaxException ex) {
            Logger.debug(ex, "Could not find Ekstazi jar. Proceeding without RTS.");
            return null;
        }
    }

    @Override
    protected Map<String, Set<UnitTest>> getTargetClassesToTestCases(Collection<String> targetClasses, Collection<UnitTest> tests) {
        Logger.info("Reading Ekstazi dependency information for " + targetClasses.size() + " classes and " + tests.size() + " tests.");
        StopWatch stopWatch = StopWatch.createStarted();
        HashMap<String, Set<UnitTest>> externalFormResults = new HashMap<>();

        Map<String, Set<UnitTest>> testClassesToTestUnits = tests.stream()
                // Group tests by class
                .collect(Collectors.groupingBy(UnitTest::getFullClassName, Collectors.toSet()));

        // Iterate over Ekstazi's dependency files
        FileUtils.iterateFiles(this.ekstaziDir, FileFilterUtils.trueFileFilter(), null)
                .forEachRemaining(file -> {
                    // Get the name of the file/test class
                    String testClass = FilenameUtils.getBaseName(file.getName());
                    // Get the unit tests of this test class
                    Set<UnitTest> unitTestsOfThisTestClass = testClassesToTestUnits.get(testClass);
                    // Get the dependencies of the test class from the
                    // dependency files
                    Set<RegData> dependenciesOfThisTestClass = storer.load(this.ekstaziDir.getAbsolutePath(), testClass, CLASS_EXT);
                    // For each dependency found
                    dependenciesOfThisTestClass.forEach((regDatum) -> {
                        // Save the dependency external path and add all tests
                        // methods in the of the test class to it (if any)
                        Set<UnitTest> targetClassesToTest = externalFormResults.computeIfAbsent(regDatum.getURLExternalForm(), k -> new HashSet<>());
                        if (unitTestsOfThisTestClass != null) {
                            targetClassesToTest.addAll(unitTestsOfThisTestClass);
                        }
                    });
                });
        Logger.debug("Reading done in " + Duration.ofMillis(stopWatch.getTime()));
        Logger.info("Transforming Ekstazi dependency information to Gin.");
        // Ekstazi saves information about files, therefore we have to transform
        // class file path to the fully qualified class name. Since we do not
        // know what is package and what is directory name, we leave the full
        // path in here and compare to the end of it later
        Map<String, Set<UnitTest>> transformedMap = externalFormResults.entrySet().stream()
                .collect(Collectors.toMap(externalForm -> {
                    // Remove extension
                    String transformed = StringUtils.removeEnd(externalForm.getKey(), ".class");
                    // Transform path separator into '.'
                    transformed = StringUtils.replace(transformed, "/", ".");
                    return transformed;
                }, Map.Entry::getValue));
        // Maps the external form of the class names obtained by Ekstazi to the
        // regular fully qualified name as used by Gin
        Map<String, Set<UnitTest>> results = targetClasses.stream()
                .collect(Collectors.toMap(Functions.identity(), targetClass -> {
                    // This code finds the target class in the map of
                    // classes-tests, and then returns the set of tests for the
                    // given class
                    Optional<Map.Entry<String, Set<UnitTest>>> matchedEntry = transformedMap.entrySet().stream()
                            // The transformed path ends with the fully
                            // qualified class name (map key)
                            .filter(entry -> entry.getKey().endsWith(targetClass))
                            .findFirst();
                    if (matchedEntry.isPresent()) {
                        // Return the set of test cases as value for the key
                        return matchedEntry.get().getValue();
                    } else {
                        // The class was not found in the dependency
                        // information, then we use all test cases for that
                        // class. This should rarely be executed, unless no
                        // dependency information was stored using Ekstazy.
                        return new HashSet<>(tests);
                    }
                }));
        stopWatch.stop();
        Logger.debug("Transformation done in " + Duration.ofMillis(stopWatch.getTime()));
        return results;
    }

}
