package com.nadoumi.finance.service;

import com.nadoumi.finance.domain.MoneyTotal;
import java.util.List;

/**
 * Money that has been reported as paid but is not confirmed yet, per currency (application-fee payments waiting for
 * staff). Implemented by the payment module so finance does not depend on it.
 */
public interface ReceivablesSource {

    List<MoneyTotal> awaitingConfirmation();
}
