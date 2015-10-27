package org.ambraproject.rhino.view.article;

import org.ambraproject.rhino.identity.ArticleIdentity;

import java.util.Date;
import java.util.Objects;

public class RecentArticleView {

  private final ArticleIdentity doi;
  private final String title;
  private final Date date;

  public RecentArticleView(ArticleIdentity article, String title, Date date) {
    this.doi = Objects.requireNonNull(article);
    this.title = Objects.requireNonNull(title);
    this.date = Objects.requireNonNull(date);
  }

  public ArticleIdentity getDoi() {
    return doi;
  }

  public String getTitle() {
    return title;
  }

  public Date getDate() {
    return date;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RecentArticleView that = (RecentArticleView) o;

    if (!date.equals(that.date)) return false;
    if (!doi.equals(that.doi)) return false;
    if (!title.equals(that.title)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = doi.hashCode();
    result = 31 * result + title.hashCode();
    result = 31 * result + date.hashCode();
    return result;
  }
}
