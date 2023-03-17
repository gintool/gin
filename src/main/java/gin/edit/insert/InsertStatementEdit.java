package gin.edit.insert;

import gin.edit.statement.StatementEdit;

import java.io.Serial;

public abstract class InsertStatementEdit extends StatementEdit {

    @Serial
    private static final long serialVersionUID = 11592091634673619L;

    @Override
    public EditType getEditType() {
        return EditType.INSERT_STATEMENT;
    }

}
