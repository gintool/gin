package gin.edit.statement;

import com.github.javaparser.ast.stmt.Statement;
import gin.SourceFile;
import gin.SourceFileTree;
import gin.edit.Edit;
import gin.misc.BlockedByJavaParserException;

import java.io.Serial;
import java.util.Random;

public class MoveStatement extends StatementEdit {

    @Serial
    private static final long serialVersionUID = 7928960721990003574L;
    private final String sourceFilename;
    private final int sourceStatement;
    private final String destinationFilename;
    private final int destinationBlock;
    private final int destinationChildInBlock;

    /**
     * create a random movestatement for the given sourcefile, using the provided RNG
     *
     * @param sourceFile to create an edit for
     * @param rng        random number generator, used to choose the target statements
     */
    public MoveStatement(SourceFile sourceFile, Random rng) {
        SourceFileTree sf = (SourceFileTree) sourceFile;
        int statementToMove = sf.getRandomStatementID(true, rng);
        int insertBlock = sf.getRandomBlockID(true, rng);
        int insertStatementID = sf.getRandomInsertPointInBlock(insertBlock, rng);
        if (insertStatementID < 0) {
            insertStatementID = 0; // insert at start of empty block
        }

        this.sourceFilename = sf.getRelativePathToWorkingDir();
        this.sourceStatement = statementToMove;
        this.destinationFilename = sf.getRelativePathToWorkingDir();
        this.destinationBlock = insertBlock;
        this.destinationChildInBlock = insertStatementID;
    }

    /**
     * @param sourceFilename            - filename containing source statement
     * @param sourceStatement           - ID of source statement
     * @param destinationFilename       - filename containing destination statement
     * @param destinationBlockID        - ID of destination block
     * @param destinationChildInBlockID - ID of child in destination block (the
     *                                  statement will be moved to immediately before the first ID
     *                                  greater than this number; if the ID is the same as the block ID
     *                                  that target is the start of the block; if the target ID was
     *                                  deleted the statement will go where the target used to be;
     *                                  if the ID exists the statement will go after it;
     *                                  if multiple statements are moved here, they will be inserted in order)
     */
    public MoveStatement(String sourceFilename, int sourceStatement, String destinationFilename, int destinationBlockID, int destinationChildInBlockID) {
        this.sourceFilename = sourceFilename;
        this.sourceStatement = sourceStatement;
        this.destinationFilename = destinationFilename;
        this.destinationBlock = destinationBlockID;
        this.destinationChildInBlock = destinationChildInBlockID;
    }

    public static Edit fromString(String description) {
        String[] tokens = description.split("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        String[] sourceTokens = tokens[1].split(":");
        String sourceFilename = sourceTokens[0].replace("\"", "");
        int sourceStatement = Integer.parseInt(sourceTokens[1]);
        String destination = tokens[3];
        String[] destTokens = destination.split(":");
        String destFilename = destTokens[0].replace("\"", "");
        int destBlock = Integer.parseInt(destTokens[1]);
        int destLine = Integer.parseInt(destTokens[2]);
        return new MoveStatement(sourceFilename, sourceStatement, destFilename, destBlock, destLine);
    }

    @Override
    public SourceFile apply(SourceFile sourceFile, Object metadata) {
        if (sourceStatement == destinationChildInBlock) {
            return sourceFile; // no-op
        }

        SourceFileTree sf = (SourceFileTree) sourceFile;
        Statement source = sf.getStatement(sourceStatement);
        Statement destination = sf.getStatement(destinationBlock);

        if ((source == null) || (destination == null)) { // need to check both as we do two operations below and we don't want one to fail but not the other
            return sf; // targeting a deleted location just does nothing.
        }

        try {
            sf = sf.insertStatement(destinationBlock, destinationChildInBlock, source);
            sf = sf.removeStatement(sourceStatement);
            return sf;
        } catch (BlockedByJavaParserException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return this.getClass().getCanonicalName() + " \"" + sourceFilename + "\":" + sourceStatement + " -> \"" + destinationFilename + "\":" + destinationBlock + ":" + destinationChildInBlock;
    }

}
