package com.example.mavenextractor.model;

/**
 * Statistics for the extraction process.
 * Uses JDK 21 record for immutable data carrier.
 */
public record ExtractionStats(
    int total,
    int sourceExtracted,
    int decompiled,
    int skipped,
    int failed
) {
    /**
     * Creates a new stats with incremented source extracted count.
     */
    public ExtractionStats incrementSourceExtracted() {
        return new ExtractionStats(total, sourceExtracted + 1, decompiled, skipped, failed);
    }

    /**
     * Creates a new stats with incremented decompiled count.
     */
    public ExtractionStats incrementDecompiled() {
        return new ExtractionStats(total, sourceExtracted, decompiled + 1, skipped, failed);
    }

    /**
     * Creates a new stats with incremented skipped count.
     */
    public ExtractionStats incrementSkipped() {
        return new ExtractionStats(total, sourceExtracted, decompiled, skipped + 1, failed);
    }

    /**
     * Creates a new stats with incremented failed count.
     */
    public ExtractionStats incrementFailed() {
        return new ExtractionStats(total, sourceExtracted, decompiled, skipped, failed + 1);
    }

    /**
     * Creates initial stats with given total count.
     */
    public static ExtractionStats initial(int total) {
        return new ExtractionStats(total, 0, 0, 0, 0);
    }
}
