package gin.edit;

import gin.SourceFile;

/**
 * The no-op Edit 
 */
public class NoEdit extends Edit {

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
