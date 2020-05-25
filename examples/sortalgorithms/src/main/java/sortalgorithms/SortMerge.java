package sortalgorithms;
public class SortMerge {
  public static Integer[] sort(  Integer[] a,  Integer length){
    mergesort_r(0,length,a);
    return a;
  }
  public static Integer[] merge(  Integer[] a,  int left_start,  int left_end,  int right_start,  int right_end){
    int left_length=left_end - left_start;
    int right_length=right_end - right_start;
    int[] left_half=new int[left_length];
    int[] right_half=new int[right_length];
    int r=0;
    int l=0;
    int i=0;
    for (i=left_start; i < left_end; i++, l++) {
      left_half[l]=a[i];
    }
    for (i=right_start; i < right_end; i++, r++) {
      right_half[r]=a[i];
    }
    for (i=left_start, r=0, l=0; l < left_length && r < right_length; i++) {
      if (left_half[l] < right_half[r]) {
        a[i]=left_half[l++];
      }
 else {
        a[i]=right_half[r++];
      }
    }
    for (; l < left_length; i++, l++) {
      a[i]=left_half[l];
    }
    for (; r < right_length; i++, r++) {
      a[i]=right_half[r];
    }
    return a;
  }
  public static Integer[] mergesort_r(  int left,  int right,  Integer[] a){
    if (right - left <= 1) {
      return a;
    }
 else {
    }
    int left_start=left;
    int left_end=(left + right) / 2;
    int right_start=left_end;
    int right_end=right;
    mergesort_r(left_start,left_end,a);
    mergesort_r(right_start,right_end,a);
    merge(a,left_start,left_end,right_start,right_end);
    return a;
  }
}

