package gin.edit.llm;

import org.checkerframework.checker.units.qual.s;

import dev.langchain4j.model.openai.OpenAiModelName;
import gin.edit.llm.PromptTemplate.PromptTag;

public class LLMConfig {

	/** the following are some default template prompts */
	public enum PromptType {
		SIMPLE(new PromptTemplate("Give me " + PromptTag.COUNT.withEscape() + " implementations of this:"
        		+ "```\n"
        		+ PromptTag.DESTINATION.withEscape()
        		+ "\n"
        		+ "```\n")), 
		
		MEDIUM(new PromptTemplate("Give me " + PromptTag.COUNT.withEscape() + " different Java implementations of this method body:"
        		+ "```\n"
        		+ PromptTag.DESTINATION.withEscape()
        		+ "\n"
        		+ "```\n"
        		+ "This code belongs to project " + PromptTag.PROJECT.withEscape() + ". "
                + "Wrap all code in curly braces, if it is not already."
                + "Do not include any method or class declarations."
                + "label all code as java.")), 
		
		DETAILED(new PromptTemplate("Give me " + PromptTag.COUNT.withEscape() + " different Java implementations of this method body:"
        		+ "```\n"
        		+ PromptTag.DESTINATION.withEscape()
        		+ "\n"
        		+ "```\n"
        		+ "This code belongs to project " + PromptTag.PROJECT.withEscape() + ". "
        		+ "In the org.jcodec.scale.BaseResampler class, the following change was helpful. I changed this:"
        		+ "```\n"
        		+ "	if (temp == null) {"
        		+ "		temp = new int[toSize.getWidth() * (fromSize.getHeight() + nTaps())];"
        		+ "		tempBuffers.set(temp);"
        		+ "	}"
        		+ "```\n"
        		+ "into this:"
        		+ "```\n"
        		+ "	if (temp == null) {"
        		+ "		if (scaleFactorX >= 0)"
        		+ "			return;"
        		+ "		temp = new int[toSize.getWidth() * (fromSize.getHeight() + nTaps())];"
        		+ "		tempBuffers.set(temp);"
        		+ "	}"
        		+ "```\n"
                + "Wrap all code in curly braces, if it is not already."
                + "Do not include any method or class declarations."
                + "label all code as java.")), 


		MASKED(new PromptTemplate("Please replace <<PLACEHOLDER>> sign in the function below with meaningfull implementation. \n"
				+ "```\n"
				+ PromptTag.DESTINATION.withEscape()
				+ "\n"
				+ "```\n"
				+ "This code belongs to project " + PromptTag.PROJECT.withEscape() + ". "
				+ "Wrap all code in curly braces, if it is not already. "
				+ "Do not include any class declarations. "
				+ "Label all code as java.")),;
		
		
		
		public final PromptTemplate template;
	    private PromptType(PromptTemplate template) {
	        this.template = template;
	    }
	}
	
    // You can use "demo" api key for demonstration purposes.
    public static String openAIKey = "demo";
    
    public static String openAIModelName = OpenAiModelName.GPT_3_5_TURBO;
    
    public static String modelType="OpenAI"; // Should be param from c'tor

    public static long timeoutInSeconds = 30;
    
    // default for langchain4j
    public static double temperature = 0.7;
    
    public static PromptType defaultPromptType = PromptType.MEDIUM;
    
    public static PromptTemplate defaultPromptTemplate = null;
    
    public static PromptTemplate getDefaultPromptTemplate() {
    	return (defaultPromptTemplate != null) ? defaultPromptTemplate : defaultPromptType.template;
    }
    
    public static String projectName = "";
    
    
    
}