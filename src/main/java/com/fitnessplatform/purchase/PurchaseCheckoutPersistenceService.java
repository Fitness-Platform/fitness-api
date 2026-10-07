package com.fitnessplatform.purchase;

import com.fitnessplatform.program.Program;
import com.fitnessplatform.program.ProgramNotFoundException;
import com.fitnessplatform.program.ProgramRepository;
import com.fitnessplatform.program.ProgramStatus;
import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserNotFoundException;
import com.fitnessplatform.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PurchaseCheckoutPersistenceService {

    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final ProgramRepository programRepository;

    public PurchaseCheckoutPersistenceService(
            PurchaseRepository purchaseRepository,
            UserRepository userRepository,
            ProgramRepository programRepository
    ) {
        this.purchaseRepository = purchaseRepository;
        this.userRepository = userRepository;
        this.programRepository = programRepository;
    }

    @Transactional
    public PendingPurchaseCheckout createPendingPurchase(
            UUID userId,
            UUID programId
    ) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(
                                () -> new UserNotFoundException(
                                        userId
                                )
                        );

        Program program =
                programRepository
                        .findById(programId)
                        .orElseThrow(
                                () -> new ProgramNotFoundException(
                                        programId
                                )
                        );

        validatePurchasable(
                program
        );

        Purchase purchase =
                purchaseRepository.saveAndFlush(
                        new Purchase(
                                user,
                                program,
                                program.getPriceCents(),
                                program.getCurrency()
                        )
                );

        return new PendingPurchaseCheckout(
                purchase.getId(),
                program.getName(),
                purchase.getAmountCents(),
                purchase.getCurrency()
        );
    }

    @Transactional
    public void attachStripeCheckoutSession(
            UUID purchaseId,
            String stripeCheckoutSessionId
    ) {
        Purchase purchase =
                purchaseRepository
                        .findById(
                                purchaseId
                        )
                        .orElseThrow();

        purchase.attachStripeCheckoutSession(
                stripeCheckoutSessionId
        );

        purchaseRepository.saveAndFlush(
                purchase
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
