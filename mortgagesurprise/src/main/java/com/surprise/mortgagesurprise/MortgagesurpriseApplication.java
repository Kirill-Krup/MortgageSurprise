package com.surprise.mortgagesurprise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.surprise.mortgagesurprise.repository.jpa")
@EnableMongoRepositories(basePackages = "com.surprise.mortgagesurprise.repository.mongo")
public class MortgagesurpriseApplication {

	public static void main(String[] args) {
		SpringApplication.run(MortgagesurpriseApplication.class, args);
	}

}
