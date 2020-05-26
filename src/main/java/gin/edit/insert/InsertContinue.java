package gin.edit.insert;

import java.util.List;
import java.util.Random;

import com.github.javaparser.ast.stmt.ContinueStmt;

import gin.SourceFile;
import gin.SourceFileTree;
import gin.edit.Edit;

/**
 * The C operator from this paper: 
 * Brownlee AEI, Petke J & Rasburn AF (2020)
 * Injecting Shortcuts for Faster Running Java Code
 * In: IEEE World Congress on Computational Intelligence, Glasgow, 19.07.2020-24.07.2020
 * Piscataway, NJ, USA: IEEE. https://wcci2020.org/
 */
public class InsertContinue extends InsertStatementEdit {

    public String destinationFilename;
    public int destinationBlock;
    public int destinationChildInBlock;

    /** 
     * create a random {@link InsertContinue} for the given sourcefile, using the provided RNG
     * @param sourceFile to create an edit for
     * @param rng random number generator, used to choose the target statements
     * */
    public InsertContinue(SourceFile sourceFile, Random rng) {
        SourceFileTree sf = (SourceFileTree)sourceFile;
        List<Integer> targetMethodBlocks = sf.getBlockIDsInTargetMethod();
        int insertBlock = targetMethodBlocks.get(rng.nextInt(targetMethodBlocks.size()));
        int insertStatementID = sf.getRandomInsertPointInBlock(insertBlock, rng);
        if (insertStatementID < 0) {
            insertStatementID = 0; // insert at start of empty block
        }
        
        this.destinationFilename = sourceFile.getFilename();
        this.destinationBlock = insertBlock;
        this.destinationChildInBlock = insertStatementID;
    }
    
    /**
     * @param sourceFile - filename containing source statement
     * @param destinationFile - filename containing destination statement
     * @param destinationBlockID - ID of destination block
     * @param destinationChildInBlockID - ID of child in destination block (the 
     *          statement will be inserted to immediately before the first ID 
     *          greater than this number; if the ID is the same as the block ID
     *          that target is the start of the block; if the target ID was 
     *          deleted the statement will go where the target used to be; 
     *          if the ID exists the statement will go after it; 
     *          if multiple statements are inserted here, they will be inserted in order)
     */
    public InsertContinue(String destinationFile, int destinationBlockID, int destinationChildInBlockID) {
        this.destinationFilename = destinationFile;
        this.destinationBlock = destinationBlockID;
        this.destinationChildInBlock = destinationChildInBlockID;
    }
    
    @Override
    public SourceFile apply(SourceFile sourceFile) {
        
        SourceFileTree sf = (SourceFileTree)sourceFile;

        ContinueStmt toInsert = new ContinueStmt();
        toInsert.removeLabel(); // a bit weird but if we don't do this we get "break empty;"
        
        // insertStatement will also just do nothing if the destination block is deleted
        sf = sf.insertStatement(destinationBlock, destinationChildInBlock, toInsert);
        
        return sf;
    }

    @Override
    public String toString() {
        return this.getClass().getCanonicalName() + " " + destinationFilename + ":" + destinationBlock + ":" + destinationChildInBlock;
    }

    public static Edit fromString(String description) {
        String tokens[] = description.split("\\s+");
        String destination = tokens[1];
        String destTokens[] = destination.split(":");
        String destFile = destTokens[0];
        int destBlock = Integer.parseInt(destTokens[1]);
        int destLine = Integer.parseInt(destTokens[2]);
        return new InsertContinue(destFile, destBlock, destLine);
    }

}
