package gin.edit.line;

import gin.edit.Edit;

import java.io.Serial;

public abstract class LineEdit extends Edit {

    @Serial
    private static final long serialVersionUID = -8096264705859395686L;

    @Override
    public EditType getEditType() {
        return EditType.LINE;
    }

}
