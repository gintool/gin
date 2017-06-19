
public class SortBubbleLoopsTest extends SortTest {

    @Override
    protected Integer[] testSpecificSort( Integer[] a, Integer length ){
        return SortBubbleLoops.sort(a,length);
    }

}
