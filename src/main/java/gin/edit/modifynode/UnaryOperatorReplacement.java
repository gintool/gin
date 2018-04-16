package gin.edit.modifynode;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.UnaryExpr.Operator;

import gin.edit.ModifyNode;

public class UnaryOperatorReplacement extends ModifyNode {
	private final Operator source;
	private final Operator replacement;
	
	// the following list is not perfect! needs refinement, if not total replacement with a better way to do this
	// also missing removal/insertion of these - e.g. where we have a complement, there isn't a "no-complement" operator, 
	// so instead we have to look for nodes with/without a complement and possibly delete/insert 
	private static final Map<Operator, List<Operator>> REPLACEMENTS = new LinkedHashMap<>();
	static {
		REPLACEMENTS.put(Operator.BITWISE_COMPLEMENT, Arrays.asList(Operator.LOGICAL_COMPLEMENT, Operator.MINUS, Operator.PLUS));
		REPLACEMENTS.put(Operator.LOGICAL_COMPLEMENT, Arrays.asList(Operator.BITWISE_COMPLEMENT, Operator.MINUS, Operator.PLUS));
		REPLACEMENTS.put(Operator.MINUS, Arrays.asList(Operator.BITWISE_COMPLEMENT, Operator.LOGICAL_COMPLEMENT, Operator.PLUS));
		REPLACEMENTS.put(Operator.PLUS, Arrays.asList(Operator.BITWISE_COMPLEMENT, Operator.LOGICAL_COMPLEMENT, Operator.MINUS));
		
		REPLACEMENTS.put(Operator.POSTFIX_DECREMENT, Arrays.asList(Operator.POSTFIX_INCREMENT, Operator.PREFIX_DECREMENT, Operator.PREFIX_INCREMENT));
		REPLACEMENTS.put(Operator.POSTFIX_INCREMENT, Arrays.asList(Operator.POSTFIX_DECREMENT, Operator.PREFIX_DECREMENT, Operator.PREFIX_INCREMENT));
		REPLACEMENTS.put(Operator.PREFIX_DECREMENT, Arrays.asList(Operator.POSTFIX_DECREMENT, Operator.POSTFIX_INCREMENT, Operator.PREFIX_INCREMENT));
		REPLACEMENTS.put(Operator.PREFIX_INCREMENT, Arrays.asList(Operator.POSTFIX_DECREMENT, Operator.POSTFIX_INCREMENT, Operator.PREFIX_DECREMENT));		
	}
	
	/**
	 * @param sourceNodes is the list of possible nodes for modification; these won't be
	 * 	      modified, just used for reference
	 * @param r is needed to choose a node and a suitable replacement 
	 *        (keeps this detail out of Patch class)
	 */
	public UnaryOperatorReplacement(List<Node> sourceNodes, UnaryOperatorReplacementFactory factory, Random r) {
		super(sourceNodes, factory, r);
		
		UnaryExpr sourceNode = (UnaryExpr)(this.sourceNodes.get(sourceNodeIndex));
		
		this.source = sourceNode.getOperator();
		this.replacement = chooseRandomReplacement(source, r);
	}
	
	@Override
	public boolean apply(CompilationUnit cu) {
		// first, get the list of nodes from the new cu
		List<Node> nodes = this.factory.appliesToNodes(cu);
		
		Node node = nodes.get(sourceNodeIndex);
		
		((UnaryExpr)node).setOperator(replacement);
		
		return true;
	}
	
	private static Operator chooseRandomReplacement(Operator original, Random r) {
		Operator replacement;
		/*
		do {
			replacement = Operator.values()[r.nextInt(Operator.values().length)];
		} while (replacement.equals(original));
		*/
		List<Operator> l = REPLACEMENTS.get(original);
		replacement = l.get(r.nextInt(l.size()));
		
		return replacement;
	}
	
	@Override
	public String toString() {
		return super.toString() + " [" + source + " -> " + replacement + "]";
	}
}
