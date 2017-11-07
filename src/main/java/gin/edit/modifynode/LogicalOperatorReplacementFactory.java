package gin.edit.modifynode;

import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;

import gin.edit.ModifyNode;
import gin.edit.ModifyNodeFactory;

public class LogicalOperatorReplacementFactory extends ModifyNodeFactory {
	public LogicalOperatorReplacementFactory(CompilationUnit cu) {
		super(cu);
	}
	
	@Override
	public ModifyNode newModifier(Random rng) {
		return new LogicalOperatorReplacement(getSourceNodes(), this, rng);
	}
	
	
	
	@Override
	public Collection<Class<? extends Node>> appliesTo() {
		return Collections.singleton(BinaryExpr.class);
	}
}
