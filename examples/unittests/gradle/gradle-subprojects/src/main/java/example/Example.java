package example;

public class Example {

  int value = 100;

  public Example() {

  }

  public int getValue() {
      try {
          Thread.sleep(1000);
      } catch (InterruptedException e) {
          e.printStackTrace();
          System.err.println("Example Gradle Project - Example.getValue() error occurred when sleeping");
          System.exit(-1);
      }
      return this.value;
  }

}
