package org.ambraproject.rhino.view.article;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.ArticleListIdentity;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public class ListInputView {

  private final Optional<ArticleListIdentity> identity;
  private final Optional<String> title;
  private final Optional<ImmutableSet<ArticleIdentity>> articleIds;

  private ListInputView(ArticleListIdentity identity, String title, Set<ArticleIdentity> articleIds) {
    this.identity = Optional.ofNullable(identity);
    this.title = Optional.ofNullable(title);
    this.articleIds = (articleIds == null) ? Optional.empty()
        : Optional.of(ImmutableSet.copyOf(articleIds));
  }

  public Optional<ArticleListIdentity> getIdentity() {
    return identity;
  }

  public Optional<String> getTitle() {
    return title;
  }

  public Optional<ImmutableSet<ArticleIdentity>> getArticleIds() {
    return articleIds;
  }


  // Helper class that defines the JSON input contract for ListInputView. Deserialized by reflection.
  private static class RawInput {
    private String type;
    private String journal;
    private String key;
    private String title;
    private Collection<String> articleDois;
  }

  public static final JsonDeserializer<ListInputView> DESERIALIZER = (json, typeOfT, context) -> {
    RawInput inp = context.deserialize(json, RawInput.class);

    final ArticleListIdentity identity1;
    if (inp.type != null && inp.journal != null && inp.key != null) {
      identity1 = new ArticleListIdentity(inp.type, inp.journal, inp.key);
    } else if (inp.type == null && inp.journal == null && inp.key == null) {
      identity1 = null;
    } else {
      throw new PartialIdentityException();
    }

    final Set<ArticleIdentity> articleIds1;
    if (inp.articleDois == null) {
      articleIds1 = null;
    } else {
      articleIds1 = Sets.newLinkedHashSetWithExpectedSize(inp.articleDois.size());
      for (String articleDoi : inp.articleDois) {
        articleIds1.add(ArticleIdentity.create(articleDoi));
      }
    }

    return new ListInputView(identity1, inp.title, articleIds1);
  };

  /**
   * Indicates that at least one, but not all, of the components of an {@link ArticleListIdentity} were parsed.
   */
  public static class PartialIdentityException extends RuntimeException {
    private PartialIdentityException() {
    }
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ListInputView that = (ListInputView) o;

    if (!articleIds.equals(that.articleIds)) return false;
    if (!identity.equals(that.identity)) return false;
    if (!title.equals(that.title)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = identity.hashCode();
    result = 31 * result + title.hashCode();
    result = 31 * result + articleIds.hashCode();
    return result;
  }
}
