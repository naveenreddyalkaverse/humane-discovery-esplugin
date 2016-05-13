package io.threesixtyfy.humaneDiscovery.analyzer;

import io.threesixtyfy.humaneDiscovery.tokenFilter.EdgeGramTokenFilterFactory;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.AbstractIndexAnalyzerProvider;
import org.elasticsearch.index.analysis.CustomAnalyzer;
import org.elasticsearch.index.analysis.KeywordTokenizerFactory;
import org.elasticsearch.index.analysis.LowerCaseTokenFilterFactory;
import org.elasticsearch.index.analysis.StandardTokenizerFactory;
import org.elasticsearch.index.analysis.TokenFilterFactory;
import org.elasticsearch.index.settings.IndexSettingsService;

public class HumaneKeywordAnalyzerProvider extends AbstractIndexAnalyzerProvider<CustomAnalyzer> {

    private final CustomAnalyzer customAnalyzer;

    @Inject
    public HumaneKeywordAnalyzerProvider(Index index, IndexSettingsService indexSettingsService, @Assisted String name, @Assisted Settings settings) {
        super(index, indexSettingsService.indexSettings(), name, settings);
        customAnalyzer = new CustomAnalyzer(new KeywordTokenizerFactory(index, indexSettingsService, name, settings),
                null,
                new TokenFilterFactory[]{
                        new LowerCaseTokenFilterFactory(index, indexSettingsService, name, settings)
                });
    }

    @Override
    public CustomAnalyzer get() {
        return this.customAnalyzer;
    }
}
