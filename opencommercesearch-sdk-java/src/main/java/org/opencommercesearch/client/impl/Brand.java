package org.opencommercesearch.client.impl;

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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Data holder for a product brand.
 *
 * @author jmendez
 * @author rmerizalde
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Brand {
  private String id;
  private String name;
  private String logo;
  private String url;
  private List<String> sites;
  private Map<String, String> siteAttributes;

  public Brand() {}

  public Brand(String id) {
    this(id, null);
  }

  public Brand(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void setValue(String value) {
    this.id = value;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLogo() {
    return logo;
  }

  public void setLogo(String logo) {
    this.logo = logo;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public List<String> getSites() {
    return sites;
  }

  public void setSites(List<String> sites) {
    this.sites = sites;
  }

  public Map<String, String> getSiteAttributes() {
    return siteAttributes;
  }

  public void setSiteAttributes(Map<String, String> siteAttributes) {
    this.siteAttributes = siteAttributes;
  }

}
