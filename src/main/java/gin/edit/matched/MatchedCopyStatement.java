package gin.edit.matched;

import gin.SourceFile;
import gin.edit.Edit;
import gin.edit.statement.CopyStatement;

import java.io.Serial;
import java.util.Random;

/**
 * Currently just a direct copy of the CopyStatement operator
 * Here for completeness
 */
public class MatchedCopyStatement extends CopyStatement {

    @Serial
    private static final long serialVersionUID = -8219981780665785484L;

    /**
     * @param sourceFile to create an edit for
     * @param rng        random number generator, used to choose the target statements
     */
    public MatchedCopyStatement(SourceFile sourceFile, Random rng) {
        super(sourceFile, rng);
    }

    public MatchedCopyStatement(String sourceFilename, int sourceStatement, String destinationFilename, int destinationBlock, int destinationChildInBlock) {
        super(sourceFilename, sourceStatement, destinationFilename, destinationBlock, destinationChildInBlock);
    }

    public static Edit fromString(String description) {
        String[] tokens = description.split("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        String[] sourceTokens = tokens[1].split(":");
        String sourceFile = sourceTokens[0].replace("\"", ""); // strip quotes;
        int sourceStatement = Integer.parseInt(sourceTokens[1]);
        String destination = tokens[3];
        String[] destTokens = destination.split(":");
        String destFile = destTokens[0].replace("\"", ""); // strip quotes;
        int destBlock = Integer.parseInt(destTokens[1]);
        int destLine = Integer.parseInt(destTokens[2]);
        return new MatchedCopyStatement(sourceFile, sourceStatement, destFile, destBlock, destLine);
    }

    @Override
    public EditType getEditType() {
        return EditType.MATCHED_STATEMENT;
    }
}
