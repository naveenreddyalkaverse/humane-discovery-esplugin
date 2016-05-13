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

    private Set<String> buildPhoneticEncodings(String word, Set<String> encodings, String prefix) {
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

    public Set<String> buildEncodings(String word, Set<String> encodings) {
        return buildPhoneticEncodings(word, encodings, null);
    }

    public Set<String> buildEncodings(String word) {
        return this.buildEncodings(word, new HashSet<>());
    }
}

