package br.com.labbs.workout.httpclientbattleserver;

import br.com.labbs.workout.httpclientbattleserver.infra.Tracing;
import br.com.labbs.workout.httpclientbattleserver.metrics.MetricCollector;
import com.google.common.collect.ImmutableMap;
import com.wizzardo.http.framework.WebApplication;
import com.wizzardo.http.response.Status;
import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.Scope;
import io.prometheus.client.SimpleTimer;

import java.util.concurrent.atomic.AtomicInteger;

public class Server {

    private static final String TRACE_OPERATION_NAME = "server";
    private static final String EVENT = "event";
    private static final String HIT_EVENT = "hit";
    private static final String HIT_CLEAR = "clear";
    private static final String COUNTER = "counter";

    public static void main(String[] args) {

        final AtomicInteger counter = new AtomicInteger(0);
        MetricCollector.INSTANCE.init(TRACE_OPERATION_NAME);

        new WebApplication(new String[]{"env=prod"}).onSetup(app -> {
            app.setWorkersCount(4);
            app.setIoThreadsCount(4);
            app.getConfig().config("server").put("maxRequestsInQueue", 10000000);
            final JaegerTracer tracer = Tracing.init();
            app.getUrlMapping()
                .append("hit_me", (request, response) -> {
                    SimpleTimer requestTimer = new SimpleTimer();
                    try (Scope scope = Tracing.startServerSpan(tracer, request, TRACE_OPERATION_NAME)) {
                        final int hitNumber = counter.incrementAndGet();
                        scope.span().setTag(EVENT, HIT_EVENT);
                        scope.span().log(ImmutableMap.of(COUNTER, hitNumber));
                        response.setBody(""+hitNumber);
                        response.setStatus(Status._200);
                        MetricCollector.INSTANCE.time(TRACE_OPERATION_NAME, 200, requestTimer.elapsedSeconds());
                        MetricCollector.INSTANCE.inc(200);
                        return response;
                    }catch (Throwable e){
                        e.printStackTrace();
                        response.setBody("ERRO : " + e.getMessage());
                        MetricCollector.INSTANCE.time(TRACE_OPERATION_NAME, 500, requestTimer.elapsedSeconds());
                        MetricCollector.INSTANCE.inc(500);
                        response.setStatus(Status._500);
                        return response;
                    }
                })
                .append("clear", (request, response) -> {
                    Scope scope = Tracing.startServerSpan(tracer, request, TRACE_OPERATION_NAME);
                    counter.set(0);
                    scope.span().setTag(EVENT, HIT_CLEAR);
                    scope.span().log(ImmutableMap.of(COUNTER, counter.get()));
                    response.setBody(""+counter.get());
                    return response;
                });
        })
        .start();
    }

}
