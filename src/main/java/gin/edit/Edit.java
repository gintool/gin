package gin.edit;

import gin.SourceFile;

/**
 * Abstract class representing an Edit.
 * <p>
 * Java fail: can't be abstract and static.
 * If you want to be able to parse your patches from a 
 * string, you'll need to implement this method
 * public static Edit fromString(String description);
 * 
 * (this is needed for at least PatchAnalyser)
 */
public abstract class Edit {

    public enum EditType { LINE, STATEMENT, MODIFY_STATEMENT, MATCHED_STATEMENT}

    public abstract EditType getEditType();

    /**
     * @param sourceFile on which to apply the edit
     * @return updated copy of the sourceFile; or null if the edit couldn't be applied for some reason 
     */
    public abstract SourceFile apply(SourceFile sourceFile);

}
