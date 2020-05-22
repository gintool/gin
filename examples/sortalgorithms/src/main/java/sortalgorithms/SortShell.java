package sortalgorithms;
public class SortShell {
  public static Integer[] sort(  Integer[] a,  Integer length){
    int increment=length / 2;
    while (increment > 0) {
      for (int i=increment; i < length; i++) {
        int j=i;
        int temp=a[i];
        while (j >= increment && a[j - increment] > temp) {
          a[j]=a[j - increment];
          j=j - increment;
        }
        a[j]=temp;
      }
      if (increment == 2) {
        increment=1;
      }
 else {
        increment*=(5.0 / 11);
      }
    }
    return a;
  }
}

