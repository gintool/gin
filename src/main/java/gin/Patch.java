package gin;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import gin.edit.CopyStatement;
import gin.edit.DeleteStatement;
import gin.edit.Edit;
import gin.edit.MoveStatement;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Patch {

    private LinkedList<Edit> edits = new LinkedList<>();
    private Program program;

    public Patch(Program program) {
        this.program = program;
    }

    // We deliberately don't try to separate concerns here, keep it simple.
    public void apply(CompilationUnit compilationUnit) {

        List<Statement> allStatements = compilationUnit.getNodesByType(Statement.class);
        List<BlockStmt> blocks = compilationUnit.getNodesByType(BlockStmt.class);

        List<Statement> toDelete = new LinkedList<>();
        List<InsertionPair> insertions = new LinkedList<InsertionPair>();

        for (Edit edit: edits) {

            if (edit instanceof DeleteStatement) {

                toDelete.add(allStatements.get(((DeleteStatement)edit).statementToDelete));

            } else if (edit instanceof MoveStatement) {

                MoveStatement move = (MoveStatement)edit;
                Statement source = allStatements.get(move.sourceStatement);
                Statement target = blocks.get(move.destinationBlock).getStatement(move.destinationChildInBlock);
                InsertionPair insertion = new InsertionPair(source, target);
                insertions.add(insertion);
                toDelete.add(allStatements.get(((MoveStatement)edit).sourceStatement));

            } else if (edit instanceof CopyStatement) {

                CopyStatement copy = (CopyStatement)edit;
                Statement source = allStatements.get(copy.sourceStatement);
                Statement target = blocks.get(copy.destinationBlock).getStatement(copy.destinationChildInBlock);
                InsertionPair insertion = new InsertionPair(source, target);
                insertions.add(insertion);

            }

        }

        for (InsertionPair pair: insertions) {
            Statement source = pair.statementToInsert.clone();
            Node parent = pair.insertionPoint.getParentNode().get();
            BlockStmt parentBlock = (BlockStmt) parent;
            int indexInParent = parentBlock.getChildNodes().indexOf(pair.insertionPoint);
            parentBlock.addStatement(indexInParent, source);
        }

        for (Statement statement: toDelete) {
            statement.remove(); // Not guaranteed to work if violate some constraints.
        }

    }

    public static Patch randomPatch(Program program, Random random, int maxLength) {

        int length = random.nextInt(maxLength-1) + 1; // range from 1..maxLength
        Patch patch = new Patch(program);

        for (int i=0; i < length; i++) {

            int editType = random.nextInt(3);
            Edit edit = null;

            switch (editType) {
                case (0):
                    int statementToDelete = random.nextInt(program.getStatementCount());
                    edit = new DeleteStatement(statementToDelete);
                    break;
                case (1):
                    int statementToCopy = random.nextInt(program.getStatementCount());
                    int insertBlock = random.nextInt(program.getBlockCount());
                    int insertStatement = random.nextInt(program.getBlockSize(insertBlock));
                    edit = new CopyStatement(statementToCopy, insertBlock, insertStatement);
                    break;
                case (2):
                    int statementToMove = random.nextInt(program.getStatementCount());
                    int moveBlock = random.nextInt(program.getBlockCount());
                    int moveStatement = random.nextInt(program.getBlockSize(moveBlock));
                    edit = new MoveStatement(statementToMove, moveBlock, moveStatement);
                    break;
            }

            patch.edits.add(edit);

        }

        return patch;

    }

    @Override
    public String toString() {
        String description = "| ";
        for (Edit edit: edits) {
            description += edit.toString() + " | ";
        }
        return description;
    }

    private class InsertionPair {

        public Statement statementToInsert;
        public Statement insertionPoint;

        public InsertionPair(Statement statementToInsert, Statement insertionPoint) {
            this.statementToInsert = statementToInsert;
            this.insertionPoint = insertionPoint;
        }

    }

}

