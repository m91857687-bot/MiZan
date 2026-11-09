package com.example.mizan.data

enum class EntryType(val label: String) {
    EXPENSE("مصروف"),
    INCOME("دخل"),
}

data class LedgerEntry(
    val id: String,
    val amount: String,
    val type: EntryType,
    val category: String,
    val note: String,
    val createdAt: Long,
)

data class CurrencyOption(
    val code: String,
    val symbol: String,
    val label: String,
)

val currencyOptions = listOf(
    CurrencyOption("YER", "ر.ي", "ريال يمني"),
    CurrencyOption("SAR", "ر.س", "ريال سعودي"),
    CurrencyOption("USD", "$", "دولار أمريكي"),
    CurrencyOption("AED", "د.إ", "درهم إماراتي"),
)

val expenseCategories = listOf("طعام", "مواصلات", "فواتير", "تسوق", "صحة", "أخرى")
val incomeCategories = listOf("راتب", "عمل إضافي", "هدية", "أخرى")
