package io.threesixtyfy.humaneDiscovery.didYouMean.commons;

import io.threesixtyfy.humaneDiscovery.tokenFilter.HumaneTokenFilter;
import io.threesixtyfy.humaneDiscovery.tokenFilter.StringTokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class PhoneticEncodingUtils {

    private final ESLogger logger = Loggers.getLogger(PhoneticEncodingUtils.class);

    private final StringTokenStream phoneticInputTokenStream = new StringTokenStream();
    private final HumaneTokenFilter phoneticTokenFilter = new HumaneTokenFilter(phoneticInputTokenStream);
    private final CharTermAttribute phoneticTermAttribute = phoneticTokenFilter.getAttribute(CharTermAttribute.class);

    private Set<String> buildPhoneticEncodings(String word, Set<String> encodings, String prefix, boolean stopWord) {
        if (word == null) {
            return encodings;
        }

        encodings.add(word);

        int wordLength = word.length();

        // we do not add phonetic encodings for 2 or less size
        if (wordLength == 1 || stopWord) {
            return encodings;
        }

        int minGram = getMin(wordLength);
        int maxGram = getMax(wordLength);

        for (int ng = minGram; ng <= maxGram; ng++) {
            String end = null;
            for (int i = 0; i < wordLength - ng + 1; i++) {
                String gram = word.substring(i, i + ng);

                if (i == 0) {
                    encodings.add("gs#" + gram);
                }

                encodings.add("g#" + gram);
                end = gram;
            }
            if (end != null) { // may not be present if len==ng1
                encodings.add("ge#" + end);
            }
        }

        if (wordLength == 2) {
            return encodings;
        }

        try {
            phoneticInputTokenStream.setValue(word);

            phoneticInputTokenStream.reset();
            phoneticTokenFilter.reset();

            while (phoneticTokenFilter.incrementToken()) {
                String phonetic = phoneticTermAttribute.toString();

                if (prefix != null) {
                    phonetic = prefix + phonetic;
                }

                encodings.add(phonetic);
            }

            phoneticTokenFilter.close();
            phoneticInputTokenStream.close();
        } catch (IOException e) {
            logger.error("IOException in generating phonetic tokens: {}", e, word);
        }

        return encodings;
    }

    public Set<String> buildEncodings(String word, Set<String> encodings, boolean stopWord) {
        return buildPhoneticEncodings(word, encodings, null, stopWord);
    }

    public Set<String> buildEncodings(String word, boolean stopWord) {
        return this.buildEncodings(word, new HashSet<>(), stopWord);
    }

    private int getMin(int l) {
//        if (l > 5) {
//            return 3;
//        }
        if (l >= 5) {
            return 2;
        }
        return 1;
    }

    private int getMax(int l) {
        if (l > 5) {
            return 4;
        }
        if (l == 5) {
            return 3;
        }
        return 2;
    }
}


