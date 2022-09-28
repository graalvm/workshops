package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.UUID;

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
        WebClient client = WebClient.create("https://httpbin.org");
        ResponseEntity<String> response = client.get()
          .retrieve()
          .toEntity(String.class)
          .block();

        String headers = response.getHeaders().toString();

        String nfv = j.generate();
	    return ResponseEntity.ok(headers);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/mint")
    ResponseEntity<String> mint() {
	    HashMap<String, String> items = new HashMap<>();
	    items.put("id", UUID.randomUUID().toString());
	    items.put("verse", j.generate());
	    try {
	            String jsonString = new ObjectMapper()
		      .writeValueAsString(items);
	            return ResponseEntity.ok(items.get("id"));
	    }
	    catch (Exception e) {
		    return ResponseEntity
		      .internalServerError()
		      .body(e.getMessage());
	    }
    }
}
