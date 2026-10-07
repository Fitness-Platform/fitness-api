package com.fitnessplatform.purchase.stripe;

import com.fitnessplatform.TestcontainersConfiguration;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class StripeWebhookControllerTest {

    private static final String PAYLOAD = """
            {
              "id": "evt_test_123",
              "object": "event",
              "type": "checkout.session.completed",
              "data": {
                "object": {
                  "id": "cs_test_123",
                  "object": "checkout.session"
                }
              }
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StripeProperties stripeProperties;

    @Test
    void shouldAcceptValidWebhookWithoutAuthenticationOrCsrf()
            throws Exception {

        String signature =
                Webhook.Signature
                        .generateSignatureHeader(
                                PAYLOAD,
                                stripeProperties.webhookSecret()
                        );

        mockMvc.perform(
                        post("/api/webhooks/stripe")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .header(
                                        "Stripe-Signature",
                                        signature
                                )
                                .content(
                                        PAYLOAD
                                )
                )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    void shouldRejectWebhookWithInvalidSignature()
            throws Exception {

        mockMvc.perform(
                        post("/api/webhooks/stripe")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .header(
                                        "Stripe-Signature",
                                        "invalid-signature"
                                )
                                .content(
                                        PAYLOAD
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Invalid Stripe webhook"
                                )
                )
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "Invalid Stripe webhook signature"
                                )
                );
    }

    @Test
    void shouldRejectWebhookWithoutStripeSignatureHeader()
            throws Exception {

        mockMvc.perform(
                        post("/api/webhooks/stripe")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        PAYLOAD
                                )
                )
                .andExpect(
                        status().isBadRequest()
                );
    }
}