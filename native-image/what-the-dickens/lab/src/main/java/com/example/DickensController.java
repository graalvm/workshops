/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
