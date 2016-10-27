package org.ambraproject.rhino.rest;

import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.rest.ClientItemId.NumberType;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/*
 * TODO: Wire as a Spring resolver
 */
public abstract class ClientItemIdResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.getParameterType() == ClientItemId.class;
  }

  /*
   * TODO: Assuming we keep this, figure out how to consistently extract DOIs from various request mappings.
   */
  protected abstract Doi extractDoi(MethodParameter parameter, NativeWebRequest webRequest);

  private static Integer parseIntParameter(NativeWebRequest webRequest, String parameterName) {
    String[] values = webRequest.getParameterValues(parameterName);
    switch (values.length) {
      case 0:
        return null;
      case 1:
        try {
          return Integer.valueOf(values[0]);
        } catch (NumberFormatException e) {
          throw new RestClientException("Numeric value required for " + parameterName, HttpStatus.BAD_REQUEST, e);
        }
      default:
        throw new RestClientException("Can't have more than one value for " + parameterName, HttpStatus.BAD_REQUEST);
    }
  }

  private static ClientItemId construct(Doi doi, Integer revision, Integer ingestion) {
    if (revision != null && ingestion != null) {
      throw new RestClientException("Can't have parameters for both revision and ingestion", HttpStatus.BAD_REQUEST);
    }
    return (revision != null) ? new ClientItemId(doi, revision, NumberType.REVISION)
        : (ingestion != null) ? new ClientItemId(doi, ingestion, NumberType.INGESTION)
        : new ClientItemId(doi, null, null);
  }

  @Override
  public ClientItemId resolveArgument(MethodParameter parameter,
                                      ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest,
                                      WebDataBinderFactory binderFactory) {
    return construct(extractDoi(parameter, webRequest),
        parseIntParameter(webRequest, "revision"),
        parseIntParameter(webRequest, "ingestion"));
  }

  /**
   * Resolve an ID from parameters extracted by hand.
   * <p>
   * This is an alternative to setting up a bean of this class as a proper {@link HandlerMethodArgumentResolver}. The
   * three arguments can be manually put on any handler method as parameters annotated with {@link
   * org.springframework.web.bind.annotation.RequestParam}.
   * <p>
   * TODO: Set up as Spring resolver; remove this method
   */
  public static ClientItemId resolve(String doi, Integer revision, Integer ingestion) {
    return construct(Doi.create(doi), revision, ingestion);
  }

}