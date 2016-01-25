package org.opencommercesearch.client.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductTranslation {
    private String description;
    private String bottomLine;
    
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getBottomLine() {
        return bottomLine;
    }
    public void setBottomLine(String bottomLine) {
        this.bottomLine = bottomLine;
    }

}
