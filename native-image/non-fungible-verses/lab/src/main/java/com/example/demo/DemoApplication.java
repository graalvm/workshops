package com.example.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
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

    @RequestMapping(method = RequestMethod.GET, path = "/jibber")
    ResponseEntity<String> jibber() {
        return ResponseEntity.ok(j.generate());
    }

    @RequestMapping(method = RequestMethod.GET, path = {"/read", "/read/{uuid}"})
    ResponseEntity<String> read(@PathVariable(required=false, name="uuid") String uuid) {
        StringBuilder sb = new StringBuilder();
        var hm = (uuid == null) ? dbRest.readItems() : dbRest.readItems(Map.of("uuid",uuid));

        ArrayList<Map> items = (ArrayList<Map>) hm.get("items");
        for (Map item : items) {
            Map<String, Object> value = (Map<String, Object>) item.get("value");
            if (value != null) {
                var uuidRead = value.get("uuid");
                var verseRead = value.get("verse");
                if ((verseRead != null) && (uuidRead != null)) {
                    sb.append("\n<record>\n")
                    .append("uuid : ")
                    .append(uuidRead)
                    .append("\n")
                    .append("verse : ")
                    .append(verseRead)
                    .append("</record>\n");    
                }
            }
        }

	    return ResponseEntity.ok(sb.toString());
    }

    @RequestMapping(method = RequestMethod.GET, path = "/mint")
    ResponseEntity<Map<String, Object>> mint() {
        Map<String, Object> bodyValues = new HashMap<>();
        bodyValues.put("uuid", UUID.randomUUID().toString());
        bodyValues.put("verse", j.generate());
        dbRest.writeItem(bodyValues);
        return new ResponseEntity<Map<String, Object>>(bodyValues, HttpStatus.CREATED);
    }
}