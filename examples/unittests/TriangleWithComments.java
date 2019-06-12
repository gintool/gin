public class TriangleWithComments {

    static final int INVALID = 0;
    static final int SCALENE = 1;
    static final int EQUALATERAL = 2;
    static final int ISOCELES = 3;

    /**
     * javadoc comment
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static int classifyTriangle(int a, int b, int c) {

        /*
         * multiline comment
         */
        
        
        delay();

        // Sort the sides so that a <= b <= c
        if (a > b) {
            int tmp = a;
            a = b;
            b = tmp;
        }

        if (a > c) { // Comment on a line with other code 
            int tmp = a;
            a = c;
            c = tmp;
        }

        /*inline comment*/ if (b > c) {
            int tmp = b;
            b = c; /*another inline comment*/
            c // inline
            =
            tmp;
        }

        if (a + b <= c) {
            return INVALID; /*
                another inline comment
        */ } else if (a == b && b == c) {
            return EQUALATERAL;
        } else if (a == b || b == c) {
            return ISOCELES;
        } else {
            return SCALENE; /*one comment*/ /*two comment*/
        }

    }

    private static void delay() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {

        }
    }

}
