package gin.edit.line;

import gin.edit.Edit;

public abstract class LineEdit extends Edit {

    @Override
    public EditType getEditType() {
        return EditType.LINE;
    }

}
