package gin.edit.matched;

import java.util.Random;

import gin.SourceFile;
import gin.edit.Edit;
import gin.edit.statement.DeleteStatement;

/*
 * Delete statement. Currently no different to regular delete statement operator;
 * here as a placeholder in case we want to do anything different.
 */
public class MatchedDeleteStatement extends DeleteStatement {

    /**
     * This is our attempt at a Java-specific grammar aware operator.
     * Delete a given statement
     * 
     * @param sourceFile to create an edit for
     * @param rng random number generator, used to choose the target statements
     */
    public MatchedDeleteStatement(SourceFile sourceFile, Random rng) {
        super(sourceFile, rng);
    }
    
    public MatchedDeleteStatement(String filename, int lineToDelete) {
        super(filename, lineToDelete);
    }


    @Override
    public String toString() {
        return super.toString(); //.replace("DeleteStatement", "MatchedDeleteStatement");
    }

    public static Edit fromString(String description) {
        String tokens[] = description.split("\\s+");
        String tokens2[] = tokens[1].split(":");
        String filename = tokens2[0];
        int statement = Integer.parseInt(tokens2[1]);
        return new MatchedDeleteStatement(filename, statement);
    }
    
    @Override
    public EditType getEditType() {
        return EditType.MATCHED_STATEMENT;
    }
}
