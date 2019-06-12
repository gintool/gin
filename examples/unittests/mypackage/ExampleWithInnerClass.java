package mypackage;

public class ExampleWithInnerClass {

    public int simpleMethod() {
        MyInner inner = new MyInner(20);
        return inner.getNumber();
    }

    class MyInner {

        int number;

        MyInner(int v) {
            number = v;
        }

        int getNumber() {
            return number;
        }

    }

}
