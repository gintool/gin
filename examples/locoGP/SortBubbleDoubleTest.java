
public class SortBubbleDoubleTest extends SortTest {
    
    @Override
    protected Integer[] testSpecificSort( Integer[] a, Integer length ){
        return SortBubbleDouble.sort(a,length);
    }
    
}
