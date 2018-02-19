package gin.edit.modifynode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;

import gin.edit.ModifyNode;
import gin.edit.ModifyNodeFactory;

public class ReorderLogicalExpressionFactory extends ModifyNodeFactory {
	public ReorderLogicalExpressionFactory(CompilationUnit cu) {
		super(cu);
	}
	
	@Override
	public ModifyNode newModifier(Random rng) {
		return new ReorderLogicalExpression(getSourceNodes(), this, rng);
	}
	
	
	
	@Override
	public Collection<Class<? extends Node>> appliesTo() {
		return Collections.singleton(BinaryExpr.class);
	}
	
	@Override
	public List<Node> appliesToNodes(CompilationUnit cu) {
		// get the static list of nodes that might be applicable (the BinaryStatements)
		// then look for the ones that have AND OR operators 
		List<Node> nodes = new ArrayList<>();
		
		for (Node n : super.appliesToNodes(cu)) {
			if ((((BinaryExpr)n).getOperator() == Operator.AND) || (((BinaryExpr)n).getOperator() == Operator.OR)) {
				nodes.add(n);
			}
		}
		
		return nodes;
	}
}
