package com.snapshop.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** @param inboxDir folder where prepared CSV files wait for an admin to preview and import them */
@ConfigurationProperties("snapshop.import")
public record ImportProperties(@DefaultValue("./inbox") Path inboxDir) {
}
