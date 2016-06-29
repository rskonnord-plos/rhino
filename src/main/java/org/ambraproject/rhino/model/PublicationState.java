package org.ambraproject.rhino.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.EnumSet;

public enum PublicationState {
  /**
   * The article version has been ingested, but is not yet visible to end users.
   */
  INGESTED(0),
  /**
   * The article version is visible to end users.
   */
  PUBLISHED(1),
  /**
   * The article version was visible to end users, but has been taken down.
   */
  DISABLED(2),
  /**
   * The article version has been replaced by a more recently ingested version with the same revision number.
   */
  REPLACED(3);

  private final String label;
  private final int value;

  private PublicationState(int value) {
    this.label = name().toLowerCase();
    this.value = value;
  }

  public String getLabel() {
    return label;
  }

  public int getValue() {
    return value;
  }

  private static final ImmutableMap<Integer, PublicationState> BY_VALUE = Maps.uniqueIndex(
      EnumSet.allOf(PublicationState.class), PublicationState::getValue);

  public static PublicationState fromValue(int value) {
    PublicationState state = BY_VALUE.get(value);
    if (state == null) {
      throw new IllegalArgumentException(
          String.format("Received value: %d. Must be one of: %s", value, BY_VALUE.keySet()));
    }
    return state;
  }
}
