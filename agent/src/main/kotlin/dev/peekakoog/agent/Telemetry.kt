package dev.peekakoog.agent

import ai.koog.agents.core.agent.GraphAIAgent
import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
import ai.koog.agents.features.opentelemetry.integration.langfuse.addLangfuseExporter
import ai.koog.agents.features.opentelemetry.integration.otlp.OtlpJsonSpanExporter
import io.opentelemetry.kotlin.context.Context
import io.opentelemetry.kotlin.export.OperationResultCode
import io.opentelemetry.kotlin.tracing.data.SpanData
import io.opentelemetry.kotlin.tracing.export.SpanExporter
import io.opentelemetry.kotlin.tracing.export.SpanProcessor
import io.opentelemetry.kotlin.tracing.model.ReadWriteSpan
import io.opentelemetry.kotlin.tracing.model.ReadableSpan

/**
 * Where agents from Level 2 on send their OpenTelemetry traces, chosen from environment variables:
 * `OTEL_EXPORTER_OTLP_ENDPOINT` (OTLP over HTTP, e.g. Jaeger at `http://localhost:4318`)
 * and/or `LANGFUSE_PUBLIC_KEY` + `LANGFUSE_SECRET_KEY`.
 */
object Telemetry {
    private val otlpEndpoint: String? = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT")?.takeIf { it.isNotBlank() }?.trimEnd('/')
    private val langfuse: Boolean = !System.getenv("LANGFUSE_PUBLIC_KEY").isNullOrBlank()

    /** Tests export here instead of a real backend. */
    internal var testExporter: SpanExporter? = null

    val enabled: Boolean get() = otlpEndpoint != null || langfuse || testExporter != null

    val description: String
        get() = listOfNotNull(otlpEndpoint?.let { "OTLP $it" }, "Langfuse".takeIf { langfuse })
            .joinToString(" + ")
            .ifEmpty { "off (set OTEL_EXPORTER_OTLP_ENDPOINT, e.g. http://localhost:4318)" }

    internal fun install(features: GraphAIAgent.FeatureContext) {
        if (!enabled) return
        features.install(OpenTelemetry) {
            setServiceInfo(serviceName = "peek-a-koog", serviceVersion = "level-2")
            // Include prompts and tool arguments in the spans: this is a demo, not production.
            setVerbose(true)
            // Off by default in Koog 1.3: without it, closing the agent never flushes its spans.
            setShutdownOnAgentClose(true)
            otlpEndpoint?.let { endpoint -> addSpanProcessor { ExportWhenAgentCloses(OtlpJsonSpanExporter("$endpoint/v1/traces")) } }
            if (langfuse) addLangfuseExporter()
            testExporter?.let { exporter -> addSpanProcessor { ExportWhenAgentCloses(exporter) } }
        }
    }
}

/**
 * Collects a game's spans and exports them in one go when the agent closes.
 *
 * Koog 1.3's default batch processor (and the SDK's simple one) dropped every span when the agent closed:
 * their exports run in the background and the shutdown cancels them. One game is one trace, so waiting is fine.
 */
private class ExportWhenAgentCloses(private val exporter: SpanExporter) : SpanProcessor {
    private val ended = mutableListOf<SpanData>()

    override fun onStart(span: ReadWriteSpan, parentContext: Context) = Unit
    override fun onEnding(span: ReadWriteSpan) = Unit
    override fun onEnd(span: ReadableSpan) {
        synchronized(ended) { ended += span.toSpanData() }
    }

    override fun isStartRequired() = false
    override fun isEndRequired() = true

    override suspend fun forceFlush(): OperationResultCode {
        val spans = synchronized(ended) { ended.toList().also { ended.clear() } }
        return if (spans.isEmpty()) OperationResultCode.Success else exporter.export(spans)
    }

    override suspend fun shutdown(): OperationResultCode {
        val result = forceFlush()
        exporter.shutdown()
        return result
    }
}
