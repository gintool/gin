public class Example {

    public int exampleMethod() {
        int x = 7;
        x++;
        if (x == 1) {
            x += 5;
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            System.err.println("Example was interrupted when sleeping");
        }
        return x;
    }

}
