package gin.edit.llm;

import java.io.IOException;

import io.github.ollama4j.Ollama;
import io.github.ollama4j.utils.OptionsBuilder;
import io.github.ollama4j.models.response.OllamaResult;
import io.github.ollama4j.exceptions.OllamaException;
import io.github.ollama4j.models.generate.OllamaGenerateRequest;

//import io.github.amithkoujalgi.ollama4j.core.OllamaAPI;
//import io.github.amithkoujalgi.ollama4j.core.models.OllamaResult;
//import io.github.amithkoujalgi.ollama4j.core.types.OllamaModelType;
//import io.github.amithkoujalgi.ollama4j.core.utils.PromptBuilder;
//import io.github.amithkoujalgi.ollama4j.core.utils.OptionsBuilder;
//import io.github.amithkoujalgi.ollama4j.core.exceptions.OllamaBaseException;

import gin.edit.llm.LLMQuery;

public class Ollama4jLLMQuery implements LLMQuery {
    private Ollama ollamaAPI;
    private String modelType;

    private static final String OLLAMA_SERVER = System.getenv("OLLAMA_SERVER");
    private static final String OLLAMA_API_KEY = System.getenv("OLLAMA_API_KEY");

    // c'tor
    public Ollama4jLLMQuery(String ollamaServerHost, String modelType) {
        this.modelType = modelType;

        if (OLLAMA_SERVER == null || OLLAMA_SERVER.isBlank() || OLLAMA_API_KEY == null || OLLAMA_API_KEY.isBlank()) {
        	this.ollamaAPI = new Ollama(ollamaServerHost);
	} else {
		this.ollamaAPI = new Ollama(OLLAMA_SERVER);
		this.ollamaAPI.setBearerAuth(OLLAMA_API_KEY);
	}
        ollamaAPI.setRequestTimeoutSeconds(LLMConfig.timeoutInSeconds);
        //ollamaAPI.setVerbose(true);
    }

    @Override
    public boolean testServerReachable() {
        boolean ret = false;
        try {
		ret = ollamaAPI.ping();
	 } catch (OllamaException e) {
                // handle the exception
                e.printStackTrace();
	}
        return ret;
    }

    @Override
    public String chatLLM(String prompt) {
        try {
                // code that might throw OllamaBaseException
                OllamaGenerateRequest req = OllamaGenerateRequest.builder()
					.withModel(this.modelType).withPrompt(prompt)
//					.withContext("You are a Java Optimization Engine that creates a valid faster code")
					.build();
                OllamaResult result = ollamaAPI.generate(req, null);
                    //ollamaAPI.ask(modelType, prompt, new OptionsBuilder().build());
                return result.getResponse();
        } catch (OllamaException e) {
                // handle the exception
                e.printStackTrace();
        //} catch (IOException | InterruptedException e) {
                // handle the IOException
        //        e.printStackTrace();
        }
        return "";
    }
}


