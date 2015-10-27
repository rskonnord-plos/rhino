package org.ambraproject.rhino.view;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

@FunctionalInterface
public interface JsonOutputView {

  public abstract JsonElement serialize(JsonSerializationContext context);

  public static final JsonSerializer<JsonOutputView> SERIALIZER = (src, typeOfSrc, context) -> src.serialize(context);

}
