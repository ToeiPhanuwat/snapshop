package com.snapshop;

import org.springframework.boot.SpringApplication;

public class TestSnapshopApplication {

	public static void main(String[] args) {
		SpringApplication.from(SnapshopApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
