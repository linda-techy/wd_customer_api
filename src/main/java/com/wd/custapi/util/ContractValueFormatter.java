package com.wd.custapi.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class ContractValueFormatter {

    private static final BigDecimal ONE_LAKH = new BigDecimal("100000");
    private static final BigDecimal ONE_CRORE = new BigDecimal("10000000");
    private static final String RUPEE = "\u20B9";

    private ContractValueFormatter() {}

    public static String formatINR(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Amount cannot be negative: " + amount);
        }
        if (amount.compareTo(ONE_CRORE) >= 0) {
            BigDecimal crores = amount.divide(ONE_CRORE, 2, RoundingMode.HALF_UP);
            return RUPEE + crores.toPlainString() + " Cr";
        }
        if (amount.compareTo(ONE_LAKH) >= 0) {
            BigDecimal lakhs = amount.divide(ONE_LAKH, 0, RoundingMode.HALF_UP);
            return RUPEE + lakhs.toPlainString() + " L";
        }
        // Indian comma grouping (e.g. 5,500 / 99,999)
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.of("en", "IN"));
        DecimalFormat fmt = new DecimalFormat("#,##,###", symbols);
        long rupees = amount.setScale(0, RoundingMode.HALF_UP).longValueExact();
        return RUPEE + fmt.format(rupees);
    }
}
