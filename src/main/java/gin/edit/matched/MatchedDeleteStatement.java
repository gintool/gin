package gin.edit.matched;

import gin.SourceFile;
import gin.edit.Edit;
import gin.edit.statement.DeleteStatement;

import java.io.Serial;
import java.util.Random;

/*
 * Delete statement. Currently no different to regular delete statement operator;
 * here as a placeholder in case we want to do anything different.
 */
public class MatchedDeleteStatement extends DeleteStatement {

    @Serial
    private static final long serialVersionUID = 6177385671420449098L;

    /**
     * This is our attempt at a Java-specific grammar aware operator.
     * Delete a given statement
     *
     * @param sourceFile to create an edit for
     * @param rng        random number generator, used to choose the target statements
     */
    public MatchedDeleteStatement(SourceFile sourceFile, Random rng) {
        super(sourceFile, rng);
    }

    public MatchedDeleteStatement(String filename, int lineToDelete) {
        super(filename, lineToDelete);
    }


    public static Edit fromString(String description) {
        String[] tokens = description.split("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        String[] tokens2 = tokens[1].split(":");
        String filename = tokens2[0].replace("\"", "");
        int statement = Integer.parseInt(tokens2[1]);
        return new MatchedDeleteStatement(filename, statement);
    }

    @Override
    public EditType getEditType() {
        return EditType.MATCHED_STATEMENT;
    }
}
