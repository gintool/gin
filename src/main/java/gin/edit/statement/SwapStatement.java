package gin.edit.statement;

import com.github.javaparser.ast.stmt.Statement;
import gin.SourceFile;
import gin.SourceFileTree;
import gin.edit.Edit;

import java.io.Serial;
import java.util.Random;

public class SwapStatement extends StatementEdit {

    @Serial
    private static final long serialVersionUID = -1356499381121956722L;
    public String sourceFilename;
    public int sourceStatement;
    public String destinationFilename;
    public int destinationStatement;

    /**
     * create a random swapstatement for the given sourcefile, using the provided RNG
     *
     * @param sourceFile to create an edit for
     * @param rng        random number generator, used to choose the target statements
     */
    public SwapStatement(SourceFile sourceFile, Random rng) {

        SourceFileTree sf = (SourceFileTree) sourceFile;

        sourceFilename = sf.getRelativePathToWorkingDir();
        destinationFilename = sf.getRelativePathToWorkingDir();

        // source and target in target method only
        sourceStatement = sf.getRandomStatementID(true, rng);
        destinationStatement = sf.getRandomStatementID(true, rng);

    }

    public SwapStatement(String sourceFilename, int sourceStatement, String destinationFilename, int destinationStatement) {
        this.sourceFilename = sourceFilename;
        this.sourceStatement = sourceStatement;
        this.destinationFilename = destinationFilename;
        this.destinationStatement = destinationStatement;
    }

    public static Edit fromString(String description) {
        String[] tokens = description.split("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        String[] srcTokens = tokens[1].split(":");
        String srcFilename = srcTokens[0].replace("\"", "");
        int source = Integer.parseInt(srcTokens[1]);
        String[] destTokens = tokens[3].split(":");
        String destFilename = destTokens[0].replace("\"", "");
        int destination = Integer.parseInt(destTokens[1]);
        return new SwapStatement(srcFilename, source, destFilename, destination);
    }

    @Override
    public SourceFile apply(SourceFile sourceFile, Object metadata) {

        if (sourceStatement == destinationStatement) {
            return sourceFile; // no-op
        }

        SourceFileTree sf = (SourceFileTree) sourceFile;

        Statement source = sf.getStatement(sourceStatement);
        Statement destination = sf.getStatement(destinationStatement);

        if ((source == null) || (destination == null)) {
            return sf; // targeting a deleted location just does nothing.
        }

        try {
            // we clone the replacement nodes, so we don't end up getting confused between the two (that would prevent us swapping statements within the same parent node)
            sf = sf.replaceNode(sourceStatement, destination.clone());
            sf = sf.replaceNode(destinationStatement, source.clone());
            return sf;
        } catch (ClassCastException e) { // JavaParser sometimes throws this if the statements don't match
            return null;
        }
    }

    @Override
    public String toString() {
        return this.getClass().getCanonicalName() + " \"" + sourceFilename + "\":" + sourceStatement + " <-> \"" + destinationFilename + "\":" + destinationStatement;
    }

}
