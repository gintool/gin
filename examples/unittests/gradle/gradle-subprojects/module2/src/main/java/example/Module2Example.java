package example;

public class Module2Example {

    public int doSomething() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.err.println("Error when thread sleeping in Gradle example project, Module 2");
            System.exit(-1);
        }
        return 91;
    }

}
