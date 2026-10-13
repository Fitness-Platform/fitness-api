package com.fitnessplatform.purchase;

public class MemberPurchaseNotFoundException
        extends RuntimeException {

    public MemberPurchaseNotFoundException() {
        super("Purchase not found");
    }
}