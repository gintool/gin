package sortalgorithms;
public class SortSelection2 {
  public static Integer[] sort(  Integer[] a,  Integer length){
    double p=0;
    int k=0;
    for (int i=0; i < length - 1; i++) {
      k=i;
      for (int j=i + 1; j < length; j++) {
        if (a[j] < a[k])         k=j;
      }
      p=a[i];
      a[i]=a[k];
      a[k]=(int)p;
    }
    return a;
  }
}

