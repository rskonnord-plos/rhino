package org.ambraproject.rhino.view.article;

/**
 * A serializable view object representing an article or some subset of an article's data.
 */
public interface ArticleView {

  /**
   * Return the article's DOI.
   *
   * @return the article's DOI
   */
  public abstract String getDoi();

}