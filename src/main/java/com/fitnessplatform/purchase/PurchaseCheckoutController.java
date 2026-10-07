package com.fitnessplatform.purchase;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/programs")
public class PurchaseCheckoutController {

    private final PurchaseCheckoutService
                purchaseCheckoutService;

    public PurchaseCheckoutController(
            PurchaseCheckoutService
                purchaseCheckoutService
    ) {
        this.purchaseCheckoutService =
                purchaseCheckoutService;
    }

    @PostMapping("/{programId}/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public CheckoutResponse createCheckout(
            @PathVariable UUID programId,
            Authentication authentication
    ) {
        UUID userId =
                (UUID) authentication.getPrincipal();

        return purchaseCheckoutService
                .createCheckout(
                        userId,
                        programId
                );
    }
}
