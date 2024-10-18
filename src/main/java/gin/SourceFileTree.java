package gin;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;
import gin.misc.BlockedByJavaParserException;
import gin.misc.CloneVisitorCopyIDs;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.util.*;

/**
 * A SourceFile designed for supporting AST-level edits (e.g. statements)
 * <p>
 * In practice SourceFile can be viewed as immutable. The only way it can be changed
 * is via the insert/delete line/statement/node or replaceNode methods, which
 * create and return a new SourceFile as part of their signature
 */
public class SourceFileTree extends SourceFile {

    /**
     * the key used to track IDs in JavaParser nodes; nodes in original source have ID greater or equal to 0
     */
    public static final DataKey<Integer> NODEKEY_ID = new DataKey<>() {
    };
    /**
     * all nodes from the original source have ID greater or equal to 0
     */
    public static final int NODE_NULL_ID = -1;
    @Serial
    private static final long serialVersionUID = -7236253343894593247L;
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
    private final CompilationUnit compilationUnit;
    private Map<Integer, Node> allNodes;
    private List<Integer> allBlockIDs;

    /**
     * keys are IDs, values are lists of statement IDs in the block
     */
    private Map<Integer, List<Integer>> insertionPointsInBlock;

    /**
     * nodes containing each target method
     */
    private List<Node> targetMethodRootNodes;

    /**
     * IDs if all nodes in a target method; i.e. descendents of the nodes in targetMethodRootNodes
     */
    private List<Integer> targetMethodNodeIDs;

    /**
     * node IDs of all statements in the target methods
     */
    private List<Integer> targetMethodStatementIDs;

    /**
     * node IDs of all statements
     */
    private List<Integer> allStatementIDs;

    /**
     * node IDs of all blockstatements in the target methods
     */
    private List<Integer> targetMethodBlockIDs;


    public SourceFileTree(String filename, List<String> targetMethodNames) {

        super(filename, targetMethodNames);

        this.compilationUnit = buildCompilationUnitFromSource(new File(filename));

        this.populateIDListsFromCompilationUnit();

    }

    public SourceFileTree(File file, String method) {
        this(file.getPath(), Collections.singletonList(method));
    }

    /**
     * create a copy of a source file
     * TODO: make this a clone?
     * SB: might want to avoid clone. <a href="http://techarticles-wasim.blogspot.com/2011/11/java-clone-method-why-to-avoid-no.html">...</a>
     * <p>
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

    /**
     * SB: CU.clone() doesn't copy node IDs. This does.
     *
     * @param cu to CompilationUnit to clone
     * @return the clone
     */
    @SuppressWarnings("unused")
    private static CompilationUnit cloneCompilationUnitWithIDs(CompilationUnit cu) {
        return (CompilationUnit) (cu.accept(new CloneVisitorCopyIDs(), null));
    }

    /*============== the following are setup methods - reading files, building ID lists etc ==============*/

    /**
     * @param cu             to CompilationUnit to clone
     * @param nodesToReplace a map of replacements (key:nodeID,value:replacement)
     * @return the clone
     */
    private static CompilationUnit cloneCompilationUnitWithIDs(CompilationUnit cu, Map<Integer, Node> nodesToReplace) {
        return (CompilationUnit) (cu.accept(new CloneVisitorCopyIDs(nodesToReplace), null));
    }

    @Override
    public SourceFile copyOf() {
        return new SourceFileTree(this);
    }

    /**
     * called when rebuilding CU from scratch (e.g. after reading from a file)
     * this will update the IDs etc.
     */
    private CompilationUnit buildCompilationUnitFromSource(File file) {

        CompilationUnit compilationUnit = null;

        try {

            // make the CU
            compilationUnit = StaticJavaParser.parse(file);

            // assign the IDs - they start at zero
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
     * <p>
     * this is called after creating a new CU from scratch
     * and after cloning an existing one
     */
    private void populateIDListsFromCompilationUnit() {

        // update the cache of IDs in the CU
        this.allNodes = new HashMap<>();
        for (Node n : this.compilationUnit.getChildNodesByType(Node.class)) {
            this.allNodes.put(n.containsData(NODEKEY_ID) ? n.getData(NODEKEY_ID) : NODE_NULL_ID, n);
        }

        // find the root nodes for the target methods
        if (this.targetMethods == null || targetMethods.isEmpty()) {
            this.targetMethodRootNodes = null;
        } else {
            this.targetMethodRootNodes = getTargetMethodRootNodesFromCU(this.compilationUnit, this.targetMethods);
        }

        findStatementsAndNodes();
        findBlocks();
    }

    public List<Node> getTargetMethodRootNode() {
        if (this.targetMethodRootNodes == null) {
            return null;
        }

        List<Node> targetNode = getTargetMethodRootNodesFromCU(this.compilationUnit, this.targetMethods);
        return targetNode;
    }

    /*============== the following are general getter methods used in various places ==============*/

    /**
     * Updates the lists of statement and nodeIDs
     * in and out of the target methods
     */
    private void findStatementsAndNodes() {
        targetMethodStatementIDs = new ArrayList<>();
        targetMethodNodeIDs = new ArrayList<>();
        allStatementIDs = new ArrayList<>();

        for (Statement s : compilationUnit.getChildNodesByType(Statement.class)) {
            Integer id = s.containsData(NODEKEY_ID) ? s.getData(NODEKEY_ID) : SourceFileTree.NODE_NULL_ID;
            allStatementIDs.add(id);
        }

        if (this.targetMethodRootNodes != null) {
            // temp keep these in a sorted set to remove duplicates for overlapping methods
            // (e.g. method in an inner class, where the inner class is also in a target method)
            SortedSet<Integer> sIDs = new TreeSet<>(); // statementIDs (a subset of nodeIDs)
            SortedSet<Integer> nIDs = new TreeSet<>(); // nodeIDs

            for (Node tn : this.targetMethodRootNodes) {
                List<Node> nodesInTargetMethod = tn.getChildNodesByType(Node.class);

                for (Node n : nodesInTargetMethod) {
                    Integer id = n.containsData(NODEKEY_ID) ? n.getData(NODEKEY_ID) : SourceFileTree.NODE_NULL_ID;
                    if (id != null) { // only track nodes in the original source, i.e. those with IDs
                        nIDs.add(id);
                        if (Statement.class.isAssignableFrom(n.getClass())) {
                            sIDs.add(id);
                        }
                    }
                }
            }

            targetMethodNodeIDs.addAll(nIDs);
            targetMethodStatementIDs.addAll(sIDs);
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
            allBlockIDs.add(b.containsData(NODEKEY_ID) ? b.getData(NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
            NodeList<Statement> statements = b.getStatements();
            List<Integer> statementIDs = new ArrayList<>(statements.size());
            statementIDs.add(b.containsData(NODEKEY_ID) ? b.getData(NODEKEY_ID) : SourceFileTree.NODE_NULL_ID); // add the blockID too, representing the start of the block
            for (Statement statement : statements) {
                statementIDs.add(statement.containsData(NODEKEY_ID) ? statement.getData(NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
            }

            insertionPointsInBlock.put(b.containsData(NODEKEY_ID) ? b.getData(NODEKEY_ID) : NODE_NULL_ID, statementIDs);
        }

        targetMethodBlockIDs = new ArrayList<>();
        if (this.targetMethodRootNodes != null) {
            for (Node n : this.targetMethodRootNodes) {
                List<BlockStmt> listTargetMethod = n.getChildNodesByType(BlockStmt.class);
                for (BlockStmt b : allBlocks) {
                    if (listTargetMethod.contains(b)) {
                        targetMethodBlockIDs.add(b.containsData(NODEKEY_ID) ? b.getData(NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
                    }
                }
            }
        } else {
            for (BlockStmt b : allBlocks) { // no target methods? just add all
                targetMethodBlockIDs.add(b.containsData(NODEKEY_ID) ? b.getData(NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
            }
        }
    }

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
        StringBuilder output = new StringBuilder();
        for (Statement statement : list) {
            output.append("[").append(counter).append("] ").append(statement.toString()).append("\n"); // can't use indexof as may appear > once
            counter++;
        }
        return output.toString();
    }

    public String blockList() {
        List<BlockStmt> list = compilationUnit.getChildNodesByType(BlockStmt.class);
        int counter = 0;
        StringBuilder output = new StringBuilder();
        for (BlockStmt block : list) {
            output.append("[").append(counter).append("] ").append(block.toString()).append("\n"); // can't use indexof as may appear > once
            counter++;
        }
        return output.toString();
    }

    public String nodeListWithIDs() {
        List<Integer> ids = new ArrayList<>(allNodes.keySet());
        Collections.sort(ids);
        StringBuilder output = new StringBuilder();
        for (Integer id : ids) {
            Node node = getNode(id);
            output.append("[").append(id).append("] ").append(node.toString()).append("\n");
        }
        return output.toString();
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

    public String statementListWithIDs() {
        List<Integer> list = getAllStatementIDs();
        StringBuilder output = new StringBuilder();
        for (Integer id : list) {
            Statement statement = getStatement(id);
            output.append("[").append(id).append("] ").append(statement.toString()).append("\n");
        }
        return output.toString();
    }

    public String blockListWithIDs() {
        List<Integer> list = getAllBlockIDs();
        StringBuilder output = new StringBuilder();
        for (Integer id : list) {
            Statement block = getStatement(id);
            output.append("[").append(id).append("] ").append(block.toString()).append("\n"); // can't use indexof as may appear > once
        }
        return output.toString();
    }

    /**
     * @param statementID - this is not an index, just an ID! use {@link #getIDForStatementNumber(int)}
     *                    to convert statement numbers to IDs for use here
     * @return a modified copy of this {@link SourceFileTree}, or this {@link SourceFileTree} if the
     * node ID does not exist (perhaps already deleted)
     * @throws BlockedByJavaParserException if delete was prevented by JavaParser
     */
    public SourceFileTree removeStatement(int statementID) throws BlockedByJavaParserException {
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
                throw new BlockedByJavaParserException("Could not delete statement with node ID " + statementID);
            }
        }
    }

    /**
     * @param blockID           - this is not an index, just an ID! use {@link #getIDForBlockNumber(int)}
     *                          to convert block numbers to IDs for use here
     * @param insertionPoint    - this is an ID of a statement within the block.
     *                          The IDs will have been in-order, so we will insert before the
     *                          first statement with a larger ID than the specified insertion point
     *                          This will capture situations where the insertion point has been deleted.
     * @param statementToInsert the statement to insert
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
            copy.setData(NODEKEY_ID, NODE_NULL_ID); // clear the ID of the copy

            Node parent = sf.allNodes.get(blockID);
            if (parent instanceof BlockStmt) {
                // find the insert point
                NodeList<Statement> statements = ((BlockStmt) parent).getStatements();

                int insertIndex = 0; // start with the possibility of inserting at beginning of block

                for (int i = 0; i < statements.size(); i++) {
                    Integer id = statements.get(i).containsData(NODEKEY_ID) ? statements.get(i).getData(NODEKEY_ID) : NODE_NULL_ID;
                    if ((id == null) || (id <= insertionPoint)) {
                        insertIndex = i + 1; // add 1 because we want to insert after the statement!
                    } else {
                        break;
                    }
                }

                // Location found! Now insert.
                ((BlockStmt) parent).addStatement(insertIndex, copy);

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
     * @param ID          of node to replace
     * @param replacement node
     * @return a modified copy of this {@link SourceFileTree}
     */
    public SourceFileTree replaceNode(int ID, Node replacement) {
        if (!this.allNodes.containsKey(ID)) {
            return this;
        } else {
            Node replacementNodeCopy = replacement.clone();
            //replacementNodeCopy.setData(NODEKEY_ID, ID);  // don't do this. it then makes edits to the replaced node possible. Issue https://github.com/drdrwhite/ginfork/issues/46
            replacementNodeCopy.setData(NODEKEY_ID, NODE_NULL_ID);

            Map<Integer, Node> nodesToReplace = Collections.singletonMap(ID, replacementNodeCopy);

            return new SourceFileTree(this, nodesToReplace);
        }
    }

    /*============== the following are methods to get IDs and counts to assist in making edits ==============*/

    /**
     * currently no checking to ensure the ID is actually for a Statement node
     * so possibly will throw a ClassCastException
     *
     * @param ID of statement to get
     * @return a clone of the specified statement, null if the corresponding node was already deleted
     */
    public Statement getStatement(int ID) {
        if (this.allNodes.containsKey(ID)) {
            Statement s = (Statement) (this.allNodes.get(ID).accept(new CloneVisitorCopyIDs(), null));
            s.setData(NODEKEY_ID, ID);
            return s;
        } else {
            return null;
        }
    }

    /**
     * @param ID of node to get
     * @return a clone of the specified node, or null if the corresponding node was already deleted
     */
    public Node getNode(int ID) {
        if (this.allNodes.containsKey(ID)) {
            Node n = (Node) (this.allNodes.get(ID).accept(new CloneVisitorCopyIDs(), null));
            n.setData(NODEKEY_ID, ID);
            return n;
        } else {
            return null;
        }
    }

    public List<Integer> getAllBlockIDs() {
        return Collections.unmodifiableList(allBlockIDs);
    }

    /**
     * also includes insertion at the start of the block
     *
     * @param block ID
     * @return null if blockID not found
     */
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
     * @param destinationInTargetMethod - true unless we are targeting whole class
     * @param sourceInTargetMethod      - true for swaps (both the statements to be in target method)
     *                                  - false for replace (only the destination to be in the target method)
     * @return a Map: each key is a statement ID; each value is a
     * list of any statements that match it; that is, the space of
     * target locations for MatchedReplaceStatement and MatchedSwapStatement
     * (the list will contain the destination statement, so if the list is
     * size 1, there are no matching statements!)
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
     * @param rng            random number generator used to choose an ID
     * @return -1 if no blocks found
     */
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
     * @param rng     random number generator used to choose an insertion point
     * @return -1 if no matching block found
     */
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
     * @param clazz          limit IDs to nodes that extend this class
     * @param rng            random number generator used to choose an ID
     * @return returns -1 if no matching nodes found
     */
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
     *
     * @param inTargetMethod limit IDs to target method if true, or anywhere the class otherwise
     * @param clazz          limit IDs to nodes that extend this class
     * @return a list of node IDs
     */
    public List<Integer> getNodeIDsByClass(boolean inTargetMethod, Class<? extends Node> clazz) {
        return getNodeIDsByClass(inTargetMethod, Collections.singletonList(clazz));
    }

    /**
     * @param inTargetMethod limit IDs to target method if true, or anywhere the class otherwise
     * @param clazzes        limit IDs to nodes that extend these classes
     * @return a list of node IDs
     */
    public List<Integer> getNodeIDsByClass(boolean inTargetMethod, List<Class<? extends Node>> clazzes) {
        List<Integer> rval;
        if (inTargetMethod) {
            rval = new ArrayList<>(targetMethodNodeIDs.size());
            for (int i : targetMethodNodeIDs) {
                for (Class<? extends Node> clazz : clazzes) {
                    if (clazz.isAssignableFrom(allNodes.get(i).getClass())) {
                        rval.add(i);
                        break;
                    }
                }
            }
        } else {
            rval = new ArrayList<>();
            for (Node n : allNodes.values()) {
                for (Class<? extends Node> clazz : clazzes) {
                    if (clazz.isAssignableFrom(n.getClass())) {
                        rval.add(n.containsData(NODEKEY_ID) ? n.getData(NODEKEY_ID) : NODE_NULL_ID); // no need to check for null, these nodes are only ones from the original and have IDs
                        break;
                    }
                }
            }
        }
        return rval;
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
     * @param index the statement index to return an ID for
     * @return the ID for the statement index, if the statements were numbered
     * in the order returned by CompilationUnit.getChildNodesByType(Statement.class)
     * NOTE: this uses the current state! so returned IDs will be different once things
     * have been moved / deleted; might even be null if you get an inserted statement
     */
    public int getIDForStatementNumber(int index) {
        List<Statement> l = compilationUnit.getChildNodesByType(Statement.class);
        if (l.get(index).containsData(NODEKEY_ID)) {
            return l.get(index).getData(NODEKEY_ID);
        } else {
            return SourceFileTree.NODE_NULL_ID;
        }
    }

    /**
     * @param index the block index to return an ID for
     * @return the ID for the block index, if the blocks were numbered
     * in the order returned by CompilationUnit.getChildNodesByType(BlockStmt.class)
     * NOTE: this uses the current state! so returned IDs will be different once things
     * have been moved / deleted
     */
    public int getIDForBlockNumber(int index) {
        List<BlockStmt> l = compilationUnit.getChildNodesByType(BlockStmt.class);
        if (l.get(index).containsData(NODEKEY_ID)) {
            return l.get(index).getData(NODEKEY_ID);
        } else {
            return SourceFileTree.NODE_NULL_ID;
        }
    }

    /**
     * @param ID the statement ID to return an index for
     * @return the statement index for the given statement ID, if the statements were numbered
     * in the order returned by CompilationUnit.getChildNodesByType(Statement.class)
     * NOTE: this uses the current state! so returned IDs will be different once things
     * have been moved / deleted; might even be null if you get an inserted statement;
     * <p>
     * returns -1 if not ID wasn't found (probably an ID for a non-statement node)
     */
    public int getStatementNumberForNodeID(int ID) {
        List<Statement> l = compilationUnit.getChildNodesByType(Statement.class);

        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).containsData(NODEKEY_ID) && l.get(i).getData(NODEKEY_ID) == ID) {
                return i;
            }
        }

        return -1;
    }



    /*============== the following are some helper methods and classes ==============*/

    /**
     * @param ID the block index to return an ID for
     * @return for the given block statement ID, return the index of that block
     * in the list returned by CompilationUnit.getChildNodesByType(BlockStmt.class)
     * NOTE: this uses the current state! so returned IDs will be different once things
     * have been moved / deleted
     * <p>
     * returns -1 if ID wasn't found (probably an ID for a non-statement node)
     */
    public int getBlockNumberForNodeID(int ID) {
        List<BlockStmt> l = compilationUnit.getChildNodesByType(BlockStmt.class);

        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).containsData(NODEKEY_ID) && l.get(i).getData(NODEKEY_ID) == ID) {
                return i;
            }
        }

        return -1;
    }

    public List<VariableTypeAndName> getPrimitiveVariablesInScopeForStatement(int ID) {
        // get parent, walk through its children until we reach the target statement
        List<VariableTypeAndName> rval = new ArrayList<>();
        Node n = allNodes.get(ID);

        while (n.getParentNode().isPresent()) {
            Node parent = n.getParentNode().get();

            // loop through children...
            for (Node child : parent.getChildNodes()) {
                // stop when we reach the present node
                if (child == n) {
                    break;
                }

                if ((child instanceof ExpressionStmt) && ((ExpressionStmt) child).getExpression() instanceof VariableDeclarationExpr) {
                    for (VariableDeclarator vd : ((VariableDeclarationExpr) ((ExpressionStmt) child).getExpression()).getVariables()) {
                        if ((vd.getType()).isPrimitiveType()) {
                            rval.add(new VariableTypeAndName(vd.getType(), vd.getName()));
                        }
                    }
                } else if (child instanceof Parameter) { // parameters of the containing method
                    if (((Parameter) child).getType().isPrimitiveType()) {
                        rval.add(new VariableTypeAndName(((Parameter) child).getType(), ((Parameter) child).getName()));
                    }
                } else if (child instanceof FieldDeclaration) {
                    for (VariableDeclarator vd : ((FieldDeclaration) child).getVariables()) {
                        if ((vd.getType()).isPrimitiveType()) {
                            rval.add(new VariableTypeAndName(vd.getType(), vd.getName()));
                        }
                    }
                }
            }

            // move up a level and try again
            n = parent;

        }

        return rval;
    }

    public static class VariableTypeAndName {
        public final Type type;
        public final SimpleName name;

        public VariableTypeAndName(Type t, SimpleName n) {
            type = t;
            name = n;
        }

        public Type getType() {
            return type;
        }

        public SimpleName getName() {
            return name;
        }

        public String toString() {
            return type + ":" + name;
        }
    }

}
