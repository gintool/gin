package gin.edit.llm;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.Node;
import org.pmw.tinylog.Logger;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.stmt.Statement;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import gin.SourceFile;
import gin.SourceFileTree;
import gin.edit.Edit;
import gin.edit.statement.StatementEdit;

public class LLMReplaceStatement extends StatementEdit {

    interface Chat {
        String chat(String userMessage);
    }
	
	private static final long serialVersionUID = 1112502387236768006L;
	
	public String destinationFilename;
    public int destinationStatement;
    
    /**fairly rubbish approach to having something meaningful for the toString*/
    private String lastReplacement;

    /**
     * create a random llmreplacestatement for the given sourcefile, using the provided RNG
     *
     * all this does is pick a location
     *
     * @param sourceFile to create an edit for
     * @param rng        random number generator, used to choose the target statements
     */
    public LLMReplaceStatement(SourceFile sourceFile, Random rng) {
        SourceFileTree sf = (SourceFileTree) sourceFile;

        destinationFilename = sourceFile.getRelativePathToWorkingDir();

        // target is in target method only
        destinationStatement = sf.getRandomBlockID(true, rng);
        
        lastReplacement = "NOT YET APPLIED";
    }

    public LLMReplaceStatement(String destinationFilename, int destinationStatement) {
        this.destinationFilename = destinationFilename;
        this.destinationStatement = destinationStatement;
        
        this.lastReplacement = "NOT YET APPLIED";
    }

    public static Edit fromString(String description) {
    	// TODO - update with lastReplacement
        String[] tokens = description.split("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        String[] destTokens = tokens[1].split(":");
        String destFilename = destTokens[0].replace("\"", "");
        int destination = Integer.parseInt(destTokens[1]);
        return new LLMReplaceStatement(destFilename, destination);
    }

    @Override
    public SourceFile apply(SourceFile sourceFile) {
    	List<SourceFile> l = applyMultiple(sourceFile, 5);
    	
    	if (l.size() > 0) {
    		return l.get(0); // TODO for now, just pick the first variant provided. Later, call applyMultiple from LocalSearch instead 
    	} else {
    		return null;
    	}
    }
    
    
    public List<SourceFile> applyMultiple(SourceFile sourceFile, int count) {
        	
        SourceFileTree sf = (SourceFileTree) sourceFile;

        Node destination = sf.getNode(destinationStatement);

        if (destination == null) {
            return Collections.singletonList(sf); // targeting a deleted location just does nothing.
        }


        // here is where the magic happens...
        OpenAiChatModel model = OpenAiChatModel.builder().apiKey(LLMConfig.openAIKey).timeout(Duration.ofSeconds(LLMConfig.timeoutInSeconds)).temperature(LLMConfig.temperature).build(); //OpenAiChatModel.withApiKey(ApiKeys.OPENAI_API_KEY);
		
		Chat chat = AiServices.builder(Chat.class)
                .chatLanguageModel(model)
                .chatMemory(MessageWindowChatMemory.withCapacity(10))
                .build();

		// TODO here, could call sourceFile.getSource() to provide whole class for context...
		
		Logger.info("Seeking replacements for:");
		Logger.info(destination);
		
		String prompt;
		
		switch (LLMConfig.promptType) {
			case SIMPLE:
				prompt = "Give me " + count + " implementations of this:"
		        		+ "```\n"
		        		+ destination
		        		+ "\n"
		        		+ "```\n";
		        
			break;
			case MEDIUM:
			default:
				prompt = "Give me " + count + " different Java implementations of this method body:"
		        		+ "```\n"
		        		+ destination
		        		+ "\n"
		        		+ "```\n"
		        		+ "This code belongs to project " + LLMConfig.projectName + ". "
		                + "Wrap all code in curly braces, if it is not already."
		                + "Do not include any method or class declarations."
		                + "label all code as java.";
			break;
			case DETAILED:
				prompt = "Give me " + count + " different Java implementations of this method body:"
		        		+ "```\n"
		        		+ destination
		        		+ "\n"
		        		+ "```\n"
		        		+ "This code belongs to project " + LLMConfig.projectName + ". "
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
		                + "label all code as java.";	
			break;
		}
		
    	Logger.info("============");
    	Logger.info("prompt:");
    	Logger.info(prompt);
    	Logger.info("============");

		
        String answer = chat.chat(prompt); 
        
        
        // answer includes code enclosed in ```java   ....``` or ```....``` blocks
        // use regex to find all of these then parse into javaparser objects for return
		Pattern pattern = Pattern.compile("```(?:java)(.*?)```", Pattern.DOTALL | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(answer);

        // now parse the strings return by LLM into JavaParser Statements
        List<String> replacementStrings = new ArrayList<>();
        List<Statement> replacementStatements = new ArrayList<>();
        while (matcher.find()) {
        	String str = matcher.group(1);

        	try {
                Statement stmt;
                stmt = StaticJavaParser.parseBlock(str);
                replacementStrings.add(str);
                replacementStatements.add(stmt);
            }
            catch (ParseProblemException e) {
                continue;
            }

        }
        
        List<SourceFile> variantSourceFiles = new ArrayList<>();
        
        int i = 1;
        for (String s : replacementStrings) {
        	Logger.info("============");
        	Logger.info("suggestion " + i++);
        	Logger.info(s);
        	Logger.info("============");
        }
        
        if (replacementStrings.isEmpty()) {
        	Logger.info("============");
        	Logger.info("No replacements found. Response was:");
        	Logger.info(answer);
        	Logger.info("============");
        	this.lastReplacement = "LLM GAVE NO SUGGESTIONS";
        } else {
        	this.lastReplacement = replacementStrings.get(0);
        }
        
        // replace the original statements with the suggested ones
        for (Statement s : replacementStatements) {
        	try {
	            variantSourceFiles.add(sf.replaceNode(destinationStatement, s));
	        } catch (ClassCastException e) { // JavaParser sometimes throws this if the statements don't match
	        	// do nothing...
	        }
        }
        
        return variantSourceFiles;
    }

    @Override
    public String toString() {
        return this.getClass().getCanonicalName() + " \"" + destinationFilename + "\":" + destinationStatement + " !!!" + lastReplacement + "!!!";
    }

}
