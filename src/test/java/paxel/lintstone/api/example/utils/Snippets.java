package paxel.lintstone.api.example.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Snippets {

    private int current = 0;

    record WeightedSnippet(Integer value, String text) implements Comparable<WeightedSnippet> {

        @Override
        public int compareTo(WeightedSnippet o) {
            return Integer.compare(value, o.value);
        }
    }

    List<WeightedSnippet> snippets = new ArrayList<>();

    public String get(double v) {
        int index = (int) (v * current);
        int i = Collections.binarySearch(snippets, new WeightedSnippet(index, ""));
        if (i < 0) {
            i = Math.abs(i) + 1;
        }
        if (snippets.size() <= i)
            return snippets.get(i).text();

        throw new IllegalStateException("double " + v + " current:" + current + " i:" + i + " size:" + snippets.size());
    }

    public void add(String snippet, int weight) {
        snippets.add(new WeightedSnippet(current + weight, snippet));
        current += weight;
    }
}
