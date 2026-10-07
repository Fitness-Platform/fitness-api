package com.fitnessplatform.purchase;

import com.fitnessplatform.purchase.stripe.StripeCheckoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PurchaseExceptionhandler {

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
}
