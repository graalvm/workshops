/*
 * Copyright © 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

package com.example.verses;

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
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
@RestController
public class DemoApplication {

    @Autowired
    Jabberwocky j;

    @Autowired
    DBRestClient dbRest;

    private ObjectMapper om = new ObjectMapper();

    private ObjectMapper getObjectMapper() {
        return om;
    }

	public static void main(String[] args) {
	    SpringApplication.run(DemoApplication.class, args);
	}

    @RequestMapping(method = RequestMethod.GET, path = "/jibber")
    ResponseEntity<String> jibber() {
        return ResponseEntity.ok(fieldAsHTML("verse", j.verseLines(10)));
    }

    @RequestMapping(method = RequestMethod.GET, path = {"/read", "/read/{uuid}"})
    ResponseEntity<String> read(@PathVariable(required=false, name="uuid") String uuid) {
        StringBuilder sb = new StringBuilder();
        var hm = (uuid == null) ? dbRest.readItems() : dbRest.readItems(Map.of("uuid",uuid));

        ArrayList<Map> items = (ArrayList<Map>) hm.get("items");
        for (Map item : items) {
            Map<String, String> value = (Map<String, String>) item.get("value");
            if (value != null) {
                String uuidRead = value.get("uuid");
                String verseRead = value.get("verse");
                String[] lines = null;
                try {
                    lines = getObjectMapper().readValue(verseRead, String[].class);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if ((lines != null) && (uuidRead != null)) {
                    sb.append(fieldAsHTML("uuid ", uuidRead))
                    .append(fieldAsHTML("verse", lines))
                    .append("<br/>");
                }
            }
        }
	    return ResponseEntity.ok(sb.toString());
    }

    private String fieldAsHTML(String name, String value) {
        return new StringBuilder("<pre>")
        .append(name)
        .append(" : ")
        .append(value)
        .append("</pre>")
        .toString();
    }

    private String fieldAsHTML(String name, String[] value) {
        return new StringBuilder("<pre>")
        .append(name)
        .append(" :<br/>")
        .append(linesAsHTML(value, name.length() + 3))
        .append("</pre>")
        .toString();
    }

    private String linesAsHTML(String[] lines, int pad) {
        StringBuilder sb = new StringBuilder();
        for (String line: lines) {
            sb.append(getSpaces(pad))
            .append(line)
            .append("<br/>");
        }
        return sb.toString();
    }

    private String getSpaces(int nSpaces) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nSpaces; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    @RequestMapping(method = RequestMethod.GET, path = "/mint")
    ResponseEntity<Map<String, Object>> mint() {
        try {
            String verse = getObjectMapper().writeValueAsString(j.verseLines());
            Map<String, Object> bodyValues = new HashMap<>();
            bodyValues.put("uuid", UUID.randomUUID().toString());
            bodyValues.put("verse", verse);
            dbRest.writeItem(bodyValues);
            return new ResponseEntity<Map<String, Object>>(bodyValues, HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}