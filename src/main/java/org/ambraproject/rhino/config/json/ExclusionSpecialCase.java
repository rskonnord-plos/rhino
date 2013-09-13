package org.ambraproject.rhino.config.json;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import org.ambraproject.models.AmbraEntity;
import org.ambraproject.models.ArticleRelationship;

/**
 * Exclusions for classes and fields when using Gson's default, reflection-based serialization logic.
 *
 * @see org.ambraproject.rhino.config.RhinoConfiguration#entityGson()
 * @see com.google.gson.GsonBuilder#setExclusionStrategies(com.google.gson.ExclusionStrategy...)
 */
public enum ExclusionSpecialCase implements ExclusionStrategy {

  /**
   * Leave out the primary key on all classes, which is internal to the database and should not be meaningful to any
   * client.
   */
  PRIMARY_KEY {
    @Override
    public boolean shouldSkipField(FieldAttributes f) {
      return "ID".equals(f.getName());
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
      return false;
    }
  },

  /**
   * When serializing {@code ArticleRelationship} objects, leave out any reference to other persistent entities, because
   * they can cause infinite recursion. Serialize only the primitive {@code ArticleRelationship} fields.
   */
  ARTICLE_RELATIONSHIP {
    @Override
    public boolean shouldSkipField(FieldAttributes f) {
      return ArticleRelationship.class.isAssignableFrom(f.getDeclaringClass())
          && AmbraEntity.class.isAssignableFrom(f.getDeclaredClass());
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
      return false;
    }
  };

}