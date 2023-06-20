package gin;

import java.io.File;

public class TestConfiguration {

    public static final String EXAMPLE_DIR_NAME = "examples" + File.separator + "unittests" + File.separator;

    // Gradle tests
    public static final String GRADLE_SIMPLE_DIR = "examples" + File.separator + "unittests" + File.separator + "gradle" + File.separator + "gradle-simple" + File.separator;
    public static final String GRADLE_SIMPLE_JUNIT5_DIR = "examples" + File.separator + "unittests" + File.separator + "gradle" + File.separator + "gradle-simple-junit5" + File.separator;
    public static final String GRADLE_SUBPROJECTS_DIR = "examples" + File.separator + "unittests" + File.separator + "gradle" + File.separator + "gradle-subprojects" + File.separator;

    // Maven tests
    public static final String MAVEN_SIMPLE_DIR = "examples" + File.separator + "maven-simple" + File.separator;
    public static final String MAVEN_SIMPLE_MEMORY_DIR = "examples" + File.separator + "unittests" + File.separator + "maven" + File.separator + "maven-simple-memory" + File.separator;
    public static final String MAVEN_SIMPLE_JUNIT5_DIR = "examples" + File.separator + "unittests" + File.separator + "maven" + File.separator + "maven-simple-junit5" + File.separator;

    public static final File EXAMPLE_DIR = new File(EXAMPLE_DIR_NAME);

}

