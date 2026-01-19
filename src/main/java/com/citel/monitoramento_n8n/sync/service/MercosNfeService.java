package com.citel.monitoramento_n8n.sync.service;

import com.citel.monitoramento_n8n.sync.DTO.MercosNfeDTO;
import com.citel.monitoramento_n8n.sync.DTO.MercosNfeResponseDTO;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class MercosNfeService {

    private final WebClient webClient;

    public MercosNfeService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    public MercosNfeResponseDTO enviarNotaFiscal(MercosNfeDTO request) {
        try {
            // 1. Baixar PDF
            byte[] pdfBytes = downloadFile(request.getPdfUrl(), request.getJwtWs());

            // 2. Baixar XML
            byte[] xmlBytes = downloadFile(request.getXmlUrl(), request.getJwtWs());

            // 3. Converter nota_fiscal para JSON
            String notaFiscalJson = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(request.getNota_fiscal());

            // 4. Montar multipart com Apache HttpClient
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

                HttpPost httpPost = new HttpPost(request.getUrlMercos());
                httpPost.setHeader("CompanyToken", request.getCompanyToken());
                httpPost.setHeader("ApplicationToken", request.getApplicationToken());

                org.apache.http.HttpEntity entity = MultipartEntityBuilder.create()
                        .addBinaryBody("arquivo_pdf", pdfBytes, ContentType.APPLICATION_OCTET_STREAM, "nota.pdf")
                        .addBinaryBody("arquivo_xml", xmlBytes, ContentType.APPLICATION_OCTET_STREAM, "nota.xml")
                        .addTextBody("nota_fiscal", notaFiscalJson, ContentType.TEXT_PLAIN)
                        .build();

                httpPost.setEntity(entity);

                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    String responseBody = EntityUtils.toString(response.getEntity());

                    if (statusCode >= 200 && statusCode < 300) {
                        return MercosNfeResponseDTO.builder()
                                .success(true)
                                .statusCode(statusCode)
                                .data(responseBody)
                                .build();
                    } else {
                        return MercosNfeResponseDTO.builder()
                                .success(false)
                                .statusCode(statusCode)
                                .error(responseBody)
                                .build();
                    }
                }
            }

        } catch (Exception e) {
            return MercosNfeResponseDTO.builder()
                    .success(false)
                    .statusCode(500)
                    .error(e.getMessage())
                    .build();
        }
    }

    private byte[] downloadFile(String url, String jwtToken) {
        return webClient.get()
                .uri(url)
                .header("Authorization", jwtToken)
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
    }
}