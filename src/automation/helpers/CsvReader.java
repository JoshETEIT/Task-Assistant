package automation.helpers;

import java.io.*;
import java.util.*;
import java.util.function.Function;

public class CsvReader {
    private static final int DEFAULT_SKIP_LINES = 1;

    public static <T> List<T> read(String filePath, Function<String[], T> mapper) throws IOException {
        return read(filePath, mapper, DEFAULT_SKIP_LINES);
    }

    public static <T> List<T> read(String filePath, Function<String[], T> mapper, int skipLines) 
            throws IOException {
        List<T> items = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // Skip header lines
            for (int i = 0; i < skipLines; i++) {
                br.readLine();
            }

            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    items.add(mapper.apply(parseCsvLine(line)));
                }
            }
        }
        return items;
    }

    private static String[] parseCsvLine(String line) {
        // Simple CSV parser - handles basic cases with quoted values
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }
}