package mypackage;

public class ExampleMethodSignature {

    public void exampleNoParamsVoidReturn(){
        return;
    }

    public int exampleNoParamsSimpleReturn(){
        return 1;
    }

    public void exampleSimpleParamVoidReturn(int param){
        return;
    }

    public int exampleSimpleParamSimpleReturn(int param){
        return 1;
    }

    public static @Inject List<List<Map<Integer, Pair<Double, Object>>>> exampleComplexParamComplexReturn(@NotNull List<List<Map<Integer, Pair<Double, Object>>>> param1, List<Map<Integer, Object>> param2){
        return null;
    }
}