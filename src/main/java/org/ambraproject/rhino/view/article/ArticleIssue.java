package org.ambraproject.rhino.view.article;

import org.ambraproject.models.Issue;
import org.ambraproject.models.Journal;
import org.ambraproject.models.Volume;

import java.util.Objects;

/**
 * Wrapper class for Issue which includes parent Volume and Journal objects
 */
public class ArticleIssue {

  private final Issue issue;
  private final Volume parentVolume;
  private final Journal parentJournal;

  public ArticleIssue(Issue issue, Volume parentVolume, Journal parentJournal) {
    this.issue = Objects.requireNonNull(issue);
    this.parentVolume = Objects.requireNonNull(parentVolume);
    this.parentJournal = Objects.requireNonNull(parentJournal);
  }

  public Issue getIssue() {
    return issue;
  }

  public Volume getParentVolume() {
    return parentVolume;
  }

  public Journal getParentJournal() {
    return parentJournal;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleIssue that = (ArticleIssue) o;

    if (!issue.equals(that.issue)) return false;
    if (!parentJournal.equals(that.parentJournal)) return false;
    if (!parentVolume.equals(that.parentVolume)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = issue.hashCode();
    result = 31 * result + parentVolume.hashCode();
    result = 31 * result + parentJournal.hashCode();
    return result;
  }
}
