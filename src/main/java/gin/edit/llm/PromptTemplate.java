package gin.edit.llm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.pmw.tinylog.Logger;

public class PromptTemplate {
	public enum PromptTag {
		COUNT, DESTINATION, ERROR, HINT, PREVIOUS, PROJECT;
		
		public String withEscape() {
			return "$" + this.name() + "$";
		}
	}
	
	private final String template;
    
	public PromptTemplate(String template) {
        this.template = template;
    }
    
    public static PromptTemplate fromFile(String filename) {
    	StringBuffer buffer = new StringBuffer();
    	
    	try {
    		BufferedReader in = new BufferedReader(new FileReader(filename));
    		
    		for (String line = null; (line = in.readLine()) != null; ) {
    			buffer.append(line);
    			buffer.append(System.lineSeparator());
    		}
    		
    		in.close();
    	} catch (IOException e) {
    		Logger.error("Error reading prompt template:");
    		Logger.error(e);
    	}
    	
    	return new PromptTemplate(buffer.toString());
    }
    
    public String replaceTags(Map<PromptTag,String> replacements) {
    	String prompt = new String(this.template);
    	
    	Set<PromptTag> unusedTags = new HashSet<>();
    	unusedTags.addAll(Arrays.asList(PromptTag.values()));
    	
    	// first replace everything we've got replacements for in the map
    	for (Entry<PromptTag,String> replacement : replacements.entrySet()) {
    		if (replacement.getValue() != null) {
    			prompt = prompt.replace(replacement.getKey().withEscape(), replacement.getValue());
    			unusedTags.remove(replacement.getKey());
    		} else {
    			Logger.error("Replacement for " + replacement.getKey() + " was null!");
    		}
    	}
    	
    	// now replace everything we've not got replacements for (these will be the only tags remaining)
    	// just blank them for now
    	for (PromptTag tag : unusedTags) {
    		prompt = prompt.replace(tag.withEscape(), "");
    	}
    	
    	return prompt;
    }
}
