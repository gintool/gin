package gin.edit.statement;

import java.util.Random;

import gin.SourceFile;
import gin.SourceFileTree;
import gin.edit.Edit;

public class DeleteStatement extends StatementEdit {

    private String sourceFilename;
    private int statementToDelete;

    /** 
     * create a random deletestatement for the given sourcefile, using the provided RNG
     * @param sourceFile to create an edit for
     * @param rng random number generator, used to choose the target statements
     * */
    public DeleteStatement(SourceFile sourceFile, Random rng) {
        this(sourceFile.getFilename(), ((SourceFileTree)sourceFile).getRandomStatementID(true, rng));
    }
    
    public DeleteStatement(String filename, int statementToDelete) {
        this.sourceFilename = filename;
        this.statementToDelete = statementToDelete;
    }
    
    @Override
    public SourceFile apply(SourceFile sourceFile) {
        SourceFileTree sf = (SourceFileTree)sourceFile;
        return sf.removeStatement(statementToDelete);
    }

    @Override
    public String toString() {
        return this.getClass().getCanonicalName() + " " + sourceFilename + ":" + statementToDelete;
    }

    public static Edit fromString(String description) {
        String tokens[] = description.split("\\s+");
        String tokens2[] = tokens[1].split(":");
        String filename = tokens2[0];
        int statement = Integer.parseInt(tokens2[1]);
        return new DeleteStatement(filename, statement);
    }

}
