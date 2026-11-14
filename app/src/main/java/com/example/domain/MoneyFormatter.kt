package com.example.domain

import com.example.data.model.CurrencyCode
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object MoneyFormatter {

    private val usSymbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }

    fun parseBigDecimal(raw: String): BigDecimal? {
        val normalized = raw.trim()
            .replace(" ", "")
            .replace(',', '.')
        if (normalized.isEmpty()) return null
        return try {
            BigDecimal(normalized)
        } catch (_: NumberFormatException) {
            null
        }
    }

    fun formatMad(
        amount: BigDecimal,
        alwaysShowDecimals: Boolean = false,
        includePlusSign: Boolean = false
    ): String {
        val formattedNum = formatNumber(amount.abs(), alwaysShowDecimals)
        val sign = when {
            amount.compareTo(BigDecimal.ZERO) < 0 -> "-"
            amount.compareTo(BigDecimal.ZERO) > 0 && includePlusSign -> "+"
            else -> ""
        }
        return "$sign$formattedNum DH"
    }

    fun formatNumber(amount: BigDecimal, alwaysShowDecimals: Boolean = false): String {
        val stripped = amount.stripTrailingZeros()
        val hasFraction = stripped.scale() > 0
        val pattern = if (alwaysShowDecimals || hasFraction) "#,##0.00" else "#,##0"
        val rounded = amount.setScale(if (alwaysShowDecimals || hasFraction) 2 else 0, RoundingMode.HALF_UP)
        return DecimalFormat(pattern, usSymbols).format(rounded)
    }

    fun formatForeignAmount(
        amount: BigDecimal,
        currency: CurrencyCode,
        alwaysShowDecimals: Boolean = false
    ): String {
        val num = formatNumber(amount, alwaysShowDecimals)
        return when (currency) {
            CurrencyCode.MAD -> "$num DH"
            CurrencyCode.USD -> "$$num"
            CurrencyCode.EUR -> "€$num"
        }
    }

    fun formatPlainInput(amount: BigDecimal): String {
        val stripped = amount.stripTrailingZeros()
        return if (stripped.scale() <= 0) {
            stripped.toPlainString()
        } else {
            amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
        }
    }

    fun formatRateInput(rate: BigDecimal): String {
        val stripped = rate.stripTrailingZeros()
        return if (stripped.scale() <= 0) {
            stripped.toPlainString()
        } else {
            stripped.toPlainString()
        }
    }
}
