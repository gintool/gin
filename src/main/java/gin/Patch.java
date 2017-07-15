package gin;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import gin.edit.CopyStatement;
import gin.edit.DeleteStatement;
import gin.edit.Edit;
import gin.edit.MoveStatement;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

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

        Edit edit = null;

        int editType = rng.nextInt(3);

        switch (editType) {
            case (0):
                int statementToDelete = rng.nextInt(sourceFile.getStatementCount());
                edit = new DeleteStatement(statementToDelete);
                break;
            case (1):
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
            case (2):
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

