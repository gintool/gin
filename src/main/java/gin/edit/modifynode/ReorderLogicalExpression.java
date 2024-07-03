package gin.edit.modifynode;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.Expression;
import gin.SourceFile;
import gin.SourceFileTree;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * exploit short-circuiting behaviour in logical expressions by swapping order of nodes within them
 * currently just swaps within binary statements.
 * better would be to look at the whole expression and explore all possible reorderings
 * this would then capture this kind of thing: (a || b) || c
 */
public class ReorderLogicalExpression extends ModifyNodeEdit {

    @Serial
    private static final long serialVersionUID = 5174511673772025047L;
    private final int targetNode;

    /**
     * @param sourceFile to create an edit for
     * @param rng        random number generator, used to choose the target statements
     * @throws NoApplicableNodesException if no suitable nodes found
     */
    // @param sourceNodes is the list of possible nodes for modification; these won't be
    //           modified, just used for reference
    // @param r is needed to choose a node and a suitable replacement
    //        (keeps this detail out of Patch class)
    public ReorderLogicalExpression(SourceFile sourceFile, Random rng) throws NoApplicableNodesException {
        SourceFileTree sf = (SourceFileTree) sourceFile;

        // get the static list of nodes that might be applicable (the BinaryStatements)
        // then look for the ones that have AND OR operators 
        List<Integer> nodes = new ArrayList<>();

        for (Integer i : sf.getNodeIDsByClass(true, BinaryExpr.class)) {
            BinaryExpr n = (BinaryExpr) (sf.getNode(i));
            if ((n.getOperator() == Operator.AND) || (n.getOperator() == Operator.OR)) {
                nodes.add(i);
            }
        }

        if (nodes.isEmpty()) {
            throw new NoApplicableNodesException();
        }

        this.targetNode = nodes.get(rng.nextInt(nodes.size()));
    }

    @Override
    public SourceFile apply(SourceFile sourceFile, Object metadata) {
        SourceFileTree sf = (SourceFileTree) sourceFile;
        Node node = sf.getNode(this.targetNode);

        if (node != null) {
            Expression left = ((BinaryExpr) node).getLeft();
            Expression right = ((BinaryExpr) node).getRight();
            ((BinaryExpr) node).setLeft(right);
            ((BinaryExpr) node).setRight(left);
            sf = sf.replaceNode(this.targetNode, node);
        }
        return sf;
    }

    @Override
    public String toString() {
        return super.toString() + " swapping child nodes in [" + this.targetNode + "]";
    }
}
