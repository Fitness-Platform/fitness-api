# Stripe Checkout — End-to-End Validation

## Objective

Validate the complete purchase flow using Stripe Test Mode.

## Environment

- Java 21 / Spring Boot
- PostgreSQL
- Stripe Test Mode
- Stripe CLI
- Stripe Java SDK
- Local backend: `http://localhost:8080`

## Stripe CLI

Start the webhook listener using an API version compatible with
the Stripe Java SDK.

For the SDK version used during validation:

```bash
stripe --api-key "$STRIPE_SECRET_KEY" listen \
  --latest \
  --events checkout.session.completed,checkout.session.expired \
  --forward-to http://localhost:8080/api/webhooks/stripe
```

The API version observed during validation was:

`2026-09-30.endive`

The webhook signing secret returned by Stripe CLI must match
the `STRIPE_WEBHOOK_SECRET` configured in the backend.

Do not commit Stripe API keys or webhook signing secrets.

## Validated Flow

1. Authenticate a member.
2. Create a Checkout Session through the backend API.
3. Complete payment using a Stripe test card.
4. Receive `checkout.session.completed`.
5. Verify successful webhook processing (HTTP 200).
6. Verify Purchase transitions from PENDING to PAID.
7. Verify PaymentIntent and paidAt are persisted.
8. Verify exactly one ProgramAccess is associated with the paid Purchase.
9. Verify the purchased program is available through the Member API.
10. Verify the access expiration date is six calendar months after payment confirmation.

## Results

**Status: PASSED**

The complete purchase flow was successfully validated on October 14, 2026.

- Stripe Checkout Session created successfully.
- Test payment completed successfully.
- Webhook delivered and processed with HTTP 200.
- Purchase persisted as PAID.
- ProgramAccess granted successfully.
- Member Programs API returned the purchased program.
- Access duration validated as six calendar months.
- Exactly one ProgramAccess was associated with the confirmed Purchase.

## Known Issue — API Version Compatibility

During the initial validation, a Stripe event using API version
`2020-08-27` could not be safely deserialized by the Stripe Java SDK,
which expected `2026-09-30.endive`.

The webhook returned HTTP 500 and the corresponding Purchase remained
PENDING.

After starting Stripe CLI with `--latest`, a new payment was processed
successfully using API version `2026-09-30.endive`.

The original test Purchase was intentionally left unchanged.

Payment reconciliation and recovery of previously failed events are
outside the scope of this validation task.

## Production Considerations

Before production deployment:

- Configure the webhook endpoint with an explicit API version compatible
  with the Stripe Java SDK.
- Do not rely on `--latest` as a permanent production configuration.
- Configure production webhook signing secrets separately.
- Ensure webhook processing failures are logged and monitored.
- Define an operational procedure for failed webhook deliveries and
  payment reconciliation.
- Never grant program access based solely on the Checkout success redirect.

## Scope

This validation used Stripe Test Mode.

It does not validate production payments, production webhook delivery,
refunds, or deployment infrastructure.