package org.example.order.application.service.refund;

import org.example.order.order.application.service.order.RefundCalculationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Currency;

public class RefundRoundingTest {

    @ParameterizedTest
    @CsvSource({
            "352.94, USD, 5, 0, 1, last_n, 70.58",
            "352.94, USD, 5, 1, 1, last_n, 70.59",
            "352.94, USD, 5, 0, 2, last_n, 141.17",
            "352.94, USD, 5, 1, 2, last_n, 141.18",
            "352.94, USD, 5, 3, 2, last_n, 141.18",
            "352.93, USD, 5, 0, 1, last_n, 70.58",
            "352.93, USD, 5, 0, 2, last_n, 141.16",
            "352.93, USD, 5, 1, 2, last_n, 141.17",
            "352.93, USD, 5, 3, 1, last_n, 70.59",
            "352.93, USD, 5, 3, 2, last_n, 141.18",
            "352.94, USD, 5, 0, 1, first_n, 70.59",
            "352.94, USD, 5, 1, 1, first_n, 70.59",
            "352.94, USD, 5, 0, 2, first_n, 141.18",
            "352.94, USD, 5, 1, 2, first_n, 141.18",
            "352.94, USD, 5, 3, 2, first_n, 141.17",
            "352.93, USD, 5, 0, 1, first_n, 70.59",
            "352.93, USD, 5, 0, 2, first_n, 141.18",
            "352.93, USD, 5, 1, 2, first_n, 141.18",
            "352.93, USD, 5, 3, 1, first_n, 70.58",
            "352.93, USD, 5, 3, 2, first_n, 141.16",
    })
    void test_rounding_refund_amount(
            String totalAmount, String currencyCode,
            int totalQuantity, int refundedQuantity,
            int suggestQuantity, String rsValue,
            String expectedS
    ) {
        var totalAmountD = new BigDecimal(totalAmount);
        var currency = Currency.getInstance(currencyCode);
        var roundingStyle = RefundCalculationService.RoundingStyle.valueOf(rsValue);
        var expectedD = new BigDecimal(expectedS);
        var result = RefundCalculationService.suggestRefundAmount(
                totalAmountD, currency.getDefaultFractionDigits(),
                totalQuantity, refundedQuantity, suggestQuantity, roundingStyle
        );
        Assertions.assertEquals(0, expectedD.compareTo(result));
    }
}
