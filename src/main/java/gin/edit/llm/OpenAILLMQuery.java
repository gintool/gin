package gin.edit.llm;

import gin.edit.llm.LLMQuery;

import java.time.Duration;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

public class OpenAILLMQuery implements LLMQuery {
    interface Chat {
        String chat(String userMessage);
    }

    private Chat chat;

    // c'tor
    public OpenAILLMQuery() {
        // here is where the magic happens...
        OpenAiChatModel model = OpenAiChatModel.builder().modelName(LLMConfig.openAIModelName).apiKey(LLMConfig.openAIKey).timeout(Duration.ofSeconds(LLMConfig.timeoutInSeconds)).temperature(LLMConfig.temperature).build();
        chat = AiServices.builder(Chat.class)
                        .chatLanguageModel(model)
                        .chatMemory(MessageWindowChatMemory.withCapacity(10))
                        .build();
    }

    @Override
    public boolean testServerReachable() {
        return true;
    }

    @Override
    public String chatLLM(String prompt) {
        return chat.chat(prompt);
    }
}
