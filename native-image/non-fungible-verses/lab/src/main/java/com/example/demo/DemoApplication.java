package com.example.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
@RestController
public class DemoApplication {

    @Autowired
    Jabberwocky j;

	public static void main(String[] args) {
	    SpringApplication.run(DemoApplication.class, args);
	}

    @RequestMapping(method = RequestMethod.GET, path = "/jibber")
    ResponseEntity<String> jibber() {
	    return ResponseEntity.ok(j.generate());
    }

    @RequestMapping(method = RequestMethod.GET, path = "/ping")
    ResponseEntity<String> ping() {
        ResponseEntity<String> response = getWebClient().get()
          .retrieve()
          .toEntity(String.class)
          .block();

	    return ResponseEntity.ok(response.getBody());
    }

    @RequestMapping(method = RequestMethod.GET, path = "/read")
    ResponseEntity<String> read() {
        var response = getWebClient().get()
          .retrieve()
          .toEntity(HashMap.class)
          .block();

        StringBuilder sb = new StringBuilder();
        var hm = response.getBody();
        ArrayList<LinkedHashMap> items = (ArrayList<LinkedHashMap>) hm.get("items");
        for (LinkedHashMap item : items) {
            sb.append(item.get("value"))
              .append("\n");
        }

	    return ResponseEntity.ok(sb.toString());
    }

    @RequestMapping(method = RequestMethod.GET, path = "/mint")
    ResponseEntity<String> mint() {
        HashMap<String, String> bodyValues = new HashMap<>();
        bodyValues.put("id", UUID.randomUUID().toString());
        bodyValues.put("verse", j.generate());
        var response = getWebClient()
          .post()
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(bodyValues))
          .retrieve()
          .bodyToMono(String.class)
          .block();

        try {
	        return ResponseEntity.ok(response);
	    }
	    catch (Exception e) {
		    return ResponseEntity
		      .internalServerError()
		      .body(e.getMessage());
	    }
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

    private WebClient webClient;

    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = WebClient.builder()
              .baseUrl(getCollectionUrl())
              .defaultHeaders(header -> header.setBasicAuth(getOrdsUser(), getOrdsPassword()))
              .build();
        }
        return webClient;
    }

    private Map<String, String> envMap;

    private Map<String, String> getEnvMap() {
        if (envMap == null) { envMap = System.getenv(); }
        return envMap;
    }

    private String getOrdsBaseUrl() {
        return getEnvMap().get("ORDS_BASE_URL");
    }

}