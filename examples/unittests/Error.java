import java.util.concurrent.TimeUnit;

public class Error {

    String notInitialised;

    public int returnTen(int i) {

        if (i == 5) {
            System.out.println(notInitialised.substring(1));
        }

        return 10;

    }
    public void thisMethodTakesASecond() throws InterruptedException {
        TimeUnit.SECONDS.sleep(1);
    }

}
