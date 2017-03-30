public class Triangle {

    public int methodOne() {
        int x = 7;
        x ++;

        if (x == 1) {
            x += 5;
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            System.err.println("Triangle was interrupted when sleeping");
        }
        return x;
    }
}
