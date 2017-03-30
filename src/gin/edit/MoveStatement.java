package gin.edit;


public class MoveStatement extends Edit {

    public int sourceStatement;
    public int destinationBlock;
    public int destinationChildInBlock;

    public MoveStatement(int sourceStatement, int destinationBlock, int destinationChildInBlock) {
        this.sourceStatement = sourceStatement;
        this.destinationBlock = destinationBlock;
        this.destinationChildInBlock = destinationChildInBlock;
    }

    @Override
    public String toString() {
        return "MOVE " + sourceStatement + " -> " + destinationBlock + ":" + destinationChildInBlock;
    }
    
}
