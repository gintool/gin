import static org.junit.Assert.assertEquals;

public class Triangle3Test {

    private void checkClassification(int[][] triangles, Triangle3.TriangleType expectedResult) {
        for (int[] triangle: triangles) {
        	Triangle3.TriangleType triangleType = Triangle3.classifyTriangle(triangle[0], triangle[1], triangle[2]);
            assertEquals(expectedResult, triangleType);
        }
    }

    @org.junit.Test
    public void testInvalidTriangles() throws Exception {
        int[][] invalidTriangles = {{1, 2, 9}, {-1, 1, 1}, {1, -1, 1}, {1, 1, -1}, {100, 80, 10000}};
        checkClassification(invalidTriangles, Triangle3.TriangleType.INVALID);
    }

    @org.junit.Test
    public void testEqualateralTriangles() throws Exception {
        int[][] equalateralTriangles = {{1, 1, 1}, {100, 100, 100}, {99, 99, 99}};
        checkClassification(equalateralTriangles, Triangle3.TriangleType.EQUALATERAL);
    }

    @org.junit.Test
    public void testIsocelesTriangles() throws Exception {
        int[][] isocelesTriangles = {{100, 90, 90}, {1000, 900, 900}, {3,2,2}, {30,16,16}};
        checkClassification(isocelesTriangles, Triangle3.TriangleType.ISOCELES);
    }

    @org.junit.Test
    public void testScaleneTriangles() throws Exception {
        int[][] scaleneTriangles = {{5, 4, 3}, {1000, 900, 101}, {3,20,21}, {999, 501, 600}};
        checkClassification(scaleneTriangles, Triangle3.TriangleType.SCALENE);
    }

}