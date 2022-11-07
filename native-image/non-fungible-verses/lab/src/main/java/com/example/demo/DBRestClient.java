package com.example.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Scope("application")
public class DBRestClient {

    private Map<String, String> envMap;
    private WebClient webClient;

    Map<String, Object> readItems() {
        return readItems(new HashMap<>());
    }

    Map<String, Object> readItems(Map<String, Object> queryMap) {
        var response = getWebClient()
          .post()
          .uri(uriBuilder -> uriBuilder.queryParam("action","query").build())
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(queryMap))
          .retrieve()
          .bodyToMono(HashMap.class)
          .block();
        return (Map<String, Object>) response;
    }

    String writeItem(Map<String, Object> itemData) {
        var response = getWebClient()
          .post()
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(itemData))
          .retrieve()
          .bodyToMono(Map.class)
          .block();
        
        var items = (ArrayList<Map>) response.get("items");
        return items.get(0).get("id").toString();
    }

    private String getOrdsUser() {
        return getEnvMap().get("ORDS_USER");
    }

    private String getOrdsPassword() {
        return getEnvMap().get("ORDS_PASSWORD");
    }

    private String getOrdsRestAlias() {
        return getEnvMap().getOrDefault("ORDS_REST_ALIAS", getOrdsUser());
    }

    private String getCollectionName() {
        return getEnvMap().get("JSON_COLLECTION_NAME");
    }

    private String collectionUrl;

    private String getCollectionUrl() {
        if (collectionUrl == null) {
            collectionUrl = new StringBuilder(getOrdsBaseUrl())
              .append(getOrdsRestAlias())
              .append("/soda/latest/")
              .append(getCollectionName())
              .toString();
        }
        return collectionUrl;
    }

    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = WebClient.builder()
              .baseUrl(getCollectionUrl())
              .defaultHeaders(header -> header.setBasicAuth(getOrdsUser(), getOrdsPassword()))
              .build();
        }
        return webClient;
    }

    private Map<String, String> getEnvMap() {
        if (envMap == null) { envMap = System.getenv(); }
        return envMap;
    }

    private String getOrdsBaseUrl() {
        return getEnvMap().get("ORDS_BASE_URL");
    }
}