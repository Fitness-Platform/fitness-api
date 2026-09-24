package com.fitnessplatform.email;

import com.resend.Resend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResendConfig {

    @Bean
    Resend resend(
            @Value("${email.resend.api-key}") String apiKey
    ) {
        return new Resend(apiKey);
    }
}
