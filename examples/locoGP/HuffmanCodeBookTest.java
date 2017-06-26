import static org.junit.Assert.*;
import java.util.Arrays;

public class SortBubbleTest {

    private String[] testSet = { "A string of text with various characters",                                      
                                 "The quick brown fox jumps over the lazy dog",                                       
                                 "His face was the true index of his mind.",
                                 "asdflkjsdflkjasdfljkeebcekekjceiueouiasknelaohuaybltnzlmaxe" +                   			
                                 "219056810287wjhanslkdhfkl23gho9a8yhzxhlfasjkdglkdnlqnw;oafncue;mjhnsklfhg" +
                                 "sadjflashtlnyrwmsdhfmawioexfmur489757q2382opqi10`kjmlkmjsufzwo;eijmzsm",
                                 "z y x w v u t s r q p o n m l k j i h g f e d c b a ` _ ^ ]  [ Z Y X W V",
                                 " U T S R Q P O N M L K J I H G F E D C B A"};
    
    private TestCase createCase(String string) {
        Byte[] testInput = ArrayUtils.toObject(string.getBytes());
        String[] sampleResult = BasicHuffman.getCodeBook(testInput);
        /*	getCodeBook sorts the array in place :/ */
        Byte[] testInputOrig = ArrayUtils.toObject(string.getBytes());
        return new prefixCodeTestCase(new Object[]{testInputOrig}, sampleResult);
    }
    
    
    @org.junit.Test
	public void checkSorting0() throws Exception {
        testSortAtIndex(0);
    }
    
}
