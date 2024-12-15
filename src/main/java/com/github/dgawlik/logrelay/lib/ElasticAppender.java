package com.github.dgawlik.logrelay.lib;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.Getter;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;

public class ElasticAppender extends AppenderBase<ILoggingEvent> {

    @Getter
    public static class Log {
        public String service;
        public String kind;
        public String message;
        public String level;
        public String logger;
        public String thread;
        public String timestamp;
        public String className;
        public String packageName;
        public String fileName;
        public String lineNumber;
        public String method;
        public Map<String, String> mdc;
        public Map<String, String> kv;

        public Log(ILoggingEvent event, String service, String kind) {
            this.service = service;
            this.kind = kind;
            this.message = event.getFormattedMessage();
            this.level = event.getLevel().toString();
            this.logger = event.getLoggerName();
            this.thread = event.getThreadName();
            this.timestamp = String.valueOf(event.getTimeStamp());
            try {
                this.packageName = Class.forName(event.getCallerData()[0].getClassName()).getPackage().getName();
                this.className = Class.forName(event.getCallerData()[0].getClassName()).getSimpleName();
            } catch (ClassNotFoundException e) {
                this.packageName = null;
            }
            this.fileName = event.getCallerData()[0].getFileName();
            this.lineNumber = String.valueOf(event.getCallerData()[0].getLineNumber());
            this.method = event.getCallerData()[0].getMethodName();
            this.mdc = event.getMDCPropertyMap();
            this.kv = event.getKeyValuePairs().stream().collect(Collectors.toMap(e -> e.key, e -> e.value.toString()));
        }
    }





    private String host;
    private Integer port;
    private String apiKey;
    private String service;
    private String kind;


    private ElasticsearchClient esClient;
    private List<Log> logs = new ArrayList<>();
    private ForkJoinTask<?> handle;

    private Runnable worker = () -> {
        try {
            while (true) {
                Thread.sleep(5000);

                List<Log> thisLogs = null;

                synchronized (ElasticAppender.this) {
                    thisLogs = this.logs;
                    this.logs = new ArrayList<>();
                }

                if (thisLogs.isEmpty()) {
                    continue;
                }

                BulkRequest.Builder br = new BulkRequest.Builder();

                for (var log : thisLogs) {
                    br.operations(o -> o.index(i ->
                            i.index("my-logs")
                                    .id(UUID.randomUUID().toString())
                                    .document(log)
                    ));
                }

                BulkResponse result = null;
                try {
                    result = esClient.bulk(br.build());
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                } finally {
                    List<Log> failedLogs = new ArrayList<>();

                    if(result == null) {
                        failedLogs.addAll(thisLogs);
                    } else {
                        for (int i = 0; i < result.items().size(); i++) {
                            if (result.items().get(i).error() != null) {
                                failedLogs.add(thisLogs.get(i));
                            }
                        }
                    }

                    synchronized (this) {
                        this.logs.addAll(failedLogs);
                    }
                }

            }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    };


    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setService(String service) {
        this.service = service;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }


    @Override
    public void start() {
        RestClient restClient = RestClient
                .builder(HttpHost.create("http://" + host + ":" + port))
                .setDefaultHeaders(new Header[]{
                        new BasicHeader("Authorization", "ApiKey " + apiKey)
                })
                .build();

        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());

        esClient = new ElasticsearchClient(transport);

        boolean indexExists = false;
        try {
            ExistsRequest request = new ExistsRequest.Builder().index("my-logs").build();
            BooleanResponse response = esClient.indices().exists(request);
            indexExists = response.value();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        if (!indexExists) {
            try {
                esClient.indices().create(b -> b.index("my-logs")
                        .mappings(mp -> mp
                                .properties("service", p -> p.text(t -> t))
                                .properties("kind", p -> p.text(t -> t))
                                .properties("message", p -> p.text(t -> t))
                                .properties("level", p -> p.text(t -> t))
                                .properties("logger", p -> p.text(t -> t))
                                .properties("thread", p -> p.text(s -> s))
                                .properties("timestamp", p -> p.date(t -> t))
                                .properties("className", p -> p.text(hf -> hf))
                                .properties("packageName", p -> p.text(s -> s))
                                .properties("method", p -> p.text(t -> t))));
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

        handle = ForkJoinPool.commonPool().submit(worker);
        super.start();
    }

    @Override
    public void stop() {
        esClient.shutdown();
        handle.cancel(true);
        super.stop();
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        synchronized (this) {
            logs.add(new Log(eventObject, service, kind));
        }
    }
}