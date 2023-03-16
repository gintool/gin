package com.mycompany.app;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

    private void checkClassification(int[][] triangles, int expectedResult) {
        for (int[] triangle: triangles) {
            int triangleType = App.classifyTriangle(triangle[0], triangle[1], triangle[2]);
            assertEquals(expectedResult, triangleType);
        }
    }

    @Test
    public void testInvalidTriangles() throws Exception {
        int[][] invalidTriangles = {{1, 2, 9}, {-1, 1, 1}, {1, -1, 1}, {1, 1, -1}, {100, 80, 10000}};
        checkClassification(invalidTriangles, App.INVALID);
    }

    @Test
    public void testEqualateralTriangles() throws Exception {
        int[][] equalateralTriangles = {{1, 1, 1}, {100, 100, 100}, {99, 99, 99}};
        checkClassification(equalateralTriangles, App.EQUALATERAL);
    }

    @Test
    public void testIsocelesTriangles() throws Exception {
        int[][] isocelesTriangles = {{100, 90, 90}, {1000, 900, 900}, {3,2,2}, {30,16,16}};
        checkClassification(isocelesTriangles, App.ISOCELES);
    }

    @Test
    public void testScaleneTriangles() throws Exception {
        int[][] scaleneTriangles = {{5, 4, 3}, {1000, 900, 101}, {3,20,21}, {999, 501, 600}};
        checkClassification(scaleneTriangles, App.SCALENE);
    }

    @Test
    public void jfrPrimeTest() {
        int[] primes = new int[30000];

        int index = 1;
        primes[0] = 2;
        int current = 3;

        while (index<30000) {
            boolean check = true;
            for (int i = 0;i<index;i++) {
                if (current%primes[i]==0) {
                    check = false;
                    break;
                }
            }
            if (check) {
                primes[index] = current;
                index++;
            }
            current++;
        }

        assertTrue(true);
    }
}
