package gin.hom;

/*
  A class used to represent a Higher Order Mutation
  Mutations are encoded as integers in the mutations vector
  The Encoding goes as follows:
  0= NoEdit, 1 = Insert Break, 2 = InsertBreakWithIf, 3 = Insert Continue
  4 = InsertContinueWithIf, 5 = Insert Return, 6 = InsertReturnWithIf
  7 = CopyLine, 8 = DeleteLine, 9 = MoveLine, 10 = ReplaceLine
  11 = SwapLine,
 */

import java.util.Vector;

public class Hom {
    private int loc;
    private Vector<Integer> mutations;
    private int test_results;

    Hom(){
        //Todo
    }

    public void run_tests(){
        //Todo
    }

    public Vector<Integer> get_mutations(){
        return this.mutations;

    }
    public int get_test_results(){
        return this.test_results;
    }

}
