package org.ambraproject.rhino.util.response;

import org.ambraproject.models.AmbraEntity;

import java.io.IOException;
import java.util.Calendar;
import java.util.Objects;

/**
 * A "transceiver" that translates a timestamped persistent entity into a view.
 *
 * @param <E> the persistent entity type
 */
public abstract class EntityTransceiver<E extends AmbraEntity> extends Transceiver {

  private E entity = null;

  private E getEntity() {
    return (entity != null) ? entity : (entity = Objects.requireNonNull(fetchEntity()));
  }

  /**
   * Read the entity from the persistence tier.
   *
   * @return the entity
   */
  protected abstract E fetchEntity();

  /**
   * {@inheritDoc}
   * <p>
   * This class fetches the full persistent entity and uses built-in last-modified date. Subclasses may override this
   * method to return the last-modified date without reading the rest of the entity for a performance improvement.
   */
  @Override
  protected Calendar getLastModifiedDate() throws IOException {
    return copyToCalendar(getEntity().getLastModified());
  }

  @Override
  protected final Object getData() throws IOException {
    return Objects.requireNonNull(getView(getEntity()));
  }

  /**
   * Translate the entity into a view object.
   *
   * @param entity the entity
   * @return a view representing the entity
   */
  protected abstract Object getView(E entity);

}
