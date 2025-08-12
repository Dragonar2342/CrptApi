package ru.zolotuhin.CrptApi;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {

    private static final String INTRODUCE_DOCUMENT_ENDPOINT = "/api/v3/lk/documents/create";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final String baseUrl;
    private final Map<String, String> defaultHeaders;
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private long windowStart = System.currentTimeMillis();
    private int requestCount = 0;

    private final AtomicInteger requestCounter = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CrptApi(String baseUrl, Map<String, String> defaultHeaders, TimeUnit timeUnit, int requestLimit) {
        if (requestLimit <= 0) {
            throw new IllegalArgumentException("requestLimit must be positive");
        }
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.defaultHeaders = defaultHeaders != null ? defaultHeaders : Collections.emptyMap();
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;

        scheduler.scheduleAtFixedRate(
                () -> requestCounter.set(0),
                0,
                1,
                timeUnit.toConcurrentUnit()
        );

    }

    public void createDocument(Document document, String signature) throws IOException, InterruptedException {
        validateAndPrepare(document);
        sendPostRequest(INTRODUCE_DOCUMENT_ENDPOINT, new DocumentRequest(document, signature));
    }

    private void sendPostRequest(String endpoint, Object payload) throws IOException, InterruptedException {
        waitForRequestSlot();
        String json = objectMapper.writeValueAsString(payload);
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(baseUrl + endpoint);
            post.setEntity(new StringEntity(json, StandardCharsets.UTF_8));
            post.setHeader("Content-Type", "application/json");
            for (Map.Entry<String, String> header : defaultHeaders.entrySet()) {
                post.setHeader(header.getKey(), header.getValue());
            }
            httpClient.execute(post).close();
        }
    }

    private synchronized void waitForRequestSlot() throws InterruptedException {
        long now = System.currentTimeMillis();
        long windowMillis = timeUnit.toConcurrentUnit().toMillis(1);

        // Если текущее окно закончилось — начинаем новое
        if (now - windowStart >= windowMillis) {
            windowStart = now;
            requestCount = 0;
        }

        // Если лимит исчерпан — ждём до конца окна
        if (requestCount >= requestLimit) {
            long waitTime = windowMillis - (now - windowStart);
            if (waitTime > 0) {
                Thread.sleep(waitTime);
            }
            // Начинаем новое окно после ожидания
            windowStart = System.currentTimeMillis();
            requestCount = 0;
        }

        requestCount++;
    }

    /**
     * Валидация и автозаполнение дат
     */
    private void validateAndPrepare(Document doc) {
        if (isEmpty(doc.getDoc_id())) throw new IllegalArgumentException("doc_id is required");
        if (isEmpty(doc.getDoc_status())) throw new IllegalArgumentException("doc_status is required");
        if (isEmpty(doc.getDoc_type())) throw new IllegalArgumentException("doc_type is required");
        if (isEmpty(doc.getOwner_inn())) throw new IllegalArgumentException("owner_inn is required");
        if (isEmpty(doc.getParticipant_inn())) throw new IllegalArgumentException("participant_inn is required");
        if (isEmpty(doc.getProducer_inn())) throw new IllegalArgumentException("producer_inn is required");
        if (isEmpty(doc.getProduction_type())) throw new IllegalArgumentException("production_type is required");
        if (isEmpty(doc.getReg_number())) throw new IllegalArgumentException("reg_number is required");

        if (isEmpty(doc.getProduction_date())) {
            doc.setProduction_date(LocalDate.now().format(DATE_FORMAT));
        }
        if (isEmpty(doc.getReg_date())) {
            doc.setReg_date(LocalDate.now().format(DATE_FORMAT));
        }

        if (doc.getProducts() == null || doc.getProducts().length == 0) {
            throw new IllegalArgumentException("At least one product is required");
        }
        for (Document.Product product : doc.getProducts()) {
            if (isEmpty(product.getUit_code()) && isEmpty(product.getUitu_code())) {
                throw new IllegalArgumentException("Each product must have uit_code or uitu_code");
            }
        }
    }

    private boolean isEmpty(String s) {
        return s == null || s.isBlank();
    }

    public void shutdown() {
        scheduler.shutdown();
    }

    @Getter
    @Setter
    public static class DocumentRequest {
        private final Document document;
        private final String signature;

        public DocumentRequest(Document document, String signature) {
            this.document = document;
            this.signature = signature;
        }
    }

    @Getter
    @Setter
    public static class Document {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private String importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private Product[] products;
        private String reg_date;
        private String reg_number;

        @Getter
        @Setter
        public static class Description {
            private String participantInn;
        }

        @Getter
        @Setter
        public static class Product {
            private String certificate_document;
            private String certificate_document_date;
            private String certificate_document_number;
            private String owner_inn;
            private String producer_inn;
            private String production_date;
            private String tnved_code;
            private String uit_code;
            private String uitu_code;
        }
    }

    public enum TimeUnit {
        SECOND(java.util.concurrent.TimeUnit.SECONDS),
        MINUTE(java.util.concurrent.TimeUnit.MINUTES),
        HOUR(java.util.concurrent.TimeUnit.HOURS);

        private final java.util.concurrent.TimeUnit concurrentUnit;

        TimeUnit(java.util.concurrent.TimeUnit concurrentUnit) {
            this.concurrentUnit = concurrentUnit;
        }

        public java.util.concurrent.TimeUnit toConcurrentUnit() {
            return concurrentUnit;
        }
    }
}