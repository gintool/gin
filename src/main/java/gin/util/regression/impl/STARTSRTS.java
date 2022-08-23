package gin.util.regression.impl;

import com.google.common.base.Functions;
import com.google.common.collect.Sets;
import edu.illinois.starts.constants.StartsConstants;
import gin.test.UnitTest;
import gin.util.regression.RTSStrategy;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class representing the STARTS RTS technique. This is a dynamic analysis tool
 * that runs with maven.
 * <p>
 * Differently from Ekstazi, it is not possible to run this in parallel, as it
 * won't allow changing the directory of the dependency files.
 * <p>
 * STARTS is executed through a Maven plugin only. Therefore, to execute it for
 * the first time, the computer must have an internet connection so Maven can
 * download dependencies. The subsequent runs can be done offline due to the
 * local caching of Maven. In order to solve this issue, the Maven plugin should
 * be converted into a Java agent with a {@code premain} method.
 * <p>
 * <b>WARNING: STARTS does not work on Windows. Use {@link EkstaziRTS}
 * instead.</b>
 *
 * @author Giovani
 * @see <a href="https://github.com/TestingResearchIllinois/starts">STARTS Home
 * Page</a>
 * see RTSProfiler
 */
public class STARTSRTS extends RTSStrategy {

    private static final long serialVersionUID = -3844384837214338743L;

    /**
     * The directory containing STARTS's dependency information.
     */
    protected File startsDir;

    /**
     * Constructs the object.
     *
     * @param projectRootDir the root directory of the project under improvement
     */
    public STARTSRTS(File projectRootDir) {
        this.startsDir = FileUtils.getFile(projectRootDir, StartsConstants.STARTS_DIRECTORY_PATH);
    }

    /**
     * Constructs the object.
     *
     * @param projectRootDir the root directory of the project under improvement
     */
    public STARTSRTS(String projectRootDir) {
        this(FileUtils.getFile(projectRootDir));
    }

    @Override
    public String getTestGoal() {
        return "edu.illinois:starts-maven-plugin:1.3:clean edu.illinois:starts-maven-plugin:1.3:starts";
    }

    @Override
    protected Map<String, Set<UnitTest>> getTargetClassesToTestCases(Collection<String> targetClasses, Collection<UnitTest> tests) {
        Logger.info("Reading STARTS dependency information for " + targetClasses.size() + " classes and " + tests.size() + " tests.");
        StopWatch stopWatch = StopWatch.createStarted();
        File startsFile = FileUtils.getFile(this.startsDir, "deps.zlc");
        try {
            final Map<String, Set<String>> classesToTestClasses = new HashMap<>();
            // If the file exists, it means that the results are contained in a single file, as default
            if (startsFile.exists()) {
                System.out.println("Exists!");
                // Read the lines of the file
                classesToTestClasses.putAll(extractDependencyInformation(startsFile));
            }
            // We need to look into subdirectories to check for modules
            Iterator<File> fileIterator = FileUtils.iterateFiles(this.startsDir.getParentFile(), FileFilterUtils.nameFileFilter("deps.zlc"), FileFilterUtils.directoryFileFilter());
            while (fileIterator.hasNext()) {
                File subStartsFile = fileIterator.next();
                Map<String, HashSet<String>> dependencyInformation = extractDependencyInformation(subStartsFile);
                dependencyInformation.forEach((key, value) -> classesToTestClasses.merge(key, value, SetUtils::union));
            }
            Logger.debug("Reading done in " + Duration.ofMillis(stopWatch.getTime()));
            Logger.info("Transforming STARTS dependency information to Gin.");
            Logger.info("Found " + classesToTestClasses.size() + " classes.");
            if (!classesToTestClasses.isEmpty()) {
                // Convert the file dependency information to classes and objects representation
                // For each target class
                Map<String, Set<UnitTest>> result = targetClasses.stream()
                        .collect(Collectors.toMap(Functions.identity(),
                                // Finds the UnitTests of each TestClass
                                targetClass -> {
                                    // This code finds the target class in the map
                                    // of classes-tests, and then returns the set of
                                    // test classes for the given class
                                    Optional<Map.Entry<String, Set<String>>> matchedEntry = classesToTestClasses.entrySet().stream()
                                            // The transformed path ends with the
                                            // fully qualified class name (map key)
                                            .filter(entry -> entry.getKey().endsWith(targetClass))
                                            .findFirst();
                                    if (matchedEntry.isPresent()) {
                                        // Return the set of test cases as value for
                                        // the key
                                        Set<String> testClassNames = matchedEntry.get().getValue();
                                        return tests.stream()
                                                .filter(test -> testClassNames.contains(test.getFullClassName()))
                                                .collect(Collectors.toSet());
                                    } else {
                                        // The class was not found in the dependency
                                        // information, then we use all test cases
                                        // for that class. This should rarely be
                                        // executed, unless no dependency
                                        // information was stored using STARTS.
                                        return new HashSet<>(tests);
                                    }
                                }));
                stopWatch.stop();
                Logger.debug("Transformation done in " + Duration.ofMillis(stopWatch.getTime()));
                return result;
            } else {
                throw new IOException("No dependency information.");
            }
        } catch (IOException ex) {
            Logger.error(ex, "Could not read the STARTS file " + startsFile.getAbsolutePath() + " or any module starts file. "
                    + "They may not exist or be corrupted. "
                    + "You can try to run STARTS outside Gin and then use the option -x with the profiler to just interpret the results.");
        }
        return new HashMap<>();
    }

    private Map<String, HashSet<String>> extractDependencyInformation(File startsFile) throws IOException {
        return FileUtils.readLines(startsFile, Charset.defaultCharset()).stream()
                // Retain only local files
                .filter(line -> line.trim().startsWith("file:"))
                .map((line) -> {
                    // Remove extension
                    String transformed = StringUtils.replace(line, ".class ", " ");
                    // Transform path separator into '.'
                    transformed = StringUtils.replace(transformed, "/", ".");
                    transformed = StringUtils.replace(transformed, "\\", ".");
                    return StringUtils.split(transformed, " ");
                })
                // Transform to a map of TargetClass -> Set<TestClasses>
                .collect(Collectors.toMap(tokens -> tokens[tokens.length - 3],
                        tokens -> Sets.newHashSet(tokens[tokens.length - 1].split(","))));
    }

}
