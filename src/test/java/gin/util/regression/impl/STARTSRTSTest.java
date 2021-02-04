package gin.util.regression.impl;

import edu.illinois.starts.constants.StartsConstants;
import gin.TestConfiguration;
import gin.util.HotMethod;
import gin.test.UnitTest;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author Giovani
 */
public class STARTSRTSTest {

    private static final String PROJECT_DIR = TestConfiguration.EXAMPLE_DIR_NAME + File.separator + "ekstazi";
    private STARTSRTS starts;

    @Before
    public void setUp() {
        this.starts = new STARTSRTS(PROJECT_DIR);
    }

    @Test
    public void testConstructor() throws IOException {
        String canonicalPath = this.starts.startsDir.getCanonicalPath();
        assertEquals(FileUtils.getFile(PROJECT_DIR + File.separator + StartsConstants.STARTS_DIRECTORY_PATH).getCanonicalPath(), canonicalPath);
        assertTrue(this.starts.startsDir.exists());
    }

    /**
     * Test of getTestGoal method, of class STARTSRTS.
     */
    @Test
    public void testGetTestGoal() {
        assertEquals("edu.illinois:starts-maven-plugin:1.3:clean edu.illinois:starts-maven-plugin:1.3:starts", starts.getTestGoal());
    }

    @Test
    public void testGetArgumentLine() throws IOException {
        String argLine = this.starts.getArgumentLine();
        assertTrue(argLine.isEmpty());
    }

    @Test
    public void testLinkMethods() {
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

        this.starts.linkTestsToMethods(methods, tests);

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
        assertFalse(subsetTests.contains(ekstaziATest1));
        assertFalse(subsetTests.contains(ekstaziATest2));
        assertFalse(subsetTests.contains(ekstaziBTest1));
        assertFalse(subsetTests.contains(ekstaziBTest2));
        assertFalse(subsetTests.contains(ekstaziBTest3));
        assertFalse(subsetTests.contains(ekstaziCTest));
        assertTrue(subsetTests.contains(ekstaziDTest));
        assertFalse(subsetTests.contains(ekstaziETest));

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

        Map<String, Set<UnitTest>> targetClassesToTestCases = this.starts.getTargetClassesToTestCases(classes, tests);
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

        Map<String, Set<UnitTest>> targetClassesToTestCases = this.starts.getTargetClassesToTestCases(classes, tests);
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

        Map<String, Set<UnitTest>> targetClassesToTestCases = this.starts.getTargetClassesToTestCases(classes, tests);
        assertNotNull(targetClassesToTestCases);
        assertEquals(1, targetClassesToTestCases.size());

        Set<UnitTest> subsetTests = targetClassesToTestCases.get("EkstaziE");
        assertNotNull(subsetTests);
        assertEquals(1, subsetTests.size());
        assertTrue(subsetTests.contains(ekstaziETest));
    }

}
