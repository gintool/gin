package gin.edit.insert;

import gin.edit.statement.StatementEdit;

public abstract class InsertStatementEdit extends StatementEdit {
    
    @Override
    public EditType getEditType() {
        return EditType.INSERT_STATEMENT;
    }
    
}
