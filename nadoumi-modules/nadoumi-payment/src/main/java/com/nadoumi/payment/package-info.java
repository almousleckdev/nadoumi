/**
 * Payment slice (platform Step 9): the Nadoumi application fee, paid by QR code for now.
 *
 * <p>A student opens a payment session (15 minutes), pays in their banking app, then reports the payment with a
 * transaction number and a screenshot. Staff verify it. Only a confirmed payment lets the application be submitted.
 * The module implements {@code PaymentGuardProvider}, which the application module owns, and books confirmed fees
 * as finance revenue. A later gateway (card, Alipay, WeChat Pay) replaces only the verification step.</p>
 */
package com.nadoumi.payment;
