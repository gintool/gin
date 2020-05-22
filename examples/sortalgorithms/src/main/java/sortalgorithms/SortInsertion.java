package sortalgorithms;
public class SortInsertion {
  public static Integer[] sort(  Integer[] a,  Integer array_size){
    int i, j, index;
    for (i=1; i < array_size; ++i) {
      index=a[i];
      for (j=i; j > 0 && a[j - 1] > index; j--) {
        a[j]=a[j - 1];
      }
      a[j]=index;
    }
    return a;
  }
}

