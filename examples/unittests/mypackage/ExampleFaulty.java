package mypackage;

public class ExampleFaulty extends ExampleBase {

    public ExampleFaulty() {
        super();
    }

    public int returnTen() {
        int result = 100;
        result = 10;
        result = 20;
        return result;
    }

    // Access another class so we can test for protection errors
    public int returnOneHundred() {
        return this.justAField;
    }

}
