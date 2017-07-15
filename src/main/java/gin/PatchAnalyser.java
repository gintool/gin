package gin;

import gin.edit.CopyStatement;
import gin.edit.DeleteStatement;
import gin.edit.MoveStatement;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/*
    A handy utility for analysing patches. Not part of the main gin system.
 */
public class PatchAnalyser {

    private static final int REPS = 10;

    public static void main(String[] args) {

        // Check and parse arguments
        if (args.length != 2) {
            System.out.println("Arguments: <SourceFilename.java> \"<patch>\"");
            System.out.println("Where patch is a sequence of | bar-separated edits");
            System.exit(0);
        }

        String sourceFilename = args[0];
        String patchText = args[1];

        // Create SourceFile and tester classes, parse the patch and generate patched source.
        SourceFile sourceFile = new SourceFile(sourceFilename);
        TestRunner testRunner = new TestRunner(sourceFile);

        // Dump statement numbering to a file
        String statementNumbering = sourceFile.statementList();
        String statementFilename = sourceFilename + ".statements";
        try {
            FileUtils.writeStringToFile(new File(statementFilename), statementNumbering, Charset.defaultCharset());
        } catch (IOException e) {
            System.err.println("Could not write statements to " + statementFilename);
            System.err.println(e);
            e.printStackTrace();
            System.exit(-1);
        }
        System.out.println("Statement numbering written to: " + statementFilename);

        // Dump block numbering to a file
        String blockNumbering = sourceFile.blockList();
        String blockFilename = sourceFilename + ".blocks";
        try {
            FileUtils.writeStringToFile(new File(blockFilename), blockNumbering, Charset.defaultCharset());
        } catch (IOException e) {
            System.err.println("Could not write blocks to " + blockFilename);
            System.err.println(e);
            e.printStackTrace();
            System.exit(-1);
        }
        System.out.println("Block numbering written to: " + blockFilename);

        Patch patch = parsePatch(patchText, sourceFile);
        String patchedSource = patch.apply().getSource();

        System.out.println("Evaluating patch for Class Source: " + sourceFilename);

        System.out.println("Patch is: " + patchText);

        // Write the patched source to file, for reference
        String patchedFilename = sourceFilename + ".patched";
        try {
            FileUtils.writeStringToFile(new File(patchedFilename), patchedSource, Charset.defaultCharset());
        } catch (IOException e) {
            System.err.println("Could not write patched source to " + sourceFilename);
            System.err.println(e);
            e.printStackTrace();
            System.exit(-1);
        }
        System.out.println("Parsed patch written to: " + patchedFilename);

        // Evaluate original class
        System.out.println("Timing original class execution...");
        Patch emptyPatch = new Patch(sourceFile);
        double originalExecutionTime = testRunner.test(emptyPatch, REPS).executionTime;
        System.out.println("Original execution time: " + originalExecutionTime);

        // Evaluate patch
        System.out.println("Timing patched sourceFile execution...");
        TestRunner.TestResult result = testRunner.test(patch, REPS);
        System.out.println("Test result: " + result);
        System.out.println("Execution time of patched sourceFile: " + result.executionTime);
        System.out.println("Speedup (%): " + (100 * ((originalExecutionTime - result.executionTime)/originalExecutionTime)));

    }

    private static Patch parsePatch(String patchText, SourceFile sourceFile) {
        Patch patch = new Patch(sourceFile);
        String[] edits = patchText.trim().split("\\|");
        for (String edit: edits) {
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
