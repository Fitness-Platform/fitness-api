package com.fitnessplatform.purchase;

import com.fitnessplatform.purchase.stripe.StripeCheckoutException;
import com.fitnessplatform.purchase.stripe.StripeWebhookException;
import com.fitnessplatform.purchase.stripe.StripeWebhookProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class PurchaseExceptionhandler {

    private static final Logger log =
            LoggerFactory.getLogger(PurchaseExceptionhandler.class);

    @ExceptionHandler(
            ProgramNotPurchasableException.class
    )
    ProblemDetail handleProgramNotPurchasable(
            ProgramNotPurchasableException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        exception.getMessage()
                );

        problem.setTitle("Program not purchasable");

        return problem;
    }

    @ExceptionHandler(
            StripeCheckoutException.class
    )
    ProblemDetail handleStripeCheckout(
            StripeCheckoutException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_GATEWAY,
                        "Unable to create checkout session"
                );

        problem.setTitle(
                "Stripe checkout unavailable"
        );

        return problem;
    }

    @ExceptionHandler(
            StripeWebhookException.class
    )
    ProblemDetail handleStripeWebhook(
            StripeWebhookException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        "Invalid Stripe webhook signature"
                );

        problem.setTitle("Invalid Stripe webhook");

        return problem;
    }

    @ExceptionHandler(PurchasePaymentConfirmationException.class)
    ProblemDetail handlePurchasePaymentConfirmation(
            PurchasePaymentConfirmationException exception
    ) {
        log.error("Failed to confirm purchase payment", exception);

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Unable to confirm purchase payment"
                );

        problem.setTitle("Purchase payment confirmation failed");

        return problem;
    }

    @ExceptionHandler(StripeWebhookProcessingException.class)
    ProblemDetail handleStripeWebhookProcessing(
            StripeWebhookProcessingException exception
    ) {

        log.error("Failed to process Stripe webhook", exception);

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Unable to process Stripe webhook event"
                );

        problem.setTitle("Stripe webhook processing failed");

        return problem;
    }

    @ExceptionHandler(MemberPurchaseNotFoundException.class)
    ProblemDetail handleMemberPurchaseNotFound(
            MemberPurchaseNotFoundException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Purchase not found"
        );

        problem.setTitle("Purchase not found");

        return problem;
    }

    @ExceptionHandler(AdminPurchaseNotFoundException.class)
    ProblemDetail handleAdminPurchaseNotFound(
            AdminPurchaseNotFoundException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Purchase not found"
        );

        problem.setTitle("Purchase not found");

        return problem;
    }
}
