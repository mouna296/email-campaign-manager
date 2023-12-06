package com.untitled.ecm;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Properties;

@Data
public class DakiyaConfiguration extends Configuration {
    @Valid
    @NotNull
    @JsonProperty("quartz-properties")
    private HashMap<String, String> quartzPropertiesMap;

    @Valid
    @NotNull
    @JsonProperty("dakiya-datasource")
    private DataSourceFactory dakiyaDataSourceFactory;

    @Valid
    @NotNull
    @JsonProperty("redshift-datasource")
    private DataSourceFactory redshiftDataSourceFactory;

    @Valid
    @NotNull
    @JsonProperty("dakiya-runtime-settings")
    private HashMap<String, String> dakiyaRuntimeSettingsMap;

    @Valid
    @NotNull
    @JsonProperty("reset-dakiya-runtime-settings")
    private boolean ResetDakiyaRuntimeSettings;

    @Valid
    @NotNull
    @Min(1)
    @Max(50)
    @JsonProperty("max-dakiya-background-tasks")
    private int maxDakiyaBackgroundTasks;

    @NotNull
    @JsonProperty("instance-type")
    private String instanceType;


    Properties getQuartzProperties() {
        Properties schedularProperties = new Properties();
        for (HashMap.Entry<String, String> pair : this.quartzPropertiesMap.entrySet()) {
            schedularProperties.setProperty(pair.getKey(), pair.getValue());
        }
        return schedularProperties;
    }
}
