package gin.edit.llm;

import gin.edit.statement.StatementEdit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pmw.tinylog.Logger;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;



import gin.SourceFile;
import gin.SourceFileTree;
import gin.edit.Edit;
import gin.edit.llm.PromptTemplate.PromptTag;
import gin.edit.statement.StatementEdit;


public class LLMMaskedStatement extends StatementEdit{
    private static final long serialVersionUID = 1112502387236768004L;
    public String destinationFilename;
    public int destinationStatement;

    private PromptTemplate promptTemplate;

    private String lastReplacement;
    private String lastPrompt;

    private Random rng = null;

    public LLMMaskedStatement(SourceFile sourceFile, Random rng, PromptTemplate promptTemplate) {
        SourceFileTree sf = (SourceFileTree) sourceFile;

        destinationFilename = sourceFile.getRelativePathToWorkingDir();

        destinationStatement = sf.getRandomBlockID(true, rng);

        this.promptTemplate = promptTemplate;

        this.rng = rng;

        lastReplacement = "NOT YET APPLIED";
        lastPrompt = "NOT YET APPLIED";
    }

    public LLMMaskedStatement(SourceFile sourceFile, Random rng) {
        this(sourceFile, rng, LLMConfig.getDefaultPromptTemplate());
    }

    public LLMMaskedStatement(String destinationFilename, int destinationStatement) {
        this.destinationFilename = destinationFilename;
        this.destinationStatement = destinationStatement;

        this.lastReplacement = "NOT YET APPLIED";
    }

    @Override
    public SourceFile apply(SourceFile sourceFile, Object tagReplacements) {
    	List<SourceFile> l = applyMultiple(sourceFile, 2, (Map<PromptTemplate.PromptTag,String>)tagReplacements);

    	if (l.size() > 0) {
    		return l.get(0); // TODO for now, just pick the first variant provided. Later, call applyMultiple from LocalSearch instead
    	} else {
    		return null;
    	}
    }

    public List<SourceFile> applyMultiple(SourceFile sourceFile, int count, Map<PromptTemplate.PromptTag,String> tagReplacements ){
        SourceFileTree sf = (SourceFileTree) sourceFile;
        

        Statement statementToMask = drawStatementFromSourceFile(sf, (rng != null ? rng : new Random()));
        Logger.info( "Statement to mask: " + statementToMask.toString());

        LLMQuery llmQuery;

        // Check which model to use.
        if ("OpenAI".equalsIgnoreCase(LLMConfig.modelType)) {
                llmQuery = new OpenAILLMQuery();
            } else {
                llmQuery = new Ollama4jLLMQuery("http://localhost:11434", LLMConfig.modelType);
            }

        if(tagReplacements == null) {
            tagReplacements = new HashMap<>();
        }

        tagReplacements.put(PromptTag.PROJECT, LLMConfig.projectName);
        tagReplacements.put(PromptTag.COUNT, Integer.toString(count));
        tagReplacements.put(PromptTag.DESTINATION, maskCode(sf, statementToMask));

        String prompt = promptTemplate.replaceTags(tagReplacements);

        Logger.info("============");
    	Logger.info("prompt:");
    	Logger.info(prompt);
    	lastPrompt = prompt;
    	Logger.info("============");

        String answer = llmQuery.chatLLM(prompt);

        Logger.info("============");
        Logger.info("response:");
        Logger.info(answer);
        Logger.info("============");
        
        // answer includes code enclosed in ```java   ....``` or ```....``` blocks
        // use regex to find all of these then parse into javaparser objects for return
        Pattern pattern = Pattern.compile("```(?:java)(.*?)```", Pattern.DOTALL | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(answer);
        
        // now parse the strings return by LLM into JavaParser Statements
        List<String> replacementStrings = new ArrayList<>();
        List<Statement> replacementStatements = new ArrayList<>();
        while (matcher.find()) {
        	String str = matcher.group(1);

            Logger.info("============");
            Logger.info("match:");
            Logger.info(str);
    

        	try {
                // extract the method body from the response

                Statement stmt;
                stmt = StaticJavaParser.parseBlock(str);

                // MethodDeclaration method;
                // method = StaticJavaParser.parseMethodDeclaration(str);
                // Statement stmt = method.getBody().orElse(null);
                replacementStrings.add(str);
                replacementStatements.add(stmt);
                

                Logger.info("here is the parsed statement:");
            }
            

            catch (ParseProblemException e) {
                
                Logger.info("PARSE PROBLEM EXCEPTION, trying with method declaration");
                try{
                    MethodDeclaration method;
                    method = StaticJavaParser.parseMethodDeclaration(str);
                    Statement stmt = method.getBody().orElse(null);
                    replacementStrings.add(str);
                    replacementStatements.add(stmt); 
                    
                } catch (ParseProblemException e2) {
                    Logger.info("PARSE PROBLEM EXCEPTION 2");
                    Logger.info(e2);
                    continue;
                }
                continue;
            }

            Logger.info("============");

        }

        List<SourceFile> variantSourceFiles = new ArrayList<>();

        if (replacementStrings.isEmpty()) {
            Logger.info("============");
            Logger.info("No replacements found. Response was:");
            Logger.info(answer);
            Logger.info("============");
            this.lastReplacement = "LLM GAVE NO SUGGESTIONS";
        } else {
            this.lastReplacement = replacementStrings.get(0);
            Logger.info("============");
            Logger.info("Applying first suggestion:");
            Logger.info(this.lastReplacement);
            Logger.info("============");
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


    public Statement drawStatementFromSourceFile(SourceFileTree sourceFileTree, Random rng){
        List<Statement> stmts = sourceFileTree.getTargetMethodRootNode().get(0).findAll(Statement.class);

        Statement stmt = stmts.get(rng.nextInt(stmts.size()));
        while(ifNonImpactfulStatement(stmt) && stmts.size() > 1){
            Logger.info("Non-impactful statement found, trying another one, the statement is: " + stmt.toString());
            stmts.remove(stmt);
            stmt = stmts.get(rng.nextInt(stmts.size()));
        }
        return stmt;
    }

    public boolean ifNonImpactfulStatement(Statement stmt){
        //TODO check if the statement is non-impactful
        if (stmt.isExpressionStmt() || 
            stmt.isBlockStmt() ||
            stmt.isForStmt() ||
            stmt.isIfStmt() ||
            stmt.isReturnStmt() ||
            stmt.isWhileStmt() ||
            stmt.isThrowStmt()){
                return false;
        }
        return true;
    }

    public String maskCode(SourceFileTree sf, Statement targetStatement){

        Statement placeholderStatement = new EmptyStmt();

        // Node targetMethodRootNode = sf.getTargetMethodRootNode().get(0).clone();
        Node targetMethodRootNode = sf.getNode(destinationStatement);

        List<Statement> stmts = targetMethodRootNode.findAll(Statement.class);
        Statement stmt = stmts.get(rng.nextInt(stmts.size()));
        placeholderStatement.setComment(new LineComment("<<PLACEHOLDER>>"));
        boolean ifReplaceSuc = stmt.replace(placeholderStatement);

        if(!ifReplaceSuc){
            Logger.error("Failed to replace the statement with placeholder");
        }

        String maskedCode = targetMethodRootNode.toString();

        Logger.info("============");
        Logger.info("masked code:");
        Logger.info(maskedCode);
        Logger.info("============");

        return maskedCode;
    }

    @Override
    public String toString() {
        return this.getClass().getCanonicalName() + " \"" + destinationFilename + "\":" + destinationStatement + "\nPrompt: !!!\n" + lastPrompt +  "\n!!! --> !!!\n" + lastReplacement + "\n!!!";
    }
}
