package com.fitnessplatform.purchase;

public class AdminPurchaseNotFoundException extends RuntimeException {

    public AdminPurchaseNotFoundException() {
        super("Purchase not found");
    }
}