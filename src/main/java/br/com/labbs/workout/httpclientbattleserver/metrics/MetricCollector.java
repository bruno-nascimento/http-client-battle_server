package br.com.labbs.workout.httpclientbattleserver.metrics;

import br.com.labbs.workout.httpclientbattleserver.infra.Akka;
import br.com.labbs.workout.httpclientbattle.shared.Env;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import io.prometheus.client.exporter.PushGateway;
import io.prometheus.client.hotspot.DefaultExports;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public enum MetricCollector {

    INSTANCE;

    MetricCollector() {
        DefaultExports.initialize();
    }

    private static final Counter totalRequestCounter = Counter.build().name("http_client_battle_server_hits").labelNames("status").help("Requisicoes recebidas").register();
    private static final Histogram timeSpentPerMessage = Histogram.build().buckets(0.001, 0.0025, 0.005, 0.0075, 0.01, 0.025, 0.05, 0.075, 0.1, 0.25, 0.5, 0.75, 1, 2.5, 5, 7.5, 10)
            .name("http_client_battle_time").help("histograma do tempo gasto em cada request").labelNames("client", "status").register();

    public void inc(int status) {
        totalRequestCounter.labels(""+status).inc();
    }

    public void time(String client, int status, double seconds){
        timeSpentPerMessage.labels(client, ""+status).observe(seconds);
    }

    public void init(final String jobName){
        Akka.INSTANCE.getSystem().scheduler().schedule(
            Duration.Zero(),
            Duration.create(250, TimeUnit.MILLISECONDS),
            () -> {
                try {
                    PushGateway pg = new PushGateway(Env.PROMETHEUS_PUSHGATEWAY.get());
                    pg.pushAdd(CollectorRegistry.defaultRegistry, "http-client-battle_"+jobName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            },
            Akka.INSTANCE.getSystem().dispatcher());
    }

}

