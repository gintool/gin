package gin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

import gin.edit.CopyStatement;
import gin.edit.DeleteStatement;
import gin.edit.Edit;
import gin.edit.ModifyNode;
import gin.edit.ModifyNodeFactory;
import gin.edit.MoveStatement;
import gin.edit.modifynode.BinaryOperatorReplacementFactory;
import gin.edit.modifynode.UnaryOperatorReplacement;
import gin.edit.modifynode.UnaryOperatorReplacementFactory;

/**
 * Represents a patch, a potential set of changes to a sourcefile.
 */
public class Patch {

    protected LinkedList<Edit> edits = new LinkedList<>();
    protected SourceFile sourceFile;

    public Patch(SourceFile sourceFile) {
        this.sourceFile = sourceFile;
    }

    public Patch clone() {
        Patch clonePatch = new Patch(this.sourceFile);
        clonePatch.edits = (LinkedList<Edit>)(this.edits.clone());
        return clonePatch;
    }

    public int size() {
        return this.edits.size();
    }

    public void add(Edit edit) {
        this.edits.add(edit);
    }

    public void remove(int index) {
        this.edits.remove(index);
    }

    /**
     * Apply this patch to the source file.
     * @return a new SourceFile object representing the patched source code.
     */
    public SourceFile apply() {

        /**
         * Helper class used in applying a patch.
         */
        class Insertion {

            Statement statementToInsert;
            int insertionPoint;
            BlockStmt insertionPointParent;

            Insertion(Statement statementToInsert, int insertionPoint, BlockStmt insertionPointParent) {
                this.statementToInsert = statementToInsert;
                this.insertionPoint = insertionPoint;
                this.insertionPointParent = insertionPointParent;
            }

        }

        CompilationUnit patchedCompilationUnit = sourceFile.getCompilationUnit().clone();

        List<Statement> allStatements = patchedCompilationUnit.getNodesByType(Statement.class);
        List<BlockStmt> blocks = patchedCompilationUnit.getNodesByType(BlockStmt.class);

        List<Statement> toDelete = new LinkedList<>();
        List<Insertion> insertions = new LinkedList<>();


        for (Edit edit: edits) {

            if (edit instanceof DeleteStatement) {

                toDelete.add(allStatements.get(((DeleteStatement)edit).statementToDelete));

            } else if (edit instanceof MoveStatement) {

                MoveStatement move = (MoveStatement)edit;
                Statement source = allStatements.get(move.sourceStatement);
                int targetStatementIndex;
                BlockStmt parent = blocks.get(move.destinationBlock);

                if (parent.isEmpty()) {
                    targetStatementIndex = 0;
                } else {
                    targetStatementIndex = move.destinationChildInBlock;
                }

                Insertion insertion = new Insertion(source, targetStatementIndex, parent);
                insertions.add(insertion);
                toDelete.add(allStatements.get(((MoveStatement)edit).sourceStatement));

            } else if (edit instanceof CopyStatement) {

                CopyStatement copy = (CopyStatement)edit;
                Statement source = allStatements.get(copy.sourceStatement);
                int targetStatementIndex;
                BlockStmt parent = blocks.get(copy.destinationBlock);

                if (parent.isEmpty()) {
                    targetStatementIndex = 0;
                } else {
                    targetStatementIndex = copy.destinationChildInBlock;
                }

                Insertion insertion = new Insertion(source, targetStatementIndex, parent);
                insertions.add(insertion);

            } else if (edit instanceof ModifyNode) {
            	// each of these has an internal list of nodes and an index
            	// the change will not affect the structure so we can
            	// make the patch without a ConcurrentModificationException
            	// Just supply the new source, and ask them to make the change.
            	@SuppressWarnings("unused") // currently ignore the result
            	boolean result = ((ModifyNode)edit).apply(patchedCompilationUnit);
            }

        }

        for (Insertion insertion: insertions) {
            Statement source = insertion.statementToInsert.clone();
            insertion.insertionPointParent.addStatement(insertion.insertionPoint, source);
        }

        boolean removedOK = true;
        for (Statement statement: toDelete) {
            removedOK &= statement.remove(); // Not guaranteed to work if violate some constraints.
        }

        if (removedOK) {
            return new SourceFile(patchedCompilationUnit);
        } else {
            return null;
        }

    }

    public void addRandomEdit(Random rng) {
        this.edits.add(randomEdit(rng));
    }

    private Edit randomEdit(Random rng) {
    	// the following factory stuff might be better elsewhere?
    	
    	// separate factories needed for different modifyNode operators
    	BinaryOperatorReplacementFactory lorFactory = new BinaryOperatorReplacementFactory(sourceFile.getCompilationUnit());
    	UnaryOperatorReplacementFactory uorFactory = new UnaryOperatorReplacementFactory(sourceFile.getCompilationUnit());
    	// ...
    	
    	// not all modifiers will be applicable! Check which are and only consider those.
    	List<ModifyNodeFactory> mnfs = new ArrayList<>();
    	if (!lorFactory.getSourceNodes().isEmpty()) {
    		mnfs.add(lorFactory);
    	}
    	if (!uorFactory.getSourceNodes().isEmpty()) {
    		mnfs.add(uorFactory);
    	} 
    	
        Edit edit = null;

        int editType = rng.nextInt(3 + mnfs.size());

        switch (editType) {
            case (0): // delete statement
                int statementToDelete = rng.nextInt(sourceFile.getStatementCount());
                edit = new DeleteStatement(statementToDelete);
                break;
            case (1): // copy statement
                int statementToCopy = rng.nextInt(sourceFile.getStatementCount());
                int insertBlock = rng.nextInt(sourceFile.getNumberOfBlocks());
                int numberOfInsertionPoints = sourceFile.getNumberOfInsertionPointsInBlock(insertBlock);
                int insertStatement;
                if (numberOfInsertionPoints == 0) {
                    insertStatement = 0; // insert at start of empty block
                } else {
                    insertStatement = rng.nextInt(numberOfInsertionPoints);
                }
                edit = new CopyStatement(statementToCopy, insertBlock, insertStatement);
                break;
            case (2): // move statement
                int statementToMove = rng.nextInt(sourceFile.getStatementCount());
                int moveBlock = rng.nextInt(sourceFile.getNumberOfBlocks());
                int numberOfDestinationPoints = sourceFile.getNumberOfInsertionPointsInBlock(moveBlock);
                int movePoint;
                if (numberOfDestinationPoints == 0) {
                    movePoint = 0; // insert at start of empty block
                } else {
                    movePoint = rng.nextInt(numberOfDestinationPoints);
                }
                edit = new MoveStatement(statementToMove, moveBlock, movePoint);
                break;
            default: // modify statement
            	// binary operators (was originally just logical operators)
            	// could do some checking for type here by calling ModifyNodeFactory.applicability().appliesTo()
            	// just to be sure there are some nodes that can be changed by a particular operator!
            
            	// also unary operators
            	
            	// will also need a random choice between different modifications...
            	// see https://github.com/gintool/gin/issues/13#issuecomment-342489660
            	
            	// which factory do we want? Subtract 3 from index (for the 3 edit types above)
            	ModifyNodeFactory mnf = mnfs.get(editType - 3);
            	edit = mnf.newModifier(rng);
            	break;
        }

        return edit;

    }

    public void writePatchedSourceToFile(String filename) {

        // Apply this patch
        SourceFile patchedSourceFile = this.apply();

        try {
            FileUtils.writeStringToFile(new File(filename), patchedSourceFile.getSource(), Charset.defaultCharset());
        } catch (IOException e) {
            System.err.println("Exception writing source code of patched program to: " + filename);
            e.printStackTrace();
            System.exit(-1);
        }

    }

    @Override
    public String toString() {
        String description = "| ";
        for (Edit edit: edits) {
            description += edit.toString() + " | ";
        }
        return description.trim();
    }


}

