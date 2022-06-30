package gin.edit.line;

import gin.edit.Edit;

public abstract class LineEdit extends Edit {

    private static final long serialVersionUID = -8096264705859395686L;

    @Override
    public EditType getEditType() {
        return EditType.LINE;
    }

}
