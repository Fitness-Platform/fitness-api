package com.fitnessplatform.purchase.stripe;

import com.stripe.model.Event;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/stripe")
public class StripeWebhookController {

    private final StripeWebhookService
            stripeWebhookService;

    private final StripeWebhookEventHandler
            stripeWebhookEventHandler;

    public StripeWebhookController(
            StripeWebhookService stripeWebhookService,
            StripeWebhookEventHandler stripeWebhookEventHandler
    ) {
        this.stripeWebhookService =
                stripeWebhookService;

        this.stripeWebhookEventHandler =
                stripeWebhookEventHandler;
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

        stripeWebhookEventHandler.handle(
                event
        );

        return ResponseEntity.ok().build();
    }
}
