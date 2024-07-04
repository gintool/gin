package gin.edit.llm;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pmw.tinylog.Logger;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.utils.Log;

import io.github.amithkoujalgi.ollama4j.core.OllamaAPI;

import gin.SourceFile;
import gin.SourceFileTree;
import gin.edit.Edit;
import gin.edit.llm.PromptTemplate.PromptTag;
import gin.edit.statement.StatementEdit;

import gin.edit.llm.LLMQuery;
import gin.edit.llm.OpenAILLMQuery;
import gin.edit.llm.Ollama4jLLMQuery;

public class LLMReplaceStatement extends StatementEdit {

    private static final long serialVersionUID = 1112502387236768006L;
    public String destinationFilename;
    public int destinationStatement;

    private PromptTemplate promptTemplate;
    //private String modelType="OpenAI"; // Should be param from c'tor
    //private String modelType = "magicoder";
    // All strings are here: https://github.com/amithkoujalgi/ollama4j/blob/main/src/main/java/io/github/amithkoujalgi/ollama4j/core/types/OllamaModelType.java

    /**fairly rubbish approach to having something meaningful for the toString*/
    private String lastReplacement;
    private String lastPrompt;

    /**
     * create a random llmreplacestatement for the given sourcefile, using the provided RNG
     *
     * all this does is pick a location
     *
     * @param sourceFile to create an edit for
     * @param rng        random number generator, used to choose the target statements
     */
    public LLMReplaceStatement(SourceFile sourceFile, Random rng, PromptTemplate promptTemplate) {
        SourceFileTree sf = (SourceFileTree) sourceFile;

        destinationFilename = sourceFile.getRelativePathToWorkingDir();

        // target is in target method only
        destinationStatement = sf.getRandomBlockID(true, rng);

        this.promptTemplate = promptTemplate;

        lastReplacement = "NOT YET APPLIED";
        lastPrompt = "NOT YET APPLIED";
    }

    public LLMReplaceStatement(SourceFile sourceFile, Random rng) {
    	this(sourceFile, rng, LLMConfig.getDefaultPromptTemplate());
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
    public SourceFile apply(SourceFile sourceFile, Object tagReplacements) {
    	List<SourceFile> l = applyMultiple(sourceFile, 5, (Map<PromptTemplate.PromptTag,String>)tagReplacements);

    	if (l.size() > 0) {
    		return l.get(0); // TODO for now, just pick the first variant provided. Later, call applyMultiple from LocalSearch instead
    	} else {
    		return null;
    	}
    }

    public List<SourceFile> applyMultiple(SourceFile sourceFile, int count, Map<PromptTemplate.PromptTag,String> tagReplacements) {

        SourceFileTree sf = (SourceFileTree) sourceFile;

        Node destination = sf.getNode(destinationStatement);

        if (destination == null) {
            return Collections.singletonList(sf); // targeting a deleted location just does nothing.
        }


	// here is where the magic happens...
	LLMQuery llmQuery;

	// Check which model to use.
	if ("OpenAI".equalsIgnoreCase(LLMConfig.modelType)) {
            llmQuery = new OpenAILLMQuery();
        } else {
            llmQuery = new Ollama4jLLMQuery("http://localhost:11434", LLMConfig.modelType);
        }

	// TODO here, could call sourceFile.getSource() to provide whole class for context...

	Logger.info("Seeking replacements for:");
	Logger.info(destination);

	if(tagReplacements == null) {
		tagReplacements = new HashMap<>();
	}
	tagReplacements.put(PromptTag.COUNT, Integer.toString(count));
	tagReplacements.put(PromptTag.DESTINATION, destination.toString());
	tagReplacements.put(PromptTag.PROJECT, LLMConfig.projectName);

	String prompt = promptTemplate.replaceTags(tagReplacements);

        Logger.info("============");
    	Logger.info("prompt:");
    	Logger.info(prompt);
    	lastPrompt = prompt;
    	Logger.info("============");

	// LLM for ChatGPT
	String answer = llmQuery.chatLLM(prompt);
	// END of LLM code

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
        return this.getClass().getCanonicalName() + " \"" + destinationFilename + "\":" + destinationStatement + "\nPrompt: !!!\n" + lastPrompt +  "\n!!! --> !!!\n" + lastReplacement + "\n!!!";
    }

}
