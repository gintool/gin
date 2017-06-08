package gin;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
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

public class Patch {

    private LinkedList<Edit> edits = new LinkedList<>();
    private SourceFile sourceFilename;

    public Patch(SourceFile sourceFilename) {
        this.sourceFilename = sourceFilename;
    }

    public Patch clone() {
        Patch clonePatch = new Patch(this.sourceFilename);
        clonePatch.edits = (LinkedList<Edit>)(this.edits.clone());
        return clonePatch;
    }

    public void add(Edit edit) {
        this.edits.add(edit);
    }

    public int size() {
        return this.edits.size();
    }

    public void remove(int index) {
        this.edits.remove(index);
    }

    /**
     * Apply this patch to the source file.
     * @return a new SourceFile object representing the patched source code.
     */
    public SourceFile apply() {

        CompilationUnit patchedCompilationUnit = sourceFilename.getCompilationUnit().clone();

        List<Statement> allStatements = patchedCompilationUnit.getNodesByType(Statement.class);
        List<BlockStmt> blocks = patchedCompilationUnit.getNodesByType(BlockStmt.class);

        List<Statement> toDelete = new LinkedList<>();
        List<Insertion> insertions = new LinkedList<Insertion>();


        for (Edit edit: edits) {

            if (edit instanceof DeleteStatement) {

                toDelete.add(allStatements.get(((DeleteStatement)edit).statementToDelete));

            } else if (edit instanceof MoveStatement) {

                MoveStatement move = (MoveStatement)edit;
                Statement source = allStatements.get(move.sourceStatement);
                Statement target = null;
                BlockStmt parent = blocks.get(move.destinationBlock);
                if (move.destinationChildInBlock != 0) {
                    target = parent.getStatement(move.destinationChildInBlock-1);
                }
                Insertion insertion = new Insertion(source, target, parent);
                insertions.add(insertion);
                toDelete.add(allStatements.get(((MoveStatement)edit).sourceStatement));

            } else if (edit instanceof CopyStatement) {

                CopyStatement copy = (CopyStatement)edit;
                Statement source = allStatements.get(copy.sourceStatement);
                Statement target = null;
                BlockStmt parent = blocks.get(copy.destinationBlock);
                if (copy.destinationChildInBlock != 0) {
                    target = parent.getStatement(copy.destinationChildInBlock-1);
                }
                Insertion insertion = new Insertion(source, target, parent);
                insertions.add(insertion);

            }

        }

        for (Insertion insertion: insertions) {
            Statement source = insertion.statementToInsert.clone();
            int indexInParent;
            if (insertion.insertionPoint == null) {
                indexInParent = 0;
            } else {
                indexInParent = insertion.insertionPointParent.getChildNodes().indexOf(insertion.insertionPoint);
            }
            insertion.insertionPointParent.addStatement(indexInParent, source);
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

    public static Patch randomPatch(SourceFile sourceFile, Random rng, int maxLength) {

        int length = rng.nextInt(maxLength-1) + 1; // range from 1..maxLength
        Patch patch = new Patch(sourceFile);

        for (int i=0; i < length; i++) {
            patch.addRandomEdit(rng);
        }

        return patch;

    }

    private Edit randomEdit(Random rng) {

        Edit edit = null;

        int editType = rng.nextInt(3);

        switch (editType) {
            case (0):
                int statementToDelete = rng.nextInt(sourceFilename.getStatementCount());
                edit = new DeleteStatement(statementToDelete);
                break;
            case (1):
                int statementToCopy = rng.nextInt(sourceFilename.getStatementCount());
                int insertBlock = rng.nextInt(sourceFilename.getNumberOfBlocks());
                int insertStatement = rng.nextInt(sourceFilename.getNumberOfInsertionPointsInBlock(insertBlock));
                edit = new CopyStatement(statementToCopy, insertBlock, insertStatement);
                break;
            case (2):
                int statementToMove = rng.nextInt(sourceFilename.getStatementCount());
                int moveBlock = rng.nextInt(sourceFilename.getNumberOfBlocks());
                int moveStatement = rng.nextInt(sourceFilename.getNumberOfInsertionPointsInBlock(moveBlock));
                edit = new MoveStatement(statementToMove, moveBlock, moveStatement);
                break;
        }

        return edit;

    }

    public void addRandomEdit(Random rng) {
        this.edits.add(randomEdit(rng));
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
        return description;
    }

    private class Insertion {

        public Statement statementToInsert;
        public Statement insertionPoint;
        public BlockStmt insertionPointParent;

        public Insertion(Statement statementToInsert, Statement insertionPoint, BlockStmt insertionPointParent) {
            this.statementToInsert = statementToInsert;
            this.insertionPoint = insertionPoint;
            this.insertionPointParent = insertionPointParent;
        }

    }

}

