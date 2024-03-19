package org.example;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptAPI {

    private final String apiUrl;
    private final long timeLimit;
    private final TimeUnit limitUnit;

    private final Semaphore semaphore;

    private final ReleaseTask releaseTask = new ReleaseTask();

    private final Timer timer = new Timer();

    public CrptAPI(long timeLimit, TimeUnit limitUnit, int requestLimit, String apiUrl) {
        this.semaphore = new Semaphore(requestLimit);
        this.timeLimit = timeLimit;
        this.limitUnit = limitUnit;
        this.apiUrl = apiUrl;
    }

    class ReleaseTask extends TimerTask {
        @Override
        public void run() {
            semaphore.release();
        }
    }

    /**
     * Метод создания документа для ввода в оборот товара, произведенного в РФ.
     *
     * @param document  объект документа
     * @param signature подпись
     */
    public void createDocument(Document document, String signature) {
        try {
            semaphore.acquire(1);
            long millis = limitUnit.toMillis(timeLimit);
            timer.schedule(new ReleaseTask(), millis);
            doRequest(document, signature);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public void doRequest(Document document, String signature) {

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(apiUrl);

            // Устанавливаем заголовки
            httpPost.setHeader("Content-Type", "application/json");

            // Преобразуем документ в JSON
            ObjectMapper objectMapper = new ObjectMapper();

            StringEntity entity = new StringEntity(objectMapper.writeValueAsString(document));
            httpPost.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                // Обработка ответа
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    System.out.println("Document successfully created!");
                } else {
                    System.out.println("Failed to create the document. Status code: " + statusCode);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        // Пример использования
        String apiUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";
        CrptAPI crptApi = new CrptAPI(30, TimeUnit.SECONDS, 5, apiUrl);

        // Создание объекта Description
        Document document = getDocument();
        String signature = "<Открепленная подпись в base64>";
        crptApi.createDocument(document, signature);

    }

    private static Document getDocument() {
        Description description = new Description("1234567890");

        // Создание объектов Product
        Product product1 = new Product("cert1", "2024-03-19", "123", "owner1", "producer1", "2024-03-19", "tnved1", "uit_1", "uitu_11");
        List<Product> products = new ArrayList<>();
        products.add(product1);

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