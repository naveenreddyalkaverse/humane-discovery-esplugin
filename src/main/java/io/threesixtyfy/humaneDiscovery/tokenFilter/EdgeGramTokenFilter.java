package io.threesixtyfy.humaneDiscovery.tokenFilter;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.analysis.util.CharacterUtils;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

public class EdgeGramTokenFilter extends TokenFilter {

    private final ESLogger logger = Loggers.getLogger(EdgeGramTokenFilter.class);

    private final CharacterUtils charUtils;
    private final int minGram;
    private final int maxGram;
    private final String prefix;
    private final boolean payload;
    //    private char[] curTermBuffer;
    private char[] curTermCharArray;
    private CharBuffer curTermBuffer;
    private int curTermLength;
    private int curCodePointCount;
    private int curGramSize;
    private int tokStart;
    private int tokEnd; // only used if the length changed before this filter
    private int savePosIncr;
    private int savePosLen;

    private String source;

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    private final PositionLengthAttribute posLenAtt = addAttribute(PositionLengthAttribute.class);

    final PayloadAttribute payloadAtt;

    /**
     * Creates EdgeNGramTokenFilter that can generate n-grams in the sizes of the given range
     *
     * @param input   {@link TokenStream} holding the input to be tokenized
     * @param minGram the smallest n-gram to generate
     * @param maxGram the largest n-gram to generate
     */
    public EdgeGramTokenFilter(TokenStream input, int minGram, int maxGram, String prefix, boolean payload) {
        super(input);

        if (minGram < 1) {
            throw new IllegalArgumentException("minGram must be greater than zero");
        }

        if (minGram > maxGram) {
            throw new IllegalArgumentException("minGram must not be greater than maxGram");
        }

        this.charUtils = CharacterUtils.getInstance();
        this.minGram = minGram;
        this.maxGram = maxGram;
        this.prefix = "".equals(prefix) ? null : prefix;
        this.payload = payload;

        if (payload) {
            payloadAtt = addAttribute(PayloadAttribute.class);
        } else {
            payloadAtt = null;
        }
    }

    @Override
    public final boolean incrementToken() throws IOException {
        while (true) {
            if (curTermBuffer == null) {
                if (!input.incrementToken()) {
                    return false;
                } else {
                    curTermCharArray = termAtt.buffer().clone();
                    source = termAtt.toString();
                    curTermBuffer = CharBuffer.wrap(curTermCharArray);
                    curTermLength = termAtt.length();
                    curCodePointCount = charUtils.codePointCount(termAtt);
                    curGramSize = minGram;
                    tokStart = offsetAtt.startOffset();
                    tokEnd = offsetAtt.endOffset();
                    savePosIncr += posIncrAtt.getPositionIncrement();
                    savePosLen = posLenAtt.getPositionLength();
                }
            }

            if (curCodePointCount < minGram) {
                // simple pass on when text size is less than minGram
                this.resetState();
                return true;
            } else if (curCodePointCount >= minGram && curGramSize <= maxGram) {         // if we have hit the end of our n-gram size range, quit
                if (curGramSize <= curCodePointCount) { // if the remaining input is too short, we can't generate any n-grams
                    // grab gramSize chars from front or back
                    clearAttributes();
                    offsetAtt.setOffset(tokStart, tokEnd);
                    // first ngram gets increment, others don't
                    if (curGramSize == minGram) {
                        posIncrAtt.setPositionIncrement(savePosIncr);
                        savePosIncr = 0;
                    } else {
                        posIncrAtt.setPositionIncrement(0);
                    }
                    posLenAtt.setPositionLength(savePosLen);
                    final int charLength = Character.offsetByCodePoints(curTermCharArray, 0, curTermLength, 0, curGramSize);

                    if (curGramSize < curCodePointCount) {
                        if (this.payload) {
                            payloadAtt.setPayload(new BytesRef(this.source.getBytes(StandardCharsets.UTF_8)));
                        }

                        if (this.prefix != null) {
                            termAtt.setEmpty().append(this.prefix);
                        }
                    }

                    termAtt.append(curTermBuffer, 0, charLength);

                    curGramSize++;
                    return true;
                }
            }

            this.resetState();
        }
    }

    private void resetState() {
        curTermBuffer = null;
        savePosIncr = 0;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
    }

}
