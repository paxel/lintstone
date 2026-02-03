package paxel.lintstone.api.example.mapreduce;

import paxel.lintstone.api.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Demo project showing a Map-Reduce like processing of a file.
 */
public class MapReduceDemo {

    public static void main(String[] args) throws Exception {
        // 1. Create a dummy CSV file if it doesn't exist
        Path tempFile = Files.createTempFile("lintstone-demo", ".csv");
        generateDummyData(tempFile, 100_000);

        System.out.println("Processing file: " + tempFile.toAbsolutePath());

        LintStoneSystem system = LintStoneSystemFactory.create();

        try {
            // 2. Register Aggregator
            LintStoneActorAccessor aggregator = system.registerActor("aggregator", 
                WordCountAggregator::new, ActorSettings.DEFAULT);

            // 3. Register a pool of Mappers
            int numMappers = Runtime.getRuntime().availableProcessors();
            List<LintStoneActorAccessor> mappers = new ArrayList<>();
            for (int i = 0; i < numMappers; i++) {
                mappers.add(system.registerActor("mapper-" + i, WordCountMapper::new, ActorSettings.DEFAULT));
            }

            long start = System.currentTimeMillis();

            // 4. Read file and distribute lines to mappers (using tellWithBackPressure)
            try (BufferedReader reader = Files.newBufferedReader(tempFile)) {
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null) {
                    mappers.get(count % numMappers).tellWithBackPressure(line, 1000);
                    count++;
                }
            }

            System.out.println("Finished reading and enqueuing lines. Waiting for processing...");

            // 5. Query result (using ask)
            // We need to wait a bit or ensure mappers are done. 
            // In a real system, we might have a completion signal.
            // For this demo, we'll just check if the queues are empty.
            while (mappers.stream().anyMatch(m -> m.getQueuedMessagesAndReplies() > 0) || 
                   aggregator.getQueuedMessagesAndReplies() > 0) {
                Thread.sleep(100);
            }

            CompletableFuture<Map<String, Long>> resultFuture = aggregator.ask("GET_RESULT");
            Map<String, Long> results = resultFuture.get(5, TimeUnit.SECONDS);

            long end = System.currentTimeMillis();

            System.out.println("Processing took: " + (end - start) + "ms");
            System.out.println("Total unique words found: " + results.size());
            
            // Print top 5 words
            results.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));

        } finally {
            system.shutDownAndWait();
            Files.deleteIfExists(tempFile);
        }
    }

    private static void generateDummyData(Path path, int lines) throws IOException {
        String[] sampleWords = {"apple", "banana", "cherry", "date", "elderberry", "fig", "grape", "honeydew"};
        try (var writer = Files.newBufferedWriter(path)) {
            for (int i = 0; i < lines; i++) {
                int wordCount = (int) (Math.random() * 10) + 1;
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < wordCount; j++) {
                    sb.append(sampleWords[(int) (Math.random() * sampleWords.length)]).append(" ");
                }
                writer.write(sb.toString().trim());
                writer.newLine();
            }
        }
    }
}
