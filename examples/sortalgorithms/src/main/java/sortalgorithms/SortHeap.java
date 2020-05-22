package sortalgorithms;
public class SortHeap {
  public static Integer[] sort(  Integer[] a,  Integer array_size){
    int i;
    for (i=(array_size / 2 - 1); i >= 0; --i) {
      int maxchild, temp, child, root=i, bottom=array_size - 1;
      while (root * 2 < bottom) {
        child=root * 2 + 1;
        if (child == bottom) {
          maxchild=child;
        }
 else {
          if (a[child] > a[child + 1]) {
            maxchild=child;
          }
 else {
            maxchild=child + 1;
          }
        }
        if (a[root] < a[maxchild]) {
          temp=a[root];
          a[root]=a[maxchild];
          a[maxchild]=temp;
        }
 else {
          break;
        }
        root=maxchild;
      }
    }
    for (i=array_size - 1; i >= 0; --i) {
      int temp;
      temp=a[i];
      a[i]=a[0];
      a[0]=temp;
      int maxchild, child, root=0, bottom=i - 1;
      while (root * 2 < bottom) {
        child=root * 2 + 1;
        if (child == bottom) {
          maxchild=child;
        }
 else {
          if (a[child] > a[child + 1]) {
            maxchild=child;
          }
 else {
            maxchild=child + 1;
          }
        }
        if (a[root] < a[maxchild]) {
          temp=a[root];
          a[root]=a[maxchild];
          a[maxchild]=temp;
        }
 else {
          break;
        }
        root=maxchild;
      }
    }
    return a;
  }
}

