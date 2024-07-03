package gin.edit.llm;

import java.io.IOException;

import io.github.amithkoujalgi.ollama4j.core.OllamaAPI;
import io.github.amithkoujalgi.ollama4j.core.models.OllamaResult;
import io.github.amithkoujalgi.ollama4j.core.types.OllamaModelType;
import io.github.amithkoujalgi.ollama4j.core.utils.PromptBuilder;
import io.github.amithkoujalgi.ollama4j.core.utils.OptionsBuilder;
import io.github.amithkoujalgi.ollama4j.core.exceptions.OllamaBaseException;

import gin.edit.llm.LLMQuery;

public class Ollama4jLLMQuery implements LLMQuery {
    private OllamaAPI ollamaAPI;
    private String modelType;

    // c'tor
    public Ollama4jLLMQuery(String ollamaServerHost, String modelType) {
        this.modelType = modelType;

        this.ollamaAPI = new OllamaAPI(ollamaServerHost);
        ollamaAPI.setRequestTimeoutSeconds(LLMConfig.timeoutInSeconds);
        ollamaAPI.setVerbose(true);
    }

   @Override
    public boolean testServerReachable() {
        return ollamaAPI.ping();
    }

    @Override
    public String chatLLM(String prompt) {
        try {
                // code that might throw OllamaBaseException
                OllamaResult result =
                    ollamaAPI.ask(modelType, prompt, new OptionsBuilder().build());
                return result.getResponse();
        } catch (OllamaBaseException e) {
                // handle the exception
                e.printStackTrace();
        } catch (IOException | InterruptedException e) {
                // handle the IOException
                e.printStackTrace();
        }
        return "";
    }
}

