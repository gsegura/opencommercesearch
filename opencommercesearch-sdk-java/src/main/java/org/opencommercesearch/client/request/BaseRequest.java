package org.opencommercesearch.client.request;

import org.apache.commons.lang.StringUtils;
import org.opencommercesearch.client.ProductApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple base implementation of Request using an underlying map to store parameters.
 * <p/>
 * This class is not thread safe.
 *
 * @author jmendez
 */
public abstract class BaseRequest implements Request {

  public static final String FILTER_QUERY_SEPARATOR = "|";

  private static Logger logger = LoggerFactory.getLogger(BaseRequest.class);

  /**
   * The URI of the current request. For example: /api-docs
   */
  private Map<String, String> params = new LinkedHashMap<String, String>();

  private Map<String, String> headerParams = new LinkedHashMap<String, String>();
  /**
   * Sets a param on the request. If a previous value existed, is replaced with the new one.
   *
   * @param name  The param name.
   * @param value The value for the given param name.
   */
  public void setParam(String name, String value) {
    params.put(name, value);
  }

  /**
   * Gets the value for the given parameter name.
   *
   * @param name The parameter name.
   * @return The value associated to the given parameter name.
   */
  public String getParam(String name) {
    return params.get(name);
  }

  /**
   * Adds a param value to an existing value. By default, multivalued parameters are comma separated.
   *
   * @param name  The name of the param
   * @param value The new value to add
   */
  public void addParam(String name, String value) {
	  addParam(name, value, ",");
  }
  
  /**
   * Adds a param value to an existing value with a custom separator
   *
   * @param name  The name of the param
   * @param value The new value to add
   */
  public void addParam(String name, String value, String separator) {
    String currentValue = getParam(name);
    if (currentValue == null) {
      setParam(name, value);
    } else if (value != null) {
      setParam(name, currentValue + separator + value);
    }
  }

  /**
   * Gets the value for the given header parameter name.
   *
   * @param name The header parameter name.
   * @return The value associated to the given header parameter name.
   */
  public String getHeaderParam(String name) {
    return headerParams.get(name);
  }

  /**
   * Gets the map containing all header parameters
   * @return The map containing all header parameters
   */
  public Map<String, String> getHeaderParams() {
      return headerParams;
  }

  /**
   * Adds a param value to the header map.
   *
   * @param name  The name of the header param
   * @param value The new value to add
   */
  public void setHeaderParam(String name, String value) {
      headerParams.put(name, value);
  }

  /**
   * Converts this request to a valid query string.
   *
   * @return A query string conformed of all parameters stored in this request.
   * @throws UnsupportedEncodingException 
   */
  public String getQueryString() {
    if (params.isEmpty()) {
      return StringUtils.EMPTY;
    }

    StringBuilder queryString = new StringBuilder();

    for (String paramName : params.keySet()) {
      String paramValue = params.get(paramName);

      if (paramValue != null) {
        queryString.append(paramName);
        queryString.append("=");
        try {
            queryString.append(URLEncoder.encode(params.get(paramName), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            if (logger.isErrorEnabled()) {
              logger.error("Cannot encode '" + params.get(paramName) + "'", e);
            }
            queryString.append(params.get(paramName));
        }
        queryString.append("&");
      }
    }

    queryString.setLength(queryString.length() - 1);
    return queryString.toString();
  }

  @Override
  public String toString() {
      return getEndpoint() + "?" + getQueryString();
  }

  /**
   * Replaces the fields param in this request with the given fields
   * @param fields is the new field list
   */
  public void setFields(String[] fields) {
    if (fields == null) {
      throw new NullPointerException("fields");
    }

    setParam("fields", StringUtils.join(fields, ","));
  }

  /**
   * Adds a new field to this request
   * @param fieldName is the new field to add
   */

  public void addField(String fieldName) {
    addParam("fields", fieldName);
  }

  /**
   * Replaces the fields param in this request with the given fields
   * @param fields is the new field list
   */
  public void setMetadataFields(String[] fields) {
    if (fields == null) {
      throw new NullPointerException("metadata");
    }

    setParam("metadata", StringUtils.join(fields, ","));
  }

  /**
   * Adds a new metadata field to this request
   * @param fieldName is the new field to add
   */
  public void addMetadataField(String fieldName) {
    addParam("metadata", fieldName);
  }


  /**
   * Sets the site for this request
   * @param site is the request site
   */
  public void setSite(String site) {
    setParam("site", site);
  }

}
