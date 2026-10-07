package com.fitnessplatform.purchase.stripe;

import com.stripe.model.Event;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/stripe")
public class StripeWebhookController {

    private final StripeWebhookService
            stripeWebhookService;

    public StripeWebhookController(
            StripeWebhookService stripeWebhookService
    ) {
        this.stripeWebhookService = stripeWebhookService;
    }

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature")
            String signatureHeader
    ) {
        Event event =
                stripeWebhookService.verifyAndParse(
                        payload,
                        signatureHeader
                );

        return ResponseEntity.ok().build();
    }
}
