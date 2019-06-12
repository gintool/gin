package gin;

import java.io.File;

public class TestConfiguration {

    public static final String EXAMPLE_DIR_NAME = "./examples/unittests/";
    public static final String GIN_JAR = "./build/gin.jar";
    public static final String GRADLE_SIMPLE_DIR = "examples/unittests/gradle/gradle-simple/";
    public static final String GRADLE_SUBPROJECTS_DIR = "examples/unittests/gradle/gradle-subprojects/";


    public static final File EXAMPLE_DIR = new File(EXAMPLE_DIR_NAME);
    public static final File TMP_DIR = new File("./examples/tmp/");

}

