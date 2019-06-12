package gin.edit.statement;

import java.util.List;
import java.util.Random;

import com.github.javaparser.ast.stmt.Statement;

import gin.SourceFile;
import gin.SourceFileTree;
import gin.edit.Edit;

public class CopyStatement extends StatementEdit {

    public String sourceFilename;
    public int sourceStatement;
    public String destinationFilename;
    public int destinationBlock;
    public int destinationChildInBlock;

    /** 
     * create a random copystatement for the given sourcefile, using the provided RNG
     * @param sourceFile to create an edit for
     * @param rng random number generator, used to choose the target statements
     * */
    public CopyStatement(SourceFile sourceFile, Random rng) {
        SourceFileTree sf = (SourceFileTree)sourceFile;
        int statementToCopy = sf.getRandomStatementID(false, rng);
        List<Integer> targetMethodBlocks = sf.getBlockIDsInTargetMethod();
        int insertBlock = targetMethodBlocks.get(rng.nextInt(targetMethodBlocks.size()));
        int insertStatementID = sf.getRandomInsertPointInBlock(insertBlock, rng);
        if (insertStatementID < 0) {
            insertStatementID = 0; // insert at start of empty block
        }
        
        this.sourceFilename = sourceFile.getFilename();
        this.sourceStatement = statementToCopy;
        this.destinationFilename = sourceFile.getFilename();
        this.destinationBlock = insertBlock;
        this.destinationChildInBlock = insertStatementID;
    }
    
    /**
     * @param sourceFile - filename containing source statement
     * @param sourceStatement - ID of source statement
     * @param destinationFile - filename containing destination statement
     * @param destinationBlockID - ID of destination block
     * @param destinationChildInBlockID - ID of child in destination block (the 
     *          statement will be copied to immediately before the first ID 
     *          greater than this number; if the ID is the same as the block ID
     *          that target is the start of the block; if the target ID was 
     *          deleted the statement will go where the target used to be; 
     *          if the ID exists the statement will go after it; 
     *          if multiple statements are copied here, they will be inserted in order)
     */
    public CopyStatement(String sourceFile, int sourceStatement, String destinationFile, int destinationBlockID, int destinationChildInBlockID) {
        this.sourceFilename = sourceFile;
        this.sourceStatement = sourceStatement;
        this.destinationFilename = destinationFile;
        this.destinationBlock = destinationBlockID;
        this.destinationChildInBlock = destinationChildInBlockID;
    }
    
    @Override
    public SourceFile apply(SourceFile sourceFile) {

        // no check for source==destination here as it will copy+insert in location
        
        SourceFileTree sf = (SourceFileTree)sourceFile;
        
        Statement source = sf.getStatement(sourceStatement);
         
        if (source == null) {
            return sf; // targeting a deleted location just does nothing.
        }
        
        // insertStatement will also just do nothing if the destination block is deleted
        sf = sf.insertStatement(destinationBlock, destinationChildInBlock, source);
        
        return sf;
    }

    @Override
    public String toString() {
        return this.getClass().getCanonicalName() + " " + sourceFilename + ":" + sourceStatement + " -> " + destinationFilename + ":" + destinationBlock + ":" + destinationChildInBlock;
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
        return new CopyStatement(sourceFile, sourceStatement, destFile, destBlock, destLine);
    }

}
