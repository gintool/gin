package gin.misc;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import gin.SourceFile;
import gin.SourceFileTree;
import gin.TestConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class FullyQualifiedNamesTest {

    private final static String exampleSourceNoPackageFilename = TestConfiguration.EXAMPLE_DIR_NAME + "Triangle.java";
    private final static String exampleSourceWithPackageFilename = TestConfiguration.EXAMPLE_DIR_NAME + "mypackage" + File.separator + "Example.java";
    private final static String exampleSourceWithInnerClasses = TestConfiguration.EXAMPLE_DIR_NAME + "mypackage" + File.separator + "TestInnerClasses.java";
    private final static String exampleForIssue52 = "examples/unittests/Triangle_Issue52.java";
    private final static String exampleSourceEnum = TestConfiguration.EXAMPLE_DIR_NAME + "mypackage" + File.separator + "EnumTest.java";
    private CompilationUnit compilationUnitNoPackage;
    private CompilationUnit compilationUnitWithPackage;
    private CompilationUnit compilationUnitWithInnerClasses;
    private CompilationUnit compilationUnitEnum;

    public static void assertEqualsWithoutWhitespace(String s1, String s2) {
        String s1NoWhitespace = s1.replaceAll("\\s+", "");
        String s2NoWhitespace = s2.replaceAll("\\s+", "");
        assertEquals(s1NoWhitespace, s2NoWhitespace);
    }

    @Before
    public void setup() throws FileNotFoundException {
        compilationUnitNoPackage = StaticJavaParser.parse(new File(exampleSourceNoPackageFilename));
        compilationUnitWithPackage = StaticJavaParser.parse(new File(exampleSourceWithPackageFilename));
        compilationUnitWithInnerClasses = StaticJavaParser.parse(new File(exampleSourceWithInnerClasses));
        compilationUnitEnum = StaticJavaParser.parse(new File(exampleSourceEnum));
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
    public void testMethodNames() {

        FullyQualifiedNames.annotateCompilationUnit(compilationUnitWithInnerClasses);

        List<String> methodNames = new ArrayList<>();
        for (MethodDeclaration m : compilationUnitWithInnerClasses.getChildNodesByType(MethodDeclaration.class)) {
            methodNames.add(m.getData(FullyQualifiedNames.NODEKEY_FQ_METHOD_NAME));
        }

        String[] expected = new String[]{
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
        };

        assertArrayEquals(expected, methodNames.toArray());

        FullyQualifiedNames.annotateCompilationUnit(compilationUnitEnum);

        List<String> methodNamesEnum = new ArrayList<>();
        for (MethodDeclaration m : compilationUnitEnum.getChildNodesByType(MethodDeclaration.class)) {
            methodNamesEnum.add(m.getData(FullyQualifiedNames.NODEKEY_FQ_METHOD_NAME));
        }

        String[] expectedEnum = new String[]{
                "mypackage.EnumTest.doStuff()",
                "mypackage.EnumTest$InnerClass.moreStuff()"
        };

        assertArrayEquals(expectedEnum, methodNamesEnum.toArray());
    }

    @Test
    public void testIssue52() {

        // was buggy
        SourceFile sfBug0 = new SourceFileTree(exampleForIssue52,
                Collections.singletonList("testMethodBug0(int,Map<List,Integer>)"));

        // was ok
        SourceFile sfBug1 = new SourceFileTree(exampleForIssue52,
                Collections.singletonList("testMethodBug1(int,Map<List,Integer>)"));

        // was buggy
        SourceFile sfBug2 = new SourceFileTree(exampleForIssue52,
                Collections.singletonList("testMethodBug2(int,List<Integer>)"));


        // was ok
        SourceFile sfBug3 = new SourceFileTree(exampleForIssue52,
                Collections.singletonList("testMethodBug3(int,Map<List,Integer>)"));

        // was ok
        SourceFile sfBug4 = new SourceFileTree(exampleForIssue52,
                Collections.singletonList("testMethodBug4(int,List<Integer>)"));

    }

}
