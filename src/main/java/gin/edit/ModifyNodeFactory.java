package gin.edit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;

/**
 * Makes ModifyNode instances
 */
public abstract class ModifyNodeFactory {
	private List<Node> sourceNodes;
	
	public ModifyNodeFactory(CompilationUnit cu) {
		this.sourceNodes = this.appliesToNodes(cu);
	}
	
	public List<Node> getSourceNodes() {
		return sourceNodes;
	}
	
	public abstract ModifyNode newModifier(Random rng);
		
    /**
     * @return a collection of Class objects identifying the statement/expression etc. classes that this applies to.
     * Typically you'd return something like Collections.singleton(Statement.class);
     * This can be ignored but might serve to be useful if we want a more intelligent mutation at some point
     * (hopefully this would mean less broken code) 
     */
	public abstract Collection<Class<? extends Node>> appliesTo();
	
    /**
     * By default this just calls appliesTo() and uses those classes as a static filter.
     * If you want to do something more interesting (e.g. filter by operator within a node),
     * then override this method.
     * 
     * @return collection of nodes from a given source file that the modification applies to
     */
    public List<Node> appliesToNodes(CompilationUnit cu) {
    	Set<Node> nodes = new LinkedHashSet<Node>(); // LinkedHashSet preserves order - keeps operation deterministic
    	
    	for (Class<? extends Node> clazz : appliesTo()) {
    		nodes.addAll(cu.getChildNodesByType(clazz));
    	}
    	
    	return Collections.unmodifiableList(new ArrayList<Node>(nodes));
    }
}
