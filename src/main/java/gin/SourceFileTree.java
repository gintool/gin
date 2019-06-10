package gin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.pmw.tinylog.Logger;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.DataKey;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

import gin.misc.CloneVisitorCopyIDs;

/**
 * A SourceFile designed for supporting AST-level edits (e.g. statements)
 * 
 * In practice SourceFile can be viewed as immutable. The only way it can be changed
 * is via the insert/delete line/statement/node or replaceNode methods, which
 * create and return a new SourceFile as part of their signature
 */
public class SourceFileTree extends SourceFile {

    /**the key used to track IDs in JavaParser nodes*/
    public static final DataKey<Integer> NODEKEY_ID = new DataKey<Integer>() { };
    
    /**
     * The compilation unit is only ever made available as a copy,
     * never a direct reference. So we can assume that it is only
     * changed by methods within SourceFile, and even then only as 
     * part of making a new SourceFile object
     * <br><br>
     * The CU nodes include, using Node.setData(), an ID that can be used
     * to reference them. For speed, these Node-ID pairs are also held
     * in a Map. Anything extra inserted by edits does not have an ID
     * (for now, maybe useful in future?) possibly under a different key
     * to the original IDs
     */
    private CompilationUnit compilationUnit;
    private Map<Integer,Node> allNodes;
    private List<Integer> allBlockIDs;

    /**keys are IDs, values are lists of statement IDs in the block*/
    private Map<Integer,List<Integer>> insertionPointsInBlock;

    /** nodes containing each target method */
    private List<Node> targetMethodRootNodes;

    /**IDs if all nodes in a target method; i.e. descendents of the nodes in targetMethodRootNodes*/
    private List<Integer> targetMethodNodeIDs;

    /**node IDs of all statements in the target methods*/
    private List<Integer> targetMethodStatementIDs;
    
    /**node IDs of all statements*/
    private List<Integer> allStatementIDs;
    
    /**node IDs of all blockstatements in the target methods*/
    private List<Integer> targetMethodBlockIDs;

    
    
    public SourceFileTree(String filename, List<String> targetMethodNames) {

        super(filename, targetMethodNames);
        
        this.compilationUnit = buildCompilationUnitFromSource(new File(filename));

        this.populateIDListsFromCompilationUnit();
        
    }

    public SourceFileTree(File file, String method) {
        this(file.getPath(), Arrays.asList(method));
    }

    /**
     * create a copy of a source file
     * TODO: make this a clone?
     * SB: might want to avoid clone. http://techarticles-wasim.blogspot.com/2011/11/java-clone-method-why-to-avoid-no.html
     *
     * this is only called by methods within this class that are going to make a change
     */
    private SourceFileTree(SourceFileTree sf) {
        
        this(sf, Collections.emptyMap());
        
    }
    
    /**
     * same as SourceFileTree(SourceFileTree sf) but allows for replacing of nodes in the copy
     */
    private SourceFileTree(SourceFileTree sf, Map<Integer, Node> nodesToReplace) {
        
        super(sf.filename, sf.targetMethods);
        
        // clone the compilation unit (including IDs)
        this.compilationUnit = cloneCompilationUnitWithIDs(sf.compilationUnit, nodesToReplace);
        
        this.populateIDListsFromCompilationUnit();
        
    }
    
    @Override
    public SourceFile copyOf() {
        return new SourceFileTree(this);
    }
    
    /*============== the following are setup methods - reading files, building ID lists etc ==============*/
    
    /**
     * called when rebuilding CU from scratch (e.g. after reading from a file)
     * this will update the IDs etc.
     */
    private CompilationUnit buildCompilationUnitFromSource(File file) {

        CompilationUnit compilationUnit = null;
        
        try {
            
            // make the CU
            compilationUnit = JavaParser.parse(file);
            
            // assign the IDs
            int id = 0;
            for (Node n : compilationUnit.getChildNodesByType(Node.class)) {
                n.setData(NODEKEY_ID, id);
                id++;
            }
            
        } catch (IOException e) {
            Logger.error("Exception reading program source: " + e);
            System.exit(-1);
        }
        
        return compilationUnit;
    }
    
    /**
     * updates the supporting lists of IDs, assuming that
     * this.compilationUnit exists and has IDs
     * 
     * this is called after creating a new CU from scratch
     * and after cloning an existing one
     */
    private void populateIDListsFromCompilationUnit() {

        // update the cache of IDs in the CU
        this.allNodes = new HashMap<>();
        for (Node n : this.compilationUnit.getChildNodesByType(Node.class)) {
            this.allNodes.put(n.getData(NODEKEY_ID), n);
        }
        
        // find the root nodes for the target methods
        if (this.targetMethods == null || targetMethods.size() == 0) {
            this.targetMethodRootNodes = null;
        } else {
            this.targetMethodRootNodes = getTargetMethodRootNodesFromCU(this.compilationUnit, this.targetMethods);
        }
        
        findStatementsAndNodes();
        findBlocks();
    }

    /**
     * Updates the lists of statement and nodeIDs 
     * in and out of the target methods
     */
    private void findStatementsAndNodes() {
        targetMethodStatementIDs = new ArrayList<>();
        targetMethodNodeIDs = new ArrayList<>();
        allStatementIDs = new ArrayList<>();
        
        for (Statement s : compilationUnit.getChildNodesByType(Statement.class)) {
            Integer id = s.getData(NODEKEY_ID);
            allStatementIDs.add(id);
        }
        
        if (this.targetMethodRootNodes != null) {
            for (Node tn : this.targetMethodRootNodes) {
                List<Node> nodesInTargetMethod = tn.getChildNodesByType(Node.class);
                
                for (Node n : nodesInTargetMethod) {
                    Integer id = n.getData(NODEKEY_ID);
                    targetMethodNodeIDs.add(id);
                    if (Statement.class.isAssignableFrom(n.getClass())) {
                        targetMethodStatementIDs.add(id);
                    }
                }
            }
        } else {
            // no target methods? just add all nodes and statements
            targetMethodStatementIDs.addAll(allStatementIDs);
            targetMethodNodeIDs.addAll(allNodes.keySet());
        }
    }

    private void findBlocks() {
        List<BlockStmt> allBlocks = compilationUnit.getChildNodesByType(BlockStmt.class);
        allBlockIDs = new ArrayList<>(allBlocks.size());
        insertionPointsInBlock = new HashMap<>();
        
        for (BlockStmt b : allBlocks) {
            allBlockIDs.add(b.getData(NODEKEY_ID));
            NodeList<Statement> statements = b.getStatements();
            List<Integer> statementIDs = new ArrayList<>(statements.size());
            statementIDs.add(b.getData(NODEKEY_ID)); // add the blockID too, representing the start of the block
            for (Statement statement : statements) {
                statementIDs.add(statement.getData(NODEKEY_ID));
            }
            
            insertionPointsInBlock.put(b.getData(NODEKEY_ID), statementIDs);
        }

        targetMethodBlockIDs = new ArrayList<>();
        if (this.targetMethodRootNodes != null) {
            for (Node n : this.targetMethodRootNodes) {
                List<BlockStmt> listTargetMethod = n.getChildNodesByType(BlockStmt.class);
                for (BlockStmt b : allBlocks) {
                    if (listTargetMethod.contains(b)) {
                        targetMethodBlockIDs.add(b.getData(NODEKEY_ID));
                    }
                }
            }
        } else {
            for (BlockStmt b : allBlocks) { // no target methods? just add all
                targetMethodBlockIDs.add(b.getData(NODEKEY_ID));
            }
        }
    }
    
    /*============== the following are general getter methods used in various places ==============*/

    /**
     * @return the source
     */
    public String getSource() {
        return this.compilationUnit.toString();
    }
    
    public PackageDeclaration getPackage() {
        return this.compilationUnit.getPackageDeclaration().orElse(null);
    }

    public String statementList() {
        List<Statement> list = compilationUnit.getChildNodesByType(Statement.class);
        int counter = 0;
        String output = "";
        for (Statement statement : list) {
            output += "[" + counter + "] " + statement.toString() + "\n"; // can't use indexof as may appear > once
            counter++;
        }
        return output;
    }

    public String blockList() {
        List<BlockStmt> list = compilationUnit.getChildNodesByType(BlockStmt.class);
        int counter = 0;
        String output = "";
        for (BlockStmt block : list) {
            output += "[" + counter + "] " + block.toString() + "\n"; // can't use indexof as may appear > once
            counter++;
        }
        return output;
    }


    //TODO
//    /**
//     * @return either root node, or nodes representing the target methods if any were specified
//     */
//    private List<Node> getTargetNodes() {
//        if (this.targetMethodRootNodes != null) {
//            return this.targetMethodRootNodes;
//        } else {
//            return Collections.singletonList(this.compilationUnit);
//        }
//    }

    /*============== the following are statement/node editing methods ==============*/

    /**
     * @param statementID - this is not an index, just an ID! use {@link #getIDForStatementNumber(int)} 
     * to convert statement numbers to IDs for use here
     * @return a modified copy of this {@link SourceFileTree}
     */
    public SourceFileTree removeStatement(int statementID) {
        // node already deleted? don't bother.
        if (!this.allNodes.containsKey(statementID)) {
            return this;
        } else {
            SourceFileTree sf = new SourceFileTree(this);
            
            Node target = sf.allNodes.get(statementID);
        
            if (target.remove()) { // only proceed if JavaParser lets us remove the node
                sf.allNodes.remove(statementID);
                return sf;
            } else {
                return this;
            }
        }
    }

    /**
     * @param blockID - this is not an index, just an ID! use {@link #getIDForBlockNumber(int)} 
     * to convert block numbers to IDs for use here
     * 
     * @param insertionPoint - this is an ID of a statement within the block.
     *        The IDs will have been in-order, so we will insert before the
     *        first statement with a larger ID than the specified insertion point
     *        This will capture situations where the insertion point has been deleted.
     *        
     * @param statementToInsert the statement to insert
     *        
     * @return a modified copy of this {@link SourceFileTree}
     */
    public SourceFileTree insertStatement(int blockID, int insertionPoint, Statement statementToInsert) {
        // if the blockID has been deleted, don't bother
        // (if the insertion point is gone, that's fine, just fill the gap)
        if (!this.allNodes.containsKey(blockID) || !this.allNodes.containsKey(insertionPoint)) {
            return this;
        } else {
            SourceFileTree sf = new SourceFileTree(this);
        
            Statement copy = statementToInsert.clone(); // always clone to avoid nasty stateful stuff
            copy.setData(NODEKEY_ID, null); // clear the ID of the copy

            Node parent = sf.allNodes.get(blockID);
            if (parent instanceof BlockStmt) {
                // find the insert point
                NodeList<Statement> statements = ((BlockStmt)parent).getStatements();
            
                int insertIndex = 0; // start with the possibility of inserting at beginning of block   
                    
                statementLoop:
                for (int i = 0; i < statements.size(); i++) {
                    Integer id = statements.get(i).getData(NODEKEY_ID);
                    if ((id != null) && (id <= insertionPoint)) {
                        insertIndex = i + 1; // add 1 because we want to insert after the statement!
                    } else {
                        break statementLoop;
                    }
                }
            
                // Location found! Now insert.
                ((BlockStmt)parent).addStatement(insertIndex, copy);
                
                return sf;
            } else {
                return this;
            }
        }
    }
    
    /**
     * for use by any edits that change nodes in-place;
     * a bit cleaner than removing and reinserting the node, especially
     * if removal would break syntax and thus be stopped by JavaParser
     * (getNode() and getStatement() return copies so you can't edit things that way)
     * 
     * @param ID of node to replace
     * @param replacement node
     * 
     * @return a modified copy of this {@link SourceFileTree}
     */
    public SourceFileTree replaceNode(int ID, Node replacement) {
        if (!this.allNodes.containsKey(ID)) {
            return this;
        } else {
            Node replacementNodeCopy = replacement.clone();
            //replacementNodeCopy.setData(NODEKEY_ID, ID);  // don't do this. it then makes edits to the replaced node possible. Issue https://github.com/drdrwhite/ginfork/issues/46
            replacementNodeCopy.setData(NODEKEY_ID, null);
            
            Map<Integer, Node> nodesToReplace = Collections.singletonMap(ID, replacementNodeCopy);
            SourceFileTree sf = new SourceFileTree(this, nodesToReplace);
            
            return sf;
        }
    }
    
    /**
     * currently no checking to ensure the ID is actually for a Statement node
     * so possibly will throw a ClassCastException
     * @param ID of statement to get
     * @return a clone of the specified statement, null if the corresponding node was already deleted
     */
    public Statement getStatement(int ID) {
        if (this.allNodes.containsKey(ID)) {
            Statement s = (Statement)(this.allNodes.get(ID).clone());
            s.setData(NODEKEY_ID, ID);
            return s;
        } else {
            return null;
        }
    }

    /**
     * @return a clone of the specified node, or null if the corresponding node was already deleted
     * @param ID of node to get
     */
    public Node getNode(int ID) {
        if (this.allNodes.containsKey(ID)) {
            Node n = this.allNodes.get(ID).clone();
            n.setData(NODEKEY_ID, ID);
            return n;
        } else {
            return null;
        }
    }
    
    /*============== the following are methods to get IDs and counts to assist in making edits ==============*/
    
    public List<Integer> getAllBlockIDs() {
        return Collections.unmodifiableList(allBlockIDs);
    }

    /**also includes insertion at the start of the block
     * @return null if blockID not found
     * @param block ID
     * */
    public List<Integer> getInsertionPointsInBlock(int block) {
        return insertionPointsInBlock.get(block);
    }

    /**
     * @return a list of indices into the list of statements for this source file
     * that sit within the target method
     */
    public List<Integer> getStatementIDsInTargetMethod() {
        return Collections.unmodifiableList(targetMethodStatementIDs);
    }
    
    public int getRandomStatementID(boolean inTargetMethod, Random rng) {
        List<Integer> l = inTargetMethod ? targetMethodStatementIDs : allStatementIDs;
        return l.get(rng.nextInt(l.size()));
    }
    
    /**
     * @return a Map: each key is a statement ID; each value is a 
     * list of any statements that match it; that is, the space of
     * target locations for MatchedReplaceStatement and MatchedSwapStatement
     * (the list will contain the destination statement, so if the list is 
     * size 1, there are no matching statements!)
     * 
     * @param destinationInTargetMethod - true unless we are targeting whole class
     * 
     * @param sourceInTargetMethod 
     *                       - true for swaps (both the statements to be in target method)
     *                       - false for replace (only the destination to be in the target method) 
     */
    public Map<Integer, List<Integer>> getMatchedStatementLists(boolean sourceInTargetMethod, boolean destinationInTargetMethod) {
        List<Integer> destinationIDs = destinationInTargetMethod ? targetMethodStatementIDs : allStatementIDs;
        Map<Integer, List<Integer>> rval = new HashMap<>();
        for (Integer destinationID : destinationIDs) {
            List<Integer> sourceIDs = getNodeIDsByClass(sourceInTargetMethod, getStatement(destinationID).getClass());
            rval.put(destinationID, sourceIDs);
        }
        
        return rval;
    }
    

    /**
     * @return a list of indices into the list of nodes for this source file
     * that sit within the target method
     */
    public List<Integer> getNodeIDsInTargetMethod() {
        return Collections.unmodifiableList(targetMethodNodeIDs);
    }
    
    /**
     * @param inTargetMethod limit IDs to target method if true, or any block in the class otherwise
     * @param rng random number generator used to choose an ID 
     * @return -1 if no blocks found
     * */
    public int getRandomBlockID(boolean inTargetMethod, Random rng) {
        List<Integer> l = inTargetMethod ? targetMethodBlockIDs : allBlockIDs;
        if (l.isEmpty()) {
            return -1;
        } else {
            return l.get(rng.nextInt(l.size()));
        }
    }
    
    /**
     * @param blockID id of a block statement in which we want to choose an insertion point
     * @param rng random number generator used to choose an insertion point
     * @return -1 if no matching block found*/
    public int getRandomInsertPointInBlock(int blockID, Random rng) {
        List<Integer> l = getInsertionPointsInBlock(blockID);
        if ((l != null) && !l.isEmpty()) {
            return l.get(rng.nextInt(l.size()));
        } else {
            return -1;
        }
    }
    
    /**
     * @param inTargetMethod limit IDs to target method if true, or anywhere in the class otherwise
     * @param clazz limit IDs to nodes that extend this class 
     * @param rng random number generator used to choose an ID 
     * @return returns -1 if no matching nodes found
     * */
    public int getRandomNodeID(boolean inTargetMethod, Class<? extends Node> clazz, Random rng) {
        List<Integer> l = getNodeIDsByClass(inTargetMethod, clazz);
        if (l.isEmpty()) {
            return -1;
        } else {
            return l.get(rng.nextInt(l.size()));
        }
    }
    
    /**
     * Get node IDs (in whole class or just the target method)
     * that match or extend the given class
     * Used for matched statement operators 
     * Call with e.g. Statement.class, BlockStmt.class etc.
     * @param inTargetMethod limit IDs to target method if true, or anywhere the class otherwise
     * @param clazz limit IDs to nodes that extend this class
     * @return a list of node IDs 
     */
    public List<Integer> getNodeIDsByClass(boolean inTargetMethod, Class<? extends Node> clazz) {
        return getNodeIDsByClass(inTargetMethod, Collections.singletonList(clazz));
    }
    
    /**
     * @param inTargetMethod limit IDs to target method if true, or anywhere the class otherwise
     * @param clazzes limit IDs to nodes that extend these classes 
     * @return a list of node IDs
     */
    public List<Integer> getNodeIDsByClass(boolean inTargetMethod, List<Class<? extends Node>> clazzes) {
        if (inTargetMethod) {
            List<Integer> rval = new ArrayList<>(targetMethodNodeIDs.size());
            
            for (int i : targetMethodNodeIDs) {
                classLoop:
                for (Class<? extends Node> clazz : clazzes) {
                    if (clazz.isAssignableFrom(allNodes.get(i).getClass())) {
                        rval.add(i);
                        break classLoop;
                    }
                }
            }
            
            return rval;
        } else {
            List<Integer> rval = new ArrayList<>();
            
            for (Node n : allNodes.values()) {
                classLoop:
                for (Class<? extends Node> clazz : clazzes) {
                    if (clazz.isAssignableFrom(n.getClass())) {
                        rval.add(n.getData(NODEKEY_ID)); // no need to check for null, these nodes are only ones from the original and have IDs
                        break classLoop;
                    }
                }
            }
            
            return rval;
        }
    }
    
    public List<Integer> getAllStatementIDs() {
        return Collections.unmodifiableList(allStatementIDs);
    }
    
    /**
     * @return a list of indices into the list of blocks for this source file
     * that sit within the target method
     */
    public List<Integer> getBlockIDsInTargetMethod() {
        return Collections.unmodifiableList(targetMethodBlockIDs);
    }
    
    /**
     * @return the ID for the statement index, if the statements were numbered 
     * in the order returned by CompilationUnit.getChildNodesByType(Statement.class)
     * NOTE: this uses the current state! so returned IDs will be different once things
     * have been moved / deleted; might even be null if you get an inserted statement
     * @param index the statement index to return an ID for
     */
    public int getIDForStatementNumber(int index) {
        List<Statement> l = compilationUnit.getChildNodesByType(Statement.class);
        return l.get(index).getData(NODEKEY_ID);
    }
    
    /**
     * @return the ID for the block index, if the blocks were numbered 
     * in the order returned by CompilationUnit.getChildNodesByType(BlockStmt.class)
     * NOTE: this uses the current state! so returned IDs will be different once things
     * have been moved / deleted  
     * @param index the block index to return an ID for
     */
    public int getIDForBlockNumber(int index) {
        List<BlockStmt> l = compilationUnit.getChildNodesByType(BlockStmt.class);
        return l.get(index).getData(NODEKEY_ID);
    }
    
    /**
     * @return the statement index for the given statement ID, if the statements were numbered 
     * in the order returned by CompilationUnit.getChildNodesByType(Statement.class)
     * NOTE: this uses the current state! so returned IDs will be different once things
     * have been moved / deleted; might even be null if you get an inserted statement;
     * 
     * returns -1 if not ID wasn't found (probably an ID for a non-statement node)
     * 
     * @param ID the statement ID to return an index for
     */
    public int getStatementNumberForNodeID(int ID) {
        List<Statement> l = compilationUnit.getChildNodesByType(Statement.class);
        
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).getData(NODEKEY_ID) == ID) {
                return i;
            }
        }

        return -1;
    }
    
    /**
     * @return the block index for the given statement ID, if the blocks were numbered 
     * in the order returned by CompilationUnit.getChildNodesByType(BlockStmt.class)
     * NOTE: this uses the current state! so returned IDs will be different once things
     * have been moved / deleted  
     * 
     * returns -1 if not ID wasn't found (probably an ID for a non-statement node)
     * 
     * @param ID the block index to return an ID for
     */
    public int getBlockNumberForNodeID(int ID) {
        List<BlockStmt> l = compilationUnit.getChildNodesByType(BlockStmt.class);

        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).getData(NODEKEY_ID) == ID) {
                return i;
            }
        }

        return -1;
    }

 
    /*============== the following are some helper methods and classes ==============*/
    
    /** SB: CU.clone() doesn't copy node IDs. This does.
     * 
     * @param cu to CompilationUnit to clone
     * @return the clone
     * */
    private static CompilationUnit cloneCompilationUnitWithIDs(CompilationUnit cu) {
        CompilationUnit rval = (CompilationUnit)(cu.accept(new CloneVisitorCopyIDs(), null));
        return rval;
    }
    
    /**
     * @param cu to CompilationUnit to clone
     * @param nodesToReplace a map of replacements (key:nodeID,value:replacement)
     * @return the clone
     */
    private static CompilationUnit cloneCompilationUnitWithIDs(CompilationUnit cu, Map<Integer, Node> nodesToReplace) {
        CompilationUnit rval = (CompilationUnit)(cu.accept(new CloneVisitorCopyIDs(nodesToReplace), null));
        return rval;
    }

}
