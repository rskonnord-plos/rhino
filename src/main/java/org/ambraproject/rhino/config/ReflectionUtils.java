package org.ambraproject.rhino.config;

import java.lang.reflect.Field;

import static org.springframework.util.ReflectionUtils.findField;
import static org.springframework.util.ReflectionUtils.getField;

/**
 * Created by jkrzemien on 8/8/14.
 */
public class ReflectionUtils {

  public static void setFieldValue(Object o, String fieldName, Object value) {
    Field field = findField(o.getClass(), fieldName);
    if (field != null) {
      field.setAccessible(true);
      try {
        field.set(o, value);
      } catch (IllegalAccessException e) {
        // Should never happen...
        e.printStackTrace();
      }
      field.setAccessible(false);
    }
  }

  public static <T> T getFieldValue(Object o, String fieldName, Class<T> castTo) {
    if (o == null)
      return null;
    Field field = findField(o.getClass(), fieldName);
    if (field != null) {
      field.setAccessible(true);
      T value = (T) getField(field, o);
      field.setAccessible(false);
      return value;
    }
    return null;
  }
}
