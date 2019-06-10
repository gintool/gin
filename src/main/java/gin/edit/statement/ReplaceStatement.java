package gin.edit.statement;

import java.util.Random;

import com.github.javaparser.ast.stmt.Statement;

import gin.SourceFile;
import gin.SourceFileTree;
import gin.edit.Edit;

public class ReplaceStatement extends StatementEdit {

    public String sourceFilename;
    public int sourceStatement;
    public String destinationFilename;
    public int destinationStatement;

    /** 
     * create a random replacestatement for the given sourcefile, using the provided RNG
     * @param sourceFile to create an edit for
     * @param rng random number generator, used to choose the target statements
     * */
    public ReplaceStatement(SourceFile sourceFile, Random rng) {
        SourceFileTree sf = (SourceFileTree)sourceFile;
        
        sourceFilename = sourceFile.getFilename();
        destinationFilename = sourceFile.getFilename();
        
        // source can be anywhere in the class
        sourceStatement = sf.getRandomStatementID(false, rng);
        
        // target is in target method only
        destinationStatement = sf.getRandomStatementID(true, rng);
    }
    
    public ReplaceStatement(String sourceFilename, int sourceStatement, String destinationFilename, int destinationStatement) {
        this.sourceFilename = sourceFilename;
        this.sourceStatement = sourceStatement;
        this.destinationFilename = destinationFilename;
        this.destinationStatement = destinationStatement;
    }
    
    @Override
    public SourceFile apply(SourceFile sourceFile) {
        
        if (sourceStatement == destinationStatement) {
            return sourceFile; // no-op
        }
        
        SourceFileTree sf = (SourceFileTree)sourceFile;
        
        Statement source = sf.getStatement(sourceStatement);
        Statement destination = sf.getStatement(destinationStatement);
        
        if ((source == null) || (destination == null)) {
            return sf; // targeting a deleted location just does nothing.
        }
        
        
        // we clone the replacement node, so we don't end up getting confused between the two (that would prevent us replacing statements within the same parent node)
        try {
            return sf.replaceNode(destinationStatement, source.clone());
        } catch (ClassCastException e) { // JavaParser sometimes throws this if the statements don't match
            return sf;
        }
    }

    @Override
    public String toString() {
        return this.getClass().getCanonicalName() + " " + sourceFilename + ":" + sourceStatement + " -> " + destinationFilename + ":" + destinationStatement;
    }

    public static Edit fromString(String description) {
        String tokens[] = description.split("\\s+");
        String srcTokens[] = tokens[1].split(":");
        String srcFilename = srcTokens[0];
        int source = Integer.parseInt(srcTokens[1]);
        String destTokens[] = tokens[3].split(":");
        String destFilename = destTokens[0];
        int destination = Integer.parseInt(destTokens[1]);
        return new ReplaceStatement(srcFilename, source, destFilename, destination);
    }
    
}
