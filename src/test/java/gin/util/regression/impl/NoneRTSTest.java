package gin.util.regression.impl;

import gin.test.UnitTest;
import gin.util.HotMethod;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Giovani
 */
public class NoneRTSTest {

    private static NoneRTS noneRTS;

    public NoneRTSTest() {
    }

    @BeforeClass
    public static void beforeClass() {
        noneRTS = new NoneRTS();
    }

    @Test
    public void testGetGoal() {
        assertEquals("test", noneRTS.getTestGoal());
    }

    @Test
    public void testGetArgumentLine() throws IOException {
        String argLine = noneRTS.getArgumentLine();
        assertNotNull(argLine);
        assertTrue(argLine.trim().isEmpty());
    }

    /**
     * Test of linkTestsToMethods method, of class NoneRTS.
     */
    @Test
    public void testLinkTestsToMethods() {
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

        noneRTS.linkTestsToMethods(methods, tests);

        Set<UnitTest> subsetTests = methodA.getTests();
        assertNotNull(subsetTests);
        assertTrue(subsetTests.contains(ekstaziATest1));
        assertTrue(subsetTests.contains(ekstaziATest2));
        assertTrue(subsetTests.contains(ekstaziBTest1));
        assertTrue(subsetTests.contains(ekstaziBTest2));
        assertTrue(subsetTests.contains(ekstaziBTest3));
        assertTrue(subsetTests.contains(ekstaziCTest));
        assertTrue(subsetTests.contains(ekstaziDTest));
        assertTrue(subsetTests.contains(ekstaziETest));

        subsetTests = methodB.getTests();
        assertNotNull(subsetTests);
        assertTrue(subsetTests.contains(ekstaziATest1));
        assertTrue(subsetTests.contains(ekstaziATest2));
        assertTrue(subsetTests.contains(ekstaziBTest1));
        assertTrue(subsetTests.contains(ekstaziBTest2));
        assertTrue(subsetTests.contains(ekstaziBTest3));
        assertTrue(subsetTests.contains(ekstaziCTest));
        assertTrue(subsetTests.contains(ekstaziDTest));
        assertTrue(subsetTests.contains(ekstaziETest));

        subsetTests = sum.getTests();
        assertNotNull(subsetTests);
        assertTrue(subsetTests.contains(ekstaziATest1));
        assertTrue(subsetTests.contains(ekstaziATest2));
        assertTrue(subsetTests.contains(ekstaziBTest1));
        assertTrue(subsetTests.contains(ekstaziBTest2));
        assertTrue(subsetTests.contains(ekstaziBTest3));
        assertTrue(subsetTests.contains(ekstaziCTest));
        assertTrue(subsetTests.contains(ekstaziDTest));
        assertTrue(subsetTests.contains(ekstaziETest));

        subsetTests = methodFromEkstaziC.getTests();
        assertNotNull(subsetTests);
        assertTrue(subsetTests.contains(ekstaziATest1));
        assertTrue(subsetTests.contains(ekstaziATest2));
        assertTrue(subsetTests.contains(ekstaziBTest1));
        assertTrue(subsetTests.contains(ekstaziBTest2));
        assertTrue(subsetTests.contains(ekstaziBTest3));
        assertTrue(subsetTests.contains(ekstaziCTest));
        assertTrue(subsetTests.contains(ekstaziDTest));
        assertTrue(subsetTests.contains(ekstaziETest));

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

    /**
     * Test of getTargetClassesToTestCases method, of class NoneRTS.
     */
    @Test
    public void testGetTargetClassesToTestCases() {
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

        Map<String, Set<UnitTest>> result = noneRTS.getTargetClassesToTestCases(classes, tests);

        assertNotNull(result);
        assertEquals(1, result.size());

        Set<UnitTest> subsetTests = result.get("EkstaziA");
        assertNotNull(subsetTests);
        assertTrue(subsetTests.contains(ekstaziATest));
        assertTrue(subsetTests.contains(ekstaziBTest));
        assertTrue(subsetTests.contains(ekstaziCTest));
        assertTrue(subsetTests.contains(ekstaziDTest));
        assertTrue(subsetTests.contains(ekstaziETest));
    }

}
