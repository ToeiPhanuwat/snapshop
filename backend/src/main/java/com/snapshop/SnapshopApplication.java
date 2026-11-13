package com.snapshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SnapshopApplication {

	public static void main(String[] args) {
		SpringApplication.run(SnapshopApplication.class, args);
	}

}
