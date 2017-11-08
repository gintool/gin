package gin.edit.modifynode;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;

import gin.edit.ModifyNode;

public class LogicalOperatorReplacement extends ModifyNode {
	private final Operator source;
	private final Operator replacement;
	
	// the following list is not perfect! needs refinement, if not total replacement with a better way to do this
	private static final Map<Operator, List<Operator>> REPLACEMENTS = new LinkedHashMap<>();
	static {
		REPLACEMENTS.put(Operator.AND, Arrays.asList(Operator.BINARY_AND, Operator.EQUALS, Operator.BINARY_OR, Operator.NOT_EQUALS, Operator.OR, Operator.XOR));
		REPLACEMENTS.put(Operator.BINARY_AND, Arrays.asList(Operator.AND, Operator.EQUALS, Operator.BINARY_OR, Operator.NOT_EQUALS, Operator.OR, Operator.XOR));
		REPLACEMENTS.put(Operator.BINARY_OR, Arrays.asList(Operator.AND, Operator.BINARY_AND, Operator.EQUALS, Operator.NOT_EQUALS, Operator.OR, Operator.XOR));
		REPLACEMENTS.put(Operator.OR, Arrays.asList(Operator.AND, Operator.BINARY_AND, Operator.EQUALS, Operator.BINARY_OR, Operator.NOT_EQUALS, Operator.XOR));
		REPLACEMENTS.put(Operator.XOR, Arrays.asList(Operator.AND, Operator.BINARY_AND, Operator.EQUALS, Operator.BINARY_OR, Operator.NOT_EQUALS, Operator.OR));
		
		REPLACEMENTS.put(Operator.GREATER, Arrays.asList(Operator.EQUALS, Operator.GREATER_EQUALS, Operator.LESS, Operator.LESS_EQUALS));
		REPLACEMENTS.put(Operator.GREATER_EQUALS, Arrays.asList(Operator.EQUALS, Operator.GREATER, Operator.LESS, Operator.LESS_EQUALS));
		REPLACEMENTS.put(Operator.LESS, Arrays.asList(Operator.EQUALS, Operator.GREATER, Operator.GREATER_EQUALS, Operator.LESS_EQUALS));
		REPLACEMENTS.put(Operator.LESS_EQUALS, Arrays.asList(Operator.EQUALS, Operator.GREATER, Operator.GREATER_EQUALS, Operator.LESS));
				
		REPLACEMENTS.put(Operator.DIVIDE, Arrays.asList(Operator.DIVIDE, Operator.LEFT_SHIFT, Operator.MINUS, Operator.MULTIPLY, Operator.PLUS, Operator.REMAINDER, Operator.SIGNED_RIGHT_SHIFT, Operator.UNSIGNED_RIGHT_SHIFT));
		REPLACEMENTS.put(Operator.LEFT_SHIFT, Arrays.asList(Operator.DIVIDE, Operator.MINUS, Operator.MULTIPLY, Operator.PLUS, Operator.REMAINDER, Operator.SIGNED_RIGHT_SHIFT, Operator.UNSIGNED_RIGHT_SHIFT));
		REPLACEMENTS.put(Operator.MINUS, Arrays.asList(Operator.DIVIDE, Operator.LEFT_SHIFT, Operator.MULTIPLY, Operator.PLUS, Operator.REMAINDER, Operator.SIGNED_RIGHT_SHIFT, Operator.UNSIGNED_RIGHT_SHIFT));
		REPLACEMENTS.put(Operator.MULTIPLY, Arrays.asList(Operator.DIVIDE, Operator.LEFT_SHIFT, Operator.MINUS, Operator.PLUS, Operator.REMAINDER, Operator.SIGNED_RIGHT_SHIFT, Operator.UNSIGNED_RIGHT_SHIFT));
		REPLACEMENTS.put(Operator.PLUS, Arrays.asList(Operator.DIVIDE, Operator.LEFT_SHIFT, Operator.MINUS, Operator.MULTIPLY, Operator.REMAINDER, Operator.SIGNED_RIGHT_SHIFT, Operator.UNSIGNED_RIGHT_SHIFT));
		REPLACEMENTS.put(Operator.REMAINDER, Arrays.asList(Operator.DIVIDE, Operator.LEFT_SHIFT, Operator.MINUS, Operator.MULTIPLY, Operator.PLUS, Operator.SIGNED_RIGHT_SHIFT, Operator.UNSIGNED_RIGHT_SHIFT));
		REPLACEMENTS.put(Operator.SIGNED_RIGHT_SHIFT, Arrays.asList(Operator.DIVIDE, Operator.LEFT_SHIFT, Operator.MINUS, Operator.MULTIPLY, Operator.PLUS, Operator.REMAINDER, Operator.UNSIGNED_RIGHT_SHIFT));
		REPLACEMENTS.put(Operator.UNSIGNED_RIGHT_SHIFT, Arrays.asList(Operator.DIVIDE, Operator.LEFT_SHIFT, Operator.MINUS, Operator.MULTIPLY, Operator.PLUS, Operator.REMAINDER, Operator.SIGNED_RIGHT_SHIFT));
		
		REPLACEMENTS.put(Operator.EQUALS, Arrays.asList(Operator.AND, Operator.BINARY_AND, Operator.BINARY_OR, Operator.GREATER_EQUALS, Operator.LESS, Operator.LESS_EQUALS, Operator.NOT_EQUALS, Operator.OR, Operator.XOR));
		REPLACEMENTS.put(Operator.NOT_EQUALS, Arrays.asList(Operator.AND, Operator.BINARY_AND, Operator.BINARY_OR, Operator.EQUALS, Operator.GREATER_EQUALS, Operator.LESS, Operator.LESS_EQUALS, Operator.OR, Operator.XOR));
	}
	
	/**
	 * Naming follows MuJava convention
	 * LOR
	 * 
	 * @param sourceNodes is the list of possible nodes for modification; these won't be
	 * 	      modified, just used for reference
	 * @param r is needed to choose a node and a suitable replacement 
	 *        (keeps this detail out of Patch class)
	 */
	public LogicalOperatorReplacement(List<Node> sourceNodes, LogicalOperatorReplacementFactory factory, Random r) {
		super(sourceNodes, factory, r);
		
		BinaryExpr sourceNode = (BinaryExpr)(this.sourceNodes.get(sourceNodeIndex));
		
		this.source = sourceNode.getOperator();
		this.replacement = chooseRandomReplacement(source, r);
	}
	
	@Override
	public boolean apply(CompilationUnit cu) {
		// first, get the list of nodes from the new cu
		List<Node> nodes = this.factory.appliesToNodes(cu);
		
		Node node = nodes.get(sourceNodeIndex);
		
		((BinaryExpr)node).setOperator(replacement);
		
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
