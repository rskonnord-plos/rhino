package org.ambraproject.rhino.view.journal;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.Issue;
import org.ambraproject.models.Journal;
import org.ambraproject.models.Volume;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.KeyedListView;

import java.util.List;

/**
 * A view of a {@link Journal} object that shows all nested volume and issue lists as JSON objects keyed by their
 * identifying URIs. Compare {@link JournalNonAssocView}, which omits the nested lists entirely.
 */
public class JournalOutputView implements JsonOutputView {

  private final Journal journal;

  public JournalOutputView(Journal journal) {
    this.journal = Preconditions.checkNotNull(journal);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = context.serialize(journal).getAsJsonObject();

    List<Volume> volumes = journal.getVolumes();
    KeyedListView<VolumeOutputView> volumeView = VolumeOutputView.wrapList(volumes);
    serialized.add("volumes", context.serialize(volumeView));

    Issue currentIssue = journal.getCurrentIssue();
    if (currentIssue != null) {
      IssueOutputView currentIssueView = new IssueOutputView(currentIssue);
      serialized.add("currentIssue", context.serialize(currentIssueView));
    }

    return serialized;
  }

}