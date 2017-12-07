package org.metaborg.spg.sentence.statistics;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

public class Histogram {
    private static final DecimalFormat decimalFormat = new DecimalFormat("#.#");

    private final List<Integer> data;
    private final int numberOfBins;
    private final int lowest;
    private final int highest;
    private final int range;
    private final int width;

    public Histogram(List<Integer> data) {
        this(data, 7);
    }

    public Histogram(List<Integer> data, int numberOfBins) {
        if (data.size() == 0) {
            throw new IllegalArgumentException("Cannot create a histogram for an empty array of data.");
        }

        Collections.sort(data);

        this.data = data;
        this.numberOfBins = numberOfBins;
        this.lowest = data.get(0);
        this.highest = data.get(data.size() - 1);
        this.range = highest - lowest;
        this.width = (int) Math.ceil(range / (double) numberOfBins);
    }

    public List<Integer> getData() {
        return data;
    }

    public int getNumberOfBins() {
        return numberOfBins;
    }

    public int getLowest() {
        return lowest;
    }

    public int getHighest() {
        return highest;
    }

    public int getRange() {
        return range;
    }

    public int getWidth() {
        return width;
    }

    public int getBinSize(int i) {
        return getEndIndex(i) - getStartIndex(i) + 1;
    }

    public int getStart(int bin) {
        return getWidth() * bin;
    }

    public int getEnd(int bin) {
        return getWidth() * (bin + 1);
    }

    // TODO: getStartIndex and getEndIndex are unnecessarily inefficient...
    public int getStartIndex(int bin) {
        for (int i = 0; i < data.size(); i++) {
            if (bin == 0 && data.get(i) >= getStart(bin)) {
                return i;
            }

            if (data.get(i) > getStart(bin)) {
                return i;
            }
        }

        return 0;
    }

    public int getEndIndex(int bin) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i) >= getEnd(bin)) {
                return i - 1;
            }
        }

        return data.size() - 1;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < numberOfBins; i++) {
            int binSize = getBinSize(i);
            double fraction = binSize / (double) data.size() * 100;

            stringBuilder
                    .append(getStart(i))
                    .append("-")
                    .append(getEnd(i))
                    .append(": ")
                    .append(binSize)
                    .append(" (")
                    .append(decimalFormat.format(fraction))
                    .append("%)")
                    .append("\n")
            ;
        }

        return stringBuilder.append("\n").toString();
    }
}
