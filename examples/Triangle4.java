public class Triangle4 {

    public enum TriangleType {
        INVALID, SCALENE, EQUALATERAL, ISOCELES
    }

    public static TriangleType classifyTriangle(int a, int b, int c) {
        
        if (delay() || (a > b)) {
            int tmp = a;
            a = b;
            b = tmp;
        }

        if (a > c) {
            int tmp = a;
            a = c;
            c = tmp;
        }

        if (b > c) {
            int tmp = b;
            b = c;
            c = tmp;
        }

        if (a + b <= c) {
            return TriangleType.INVALID;
        } else if (a == b && b == c) {
            return TriangleType.EQUALATERAL;
        } else if (a == b || b == c) {
            return TriangleType.ISOCELES;
        } else {
            return TriangleType.SCALENE;
        }

    }

    private static boolean delay() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // do nothing
        }
        
        return true;
    }

}