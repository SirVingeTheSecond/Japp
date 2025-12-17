package com.japp.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.japp.api.responses.Currency
import java.text.NumberFormat

/**
 * Centralized formatting utilities for consistent number and currency display.
 */
object FormatHelper {

    /**
     * Default currency when none is specified.
     * ToDo: This should come from user preferences or group settings but I simply cannot be bothered at this point.
     */
    val defaultCurrency: Currency = Currency.DKK

    private val currencyFormatters = mutableMapOf<Currency, NumberFormat>()

    /**
     * Get a NumberFormat instance for the given currency.
     * Uses device locale for number formatting (to get the correct decimal separators)
     */
    fun getNumberFormat(fractionDigits: Int = 2): NumberFormat {
        return NumberFormat.getNumberInstance().apply {
            minimumFractionDigits = fractionDigits
            maximumFractionDigits = fractionDigits
        }
    }

    /**
     * Format a number with the specified decimal places.
     */
    fun formatNumber(value: Double, fractionDigits: Int = 2): String {
        return getNumberFormat(fractionDigits).format(value)
    }

    /**
     * Format a value with currency symbol.
     *
     * @param value The amount to format
     * @param currency The currency to use (defaults to DKK)
     * @param fractionDigits Number of decimal places (defaults to 2)
     */
    fun formatCurrency(
        value: Double,
        currency: Currency = defaultCurrency,
        fractionDigits: Int = 2
    ): String {
        val formatted = formatNumber(value, fractionDigits)
        return "$formatted ${currency.symbol}"
    }

    /**
     * Format a balance value, showing absolute value with currency.
     * Useful for "You owe X" or "You are owed X".
     */
    fun formatBalance(
        value: Double,
        currency: Currency = defaultCurrency
    ): String {
        return formatCurrency(kotlin.math.abs(value), currency)
    }
}

@Composable
fun rememberNumberFormat(fractionDigits: Int = 2): NumberFormat {
    return remember(fractionDigits) {
        FormatHelper.getNumberFormat(fractionDigits)
    }
}