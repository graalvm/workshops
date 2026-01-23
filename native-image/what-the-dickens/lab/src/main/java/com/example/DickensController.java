/*
 * Copyright © 2023, 2025, 2026 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rita.RiMarkov;

/**
 * REST Controller which serves as an entry-point for requests for Dickens
 * nonsense prose.
 *
 */
@RestController
public class DickensController {

    private final static Logger LOG = Logger.getLogger(DickensController.class.getName());
    private final static String COMMENT = "***";
    private final static String START_MARKER = "START";
    private final static String END_MARKER = "END";
    private final static Set<String> NOVELS = new HashSet<>();
    private final static RiMarkov MARKOV_MODEL = new RiMarkov(5);
    private final static Map<String, Object> GEN_MAP;

    static {
        GEN_MAP = Map.of(
                "minLength", 15,
                "temperature", 100f,
                "allowDuplicates", false
        );
        NOVELS.add("Christmas_Carol.txt");
        NOVELS.add("Hard_Times.txt");
        NOVELS.add("A_Tale_of_Two_Cities.txt");
        NOVELS.add("Oliver_Twist.txt");
        /* Uncomment the following lines to add more novels */
        //NOVELS.add("Great_Expectations.txt");
        //NOVELS.add("The_Old_Curiosity_Shop.txt");
        //NOVELS.add("Little_Dorrit.txt");
        //NOVELS.add("Martin_Chuzzlewit.txt");
        //NOVELS.add("Nicholas_Nickleby.txt");
        //NOVELS.add("Bleak_House.txt");
        //NOVELS.add("David_Copperfield.txt");
        //NOVELS.add("The_Pickwick_Papers.txt");
        long startTime = System.currentTimeMillis();
        for (String novel : NOVELS) {
            try {
                ingestNovel(novel);
                LOG.log(Level.INFO, "Ingested novel: {0}", novel);
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
        LOG.log(Level.INFO, "Time taken to ingest novels: {0}ms", System.currentTimeMillis() - startTime);
    }

    private static final void ingestNovel(String novel) throws IOException {
        StringBuilder out = new StringBuilder();
        String readLine;
        boolean inStory = false;
        try ( BufferedReader in = new BufferedReader(new InputStreamReader(DickensController.class.getClassLoader().getResourceAsStream(novel), StandardCharsets.UTF_8))) {
            while (true) {
                readLine = in.readLine();
                if (readLine.startsWith(COMMENT)) {
                    if (readLine.contains(START_MARKER)) {
                        // Starting Story
                        inStory = true;
                    } else {
                        if (readLine.contains(END_MARKER)) {
                            // Reached end of story
                            break;
                        }
                    }
                } else {
                    if (inStory) {
                        out.append(readLine);
                        out.append(" ");
                    }
                }
            }
        }
        String prose = out.toString();
        MARKOV_MODEL.addText(prose);
    }

    public String generate() {
        return generate(10);
    }

    public String generate(final int numLines) {
        String[] lines = MARKOV_MODEL.generate(numLines, GEN_MAP);
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append("<p>");
            sb.append(line);
            sb.append("</p>");
        }
        return sb.toString();
    }

    @GetMapping("/whatTheDickens")
    ResponseEntity<String> whatTheDickens() {
        return ResponseEntity.ok(generate(10));
    }

    @RequestMapping(value = "whatTheDickens/{number}")
    ResponseEntity<String> whatTheDickensN(@PathVariable int number) {
        return ResponseEntity.ok(generate(number));
    }
}
