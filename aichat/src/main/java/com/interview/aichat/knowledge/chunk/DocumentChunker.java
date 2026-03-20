package com.interview.aichat.knowledge.chunk;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple character-based chunker for RAG ingestion.
 *
 * Notes:
 * - We try to cut chunks on paragraph/sentence boundaries to preserve semantics.
 * - We apply character overlap to reduce context loss across chunk borders.
 */
@Component
public class DocumentChunker {

    public List<String> chunk(String text, int chunkSizeChars, int overlapChars) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        if (chunkSizeChars <= 0) {
            throw new IllegalArgumentException("chunkSizeChars must be > 0");
        }
        if (overlapChars < 0) {
            throw new IllegalArgumentException("overlapChars must be >= 0");
        }

        // overlap must be smaller than chunkSize to guarantee forward progress
        overlapChars = Math.min(overlapChars, chunkSizeChars - 1);

        String normalized = text.strip();
        int n = normalized.length();

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < n) {
            int endExclusive = Math.min(start + chunkSizeChars, n);
            int cut = findBestCut(normalized, start, endExclusive);
            if (cut <= start) {
                cut = endExclusive;
            }

            String chunk = normalized.substring(start, cut).strip();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }

            if (cut >= n) {
                break;
            }

            start = cut - overlapChars;
            if (start < 0) {
                start = 0;
            }
        }

        return chunks;
    }

    private int findBestCut(String text, int startInclusive, int endExclusive) {
        // Prefer paragraph boundary within the window.
        int windowLen = endExclusive - startInclusive;
        int minCutPosInWindow = Math.max(0, windowLen / 4);

        String window = text.substring(startInclusive, endExclusive);

        int lastDoubleNewline = window.lastIndexOf("\n\n");
        if (lastDoubleNewline >= minCutPosInWindow) {
            return startInclusive + lastDoubleNewline;
        }

        int lastSingleNewline = window.lastIndexOf("\n");
        if (lastSingleNewline >= minCutPosInWindow) {
            return startInclusive + lastSingleNewline;
        }

        // Prefer sentence-ending punctuation.
        int lastChinesePeriod = window.lastIndexOf('。');
        int lastEnglishPeriod = window.lastIndexOf('.');
        int lastQuestion = window.lastIndexOf('？');
        int lastExclamation = window.lastIndexOf('！');

        int lastSentenceEnd = Math.max(
                Math.max(lastChinesePeriod, lastEnglishPeriod),
                Math.max(lastQuestion, lastExclamation)
        );
        if (lastSentenceEnd >= minCutPosInWindow) {
            return startInclusive + lastSentenceEnd + 1; // keep the punctuation
        }

        return endExclusive;
    }
}

