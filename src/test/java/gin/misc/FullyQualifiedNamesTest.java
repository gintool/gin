package gin.misc;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;

import gin.TestConfiguration;

public class FullyQualifiedNamesTest {

    private CompilationUnit compilationUnitNoPackage;
    private CompilationUnit compilationUnitWithPackage;
    private CompilationUnit compilationUnitWithInnerClasses;
    private CompilationUnit compilationUnitEnum;
    
    private final static String exampleSourceNoPackageFilename = TestConfiguration.EXAMPLE_DIR_NAME + "Triangle.java";
    private final static String exampleSourceWithPackageFilename = TestConfiguration.EXAMPLE_DIR_NAME + "mypackage" + File.separator + "Example.java";
    
    private final static String exampleSourceWithInnerClasses = TestConfiguration.EXAMPLE_DIR_NAME + "mypackage" + File.separator + "TestInnerClasses.java";

    private final static String exampleSourceEnum = TestConfiguration.EXAMPLE_DIR_NAME + "mypackage" + File.separator + "EnumTest.java";

    private final static Charset charSet = Charset.forName("UTF-8");

    @Before
    public void setup() throws FileNotFoundException {
        compilationUnitNoPackage = JavaParser.parse(new File(exampleSourceNoPackageFilename));
        compilationUnitWithPackage = JavaParser.parse(new File(exampleSourceWithPackageFilename));
        compilationUnitWithInnerClasses = JavaParser.parse(new File(exampleSourceWithInnerClasses));
        compilationUnitEnum = JavaParser.parse(new File(exampleSourceEnum));
    }

    @Test
    public void makeMethodNameFullyQualified() throws Exception {
        
        assertEquals("mypackage.Example.returnTen()", FullyQualifiedNames.makeMethodNameFullyQualified("returnTen()", compilationUnitWithPackage));
        assertEquals("mypackage.Example.returnTen()", FullyQualifiedNames.makeMethodNameFullyQualified("Example.returnTen()", compilationUnitWithPackage));
        assertEquals("mypackage.Example.returnTen()", FullyQualifiedNames.makeMethodNameFullyQualified("mypackage.Example.returnTen()", compilationUnitWithPackage));
        
        assertEquals("Triangle.classifyTriangle(int,int,int)", FullyQualifiedNames.makeMethodNameFullyQualified("classifyTriangle(int,int,int)", compilationUnitNoPackage));
        assertEquals("Triangle.classifyTriangle(int,int,int)", FullyQualifiedNames.makeMethodNameFullyQualified("Triangle.classifyTriangle(int,int,int)", compilationUnitNoPackage));
        
    }
    
    @Test
    public void testMethodNames() throws Exception {
        
        FullyQualifiedNames.annotateCompilationUnit(compilationUnitWithInnerClasses);
        
        List<String> methodNames = new ArrayList<>();
        for (MethodDeclaration m : compilationUnitWithInnerClasses.getChildNodesByType(MethodDeclaration.class)) {
            methodNames.add(m.getData(FullyQualifiedNames.NODEKEY_FQ_METHOD_NAME));
        }
        
        List<String> expected = Arrays.asList(new String[] {
                "mypackage.TestInnerClasses.topLevelMethod()",
                "mypackage.TestInnerClasses.topLevelMethod(List<?extendsComparable<?>>)",
                "mypackage.TestInnerClasses.topLevelStaticMethod()",
                "mypackage.TestInnerClasses.topLevelMethod1(int,int,int)",
                "mypackage.TestInnerClasses$1.get()",
                "mypackage.TestInnerClasses$LocalInnerClass.innerClassMethod()",
                "mypackage.TestInnerClasses$2.get()",
                "mypackage.TestInnerClasses$2$1.compareTo(Object)",
                "mypackage.TestInnerClasses$2$LocalInnerClassWithinAnonInnerClass.methodLA()",
                "mypackage.TestInnerClasses$2.doNothing()",
                "mypackage.TestInnerClasses.topLevelMethod2()",
                "mypackage.TestInnerClasses$3.method2()",
                "mypackage.TestInnerClasses$4.run()",
                "mypackage.TestInnerClasses$TestInnerClassA.toString()",
                "mypackage.TestInnerClasses$TestInnerClassA.methodA()",
                "mypackage.TestInnerClasses$TestInnerClassB.toString()",
                "mypackage.TestInnerClasses$TestInnerClassB.methodB()"
        });

        assertThat(methodNames, is(expected));
        
        FullyQualifiedNames.annotateCompilationUnit(compilationUnitEnum);
        
        List<String> methodNamesEnum = new ArrayList<>();
        for (MethodDeclaration m : compilationUnitEnum.getChildNodesByType(MethodDeclaration.class)) {
            methodNamesEnum.add(m.getData(FullyQualifiedNames.NODEKEY_FQ_METHOD_NAME));
        }
        
        List<String> expectedEnum = Arrays.asList(new String[] {
                "mypackage.EnumTest.doStuff()",
                "mypackage.EnumTest$InnerClass.moreStuff()"
        });

        assertThat(methodNamesEnum, is(expectedEnum));
        
    }

    public static void assertEqualsWithoutWhitespace(String s1, String s2) {
        String s1NoWhitespace = s1.replaceAll("\\s+", "");
        String s2NoWhitespace = s2.replaceAll("\\s+", "");
        assertEquals(s1NoWhitespace, s2NoWhitespace);
    }

}
