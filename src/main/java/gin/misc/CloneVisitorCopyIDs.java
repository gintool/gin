package gin.misc;


import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.modules.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import gin.SourceFileTree;
import org.apache.commons.collections4.map.HashedMap;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

/**
 * A visitor that clones (copies) a node and all its children.
 * Extended to copy IDs as well.
 */
public class CloneVisitorCopyIDs extends CloneVisitor implements Serializable {

    @Serial
    private static final long serialVersionUID = -7030362755496561991L;
    private final Map<Integer, Node> nodesToReplace;

    public CloneVisitorCopyIDs() {
        nodesToReplace = new HashedMap<>();
    }

    public CloneVisitorCopyIDs(Map<Integer, Node> nodesToReplace) {
        this.nodesToReplace = new HashedMap<>();
        this.nodesToReplace.putAll(nodesToReplace);
    }

    @Override
    public Visitable visit(final CompilationUnit n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((CompilationUnit) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final PackageDeclaration n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((PackageDeclaration) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final TypeParameter n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((TypeParameter) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final LineComment n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((LineComment) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final BlockComment n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((BlockComment) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final ClassOrInterfaceDeclaration n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ClassOrInterfaceDeclaration) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final EnumDeclaration n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((EnumDeclaration) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final EnumConstantDeclaration n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((EnumConstantDeclaration) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final AnnotationDeclaration n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((AnnotationDeclaration) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final AnnotationMemberDeclaration n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((AnnotationMemberDeclaration) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final FieldDeclaration n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((FieldDeclaration) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final VariableDeclarator n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((VariableDeclarator) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final ConstructorDeclaration n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ConstructorDeclaration) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final MethodDeclaration n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((MethodDeclaration) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final Parameter n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((Parameter) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final InitializerDeclaration n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((InitializerDeclaration) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final JavadocComment n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((JavadocComment) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final ClassOrInterfaceType n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ClassOrInterfaceType) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final PrimitiveType n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((PrimitiveType) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final ArrayType n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ArrayType) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final ArrayCreationLevel n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ArrayCreationLevel) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final IntersectionType n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((IntersectionType) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final UnionType n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((UnionType) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final VoidType n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((VoidType) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final WildcardType n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((WildcardType) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final UnknownType n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((UnknownType) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final ArrayAccessExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ArrayAccessExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final ArrayCreationExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ArrayCreationExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final ArrayInitializerExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ArrayInitializerExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final AssignExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((AssignExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final BinaryExpr n, final Object arg) {
        Integer id = n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID;
        if (nodesToReplace.containsKey(id)) {
            return nodesToReplace.get(id);
        }

        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((BinaryExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final CastExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((CastExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final ClassExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ClassExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final ConditionalExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ConditionalExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final EnclosedExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((EnclosedExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final FieldAccessExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((FieldAccessExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final InstanceOfExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((InstanceOfExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final StringLiteralExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((StringLiteralExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final IntegerLiteralExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((IntegerLiteralExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final LongLiteralExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((LongLiteralExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final CharLiteralExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((CharLiteralExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final DoubleLiteralExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((DoubleLiteralExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final BooleanLiteralExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((BooleanLiteralExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final NullLiteralExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((NullLiteralExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final MethodCallExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((MethodCallExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final NameExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((NameExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final ObjectCreationExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ObjectCreationExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final Name n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((Name) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final SimpleName n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((SimpleName) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final ThisExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ThisExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final SuperExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((SuperExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final UnaryExpr n, final Object arg) {
        Integer id = n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID;
        if (nodesToReplace.containsKey(id)) {
            //r.setData(SourceFileTree.NODEKEY_ID, id);
            return nodesToReplace.get(id);
        }

        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((UnaryExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final VariableDeclarationExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((VariableDeclarationExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final MarkerAnnotationExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((MarkerAnnotationExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final SingleMemberAnnotationExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((SingleMemberAnnotationExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final NormalAnnotationExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((NormalAnnotationExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final MemberValuePair n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((MemberValuePair) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final ExplicitConstructorInvocationStmt n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ExplicitConstructorInvocationStmt) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final LocalClassDeclarationStmt n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((LocalClassDeclarationStmt) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final AssertStmt n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((AssertStmt) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final BlockStmt n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((BlockStmt) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final LabeledStmt n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((LabeledStmt) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final EmptyStmt n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((EmptyStmt) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final ExpressionStmt n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ExpressionStmt) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final SwitchStmt n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((SwitchStmt) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final SwitchEntry n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((SwitchEntry) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final BreakStmt n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((BreakStmt) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final ReturnStmt n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ReturnStmt) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final IfStmt n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((IfStmt) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final WhileStmt n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((WhileStmt) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final ContinueStmt n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ContinueStmt) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final DoStmt n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((DoStmt) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final ForEachStmt n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ForEachStmt) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final ForStmt n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ForStmt) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final ThrowStmt n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ThrowStmt) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final SynchronizedStmt n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((SynchronizedStmt) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final TryStmt n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((TryStmt) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final CatchClause n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((CatchClause) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final LambdaExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((LambdaExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final MethodReferenceExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((MethodReferenceExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final TypeExpr n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((TypeExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Node visit(final ImportDeclaration n, final Object arg) {
        Node r = super.visit(n, arg);
        r.setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        return r;
    }

    @Override
    public Visitable visit(final ModuleDeclaration n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ModuleDeclaration) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final ModuleRequiresDirective n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ModuleRequiresDirective) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    protected <T extends Node> T cloneNode(Optional<T> node, Object arg) {
        return super.cloneNode(node, arg);
    }

    @Override
    protected <T extends Node> T cloneNode(T node, Object arg) {
        return super.cloneNode(node, arg);
    }

    @Override
    public Visitable visit(final ModuleExportsDirective n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ModuleExportsDirective) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final ModuleProvidesDirective n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ModuleProvidesDirective) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final ModuleUsesDirective n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ModuleUsesDirective) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final ModuleOpensDirective n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ModuleOpensDirective) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(final UnparsableStmt n, final Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((UnparsableStmt) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(Modifier n, Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((Modifier) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(ReceiverParameter n, Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((ReceiverParameter) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(SwitchExpr n, Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((SwitchExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(TextBlockLiteralExpr n, Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((TextBlockLiteralExpr) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(VarType n, Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((VarType) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    @Override
    public Visitable visit(YieldStmt n, Object arg) {
        Visitable r = checkForReplacement(n);
        if (r == null) {
            r = super.visit(n, arg);
            ((YieldStmt) r).setData(SourceFileTree.NODEKEY_ID, n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID);
        }

        return r;
    }

    private Node checkForReplacement(Node n) {
        Integer id = n.containsData(SourceFileTree.NODEKEY_ID) ? n.getData(SourceFileTree.NODEKEY_ID) : SourceFileTree.NODE_NULL_ID;
        return nodesToReplace.getOrDefault(id, null);
    }

}
