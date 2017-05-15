import static org.junit.Assert.*;

public class TriangleTest {

    private void checkClassification(int[][] triangles, Triangle.TriangleType expectedResult) {
        for (int[] triangle: triangles) {
            Triangle.TriangleType triangleType = Triangle.classifyTriangle(triangle[0], triangle[1], triangle[2]);
            assertEquals(triangleType, expectedResult);
        }
    }

    @org.junit.Test
    public void testInvalidTriangles() throws Exception {
        int[][] invalidTriangles = {{1, 2, 9}, {-1, 1, 1}, {1, -1, 1}, {1, 1, -1}, {100, 80, 10000}};
        checkClassification(invalidTriangles, Triangle.TriangleType.INVALID);
    }

    @org.junit.Test
    public void testEqualateralTriangles() throws Exception {

        int[][] equalateralTriangles = {{1, 1, 1}, {100, 100, 100}, {99, 99, 99},
                {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,},
                {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE}};

        checkClassification(equalateralTriangles, Triangle.TriangleType.EQUALATERAL);

    }

    @org.junit.Test
    public void testIsocelesTriangles() throws Exception {

    }

    @org.junit.Test
    public void testScaleneTriangles() throws Exception {

    }

}