package br.com.labbs.workout.httpclientbattleserver.infra;

import com.wizzardo.http.HttpConnection;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import io.jaegertracing.Configuration;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Scope;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.tag.Tags;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class Tracing {
    private Tracing() {
    }

    public static JaegerTracer init() {
        Configuration.SamplerConfiguration samplerConfig = Configuration.SamplerConfiguration.fromEnv()
                .withType(ConstSampler.TYPE)
                .withParam(1);

        Configuration.ReporterConfiguration reporterConfig = Configuration.ReporterConfiguration.fromEnv()
                .withLogSpans(true);

//        Configuration.CodecConfiguration codecConfiguration = new Configuration.CodecConfiguration()
//                .withCodec(Format.Builtin.HTTP_HEADERS, new B3TextMapCodec.Builder().build());

        Configuration config = new Configuration("http_client_battle_SERVER")
//                .withCodec(codecConfiguration)
                .withSampler(samplerConfig)
                .withReporter(reporterConfig);

        return config.getTracer();
    }

    public static Scope startServerSpan(Tracer tracer, Request<HttpConnection, Response> request, String operationName) {

        Map<String, String> headers = new HashMap<>();
        request.headers().forEach((chave, valor)-> headers.put(chave, valor.getValue()));

        Tracer.SpanBuilder spanBuilder;
        try {
            SpanContext parentSpanCtx = tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
            if (parentSpanCtx == null) {
                spanBuilder = tracer.buildSpan(operationName);
            } else {
                spanBuilder = tracer.buildSpan(operationName).asChildOf(parentSpanCtx);
            }
        } catch (IllegalArgumentException e) {
            spanBuilder = tracer.buildSpan(operationName);
        }
        // TODO could add more tags like http.url
        return spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER).startActive(true);
    }

    public static TextMap requestBuilderCarrier(Response response) {
        return new TextMap() {
            @Override
            public Iterator<Map.Entry<String, String>> iterator() {
                throw new UnsupportedOperationException("carrier is write-only");
            }

            @Override
            public void put(String key, String value) {
                response.appendHeader(key, value);
            }
        };
    }
}
