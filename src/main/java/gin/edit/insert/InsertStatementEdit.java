package gin.edit.insert;

import gin.edit.statement.StatementEdit;

public abstract class InsertStatementEdit extends StatementEdit {

    private static final long serialVersionUID = 11592091634673619L;

    @Override
    public EditType getEditType() {
        return EditType.INSERT_STATEMENT;
    }
    
}
