package gin.edit.matched;

import gin.SourceFile;
import gin.SourceFileTree;
import gin.edit.Edit;
import gin.edit.statement.SwapStatement;

import java.io.Serial;
import java.util.Random;

/*
 * Similar to the move statement operator, but this simply swaps two matching statements
 */
public class MatchedSwapStatement extends SwapStatement {

    @Serial
    private static final long serialVersionUID = -5831284385801518450L;

    /**
     * This is our attempt at a Java-specific grammar aware operator.
     * Swap one statement with another one of the same type
     *
     * @param sourceFile to create an edit for
     * @param rng        random number generator, used to choose the target statements
     */
    // @param cu is provided to allow a matching node to be chosen
    public MatchedSwapStatement(SourceFile sourceFile, Random rng) {

        super(sourceFile.getRelativePathToWorkingDir(), 0, sourceFile.getRelativePathToWorkingDir(), 0);

        SourceFileTree sf = (SourceFileTree) sourceFile;

        // target is in target method only
        destinationStatement = sf.getRandomStatementID(true, rng);

        // source can be anywhere in the class, but must be of a matching type
        sourceStatement = sf.getRandomNodeID(false, sf.getStatement(destinationStatement).getClass(), rng);

    }

    private MatchedSwapStatement(String sourceFilename, int sourceStatement, String destinationFilename, int destinationStatement) {
        super(sourceFilename, sourceStatement, destinationFilename, destinationStatement);
    }

    public static Edit fromString(String description) {
        String[] tokens = description.split("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        String[] srcTokens = tokens[1].split(":");
        String srcFilename = srcTokens[0].replace("\"", "");
        int source = Integer.parseInt(srcTokens[1]);
        String[] destTokens = tokens[3].split(":");
        String destFilename = destTokens[0].replace("\"", "");
        int destination = Integer.parseInt(destTokens[1]);
        return new MatchedSwapStatement(srcFilename, source, destFilename, destination);
    }

    @Override
    public EditType getEditType() {
        return EditType.MATCHED_STATEMENT;
    }
}
