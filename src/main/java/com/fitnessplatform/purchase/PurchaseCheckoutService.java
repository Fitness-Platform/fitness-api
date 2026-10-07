package com.fitnessplatform.purchase;

import com.fitnessplatform.program.Program;
import com.fitnessplatform.program.ProgramNotFoundException;
import com.fitnessplatform.program.ProgramRepository;
import com.fitnessplatform.program.ProgramStatus;
import com.fitnessplatform.purchase.stripe.StripeCheckoutGateway;
import com.fitnessplatform.purchase.stripe.StripeCheckoutSession;
import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserNotFoundException;
import com.fitnessplatform.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PurchaseCheckoutService {

    private final PurchaseRepository
            purchaseRepository;

    private final UserRepository
            userRepository;

    private final ProgramRepository
            programRepository;

    private final StripeCheckoutGateway
            stripeCheckoutGateway;

    public PurchaseCheckoutService(
            PurchaseRepository purchaseRepository,
            UserRepository userRepository,
            ProgramRepository programRepository,
            StripeCheckoutGateway stripeCheckoutGateway
    ) {
        this.purchaseRepository =
                purchaseRepository;

        this.userRepository =
                userRepository;

        this.programRepository =
                programRepository;

        this.stripeCheckoutGateway =
                stripeCheckoutGateway;
    }

    @Transactional
    public CheckoutResponse createCheckout(
            UUID userId,
            UUID programId
    ) {
        User user =
                userRepository
                        .findById(
                                userId
                        )
                        .orElseThrow(
                                () -> new UserNotFoundException(
                                        userId
                                )
                        );

        Program program =
                programRepository
                        .findById(
                                programId
                        )
                        .orElseThrow(
                                () -> new ProgramNotFoundException(
                                        programId
                                )
                        );

        validatePurchasable(
                program
        );

        Purchase purchase =
                new Purchase(
                        user,
                        program,
                        program.getPriceCents(),
                        program.getCurrency()
                );

        purchase =
                purchaseRepository
                        .saveAndFlush(
                                purchase
                        );

        StripeCheckoutSession stripeSession =
                stripeCheckoutGateway
                        .createSession(
                                purchase.getId(),
                                program.getName(),
                                purchase.getAmountCents(),
                                purchase.getCurrency()
                        );

        purchase.attachStripeCheckoutSession(
                stripeSession.id()
        );

        purchaseRepository.saveAndFlush(
                purchase
        );

        return new CheckoutResponse(
                purchase.getId(),
                stripeSession.url()
        );
    }

    private void validatePurchasable(
            Program program
    ) {
        if (
                program.getStatus()
                        != ProgramStatus.PUBLISHED
        ) {
            throw new ProgramNotPurchasableException(
                    program.getId(),
                    "Program is not published"
            );
        }

        if (
                program.getPriceCents() == null
                        || program.getPriceCents() <= 0
        ) {
            throw new ProgramNotPurchasableException(
                    program.getId(),
                    "Program price is not configured"
            );
        }
    }
}
