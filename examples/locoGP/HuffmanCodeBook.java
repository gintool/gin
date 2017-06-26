
public class BasicHuffman {
  public static String[] getCodeBook(  Byte[] bytes){
    BubbleSort.sort(bytes,bytes.length);
    Byte[] uniqueChars=getUniqueChars(bytes);
    huffmanNode[] freqTable=getCharFreq(bytes,uniqueChars);
    huffmanNode huffTree=buildTree(freqTable);
    String[] codeBook=new String[0];
    codeBook=getCodes(huffTree,"",codeBook);
    return codeBook;
  }
  private static String[] getCodes(  huffmanNode huffTree,  String prefix,  String[] codeBook){
    if (huffTree.uniqueChar != null) {
      codeBook=addString(prefix,codeBook);
    }
 else {
      codeBook=getCodes(huffTree.left,prefix + "1",codeBook);
      codeBook=getCodes(huffTree.right,prefix + "0",codeBook);
    }
    return codeBook;
  }
  private static String[] addString(  String aStr,  String[] otherStrings){
    String[] newStrings=new String[otherStrings.length + 1];
    for (int i=0; i < otherStrings.length; i++) {
      newStrings[i]=otherStrings[i];
    }
    newStrings[newStrings.length - 1]=aStr;
    return newStrings;
  }
  private static huffmanNode buildTree(  huffmanNode[] freqTable){
    BubbleSort.sort(freqTable,freqTable.length);
    huffmanNode aRight=freqTable[freqTable.length - 1];
    huffmanNode aLeft=freqTable[freqTable.length - 2];
    huffmanNode newNode=new huffmanNode(aRight.getFreq() + aLeft.getFreq(),aRight,aLeft);
    huffmanNode[] newList=new huffmanNode[freqTable.length - 1];
    for (int i=0; i < newList.length; i++) {
      newList[i]=freqTable[i];
    }
    newList[newList.length - 1]=newNode;
    if (newList.length == 1) {
      return newList[0];
    }
 else {
      return buildTree(newList);
    }
  }
  private static huffmanNode[] getCharFreq(  Byte[] bytes,  Byte[] uniqueChars){
    int[] freqInts=new int[uniqueChars.length];
    int charIndex=0;
    for (int i=0; i < bytes.length; i++) {
      if (bytes[i].compareTo(uniqueChars[charIndex]) == 0) {
        freqInts[charIndex]++;
      }
 else {
        charIndex++;
        freqInts[charIndex]++;
      }
    }
    huffmanNode[] freqTable=new huffmanNode[uniqueChars.length];
    for (int i=0; i < uniqueChars.length; i++) {
      freqTable[i]=new huffmanNode(uniqueChars[i],freqInts[i]);
    }
    return freqTable;
  }
  private static Byte[] getUniqueChars(  Byte[] bytes){
    Byte[] returnChars=new Byte[1];
    returnChars[0]=bytes[0];
    for (int i=0; i < bytes.length; i++) {
      if (returnChars[returnChars.length - 1].compareTo(bytes[i]) != 0) {
        Byte[] tempChars=returnChars;
        returnChars=new Byte[tempChars.length + 1];
        for (int j=0; j < tempChars.length; j++) {
          returnChars[j]=tempChars[j];
        }
        returnChars[returnChars.length - 1]=bytes[i];
      }
    }
    return returnChars;
  }
}


private class BubbleSort {
  public static <T extends Comparable<? super T>>void sort(  T[] a,  Integer length){
    for (int i=0; i < length; i++) {
      for (int j=0; j < length - 1; j++) {
        if (a[j].compareTo(a[j + 1]) < 0) {
          T k=a[j];
          a[j]=a[j + 1];
          a[j + 1]=k;
        }
      }
    }
  }
}


private class huffmanNode implements Comparable {
  Byte uniqueChar=null;
  int freq=0;
  huffmanNode left, right;
  public int getFreq(){
    return freq;
  }
  huffmanNode(  byte aChar,  int freq){
    uniqueChar=aChar;
    this.freq=freq;
  }
  huffmanNode(  int freq,  huffmanNode left,  huffmanNode right){
    this.freq=freq;
    this.right=right;
    this.left=left;
  }
  @Override public int compareTo(  Object hN){
    return this.freq - ((huffmanNode)hN).freq;
  }
}

