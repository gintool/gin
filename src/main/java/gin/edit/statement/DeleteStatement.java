package gin.edit.statement;

import gin.SourceFile;
import gin.SourceFileTree;
import gin.edit.Edit;
import gin.misc.BlockedByJavaParserException;

import java.io.Serial;
import java.util.Random;

public class DeleteStatement extends StatementEdit {

    @Serial
    private static final long serialVersionUID = -8946372835353498185L;
    private final String sourceFilename;
    private final int statementToDelete;

    /**
     * create a random deletestatement for the given sourcefile, using the provided RNG
     *
     * @param sourceFile to create an edit for
     * @param rng        random number generator, used to choose the target statements
     */
    public DeleteStatement(SourceFile sourceFile, Random rng) {
        this(sourceFile.getRelativePathToWorkingDir(), ((SourceFileTree) sourceFile).getRandomStatementID(true, rng));
    }

    public DeleteStatement(String filename, int statementToDelete) {
        this.sourceFilename = filename;
        this.statementToDelete = statementToDelete;
    }

    public static Edit fromString(String description) {
        String[] tokens = description.split("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        String[] tokens2 = tokens[1].split(":");
        String filename = tokens2[0].replace("\"", "");
        int statement = Integer.parseInt(tokens2[1]);
        return new DeleteStatement(filename, statement);
    }

    @Override
    public SourceFile apply(SourceFile sourceFile, Object metadata) {
        SourceFileTree sf = (SourceFileTree) sourceFile;
        try {
            return sf.removeStatement(statementToDelete);
        } catch (BlockedByJavaParserException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return this.getClass().getCanonicalName() + " \"" + sourceFilename + "\":" + statementToDelete;
    }

}
