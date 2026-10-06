package com.fitnessplatform;

import com.fitnessplatform.purchase.stripe.StripeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(
		StripeProperties.class
)
public class FitnessApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(FitnessApiApplication.class, args);
	}

}
