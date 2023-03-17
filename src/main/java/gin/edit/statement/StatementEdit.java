package gin.edit.statement;

import gin.edit.Edit;

import java.io.Serial;

public abstract class StatementEdit extends Edit {

    @Serial
    private static final long serialVersionUID = -213713658763660537L;

    @Override
    public EditType getEditType() {
        return EditType.STATEMENT;
    }

}
