import org.mdkt.compiler.InMemoryJavaCompiler;

public class MinimalExample {

    public static void main(String args[]) throws Exception {

        Class<?> oldClass = Example.class;

        InMemoryJavaCompiler compiler = InMemoryJavaCompiler.newInstance();
        Class<?> newClass = compiler.compile("Example", "public class Example {\n }\n");

        if (oldClass.hashCode() == newClass.hashCode()) {
            System.out.println("I have two copies of the old class.");
        }

    }

}