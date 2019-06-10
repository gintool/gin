package mypackage;

public class Example extends ExampleBase {

    public Example() {
        super();
    }

    public int returnTen() {
        int result = 100;
        result = 20;
        result = 10;
        return result;
    }

    // Access another class so we can test for protection errors
    public int returnOneHundred() {
        return this.justAField;
    }

}
