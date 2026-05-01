package gin.edit.modifynode;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import gin.SourceFile;
import gin.SourceFileTree;
import gin.edit.Edit;

import java.io.Serial;
import java.util.*;

public class BinaryOperatorReplacement extends ModifyNodeEdit {

    @Serial
    private static final long serialVersionUID = 3685538795989546595L;
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

    private final int targetNode;
    private final Operator source;
    private final Operator replacement;
    public String targetFilename;

    /**
     * Following MuJava, this covers the binary operator replacements in:
     * arithmetic operator replacement, relational op repl, conditional op repl, shift op repl,
     *
     * @param sourceFile to create an edit for
     * @param rng        random number generator, used to choose the target statements
     * @throws NoApplicableNodesException if sourcefile doesn't contain any binary operators
     */
    // @param sourceNodes is the list of possible nodes for modification; these won't be
    //           modified, just used for reference
    // @param r is needed to choose a node and a suitable replacement
    //        (keeps this detail out of Patch class)
    public BinaryOperatorReplacement(SourceFile sourceFile, Random rng) throws NoApplicableNodesException {
        SourceFileTree sf = (SourceFileTree) sourceFile;
        this.targetNode = sf.getRandomNodeID(true, BinaryExpr.class, rng);

        if (this.targetNode < 0) {
            throw new NoApplicableNodesException();
        }

        this.source = ((BinaryExpr) sf.getNode(this.targetNode)).getOperator();
        this.replacement = chooseRandomReplacement(source, rng);
        this.targetFilename = sourceFile.getRelativePathToWorkingDir();
    }

    public BinaryOperatorReplacement(String sourceFileName, int targetNodeID, Operator sourceOperator, Operator replacementOperator) {
        this.targetNode = targetNodeID;
        this.source = sourceOperator;
        this.replacement = replacementOperator;
        this.targetFilename = sourceFileName;
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

    public static Edit fromString(String description) {
        String[] tokens = description.split("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        String[] sourceTokens = tokens[1].split(":");
        String sourceFile = sourceTokens[0].replace("\"", "");
        int targetNodeID = Integer.parseInt(sourceTokens[1]);
        Operator sourceOperator = Operator.valueOf(tokens[2]);
        Operator replacementOperator = Operator.valueOf(tokens[4]);

        return new BinaryOperatorReplacement(sourceFile, targetNodeID, sourceOperator, replacementOperator);
    }

    @Override
    public SourceFile apply(SourceFile sourceFile, Object metadata) {
        SourceFileTree sf = (SourceFileTree) sourceFile;

        // first, get the node from the cu
        Node node = sf.getNode(this.targetNode);
        // targeting a deleted location does nothing, that's why we need this check before.
        if (node != null) {
            ((BinaryExpr) node).setOperator(replacement);
            sf = sf.replaceNode(this.targetNode, node);
        }
        return sf;
    }

    @Override
    public String toString() {
        return super.toString() + " \"" + targetFilename + "\":" + targetNode + " " + source + " -> " + replacement + "";
    }
}
