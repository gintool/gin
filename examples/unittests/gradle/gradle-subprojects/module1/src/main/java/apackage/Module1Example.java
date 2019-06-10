package apackage;

public class Module1Example {

    public int doSomething() {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.err.println("Error when thread sleeping in Gradle example project, Module 1");
            System.exit(-1);
        }
        return 88;
    }

}
