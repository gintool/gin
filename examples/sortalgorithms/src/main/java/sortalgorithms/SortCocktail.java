package sortalgorithms;
public class SortCocktail {
  public static Integer[] sort(  Integer[] a,  Integer length){
    boolean swapped;
    do {
      swapped=false;
      for (int i=0; i <= length - 2; i++) {
        if (a[i] > a[i + 1]) {
          int temp=a[i];
          a[i]=a[i + 1];
          a[i + 1]=temp;
          swapped=true;
        }
      }
      if (!swapped) {
        break;
      }
      swapped=false;
      for (int i=length - 2; i >= 0; i--) {
        if (a[i] > a[i + 1]) {
          int temp=a[i];
          a[i]=a[i + 1];
          a[i + 1]=temp;
          swapped=true;
        }
      }
    }
 while (swapped);
    return a;
  }
}

