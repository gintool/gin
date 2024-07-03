package gin.edit.modifynode;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.UnaryExpr.Operator;
import gin.SourceFile;
import gin.SourceFileTree;
import gin.edit.Edit;

import java.io.Serial;
import java.util.*;

public class UnaryOperatorReplacement extends ModifyNodeEdit {

    @Serial
    private static final long serialVersionUID = -3582626839165494107L;
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

    private final int targetNode;
    private final Operator source;
    private final Operator replacement;
    public String targetFilename;

    /**
     * @param sourceFile to create an edit for
     * @param rng        random number generator, used to choose the target statements
     * @throws NoApplicableNodesException if sourcefile doesn't contain any unary operators
     */
    // @param r is needed to choose a node and a suitable replacement
    //        (keeps this detail out of Patch class)
    public UnaryOperatorReplacement(SourceFile sourceFile, Random rng) throws NoApplicableNodesException {
        SourceFileTree sf = (SourceFileTree) sourceFile;
        this.targetNode = sf.getRandomNodeID(true, UnaryExpr.class, rng);

        if (this.targetNode < 0) {
            throw new NoApplicableNodesException();
        }

        this.source = ((UnaryExpr) sf.getNode(this.targetNode)).getOperator();
        this.replacement = chooseRandomReplacement(source, rng);
        this.targetFilename = sourceFile.getRelativePathToWorkingDir();
    }

    public UnaryOperatorReplacement(String sourceFileName, int targetNodeID, Operator sourceOperator, Operator replacementOperator) {
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

        return new UnaryOperatorReplacement(sourceFile, targetNodeID, sourceOperator, replacementOperator);
    }

    @Override
    public SourceFile apply(SourceFile sourceFile, Object metadata) {
        SourceFileTree sf = (SourceFileTree) sourceFile;
        Node node = sf.getNode(targetNode);

        // targeting a deleted location just does nothing.
        if (node != null) {
            ((UnaryExpr) node).setOperator(replacement);
            sf = sf.replaceNode(this.targetNode, node);
        }
        return sf;
    }

    @Override
    public EditType getEditType() {
        return EditType.MODIFY_STATEMENT;
    }

    @Override
    public String toString() {
        return super.toString() + " \"" + targetFilename + "\":" + targetNode + " " + source + " -> " + replacement + "";
    }
}
