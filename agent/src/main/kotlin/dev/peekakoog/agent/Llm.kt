package dev.peekakoog.agent

import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.executor.ollama.client.OllamaModels
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel

/** Which LLM the seeker thinks with. Picked from environment variables, falling back to a local Ollama model. */
class Llm(val executor: PromptExecutor, val model: LLModel) {
    val label: String get() = "${model.provider.display} · ${model.id}"

    companion object {
        fun fromEnvironment(): Llm {
            System.getenv("ANTHROPIC_API_KEY")?.takeIf { it.isNotBlank() }?.let { key ->
                return Llm(
                    MultiLLMPromptExecutor(mapOf(LLMProvider.Anthropic to AnthropicLLMClient(key))),
                    AnthropicModels.Sonnet_5,
                )
            }
            System.getenv("OPENAI_API_KEY")?.takeIf { it.isNotBlank() }?.let { key ->
                return Llm(
                    MultiLLMPromptExecutor(mapOf(LLMProvider.OpenAI to OpenAILLMClient(key))),
                    OpenAIModels.Chat.GPT5Mini,
                )
            }
            return Llm(
                MultiLLMPromptExecutor(mapOf(LLMProvider.Ollama to OllamaClient())),
                OllamaModels.Meta.LLAMA_3_2,
            )
        }
    }
}
