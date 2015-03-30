package org.ambraproject.rhino.models;

import org.ambraproject.models.Article;

import java.util.List;
import java.util.Set;

/**
 * Created by jfesenko on 3/26/15.
 */
public class ArticleCollection extends org.ambraproject.models.AmbraEntity {

  private java.lang.String key;
  private java.lang.String title;
  private java.util.Set<org.ambraproject.models.Article> articles;

  public String getKey() {
    return key;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Set<Article> getArticles() {
    return articles;
  }

  public void setArticles(Set<Article> articles) {
    this.articles = articles;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleCollection that = (ArticleCollection) o;

    if (articles != null ? !articles.equals(that.articles) : that.articles != null) return false;
    if (!key.equals(that.key)) return false;
    if (!title.equals(that.title)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = key.hashCode();
    result = 31 * result + title.hashCode();
    result = 31 * result + (articles != null ? articles.hashCode() : 0);
    return result;
  }
}
