package org.opencommercesearch.client.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import org.apache.commons.lang.StringUtils;
import org.opencommercesearch.client.ProductSummary;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/*
* Licensed to OpenCommerceSearch under one
* or more contributor license agreements. See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership. OpenCommerceSearch licenses this
* file to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/

/**
 * Represents API product summary information for a product.
 *
 * @author jmendez
 * @author rmerizalde
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DefaultProductSummary implements ProductSummary {
  /**
   * The group summary data for a given product, as a Json node.
   */
  private JsonNode data;

  public DefaultProductSummary(JsonNode summaryData) {
    this.data = summaryData;
  }

  public Double getMinListPrice() {
    return getDouble("listPrice", "min");
  }

  public Double getMaxListPrice() {
    return getDouble("listPrice", "max");
  }

  public Double getMinDiscountPercent() {
    return getDouble("discountPercent", "min");
  }

  public Double getMaxDiscountPercent() {
    return getDouble("discountPercent", "max");
  }

  public Double getMinSalePrice() {
    return getDouble("salePrice", "min");
  }

  public Double getMaxSalePrice() {
    return getDouble("salePrice", "max");
  }

  public List<String> getColorFamilies() {
    JsonNode colorFamilies = getValue("color", "families");

    if (colorFamilies == null) {
      colorFamilies = getValue("colorFamily", "families");
      return colorFamilies != null ? Arrays.asList(StringUtils.split(colorFamilies.asText(), "[], ")) : null;
    } else {
      List<String> families = new LinkedList<String>();

      for (JsonNode colorFamily : colorFamilies) {
        families.add(colorFamily.asText());
      }
      return families;
    }
  }

  public Integer getColorCount() {
    JsonNode colorCount = getValue("color", "count");
    return colorCount != null ? colorCount.asInt() : null;
  }

  protected Double getDouble(String fieldName, String value) {
    JsonNode doubleValue = getValue(fieldName, value);
    return doubleValue != null ? doubleValue.asDouble() : null;
  }

  protected JsonNode getValue(String fieldName, String value) {
    if (data == null) {
      return null;
    }

    JsonNode field = data.get(fieldName);
    return field != null ? field.get(value) : null;
  }

  public Map<String, Integer> getBuckets(String fieldName) {
    JsonNode buckets = getValue(fieldName, "buckets");
    if (buckets == null || buckets.size() == 0) {
      return null;
    }
    Map<String, Integer> bucketMap = new HashMap<String, Integer>(buckets.size());

    for (Iterator<String> it = buckets.fieldNames(); it.hasNext();) {
      String bucket = it.next();
      bucketMap.put(bucket, buckets.get(bucket).asInt());
    }
    return bucketMap;
  }
}
