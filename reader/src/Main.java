import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


public class Main {

    private static String formatTimeDifference(long timeDifference) {
        String[] units = {"ms", "s", "min", "h", "d", "y"};
        long[] conversions = {1, 1000, 60, 60, 24, 365};
        int unitIndex = 0;
        while (unitIndex < units.length - 1 && timeDifference >= conversions[unitIndex + 1]) {
            timeDifference /= conversions[unitIndex + 1];
            unitIndex++;
        }
        return timeDifference + " " + units[unitIndex];
    }
    private static final String[] logLevels = {"SEVERE", "WARNING", "INFO", "CONFIG", "FINE", "FINER", "FINEST", "DEBUG", "FATAL"};

    private static Map<String, Integer> initializeLogLevelCounts() {
        Map<String, Integer> logLevelCounts = new HashMap<>();
        for (String level : logLevels) {
            logLevelCounts.put(level, 0);
        }
        return logLevelCounts;
    }

    private static String extractLogLevel(String logLine) {
        for (String level : logLevels) {
            if (logLine.contains(level)) {
                return level;
            }
        }
        return null;
    }

    private static void extractLibraries(String logLine, Set<String> uniqueLibraries) {
        int startIndex = logLine.indexOf("[");
        int endIndex = logLine.indexOf("]", startIndex);

        while (startIndex != -1 && endIndex != -1) {
            String library = logLine.substring(startIndex + 1, endIndex);
            uniqueLibraries.add(library);

            startIndex = logLine.indexOf("[", endIndex);
            endIndex = logLine.indexOf("]", startIndex);
        }
    }

    private static void logProcess(File logFile) {
        long startTime = System.currentTimeMillis();
        System.out.println("Przetwarzanie pliku: " + logFile.getName());

        try {
            List<String> logLines = Files.readAllLines(logFile.toPath(), StandardCharsets.UTF_8);

            Map<String, Integer> logLevelCounts = initializeLogLevelCounts();
            int totalLogCount = logLines.size();
            int errorLogCount = 0;
            Set<String> uniqueLibraries = new HashSet<>();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
            Date firstLogTime = null;
            Date lastLogTime = null;

            for (String logLine : logLines) {
                String logLevel = extractLogLevel(logLine);
                if (logLevel != null) {
                    logLevelCounts.put(logLevel, logLevelCounts.get(logLevel) + 1);
                    if (logLevel.equals("SEVERE") || logLevel.equals("FATAL")) {
                        errorLogCount++;
                    }

                    extractLibraries(logLine, uniqueLibraries);
                }

                try {
                    if (logLine.length() < 24) {
                        continue;
                    }
                    Date logTime = dateFormat.parse(logLine.substring(0, 23));
                    if (firstLogTime == null) {
                        firstLogTime = logTime;
                    }
                    lastLogTime = logTime;
                } catch (ParseException e) {
                }
            }

            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            long logTimeDiff = (lastLogTime != null && firstLogTime != null) ? firstLogTime.getTime() - lastLogTime.getTime() : 0;

            String formattedLogTimeDiff = formatTimeDifference(logTimeDiff);

            double errorLogRatio = (double) errorLogCount / totalLogCount;

            System.out.println("Czas przetwarzania: " + elapsedTime + " ms");
            System.out.println("Zakres logów: " + formattedLogTimeDiff);
            System.out.println("Liczba logów wg severity: " + logLevelCounts);
            System.out.println("Stosunek ERROR logów: " + errorLogRatio);
            System.out.println("Ilość unikalnych wystąpień bibliotek: " + uniqueLibraries.size());

        } catch (IOException e) {
            System.err.println("Błąd odczytu pliku: " + logFile.getName());
        }
    }

    public static void main(String[] args) {
        File logDirectory = new File("C:/logs");
        if (!logDirectory.exists()) {
            System.out.println("Nie znaleziono folderu " + logDirectory.getPath());
            return;
        }

        File[] logFiles = logDirectory.listFiles((dir, name) -> name.endsWith(".log"));

        if (logFiles == null) {
            System.out.println("Nie ma plików z końcówką .log");
            return;
        }
        Arrays.sort(logFiles, Comparator.comparingLong(File::lastModified).reversed());

        for (File logFile : logFiles) {
            logProcess(logFile);
        }
    }


}
