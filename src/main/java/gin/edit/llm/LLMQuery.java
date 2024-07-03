package gin.edit.llm;

public interface LLMQuery {
    boolean testServerReachable();
    String chatLLM(String prompt);
}
