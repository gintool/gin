package gin.util;

import gin.TestConfiguration;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import static org.junit.Assert.*;
import org.junit.Assume;

public class ProjectTest {

    private static final File GRADLE_MULTI_DIR = new File(TestConfiguration.GRADLE_SUBPROJECTS_DIR);
    private static final File GRADLE_SIMPLE = new File(TestConfiguration.GRADLE_SIMPLE_DIR);
    private static final File MAVEN_SIMPLE = new File(TestConfiguration.MAVEN_SIMPLE_DIR);

    Project simpleProject = new Project(GRADLE_SIMPLE, "simple");

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testDetectionGradleMultiProject() {

        Project project = new Project(GRADLE_MULTI_DIR, "myproject");

        // Basic fields
        assertEquals(project.getProjectDir().getAbsoluteFile(), GRADLE_MULTI_DIR.getAbsoluteFile());
        assertEquals(project.getProjectName(), "myproject");
        assertEquals(project.getBuildType(), Project.BuildType.GRADLE);

        // Module directories
        List<File> moduleDirs = project.getModuleDirs();
        Set<File> moduleDirSet = new HashSet(moduleDirs);
        Set<File> expectedModules = new HashSet<>();
        expectedModules.add(GRADLE_MULTI_DIR.getAbsoluteFile());
        expectedModules.add(new File (GRADLE_MULTI_DIR, "module1").getAbsoluteFile());
        expectedModules.add(new File (GRADLE_MULTI_DIR, "module2").getAbsoluteFile());
        assertEquals(expectedModules, moduleDirSet);

        // Source directories
        Set<File> expectedMainSourceDirs = new HashSet<>();
        expectedMainSourceDirs.add(new File(GRADLE_MULTI_DIR, "src"+ File.separator +"main"+ File.separator +"java").getAbsoluteFile());
        expectedMainSourceDirs.add(new File(GRADLE_MULTI_DIR, "src"+ File.separator +"main"+ File.separator +"resources").getAbsoluteFile());
        expectedMainSourceDirs.add(new File(GRADLE_MULTI_DIR, "module1"+ File.separator +"src"+ File.separator +"main"+ File.separator +"java").getAbsoluteFile());
        expectedMainSourceDirs.add(new File(GRADLE_MULTI_DIR, "module1"+ File.separator +"src"+ File.separator +"main"+ File.separator +"resources").getAbsoluteFile());
        expectedMainSourceDirs.add(new File(GRADLE_MULTI_DIR, "module2"+ File.separator +"src"+ File.separator +"main"+ File.separator +"java").getAbsoluteFile());
        expectedMainSourceDirs.add(new File(GRADLE_MULTI_DIR, "module2"+ File.separator +"src"+ File.separator +"main"+ File.separator +"resources").getAbsoluteFile());
        Set<File> actualMainSourceDirs = new HashSet<>(project.getMainSourceDirs());
        assertEquals(expectedMainSourceDirs, actualMainSourceDirs);

        // Test source directories
        Set<File> expectedTestSourceDirs = new HashSet<>();
        expectedTestSourceDirs.add(new File(GRADLE_MULTI_DIR, "src"+ File.separator +"test"+ File.separator +"java").getAbsoluteFile());
        expectedTestSourceDirs.add(new File(GRADLE_MULTI_DIR, "module1"+ File.separator +"src"+ File.separator +"test"+ File.separator +"java").getAbsoluteFile());
        expectedTestSourceDirs.add(new File(GRADLE_MULTI_DIR, "module2"+ File.separator +"src"+ File.separator +"test"+ File.separator +"java").getAbsoluteFile());
        expectedTestSourceDirs.add(new File(GRADLE_MULTI_DIR, "src"+ File.separator +"test"+ File.separator +"resources").getAbsoluteFile());
        expectedTestSourceDirs.add(new File(GRADLE_MULTI_DIR, "module1"+ File.separator +"src"+ File.separator +"test"+ File.separator +"resources").getAbsoluteFile());
        expectedTestSourceDirs.add(new File(GRADLE_MULTI_DIR, "module2"+ File.separator +"src"+ File.separator +"test"+ File.separator +"resources").getAbsoluteFile());
        Set<File> actualTestSourceDirs = new HashSet<>(project.getTestSourceDirs());
        assertEquals(expectedTestSourceDirs, actualTestSourceDirs);

        // Main class dirs
        Set<File> expectedMainClassDirs = new HashSet<>();
        expectedMainClassDirs.add(new File(GRADLE_MULTI_DIR, "module1"+ File.separator +"build"+ File.separator +"classes"+ File.separator +"java"+ File.separator +"main").getAbsoluteFile());
        expectedMainClassDirs.add(new File(GRADLE_MULTI_DIR, "module2"+ File.separator +"build"+ File.separator +"classes"+ File.separator +"java"+ File.separator +"main").getAbsoluteFile());
        expectedMainClassDirs.add(new File(GRADLE_MULTI_DIR, "build"+ File.separator +"classes"+ File.separator +"java"+ File.separator +"main").getAbsoluteFile());
        Set<File> actualMainClassDirs = new HashSet<>(project.getMainClassDirs());
        assertEquals(expectedMainClassDirs, actualMainClassDirs);

        // Test class dirs
        Set<File> expectedTestClassDirs = new HashSet<>();
        expectedTestClassDirs.add(new File(GRADLE_MULTI_DIR, "module1"+ File.separator +"build"+ File.separator +"classes"+ File.separator +"java"+ File.separator +"test").getAbsoluteFile());
        expectedTestClassDirs.add(new File(GRADLE_MULTI_DIR, "module2"+ File.separator +"build"+ File.separator +"classes"+ File.separator +"java"+ File.separator +"test").getAbsoluteFile());
        expectedTestClassDirs.add(new File(GRADLE_MULTI_DIR, "build"+ File.separator +"classes"+ File.separator +"java"+ File.separator +"test").getAbsoluteFile());
        Set<File> actualTestClassDirs = new HashSet<>(project.getTestClassDirs());
        assertEquals(expectedTestClassDirs, actualTestClassDirs);


    }

    @Test
    public void getMavenHome() {
        // Default
        assertEquals(new File(""+ File.separator +"usr"+ File.separator +"local"), simpleProject.getMavenHome());
    }

    @Test
    public void setMavenHome() {
        File exampleDir = new File(""+ File.separator +"home"+ File.separator +"exampleuser"+ File.separator +"apps"+ File.separator +"maven"+ File.separator);
        simpleProject.setMavenHome(exampleDir);
        assertEquals(exampleDir, simpleProject.getMavenHome());
    }

    /**
     * Tests the classpath retrieval in case Maven is configured in the System's
     * path. If it is the case, it should be able to create a temp file
     * containing the dependencies and retrieve the classpath. If Maven is not
     * configured, skip the test.
     */
    @Test
    public void classpath() {
        Project simpleProject = new Project(MAVEN_SIMPLE, "simple");
        String mavenHome = MavenUtils.findMavenHomePath();
        Assume.assumeTrue(FileUtils.getFile(mavenHome, "bin/mvn").exists()
                || FileUtils.getFile(mavenHome, "mvn").exists()
                || FileUtils.getFile(mavenHome, "bin/mvn.cmd").exists()
                || FileUtils.getFile(mavenHome, "mvn.cmd").exists());
        simpleProject.setMavenHome(FileUtils.getFile(mavenHome));
        String classpath = simpleProject.classpath();
        assertNotNull(classpath);
        String[] classPathItems = StringUtils.split(classpath, File.pathSeparator);
        assertEquals(5, classPathItems.length);
        assertTrue(StringUtils.endsWith(classPathItems[0], "target" + File.separator + "classes"));
        assertTrue(StringUtils.endsWith(classPathItems[1], "target" + File.separator + "test-classes"));
        assertTrue(StringUtils.endsWith(classPathItems[2], "junit-4.13.1.jar"));
        assertTrue(StringUtils.endsWith(classPathItems[3], "hamcrest-core-1.3.jar"));
        assertTrue(StringUtils.endsWith(classPathItems[4], "evosuite-standalone-runtime-1.0.6.jar"));
    }

    @Test
    public void getProjectName() {
    }

    @Test
    public void getGradleVersion() {
    }

    @Test
    public void setGradleVersion() {
    }

    @Test
    public void getBuildType() {
    }

    @Test
    public void allClassDirs() {
    }

    @Test
    public void allSourceDirs() {
    }

    @Test
    public void getModuleDirs() {
    }

    @Test
    public void findSourceFile() {
    }

    @Test
    public void runAllUnitTests() {
    }

    @Test
    public void parseTestReports() {
    }

    @Test
    public void runUnitTest() {
    }

    @Test
    public void runUnitTestGradle() {
    }

    @Test
    public void runUnitTestMaven() {
    }

    @Test
    public void listOfMainClasses() {
    }

    @Test
    public void getMethodSignature() {
    }
}
