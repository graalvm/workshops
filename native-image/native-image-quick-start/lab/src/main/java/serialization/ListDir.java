/*
* Copyright © 2025 Oracle and/or its affiliates. All rights reserved.
*  Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
*/

package serialization;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

class FileCount {
    private long size;
    private long count;

    public FileCount() {} // Required for deserialization

    @JsonCreator
    public FileCount(@JsonProperty("size") long size, @JsonProperty("count") long count) {
        this.size = size;
        this.count = count;
    }

    public long getSize() { return size; }
    public long getCount() { return count; }

    public void setSize(long size) { this.size = size; }
    public void setCount(long count) { this.count = count; }
}

public class ListDir {
    public static final FileCount list(final String dirName) throws IOException {
        long[] size = {0};
        long[] count = {0};

        try (Stream<Path> paths = Files.walk(Paths.get(dirName))) {
            paths.filter(Files::isRegularFile).forEach((Path p) -> {
                File f = p.toFile();
                size[0] += f.length();
                count[0] += 1;
            });
        }
        return new FileCount(size[0], count[0]);
    }

    public static final String humanReadableByteCountBin(final long bytes) {
        long b = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        return b < 1024L ? bytes + " B"
                : b <= 0xfffccccccccccccL >> 40 ? String.format("%.1f KiB", bytes / 0x1p10)
                : b <= 0xfffccccccccccccL >> 30 ? String.format("%.1f MiB", bytes / 0x1p20)
                : b <= 0xfffccccccccccccL >> 20 ? String.format("%.1f GiB", bytes / 0x1p30)
                : b <= 0xfffccccccccccccL >> 10 ? String.format("%.1f TiB", bytes / 0x1p40)
                : b <= 0xfffccccccccccccL ? String.format("%.1f PiB", (bytes >> 10) / 0x1p40)
                : String.format("%.1f EiB", (bytes >> 20) / 0x1p40);
    }
}