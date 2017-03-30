package gin.edit;


public class DeleteStatement extends Edit {

    public int statementToDelete;

    public DeleteStatement(int lineToDelete) {
        this.statementToDelete = lineToDelete;
    }

    @Override
    public String toString() {
        return "DEL " + statementToDelete;
    }
}
