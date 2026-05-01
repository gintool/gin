package gin.edit.insert;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.PrimitiveType.Primitive;
import gin.SourceFile;
import gin.SourceFileTree;
import gin.SourceFileTree.VariableTypeAndName;
import gin.edit.Edit;

import java.io.Serial;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The B_{if} operator from this paper:
 * Brownlee AEI, Petke J and Rasburn AF (2020)
 * Injecting Shortcuts for Faster Running Java Code
 * In: IEEE World Congress on Computational Intelligence, Glasgow, 19.07.2020-24.07.2020
 * Piscataway, NJ, USA: IEEE. <a href="https://wcci2020.org/">...</a>
 */
public class InsertBreakWithIf extends InsertStatementEdit {

    @Serial
    private static final long serialVersionUID = -8158634873685044341L;
    static Pattern p = Pattern.compile("\\[if \\((.*)\\)\\s+break;\\]");
    private final Statement toInsert;
    public String destinationFilename;
    public int destinationBlock;
    public int destinationChildInBlock;

    /**
     * create a random {@link InsertBreakWithIf} for the given sourcefile, using the provided RNG
     *
     * @param sourceFile to create an edit for
     * @param rng        random number generator, used to choose the target statements
     */
    public InsertBreakWithIf(SourceFile sourceFile, Random rng) {
        SourceFileTree sf = (SourceFileTree) sourceFile;
        List<Integer> targetMethodBlocks = sf.getBlockIDsInTargetMethod();
        int insertBlock = targetMethodBlocks.get(rng.nextInt(targetMethodBlocks.size()));
        int insertStatementID = sf.getRandomInsertPointInBlock(insertBlock, rng);
        if (insertStatementID < 0) {
            insertStatementID = 0; // insert at start of empty block
        }

        this.destinationFilename = sourceFile.getRelativePathToWorkingDir();
        this.destinationBlock = insertBlock;
        this.destinationChildInBlock = insertStatementID;

        BreakStmt stmt = new BreakStmt();
        stmt.removeLabel(); // a bit weird but if we don't do this we get "break empty;"

        // get vars in scope
        List<VariableTypeAndName> vds = sf.getPrimitiveVariablesInScopeForStatement(insertStatementID);
        if (!vds.isEmpty()) {
            // choose one
            VariableTypeAndName v = vds.get(rng.nextInt(vds.size()));
            // build a comparison
            Expression condition;
            if (v.getType().asPrimitiveType().getType() == Primitive.BOOLEAN) {
                // if a boolean, decide whether we want true or false
                if (rng.nextBoolean()) {
                    condition = new NameExpr(v.getName());
                } else {
                    condition = new UnaryExpr(new NameExpr(v.getName()), com.github.javaparser.ast.expr.UnaryExpr.Operator.LOGICAL_COMPLEMENT);
                }
            } else {
                // otherwise, for now, we just want to compare to zero
                Operator operator = new Operator[]{Operator.LESS,
                        Operator.LESS_EQUALS,
                        Operator.EQUALS,
                        Operator.GREATER_EQUALS,
                        Operator.GREATER,
                }[rng.nextInt(5)];
                condition = new BinaryExpr(new NameExpr(v.getName()), new IntegerLiteralExpr(String.valueOf(0)), operator);
            }

            IfStmt ifs = new IfStmt();
            ifs.setCondition(condition);
            ifs.setThenStmt(stmt);
            toInsert = ifs;
        } else {
            toInsert = stmt; // no vars in scope? add a plain return.
        }
    }

    /**
     * @param stmt                      - JP statement to insert
     * @param destinationFile           - filename containing destination statement
     * @param destinationBlockID        - ID of destination block
     * @param destinationChildInBlockID - ID of child in destination block (the
     *                                  statement will be inserted to immediately before the first ID
     *                                  greater than this number; if the ID is the same as the block ID
     *                                  that target is the start of the block; if the target ID was
     *                                  deleted the statement will go where the target used to be;
     *                                  if the ID exists the statement will go after it;
     *                                  if multiple statements are inserted here, they will be inserted in order)
     */
    public InsertBreakWithIf(Statement stmt, String destinationFile, int destinationBlockID, int destinationChildInBlockID) {
        this.toInsert = stmt;
        this.destinationFilename = destinationFile;
        this.destinationBlock = destinationBlockID;
        this.destinationChildInBlock = destinationChildInBlockID;
    }

    public static Edit fromString(String description) {
        Statement stmt;

        Matcher m = p.matcher(description);

        if (m.find()) { // i.e. an "if", not a plain break;
            String strStatement = m.group(1);

            Expression condition;
            // if the statement starts with !, we've got a if (!var)
            if (strStatement.startsWith("!")) {
                strStatement = strStatement.substring(1);
                condition = new UnaryExpr(new NameExpr(strStatement), com.github.javaparser.ast.expr.UnaryExpr.Operator.LOGICAL_COMPLEMENT);
            } else if (!strStatement.matches("\\s+")) { // no whitespace
                // if the statement contains only a variable name, we've got a if (var)
                condition = new NameExpr(strStatement);
            } else {
                // otherwise it's if (var ?? 0)
                String[] condSplit = strStatement.split("\\s+");
                Operator o = Operator.valueOf(condSplit[1]);
                condition = new BinaryExpr(new NameExpr(condSplit[0]), new IntegerLiteralExpr(String.valueOf(0)), o);
            }

            BreakStmt bstmt = new BreakStmt();
            bstmt.removeLabel();
            IfStmt ifs = new IfStmt();
            ifs.setCondition(condition);
            ifs.setThenStmt(bstmt);
            stmt = ifs;
        } else {
            BreakStmt bstmt = new BreakStmt();
            bstmt.removeLabel();
            stmt = bstmt;
        }

        String[] tokens = description.split("\\s+");
        String destination = tokens[1];
        String[] destTokens = destination.split(":");
        String destFile = destTokens[0];
        int destBlock = Integer.parseInt(destTokens[1]);
        int destLine = Integer.parseInt(destTokens[2]);


        return new InsertBreakWithIf(stmt, destFile, destBlock, destLine);
    }

    @Override
    public SourceFile apply(SourceFile sourceFile, Object metadata) {

        SourceFileTree sf = (SourceFileTree) sourceFile;

        // insertStatement will also just do nothing if the destination block is deleted
        sf = sf.insertStatement(destinationBlock, destinationChildInBlock, toInsert);

        return sf;
    }

    @Override
    public String toString() {
        return this.getClass().getCanonicalName() + " " + destinationFilename + ":" + destinationBlock + ":" + destinationChildInBlock + " [" + toInsert.toString().replace("\n", "") + "]";
    }

}
