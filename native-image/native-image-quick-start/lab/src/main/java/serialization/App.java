/*
 * Copyright © 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

package serialization;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

public final class App {
    public static void main(String[] args) throws Exception {
        String root = args.length > 0 ? args[0] : ".";

        File dir = new File(root);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("Error: \"" + root + "\" is not a valid directory.");
            return;
        }

        final FileCount count = ListDir.list(root);

        ObjectMapper mapper = new ObjectMapper();

        // Pretty-print JSON and write to file
        File outputFile = new File("file-stats.json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, count);

        System.out.println("Counting directory: " + root);
        System.out.println("JSON output written to: " + outputFile.getAbsolutePath());
    }
}
