package gin.edit.statement;

import gin.edit.Edit;

public abstract class StatementEdit extends Edit {
    
    private static final long serialVersionUID = -213713658763660537L;

    @Override
    public EditType getEditType() {
        return EditType.STATEMENT;
    }
    
}
