package gin;

import com.github.javaparser.ast.CompilationUnit;
import gin.edit.CopyStatement;
import gin.edit.DeleteStatement;
import gin.edit.MoveStatement;

/**
 * Created by ucacdrw on 06/06/2017.
 */
public class PatchAnalyser {


    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Arguments should be as follows: ");
            System.out.println("<program> \"<patch>\"");
            System.out.println("Where patch is | bar-separated edits");
            System.exit(0);
        }

        String programFilename = args[0];
        String patchText = args[1];

        Program program = new Program(programFilename);
        TestRunner testRunner = new TestRunner(program);

        System.out.println("\nEvaluating patch for program: " + programFilename);

        Patch patch = parsePatch(patchText, program);

        System.out.println("Parsed patch is: " + patch);

        TestRunner.TestResult result = testRunner.test(patch);

        if (!result.patchSuccess) {
            System.out.println("Patch application failed");
            System.exit(0);
        }

        System.out.println("Source code of patched program:\n\n" + result.patchedProgram);

        if (!result.compiled) {
            System.out.println("Patch compilation failed");
            System.exit(0);
        }

        System.out.println("Execution time: " + result.executionTime);

    }

    private static Patch parsePatch(String patchText, Program program) {
        Patch patch = new Patch(program);
        String[] edits = patchText.trim().split("\\|");
        for (String edit: edits) {
            System.out.println(edit);
            String[] tokens = edit.trim().split("\\s+");
            switch (tokens[0]) {
                case "DEL": // DEL <sourcestatement>
                    int line = Integer.parseInt(tokens[1]);
                    patch.add(new DeleteStatement(line));
                    break;
                case "COPY": // COPY <sourcestatement> -> <destinationBlock>:<destinationChild>
                    int source = Integer.parseInt(tokens[1]);
                    String[] destination = tokens[3].split("\\:");
                    int destinationBlock = Integer.parseInt(destination[0]);
                    int destinationChild = Integer.parseInt(destination[1]);
                    patch.add(new CopyStatement(source, destinationBlock, destinationChild));
                    break;
                case "MOVE": // MOVE <sourcestatement> -> <destinationBlock>:<destinationChild>
                    int moveSource = Integer.parseInt(tokens[1]);
                    String[] moveDestination = tokens[3].split("\\:");
                    int moveDestinationBlock = Integer.parseInt(moveDestination[0]);
                    int moveDestinationChild = Integer.parseInt(moveDestination[1]);
                    patch.add(new MoveStatement(moveSource, moveDestinationBlock, moveDestinationChild));
                    break;
                case "": // could be a leading "|"
                    break;
                default:
                    System.out.println("Unrecognised edit: " + tokens[0]);
                    System.exit(0);
            }
        }
        return patch;
    }

}
