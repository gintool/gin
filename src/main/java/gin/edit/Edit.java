package gin.edit;

import gin.SourceFile;

public abstract class Edit {

    public enum EditType { LINE, STATEMENT, MODIFY_STATEMENT, MATCHED_STATEMENT}
    
    public abstract EditType getEditType();

    public abstract SourceFile apply(SourceFile sourceFile);

    // Java fail: can't be abstract and static.
    // If you want to be able to parse your patches from a string, you'll need to implement this method
    // public static Edit fromString(String description);

}
