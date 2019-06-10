package gin.edit.statement;

import gin.edit.Edit;

public abstract class StatementEdit extends Edit {
    
    @Override
    public EditType getEditType() {
        return EditType.STATEMENT;
    }
    
}
