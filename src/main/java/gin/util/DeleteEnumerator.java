package gin.util;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import gin.Patch;
import gin.SourceFileLine;
import gin.SourceFileTree;
import gin.edit.line.DeleteLine;
import gin.edit.statement.DeleteStatement;
import gin.test.UnitTest;
import gin.test.UnitTestResultSet;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.apache.commons.rng.simple.JDKRandomBridge;
import org.apache.commons.rng.simple.RandomSource;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.io.Serial;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Evaluates all possible program variants with line/statemnt removed from each of the mn target methods, chosen at random.
 * <p>
 * Takes a list of target methods for project (with associated tests) from methodFile (saved in methodData).
 * For each sampled method and each line/statement in that method:
 * applies mutation and runs tests associated with that method.
 * Samplling is done without replacement. All methods sampled at random.
 */
public class DeleteEnumerator extends Sampler {

    @Serial
    private static final long serialVersionUID = 2138053210278989403L;

    @Argument(alias = "ps", description = "Patch size")
    protected Integer patchSize = 1;

    @Argument(alias = "rs", description = "Random seed for method selection")
    protected Integer randomSeed = 123;

    public DeleteEnumerator(String[] args) {
        super(args);
        Args.parseOrExit(this, args);
        printAdditionalArguments();
    }

    // Constructor used for testing
    public DeleteEnumerator(File projectDir, File methodFile) {
        super(projectDir, methodFile);
    }

    public static void main(String[] args) {
        DeleteEnumerator enumerator = new DeleteEnumerator(args);
        enumerator.sampleMethods();
    }

    private void printAdditionalArguments() {
        Logger.info("Patch size: " + patchSize);
        Logger.info("Random seed for method selection: " + randomSeed);
    }

    protected void sampleMethodsHook() {

        Random rng = new JDKRandomBridge(RandomSource.MT, Long.valueOf(randomSeed));

        writeHeader();

        while (!methodData.isEmpty()) {

            // Pick a random method
            TargetMethod method = methodData.get(rng.nextInt(methodData.size()));

            // Get all method data
            File source = method.getFileSource();
            String className = method.getClassName();
            String methodName = method.getMethodName();
            Integer methodID = method.getMethodID();
            List<UnitTest> tests = method.getGinTests();

            String fileName = source.getPath();

            Patch patch;

            Logger.info("Testing all delete line edits of size " + patchSize + " for method: " + method + " with ID " + methodID);

            // Create a SourceFile object with one target method
            SourceFileLine sourceFileLine = new SourceFileLine(source, methodName);

            List<Integer> lines = sourceFileLine.getLineIDsNonEmptyOrComments(true);

            // For each combination of patchSize lines, create and test patch
            Iterator<int[]> iterator = CombinatoricsUtils.combinationsIterator(lines.size(), patchSize);
            while (iterator.hasNext()) {

                int[] combination = iterator.next();
                patch = new Patch(sourceFileLine);
                for (int line : combination) {
                    patch.add(new DeleteLine(fileName, lines.get(line)));
                }
                UnitTestResultSet results = testPatch(className, tests, patch, null);
                writeResults(results, methodID);

            }

            Logger.info("Testing all delete statement edits of size " + patchSize + " for method: " + method + " with ID " + methodID);

            // Create a SourceFile object with one target method
            SourceFileTree sourceFileTree = new SourceFileTree(source, methodName);

            List<Integer> stmts = sourceFileTree.getStatementIDsInTargetMethod();

            // For each combination of patchSize statements, create and test patch
            iterator = CombinatoricsUtils.combinationsIterator(stmts.size(), patchSize);
            while (iterator.hasNext()) {

                int[] combination = iterator.next();
                patch = new Patch(sourceFileTree);
                for (int stmt : combination) {
                    patch.add(new DeleteStatement(fileName, stmts.get(stmt)));
                }
                UnitTestResultSet results = testPatch(className, tests, patch, null);
                writeResults(results, methodID);

            }

            // Remove method from consideration for the next step
            methodData.remove(method);
        }

        Logger.info("Results saved to: " + outputFile);
    }


}
