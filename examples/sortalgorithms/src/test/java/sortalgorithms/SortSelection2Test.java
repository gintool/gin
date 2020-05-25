package sortalgorithms;
import static org.junit.Assert.*;
import java.util.Arrays;

public class SortSelection2Test {

	Integer[][] testArrSet = { { 2, 1 }, // This will test for a swap only
			{ 2, 3, 1 }, { 1, 2, 3 }, { 1, 939, 950, 520, 3346, 3658, 2335, 6174, 2377, 796 },
			{ 1000024, 999927, 999849, 999761, 999650, 999576, 999422, 999378, 999276, 999144 }, // reverse
			{ -1935783155, 805693102, 1011599466, -368696979, 814152454, 1502428812, 1640419215, 879631257, -1555817806,
					-987937568 },
			{ -1935783155, 8, 6, 101, -368696979, 8, 1, 5, 0, 2, 4, 2, 8, 8, 1, 2, 1, 6, 4, 0, 4, 1, 9, 2, 1, 5, 8, 7,
					9, 6, 3, 1, 2, 5, 7, -1555817806, -95, 68 } };

	Integer[][] expectedArrSet = sortArrays();

	private Integer[][] sortArrays() {
		Integer[][] sortedArraySet = new Integer[testArrSet.length][];
		for (int i = 0; i < testArrSet.length; i++) {
			Integer[] clonedTestArr = testArrSet[i].clone();
			Arrays.sort(clonedTestArr);
			sortedArraySet[i] = clonedTestArr;
		}
		return sortedArraySet;
	}

	// protected abstract Integer[] testSpecificSort( Integer[] a, Integer
	// length ); // nein
	protected Integer[] testSpecificSort(Integer[] a, Integer length) {
		return SortSelection2.sort(a, length);
	}

	private void testSortAtIndex(int sortArrIndex) {
		Integer[] sortingAttempt = testSpecificSort(testArrSet[sortArrIndex].clone(), testArrSet[sortArrIndex].length);
		assertArrayEquals(expectedArrSet[sortArrIndex], sortingAttempt);
	}

	@org.junit.Test
	public void checkSorting0() throws Exception {
		testSortAtIndex(0);
	}

	@org.junit.Test
	public void checkSorting1() throws Exception {
		testSortAtIndex(1);
	}

	@org.junit.Test
	public void checkSorting2() throws Exception {
		testSortAtIndex(2);
	}

	@org.junit.Test
	public void checkSorting3() throws Exception {
		testSortAtIndex(3);
	}

	@org.junit.Test
	public void checkSorting4() throws Exception {
		testSortAtIndex(4);
	}

	@org.junit.Test
	public void checkSorting5() throws Exception {
		testSortAtIndex(5);
	}

	@org.junit.Test
	public void checkSorting6() throws Exception {
		testSortAtIndex(6);
	}

}
