package gin.edit;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;

public abstract class ModifyNode extends Edit {
	/**the node to be modified*/
    public final int sourceNodeIndex;

    /**the applicable nodes in the corresponding factory*/
    public final List<Node> sourceNodes;
    
    public final ModifyNodeFactory factory;
    
    /**
     * Note: this will assume that there will be some applicable nodes!
     * @throws IllegalArgumentException if sourceNodes is empty
     * @param sourceNodes is the list of possible nodes for modification; these won't be
	 * 	      modified, just used for reference
     * @param r is provided to choose a node to modify, and choose a replacement*/
    public ModifyNode(List<Node> sourceNodes, ModifyNodeFactory factory, Random r) {
    	this.sourceNodes = Collections.unmodifiableList(sourceNodes);
        this.sourceNodeIndex = r.nextInt(sourceNodes.size());
        this.factory = factory;
    }
    
    /**
     * Apply this patch to the supplied compilation unit.
     * Assumes that the CU is identical in structure to the one used
     * to create this edit
     * @return true if modification was successful
     */
    public abstract boolean apply(CompilationUnit cu);
    
    @Override
    public String toString() {
        return "MODIFY " + sourceNodeIndex + " ( " + this.getClass().getName() + " )";
    }
}
