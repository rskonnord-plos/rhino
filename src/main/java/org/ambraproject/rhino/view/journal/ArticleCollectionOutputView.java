package org.ambraproject.rhino.view.journal;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.models.ArticleCollection;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.KeyedListView;

import java.util.Collection;


public class ArticleCollectionOutputView implements JsonOutputView {

  private final ArticleCollection articleCollection;

  public ArticleCollectionOutputView(ArticleCollection collection) {
    this.articleCollection = Preconditions.checkNotNull(collection);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = context.serialize(articleCollection).getAsJsonObject();

    serialized.add("title", context.serialize(articleCollection.getTitle()));

    return serialized;
  }

  public static class ListView extends KeyedListView<ArticleCollection> {
    private ListView(Collection<? extends ArticleCollection> values) {
      super(values);
    }

    @Override
    protected String getKey(ArticleCollection value) {
      return value.getKey();
    }

    @Override
    protected Object wrap(ArticleCollection value) {
      return new ArticleCollectionOutputView(value);
    }
  }

  public static KeyedListView<ArticleCollection> wrapList(Collection<ArticleCollection> articleCollections) {
    return new ListView(articleCollections);
  }

}