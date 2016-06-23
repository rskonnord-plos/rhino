package org.ambraproject.rhino.view.comment;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.model.Annotation;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.model.Flag;
import org.ambraproject.rhino.view.JsonOutputView;

import java.util.Objects;

/**
 * A view of an comment with no relationships to its parent or child comments.
 */
public class CommentNodeView implements JsonOutputView {

  // Slightly different from org.ambraproject.rhino.view.article.ArticleVisibility, which might be a bad thing
  public static class ArticleReference {
    private final String doi;
    private final String title;
    private final String journal;

    private ArticleReference(String doi, String title, String journal) {
      this.doi = Objects.requireNonNull(doi);
      this.title = Objects.requireNonNull(title);
      this.journal = Objects.requireNonNull(journal);
    }
  }

  private final Annotation comment;
  private final CompetingInterestStatement competingInterestStatement;
  private final ArticleReference parentArticle;

  private CommentNodeView(Annotation comment, CompetingInterestStatement competingInterestStatement,
      ArticleReference parentArticle) {
    this.comment = Objects.requireNonNull(comment);
    this.competingInterestStatement = Objects.requireNonNull(competingInterestStatement);
    this.parentArticle = Objects.requireNonNull(parentArticle);
  }

  private CommentNodeView(Annotation comment, CompetingInterestStatement competingInterestStatement) {
    this.comment = Objects.requireNonNull(comment);
    this.competingInterestStatement = Objects.requireNonNull(competingInterestStatement);
    this.parentArticle = null;
  }

  public static class Factory {
    private final CompetingInterestPolicy competingInterestPolicy;

    public Factory(RuntimeConfiguration runtimeConfiguration) {
      this.competingInterestPolicy = new CompetingInterestPolicy(runtimeConfiguration);
    }

    public CommentNodeView create(Annotation comment, String journalKey, String articleDoi,
        String articleTitle) {
      return new CommentNodeView(comment,
          competingInterestPolicy.createStatement(comment),
          new ArticleReference(articleDoi, articleTitle, journalKey));
    }

    public CommentNodeView create(Annotation comment) {
      return new CommentNodeView(comment, competingInterestPolicy.createStatement(comment));
    }

    public CommentFlagOutputView createFlagView(Flag commentFlag) {
      return new CommentFlagOutputView(commentFlag, create(commentFlag.getFlaggedAnnotation()));
    }
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = CommentOutputView.serializeBase(context, comment, competingInterestStatement);
    serialized.add("parentArticle", context.serialize(parentArticle));
    return serialized;
  }

}
