package gin.edit;

import gin.SourceFile;

import java.io.Serial;

/**
 * The no-op Edit
 */
public class NoEdit extends Edit {

    @Serial
    private static final long serialVersionUID = 3415362789416357180L;

    public static Edit fromString(String description) {
        return new NoEdit();
    }

    @Override
    public EditType getEditType() {
        return null;
    }

    @Override
    public SourceFile apply(SourceFile sourceFile, Object metadata) {
        return sourceFile;
    }

}
