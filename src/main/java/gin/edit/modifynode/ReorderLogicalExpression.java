package gin.edit.modifynode;

import java.util.List;
import java.util.Random;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;

import gin.edit.ModifyNode;

/**
 * exploit short-circuiting behaviour in logical expressions by swapping order of nodes within them
 * currently just swaps within binary statements.
 * better would be to look at the whole expression and explore all possible reorderings
 * this would then capture this kind of thing: (a || b) || c
 */
public class ReorderLogicalExpression extends ModifyNode {
	private final BinaryExpr sourceNode;
	
	/**
	 * @param sourceNodes is the list of possible nodes for modification; these won't be
	 * 	      modified, just used for reference
	 * @param r is needed to choose a node and a suitable replacement 
	 *        (keeps this detail out of Patch class)
	 */
	public ReorderLogicalExpression(List<Node> sourceNodes, ReorderLogicalExpressionFactory factory, Random r) {
		super(sourceNodes, factory, r);
		
		// just used for toString and debugging...
		sourceNode = (BinaryExpr)(this.sourceNodes.get(sourceNodeIndex));
	}
	
	@Override
	public boolean apply(CompilationUnit cu) {
		// first, get the list of nodes from the new cu
		List<Node> nodes = this.factory.appliesToNodes(cu);
		
		Node node = nodes.get(sourceNodeIndex);

		Expression left = ((BinaryExpr)node).getLeft();
		Expression right = ((BinaryExpr)node).getRight();
		
		((BinaryExpr)node).setLeft(right);
		((BinaryExpr)node).setRight(left);
		
		return true;
	}
	
	@Override
	public String toString() {
		return super.toString() + " swapping child nodes in [" + sourceNode + "] to " + sourceNode.getRight() + "," + sourceNode.getLeft();
	}
}
