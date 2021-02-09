package gin.util.regression.impl;

import com.google.common.base.Functions;
import com.google.common.collect.Sets;
import edu.illinois.starts.constants.StartsConstants;
import gin.test.UnitTest;
import java.io.File;
import java.util.Collection;
import org.apache.commons.io.FileUtils;
import gin.util.regression.RTSStrategy;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.pmw.tinylog.Logger;

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
 *
 * @see <a href="https://github.com/TestingResearchIllinois/starts">STARTS Home
 * Page</a>
 * see RTSProfiler
 */
public class STARTSRTS extends RTSStrategy {

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

    @Override
    public String getTestGoal() {
        return "edu.illinois:starts-maven-plugin:1.3:clean edu.illinois:starts-maven-plugin:1.3:starts";
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
    protected Map<String, Set<UnitTest>> getTargetClassesToTestCases(Collection<String> targetClasses, Collection<UnitTest> tests) {
        Logger.info("Reading STARTS dependency information for " + targetClasses.size() + " classes and " + tests.size() + " tests.");
        StopWatch stopWatch = StopWatch.createStarted();
        final File startsFile = FileUtils.getFile(this.startsDir, "deps.zlc");
        try {
            // Read the lines of the file
            final Map<String, Set<String>> classesToTestClasses = FileUtils.readLines(startsFile, Charset.defaultCharset()).stream()
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
            Logger.debug("Reading done in " + Duration.ofMillis(stopWatch.getTime()));
            Logger.info("Transforming STARTS dependency information to Gin.");
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
                                    return new HashSet<UnitTest>(tests);
                                }
                            }));
            stopWatch.stop();
            Logger.debug("Transformation done in " + Duration.ofMillis(stopWatch.getTime()));
            return result;
        } catch (IOException ex) {
            Logger.error(ex, "Could not read the STARTS file " + startsFile.getAbsolutePath() + ". "
                    + "It may not exist or be corrupted. "
                    + "You can try to run STARTS outside Gin and then use the option -x with the profiler to just interpret the results.");
            System.exit(-1);
        }
        return new HashMap<>();
    }

}
