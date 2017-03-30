package gin.edit;


public class CopyStatement extends Edit {

    public int sourceStatement;
    public int destinationBlock;
    public int destinationChildInBlock;

    public CopyStatement(int sourceStatement, int destinationBlock, int destinationChildInBlock) {
        this.sourceStatement = sourceStatement;
        this.destinationBlock = destinationBlock;
        this.destinationChildInBlock = destinationChildInBlock;
    }

    @Override
    public String toString() {
        return "COPY " + sourceStatement + " -> " + destinationBlock + ":" + destinationChildInBlock;
    }

}
