package org.example;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptAPI {

    private final String apiUrl;
    private final long timeLimit;
    private final TimeUnit limitUnit;
    private final Semaphore semaphore;


    public CrptAPI(long timeLimit, TimeUnit limitUnit, int requestLimit, String apiUrl) {
        this.timeLimit = timeLimit;
        this.limitUnit = limitUnit;
        this.semaphore = new Semaphore(requestLimit);
        this.apiUrl = apiUrl;
    }

    /**
     * Метод создания документа для ввода в оборот товара, произведенного в РФ.
     *
     * @param document  объект документа
     * @param signature подпись
     */
    public void createDocument(Object document, String signature) {
        synchronized (semaphore) {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                semaphore.wait(limitUnit.toMillis(timeLimit));
                HttpPost httpPost = new HttpPost(apiUrl);

                // Устанавливаем заголовки
                httpPost.setHeader("Content-Type", "application/json");

                // Преобразуем документ в JSON
                ObjectMapper objectMapper = new ObjectMapper();
                String documentJson = objectMapper.writeValueAsString(document);

                // Формируем тело запроса
                String requestBody = String.format("{ \"product_document\": \"%s\", \"document_format\": \"MANUAL\", \"type\": \"LP_INTRODUCE_GOODS\", \"signature\": \"%s\" }", documentJson, signature);
                StringEntity entity = new StringEntity(requestBody);
                httpPost.setEntity(entity);

//            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
//                // Обработка ответа
//                int statusCode = response.getStatusLine().getStatusCode();
//                if (statusCode == 200) {
//                    System.out.println("Document successfully created!");
//                } else {
//                    System.out.println("Failed to create the document. Status code: " + statusCode);
//                }
//            }
//

                String uuid = String.valueOf(UUID.randomUUID());
                System.out.println("Started " + uuid);
                Thread.sleep(3000);
                System.out.println("Ended " + uuid);

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                semaphore.release();
            }
        }

    }

    public static void main(String[] args) {
        // Пример использования
        String apiUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";
        CrptAPI crptApi = new CrptAPI(1, TimeUnit.SECONDS, 61, apiUrl);

        // Создание объекта Description
        Document document = getDocument();
        String signature = "<Открепленная подпись в base64>";
        crptApi.createDocument(document, signature);
        for (int i = 0; i < 10; i++) {
            Runnable task2 = () -> crptApi.createDocument(document, signature);
            Thread thread = new Thread(task2);
            thread.start();
        }
    }

    private static Document getDocument() {
        Description description = new Description("1234567890");

        // Создание объектов Product
        Product product1 = new Product("cert1", "2023-07-27", "123", "owner1", "producer1", "2023-07-27", "tnved1", "uit1", "uitu1");
        Product product2 = new Product("cert2", "2023-07-28", "456", "owner2", "producer2", "2023-07-28", "tnved2", "uit2", "uitu2");
        List<Product> products = new ArrayList<>();
        products.add(product1);
        products.add(product2);

        // Создание объекта Document
        return new Document(description, "doc123", "approved", "type1", true, "owner_inn", "participant_inn", "producer_inn", "2023-07-27", "type2", products, "2023-07-27", "reg123");
    }
}


@Getter
@Setter
class Document {

    private Description description;
    private String doc_id;
    private String doc_status;
    private String doc_type;
    private boolean importRequest;
    private String owner_inn;
    private String participant_inn;
    private String producer_inn;
    private String production_date;
    private String production_type;
    private List<Product> products;
    private String reg_date;
    private String reg_number;

    public Document(Description description, String doc_id, String doc_status, String doc_type, boolean importRequest, String owner_inn, String participant_inn, String producer_inn, String production_date, String production_type, List<Product> products, String reg_date, String reg_number) {
        this.description = description;
        this.doc_id = doc_id;
        this.doc_status = doc_status;
        this.doc_type = doc_type;
        this.importRequest = importRequest;
        this.owner_inn = owner_inn;
        this.participant_inn = participant_inn;
        this.producer_inn = producer_inn;
        this.production_date = production_date;
        this.production_type = production_type;
        this.products = products;
        this.reg_date = reg_date;
        this.reg_number = reg_number;
    }

}


@Setter
@Getter
class Description {

    private String participantInn;

    public Description(String participantInn) {
        this.participantInn = participantInn;
    }

}

@Setter
@Getter
class Product {
    private String certificate_document;
    private String certificate_document_date;
    private String certificate_document_number;
    private String owner_inn;
    private String producer_inn;
    private String production_date;
    private String tnved_code;
    private String uit_code;
    private String uitu_code;

    public Product(String certificate_document, String certificate_document_date, String certificate_document_number, String owner_inn, String producer_inn, String production_date, String tnved_code, String uit_code, String uitu_code) {
        this.certificate_document = certificate_document;
        this.certificate_document_date = certificate_document_date;
        this.certificate_document_number = certificate_document_number;
        this.owner_inn = owner_inn;
        this.producer_inn = producer_inn;
        this.production_date = production_date;
        this.tnved_code = tnved_code;
        this.uit_code = uit_code;
        this.uitu_code = uitu_code;
    }

}