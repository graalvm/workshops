package com.example.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class DemoApplication {

    @Autowired
    Jabberwocky j;

    @Autowired
    DBRestClient dbRest;

	public static void main(String[] args) {
	    SpringApplication.run(DemoApplication.class, args);
	}

    @RequestMapping(method = RequestMethod.GET, path = "/read")
    ResponseEntity<String> read() {

        StringBuilder sb = new StringBuilder();
        var hm = dbRest.readItems(); //.getBody();
        ArrayList<Map> items = (ArrayList<Map>) hm.get("items");
        for (Map item : items) {
            var links = (ArrayList<Map>) item.get("links");
            sb.append("\n");
            sb.append(links.get(0).get("href"));
            sb.append("\n");
            sb.append(item.get("value"))
            .append("\n");
        }

	    return ResponseEntity.ok(sb.toString());
    }

    @RequestMapping(method = RequestMethod.GET, path = "/mint")
    ResponseEntity<HashMap<String, String>> mint() {
        HashMap<String, String> bodyValues = new HashMap<>();
        bodyValues.put("uuid", UUID.randomUUID().toString());
        bodyValues.put("verse", j.generate());   
        String itemUrl = dbRest.writeItem(bodyValues);
        bodyValues.put("url", itemUrl);
        return new ResponseEntity<HashMap<String, String>>(bodyValues, HttpStatus.CREATED);
    }
}