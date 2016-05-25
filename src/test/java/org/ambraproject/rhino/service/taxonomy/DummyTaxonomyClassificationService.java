package org.ambraproject.rhino.service.taxonomy;

import com.google.common.collect.ImmutableList;
import org.ambraproject.rhino.model.Article;
import org.w3c.dom.Document;

import java.io.IOException;
import java.util.List;

public class DummyTaxonomyClassificationService implements TaxonomyClassificationService {

  public static final ImmutableList<WeightedTerm> DUMMY_DATA = ImmutableList.<WeightedTerm>builder()
      .add(new WeightedTerm("/TopLevel1/term1", 5))
      .add(new WeightedTerm("/TopLevel2/term2", 10))
      .build();

  @Override
  public List<WeightedTerm> classifyArticle(Document articleXml, Article article) {
    return DUMMY_DATA;
  }

  @Override
  public List<String> getRawTerms(Document articleXml, Article article,
                                  boolean isTextRequired) throws IOException {
    if (isTextRequired) {
      return ImmutableList.of("dummy text sent to MAIstro", "dummy raw term");
    }
    return ImmutableList.of("dummy raw term");
  }

}
