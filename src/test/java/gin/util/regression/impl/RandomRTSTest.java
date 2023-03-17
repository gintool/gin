/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gin.util.regression.impl;

import gin.test.UnitTest;
import gin.util.HotMethod;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Giovani
 */
public class RandomRTSTest {

    private RandomRTS randomRTS;

    @Before
    public void setUp() {
        this.randomRTS = new RandomRTS(1);
    }

    @Test
    public void testGetGoal() {
        assertEquals("test", this.randomRTS.getTestGoal());
    }

    @Test
    public void testGetArgumentLine() throws IOException {
        String argLine = this.randomRTS.getArgumentLine();
        assertNotNull(argLine);
        assertTrue(argLine.trim().isEmpty());
    }

    /**
     * Test of linkTestsToMethods method, of class RandomRTS.
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

        randomRTS.linkTestsToMethods(methods, tests);

        Set<UnitTest> subsetTests = methodA.getTests();
        assertNotNull(subsetTests);
        assertFalse(subsetTests.isEmpty());

        subsetTests = methodB.getTests();
        assertNotNull(subsetTests);
        assertFalse(subsetTests.isEmpty());

        subsetTests = sum.getTests();
        assertNotNull(subsetTests);
        assertFalse(subsetTests.isEmpty());

        subsetTests = methodFromEkstaziC.getTests();
        assertNotNull(subsetTests);
        assertFalse(subsetTests.isEmpty());

        subsetTests = methodFromEkstaziD.getTests();
        assertNotNull(subsetTests);
        assertFalse(subsetTests.isEmpty());

        subsetTests = methodFromEkstaziE.getTests();
        assertNotNull(subsetTests);
        assertFalse(subsetTests.isEmpty());
    }

}
