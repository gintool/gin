package gin.util.regression.impl;

import gin.TestConfiguration;
import gin.test.UnitTest;
import gin.util.HotMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.ekstazi.Names;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Giovani
 */
public class EkstaziRTSTest {

    private static final String PROJECT_DIR = TestConfiguration.EXAMPLE_DIR_NAME + File.separator + "ekstazi";
    private EkstaziRTS ekstazi;

    @Before
    public void setUp() {
        this.ekstazi = new EkstaziRTS(PROJECT_DIR);
    }

    @Test
    public void testConstructor() throws IOException {
        String canonicalPath = this.ekstazi.ekstaziDir.getCanonicalPath();
        assertEquals(FileUtils.getFile(PROJECT_DIR + File.separator + Names.EKSTAZI_ROOT_DIR_NAME).getCanonicalPath(), canonicalPath);
        assertTrue(this.ekstazi.ekstaziDir.exists());
    }

    @Test
    public void testConstructor2() throws IOException {
        this.ekstazi = new EkstaziRTS();
        String tempPath = this.ekstazi.ekstaziDir.getParentFile().getParentFile().getCanonicalPath();
        assertEquals(FileUtils.getFile(System.getProperty("java.io.tmpdir")).getCanonicalPath(), tempPath);
        assertTrue(this.ekstazi.ekstaziDir.exists());
    }

    @Test
    public void testGetGoal() {
        assertEquals("test", this.ekstazi.getTestGoal());
    }

    @Test
    public void testGetArgumentLine() throws IOException {
        String argLine = this.ekstazi.getArgumentLine();
        String canonicalPath = this.ekstazi.ekstaziDir.getCanonicalPath();
        assertTrue(StringUtils.startsWith(argLine, "-javaagent:"));
        assertTrue(StringUtils.endsWith(argLine, "=mode=junit,force.all=true,root.dir=" + canonicalPath));
        assertTrue(FileUtils.getFile(StringUtils.substringBetween(argLine, "-javaagent:", "=mode=junit,force.all=true,root.dir=" + canonicalPath)).exists());
    }

    @Test
    public void testGetArgumentLineWithTempDir() throws IOException {
        this.ekstazi = new EkstaziRTS();
        String argLine = this.ekstazi.getArgumentLine();
        String canonicalPath = this.ekstazi.ekstaziDir.getCanonicalPath();
        assertTrue(StringUtils.startsWith(argLine, "-javaagent:"));
        assertTrue(StringUtils.endsWith(argLine, "=mode=junit,force.all=true,root.dir=" + canonicalPath));
        assertTrue(FileUtils.getFile(StringUtils.substringBetween(argLine, "-javaagent:", "=mode=junit,force.all=true,root.dir=" + canonicalPath)).exists());
    }

    @Test
    public void testLinkTestsToHotMethods() {
        List<HotMethod> methods = new ArrayList<>();
        HotMethod methodA = new HotMethod("EkstaziA", "methodA", 0, new HashSet<>());
        HotMethod methodB = new HotMethod("EkstaziA", "methodB", 0, new HashSet<>());
        HotMethod sum = new HotMethod("EkstaziB", "sum", 0, new HashSet<>());
        HotMethod methodFromEkstaziC = new HotMethod("EkstaziC", "methodFromEkstaziC", 0, new HashSet<>());
        HotMethod methodFromEkstaziD = new HotMethod("EkstaziD", "methodFromEkstaziD", 0, new HashSet<>());
        HotMethod methodFromEkstaziE = new HotMethod("EkstaziE", "methodFromEkstaziE", 0, new HashSet<>());

        methods.add(methodA);
        methods.add(methodB);
        methods.add(sum);
        methods.add(methodFromEkstaziC);
        methods.add(methodFromEkstaziD);
        methods.add(methodFromEkstaziE);

        List<UnitTest> tests = new ArrayList<>();
        UnitTest ekstaziATest1 = new UnitTest("EkstaziATest", "testMethodA1");
        tests.add(ekstaziATest1);
        UnitTest ekstaziATest2 = new UnitTest("EkstaziATest", "testMethodB1");
        tests.add(ekstaziATest2);
        UnitTest ekstaziBTest1 = new UnitTest("EkstaziBTest", "testSum1");
        tests.add(ekstaziBTest1);
        UnitTest ekstaziBTest2 = new UnitTest("EkstaziBTest", "testSum2");
        tests.add(ekstaziBTest2);
        UnitTest ekstaziBTest3 = new UnitTest("EkstaziBTest", "testSum3");
        tests.add(ekstaziBTest3);
        UnitTest ekstaziCTest = new UnitTest("EkstaziCTest", "testC1");
        tests.add(ekstaziCTest);
        UnitTest ekstaziDTest = new UnitTest("EkstaziDTest", "testD1");
        tests.add(ekstaziDTest);
        UnitTest ekstaziETest = new UnitTest("EkstaziETest", "This unit test does not exist");
        tests.add(ekstaziETest);

        this.ekstazi.linkTestsToMethods(methods, tests);

        Set<UnitTest> subsetTests = methodA.getTests();
        assertNotNull(subsetTests);
        assertTrue(subsetTests.contains(ekstaziATest1));
        assertTrue(subsetTests.contains(ekstaziATest2));
        assertFalse(subsetTests.contains(ekstaziBTest1));
        assertFalse(subsetTests.contains(ekstaziBTest2));
        assertFalse(subsetTests.contains(ekstaziBTest3));
        assertFalse(subsetTests.contains(ekstaziCTest));
        assertFalse(subsetTests.contains(ekstaziDTest));
        assertFalse(subsetTests.contains(ekstaziETest));

        subsetTests = methodB.getTests();
        assertNotNull(subsetTests);
        assertTrue(subsetTests.contains(ekstaziATest1));
        assertTrue(subsetTests.contains(ekstaziATest2));
        assertFalse(subsetTests.contains(ekstaziBTest1));
        assertFalse(subsetTests.contains(ekstaziBTest2));
        assertFalse(subsetTests.contains(ekstaziBTest3));
        assertFalse(subsetTests.contains(ekstaziCTest));
        assertFalse(subsetTests.contains(ekstaziDTest));
        assertFalse(subsetTests.contains(ekstaziETest));

        subsetTests = sum.getTests();
        assertNotNull(subsetTests);
        assertTrue(subsetTests.contains(ekstaziATest1));
        assertTrue(subsetTests.contains(ekstaziATest2));
        assertTrue(subsetTests.contains(ekstaziBTest1));
        assertTrue(subsetTests.contains(ekstaziBTest2));
        assertTrue(subsetTests.contains(ekstaziBTest3));
        assertFalse(subsetTests.contains(ekstaziCTest));
        assertFalse(subsetTests.contains(ekstaziDTest));
        assertFalse(subsetTests.contains(ekstaziETest));

        subsetTests = methodFromEkstaziC.getTests();
        assertNotNull(subsetTests);
        assertFalse(subsetTests.contains(ekstaziATest1));
        assertFalse(subsetTests.contains(ekstaziATest2));
        assertFalse(subsetTests.contains(ekstaziBTest1));
        assertFalse(subsetTests.contains(ekstaziBTest2));
        assertFalse(subsetTests.contains(ekstaziBTest3));
        assertTrue(subsetTests.contains(ekstaziCTest));
        assertFalse(subsetTests.contains(ekstaziDTest));
        assertFalse(subsetTests.contains(ekstaziETest));

        subsetTests = methodFromEkstaziD.getTests();
        assertNotNull(subsetTests);
        assertTrue(subsetTests.contains(ekstaziATest1));
        assertTrue(subsetTests.contains(ekstaziATest2));
        assertTrue(subsetTests.contains(ekstaziBTest1));
        assertTrue(subsetTests.contains(ekstaziBTest2));
        assertTrue(subsetTests.contains(ekstaziBTest3));
        assertTrue(subsetTests.contains(ekstaziCTest));
        assertTrue(subsetTests.contains(ekstaziDTest));
        assertTrue(subsetTests.contains(ekstaziETest));

        subsetTests = methodFromEkstaziE.getTests();
        assertNotNull(subsetTests);
        assertTrue(subsetTests.contains(ekstaziATest1));
        assertTrue(subsetTests.contains(ekstaziATest2));
        assertTrue(subsetTests.contains(ekstaziBTest1));
        assertTrue(subsetTests.contains(ekstaziBTest2));
        assertTrue(subsetTests.contains(ekstaziBTest3));
        assertTrue(subsetTests.contains(ekstaziCTest));
        assertTrue(subsetTests.contains(ekstaziDTest));
        assertTrue(subsetTests.contains(ekstaziETest));
    }

    @Test
    public void testGetTargetClassesToTestCases2() {
        List<String> classes = new ArrayList<>();
        classes.add("EkstaziA");

        List<UnitTest> tests = new ArrayList<>();

        UnitTest ekstaziATest = new UnitTest("EkstaziATest", "does not matter");
        tests.add(ekstaziATest);
        UnitTest ekstaziBTest = new UnitTest("EkstaziBTest", "does not matter");
        tests.add(ekstaziBTest);
        UnitTest ekstaziCTest = new UnitTest("EkstaziCTest", "does not matter");
        tests.add(ekstaziCTest);
        UnitTest ekstaziDTest = new UnitTest("EkstaziDTest", "does not matter");
        tests.add(ekstaziDTest);
        UnitTest ekstaziETest = new UnitTest("EkstaziETest", "This class does not exist");
        tests.add(ekstaziETest);

        Map<String, Set<UnitTest>> targetClassesToTestCases = this.ekstazi.getTargetClassesToTestCases(classes, tests);
        assertNotNull(targetClassesToTestCases);
        assertEquals(1, targetClassesToTestCases.size());

        Set<UnitTest> subsetTests = targetClassesToTestCases.get("EkstaziA");
        assertNotNull(subsetTests);
        assertTrue(subsetTests.contains(ekstaziATest));
        assertFalse(subsetTests.contains(ekstaziBTest));
        assertFalse(subsetTests.contains(ekstaziCTest));
        assertFalse(subsetTests.contains(ekstaziDTest));
        assertFalse(subsetTests.contains(ekstaziETest));
    }

    @Test
    public void testGetTargetClassesToTestCases3() {
        List<String> classes = new ArrayList<>();
        classes.add("EkstaziA");

        List<UnitTest> tests = new ArrayList<>();

        UnitTest ekstaziETest = new UnitTest("EkstaziETest", "This class does not exist");
        tests.add(ekstaziETest);

        Map<String, Set<UnitTest>> targetClassesToTestCases = this.ekstazi.getTargetClassesToTestCases(classes, tests);
        assertNotNull(targetClassesToTestCases);
        assertEquals(1, targetClassesToTestCases.size());

        Set<UnitTest> subsetTests = targetClassesToTestCases.get("EkstaziA");
        assertNotNull(subsetTests);
        assertEquals(0, subsetTests.size());
        assertFalse(subsetTests.contains(ekstaziETest));
    }

    @Test
    public void testGetTargetClassesToTestCases4() {
        List<String> classes = new ArrayList<>();
        classes.add("EkstaziE");

        List<UnitTest> tests = new ArrayList<>();

        UnitTest ekstaziETest = new UnitTest("EkstaziETest", "This class does not exist");
        tests.add(ekstaziETest);

        Map<String, Set<UnitTest>> targetClassesToTestCases = this.ekstazi.getTargetClassesToTestCases(classes, tests);
        assertNotNull(targetClassesToTestCases);
        assertEquals(1, targetClassesToTestCases.size());

        Set<UnitTest> subsetTests = targetClassesToTestCases.get("EkstaziE");
        assertNotNull(subsetTests);
        assertEquals(1, subsetTests.size());
        assertTrue(subsetTests.contains(ekstaziETest));
    }

}
