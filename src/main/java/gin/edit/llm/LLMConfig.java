package gin.edit.llm;

public class LLMConfig {

	public enum PromptType { SIMPLE, MEDIUM, DETAILED }
	
    // You can use "demo" api key for demonstration purposes.
    public static String openAIKey = "demo";

    public static long timeoutInSeconds = 30;
    
    // default for langchain4j
    public static double temperature = 0.7;
    
    public static PromptType promptType = PromptType.MEDIUM;
    
    public static String projectName = "";
    
}