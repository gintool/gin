package gin.edit.matched;

import java.util.Random;

import gin.SourceFile;
import gin.edit.Edit;
import gin.edit.statement.CopyStatement;

/**
 * Currently just a direct copy of the CopyStatement operator
 * Here for completeness
 */
public class MatchedCopyStatement extends CopyStatement {
    
    /**
     * @param sourceFile to create an edit for
     * @param rng random number generator, used to choose the target statements
     */
    public MatchedCopyStatement(SourceFile sourceFile, Random rng) {
        super(sourceFile, rng);
    }

    public MatchedCopyStatement(String sourceFilename, int sourceStatement, String destinationFilename, int destinationBlock, int destinationChildInBlock) {
        super(sourceFilename, sourceStatement, destinationFilename, destinationBlock, destinationChildInBlock);
    }

    @Override
    public String toString() {
        return super.toString(); //.replace("CopyStatement", "MatchedCopyStatement");
    }

    public static Edit fromString(String description) {
        String tokens[] = description.split("\\s+");
        String sourceTokens[] = tokens[1].split(":");
        String sourceFile = sourceTokens[0];
        int sourceStatement = Integer.parseInt(sourceTokens[1]);
        String destination = tokens[3];
        String destTokens[] = destination.split(":");
        String destFile = destTokens[0];
        int destBlock = Integer.parseInt(destTokens[1]);
        int destLine = Integer.parseInt(destTokens[2]);
        return new MatchedCopyStatement(sourceFile, sourceStatement, destFile, destBlock, destLine);
    }

    @Override
    public EditType getEditType() {
        return EditType.MATCHED_STATEMENT;
    }
}
