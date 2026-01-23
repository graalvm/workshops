/*
 * Copyright © 2025, 2026 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

package oracle;
public final class App {

    /**
     * Runs the application. This will count the files
     * @param args The arguments of the program.
     */
    public static void main(String[] args) throws Exception {

		String root = ".";
		if (args.length > 0) {
			root = args[0];
		}

        final FileCount count = ListDir.list(root);
        final String size = ListDir.humanReadableByteCountBin(count.getSize());

        System.out.println("Counting directory: " + root);
        System.out.println("Total: "
            + count.getCount()
            + " files, total size = "
            + size);
    }
}