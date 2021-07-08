package gin.edit;

import gin.SourceFile;

/**
 * The no-op Edit 
 */
public class NoEdit extends Edit {

    private static final long serialVersionUID = 3415362789416357180L;

    @Override
    public EditType getEditType() {
        return null;
    }

    @Override
    public SourceFile apply(SourceFile sourceFile) {
        return sourceFile;
    }

    public static Edit fromString(String description) {
        return new NoEdit();
    }

}
