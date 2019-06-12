package gin.edit.modifynode;

import gin.edit.Edit;
import gin.edit.statement.StatementEdit;
import org.apache.commons.lang3.NotImplementedException;


public abstract class ModifyNodeEdit extends StatementEdit {
    @Override
    public String toString() {
        return this.getClass().getCanonicalName() + " ";
    }

    @Override
    public EditType getEditType() {
        return EditType.MODIFY_STATEMENT;
    }

    public static Edit fromString(String description) {
        throw new NotImplementedException("Parsing from string not supported for node modification classes.");
    }

}
