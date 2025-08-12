package ru.zolotuhin.CrptApi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CrptApiApplicationTests {

    private CrptApi api;

    @BeforeEach
    void setUp() {
        api = new CrptApi(
                "https://ismp.crpt.ru",
                Map.of("Authorization", "Bearer test_token"),
                CrptApi.TimeUnit.SECOND,
                2
        );
    }

    @Test
    void testValidationFailsOnMissingFields() {
        CrptApi.Document doc = new CrptApi.Document();
        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            api.createDocument(doc, "signature");
        });
        assertTrue(ex.getMessage().contains("doc_id"));
    }

    @Test
    void testAutoDatePopulation() throws Exception {
        CrptApi.Document doc = buildValidDocument();
        doc.setProduction_date(null);
        doc.setReg_date(null);

        api.createDocument(doc, "signature");

        assertEquals(LocalDate.now().toString(), doc.getProduction_date());
        assertEquals(LocalDate.now().toString(), doc.getReg_date());
    }

    @Test
    void testUitOrUituCodeRequired() {
        CrptApi.Document doc = buildValidDocument();
        doc.getProducts()[0].setUit_code(null);
        doc.getProducts()[0].setUitu_code(null);

        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            api.createDocument(doc, "signature");
        });
        assertTrue(ex.getMessage().contains("uit_code or uitu_code"));
    }

    @Test
    void testRequestLimitWaits() throws Exception {
        CrptApi apiLimited = new CrptApi(
                "https://ismp.crpt.ru",
                Map.of(),
                CrptApi.TimeUnit.SECOND,
                1 // только 1 запрос в секунду
        );

        long start = System.currentTimeMillis();
        apiLimited.createDocument(buildValidDocument(), "sig1");
        apiLimited.createDocument(buildValidDocument(), "sig2"); // Должен ждать
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed >= 1000, "Должна быть пауза не менее 1 секунды");
    }

    private CrptApi.Document buildValidDocument() {
        CrptApi.Document doc = new CrptApi.Document();
        doc.setDoc_id("doc123");
        doc.setDoc_status("NEW");
        doc.setDoc_type("LP_INTRODUCE_GOODS");
        doc.setOwner_inn("1234567890");
        doc.setParticipant_inn("1234567890");
        doc.setProducer_inn("1234567890");
        doc.setProduction_date(LocalDate.now().toString());
        doc.setProduction_type("type");
        doc.setReg_date(LocalDate.now().toString());
        doc.setReg_number("reg123");

        CrptApi.Document.Product product = new CrptApi.Document.Product();
        product.setOwner_inn("1234567890");
        product.setProducer_inn("1234567890");
        product.setProduction_date(LocalDate.now().toString());
        product.setTnved_code("6401990000");
        product.setUit_code("code123");

        doc.setProducts(new CrptApi.Document.Product[]{product});
        return doc;
    }
}

