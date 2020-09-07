import java.util.List;
import java.util.Map;

/** for fixing https://github.com/gintool/gin/issues/52 */
public class Triangle_Issue52 {

    static final int INVALID = 0;
    static final int SCALENE = 1;
    static final int EQUALATERAL = 2;
    static final int ISOCELES = 3;

    public static int classifyTriangle(int a, int b, int c) {

        delay();

        // Sort the sides so that a <= b <= c
        if (a > b) {
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
            return INVALID;
        } else if (a == b && b == c) {
            return EQUALATERAL;
        } else if (a == b || b == c) {
            return ISOCELES;
        } else {
            return SCALENE;
        }

    }
    
    // identified in the issue
    public Map<List, Integer> testMethodBug0(int a, Map<List,Integer> arg) {
    	System.out.println();
    	return null;
    }
    
    public List<Integer> testMethodBug1(int a, Map<List,Integer> arg) {
    	System.out.println();
    	return null;
    }
    
    public Map<List, Integer> testMethodBug2(int a, List<Integer> arg) {
    	System.out.println();
    	return null;
    }
    
    public Integer testMethodBug3(int a, Map<List,Integer> arg) {
    	System.out.println();
    	return null;
    }
    
    public Integer testMethodBug4(int a, List<Integer> arg) {
    	System.out.println();
    	return null;
    }

    private static void delay() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {

        }
    }

}
