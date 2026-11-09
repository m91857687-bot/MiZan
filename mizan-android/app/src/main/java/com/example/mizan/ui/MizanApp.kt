package com.example.mizan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.mizan.data.CurrencyOption
import com.example.mizan.data.EntryStore
import com.example.mizan.data.EntryType
import com.example.mizan.data.LedgerEntry
import com.example.mizan.data.currencyOptions
import com.example.mizan.data.expenseCategories
import com.example.mizan.data.incomeCategories
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

private val Ink = Color(0xFF183C32)
private val Green = Color(0xFF236B52)
private val Mint = Color(0xFFBDE4CE)
private val Canvas = Color(0xFFF5F4EF)
private val Muted = Color(0xFF6D7771)
private val IncomeColor = Color(0xFF2A8A62)
private val ExpenseColor = Color(0xFFB45D4D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MizanApp() {
    val context = LocalContext.current
    val store = remember { EntryStore(context) }
    var entries by remember { mutableStateOf(store.loadEntries()) }
    var currencyCode by remember {
        mutableStateOf(
            store.loadCurrency().takeIf { code -> currencyOptions.any { it.code == code } } ?: "YER",
        )
    }
    var selectedMonthMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    var editorVisible by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<LedgerEntry?>(null) }
    var pendingDelete by remember { mutableStateOf<LedgerEntry?>(null) }
    var entryType by remember { mutableStateOf(EntryType.EXPENSE) }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(expenseCategories.first()) }
    var note by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }

    val currency = currencyOptions.firstOrNull { it.code == currencyCode } ?: currencyOptions.first()
    val monthCalendar = remember(selectedMonthMillis) {
        Calendar.getInstance().apply { timeInMillis = selectedMonthMillis }
    }
    val displayedYear = monthCalendar.get(Calendar.YEAR)
    val displayedMonth = monthCalendar.get(Calendar.MONTH)
    val monthEntries = remember(entries, displayedYear, displayedMonth) {
        entries.filter { entry ->
            val entryCalendar = Calendar.getInstance().apply { timeInMillis = entry.createdAt }
            entryCalendar.get(Calendar.YEAR) == displayedYear &&
                entryCalendar.get(Calendar.MONTH) == displayedMonth
        }.sortedByDescending { it.createdAt }
    }
    val totalIncome = monthEntries
        .filter { it.type == EntryType.INCOME }
        .fold(BigDecimal.ZERO) { total, entry -> total + (entry.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) }
    val totalExpenses = monthEntries
        .filter { it.type == EntryType.EXPENSE }
        .fold(BigDecimal.ZERO) { total, entry -> total + (entry.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) }
    val netTotal = totalIncome - totalExpenses

    fun openEditor(entry: LedgerEntry? = null) {
        editingEntry = entry
        entryType = entry?.type ?: EntryType.EXPENSE
        amountText = entry?.amount ?: ""
        category = entry?.category ?: expenseCategories.first()
        note = entry?.note ?: ""
        amountError = null
        editorVisible = true
    }

    fun saveEditor() {
        val parsedAmount = parseAmount(amountText)
        if (parsedAmount == null) {
            amountError = "أدخل مبلغًا صحيحًا أكبر من صفر"
            return
        }

        val existing = editingEntry
        val savedEntry = LedgerEntry(
            id = existing?.id ?: UUID.randomUUID().toString(),
            amount = parsedAmount.stripTrailingZeros().toPlainString(),
            type = entryType,
            category = category,
            note = note.trim(),
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
        )
        entries = if (existing == null) {
            listOf(savedEntry) + entries
        } else {
            entries.map { current -> if (current.id == existing.id) savedEntry else current }
        }.sortedByDescending { it.createdAt }
        store.saveEntries(entries)
        editorVisible = false
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Green,
            onPrimary = Color.White,
            secondary = Ink,
            background = Canvas,
            surface = Color.White,
            onSurface = Ink,
            onBackground = Ink,
        ),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Scaffold(
                containerColor = Canvas,
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ميزان", fontWeight = FontWeight.Bold, color = Ink)
                                Text("مصروفاتك بوضوح", color = Muted, fontSize = 11.sp)
                            }
                        },
                        actions = {
                            Box {
                                TextButton(onClick = { currencyMenuExpanded = true }) {
                                    Text(currency.symbol, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = currencyMenuExpanded,
                                    onDismissRequest = { currencyMenuExpanded = false },
                                ) {
                                    currencyOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text("${option.label} (${option.symbol})") },
                                            onClick = {
                                                currencyCode = option.code
                                                store.saveCurrency(option.code)
                                                currencyMenuExpanded = false
                                            },
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Canvas,
                        ),
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { openEditor() },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("إضافة حركة", fontWeight = FontWeight.SemiBold) },
                        containerColor = Green,
                        contentColor = Color.White,
                    )
                },
            ) { contentPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        MonthSelector(
                            monthMillis = selectedMonthMillis,
                            onPrevious = {
                                selectedMonthMillis = shiftMonth(selectedMonthMillis, -1)
                            },
                            onNext = {
                                selectedMonthMillis = shiftMonth(selectedMonthMillis, 1)
                            },
                        )
                    }
                    item {
                        SummaryCard(
                            net = netTotal,
                            income = totalIncome,
                            expenses = totalExpenses,
                            currency = currency,
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "سجل الحركات",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Ink,
                            )
                            Text(
                                "${monthEntries.size} حركة",
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted,
                            )
                        }
                    }

                    if (monthEntries.isEmpty()) {
                        item {
                            EmptyMonthState(onAdd = { openEditor() })
                        }
                    } else {
                        items(monthEntries, key = { it.id }) { entry ->
                            EntryCard(
                                entry = entry,
                                currency = currency,
                                onClick = { openEditor(entry) },
                                onDelete = { pendingDelete = entry },
                            )
                        }
                    }
                }
            }

            if (editorVisible) {
                EntryEditorDialog(
                    isEditing = editingEntry != null,
                    type = entryType,
                    onTypeChange = { nextType ->
                        entryType = nextType
                        val allowedCategories = categoriesFor(nextType)
                        if (category !in allowedCategories) category = allowedCategories.first()
                    },
                    amount = amountText,
                    onAmountChange = {
                        amountText = it
                        amountError = null
                    },
                    amountError = amountError,
                    category = category,
                    onCategoryChange = { category = it },
                    note = note,
                    onNoteChange = { note = it },
                    onDismiss = { editorVisible = false },
                    onSave = { saveEditor() },
                )
            }

            pendingDelete?.let { entry ->
                AlertDialog(
                    onDismissRequest = { pendingDelete = null },
                    title = { Text("حذف الحركة؟", textAlign = TextAlign.Right) },
                    text = {
                        Text(
                            "لن يمكن استعادة هذه الحركة بعد حذفها.",
                            textAlign = TextAlign.Right,
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                entries = entries.filterNot { it.id == entry.id }
                                store.saveEntries(entries)
                                pendingDelete = null
                            },
                        ) {
                            Text("حذف", color = ExpenseColor)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingDelete = null }) {
                            Text("إلغاء")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun MonthSelector(
    monthMillis: Long,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "الشهر السابق", tint = Ink)
        }
        Text(
            text = SimpleDateFormat("MMMM yyyy", Locale("ar")).format(Date(monthMillis)),
            style = MaterialTheme.typography.titleMedium,
            color = Ink,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "الشهر التالي", tint = Ink)
        }
    }
}

@Composable
private fun SummaryCard(
    net: BigDecimal,
    income: BigDecimal,
    expenses: BigDecimal,
    currency: CurrencyOption,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Ink),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
        ) {
            Text("صافي هذا الشهر", color = Mint, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                formatMoney(net, currency),
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryAmount(
                    modifier = Modifier.weight(1f),
                    title = "الدخل",
                    amount = income,
                    currency = currency,
                    icon = {
                        Icon(
                            Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = Mint,
                        )
                    },
                    tint = Mint,
                )
                SummaryAmount(
                    modifier = Modifier.weight(1f),
                    title = "المصروف",
                    amount = expenses,
                    currency = currency,
                    icon = {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = Color(0xFFFFC3AD),
                        )
                    },
                    tint = Color(0xFFFFC3AD),
                )
            }
        }
    }
}

@Composable
private fun SummaryAmount(
    modifier: Modifier,
    title: String,
    amount: BigDecimal,
    currency: CurrencyOption,
    icon: @Composable () -> Unit,
    tint: Color,
) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            icon()
            Text(title, color = tint, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            formatMoney(amount, currency),
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun EntryCard(
    entry: LedgerEntry,
    currency: CurrencyOption,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val isIncome = entry.type == EntryType.INCOME
    val tint = if (isIncome) IncomeColor else ExpenseColor
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(tint.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = tint,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.category, fontWeight = FontWeight.SemiBold, color = Ink)
                val detail = listOfNotNull(
                    formatDate(entry.createdAt),
                    entry.note.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                Text(detail, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (isIncome) "+" else "−"}${formatMoney(entry.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO, currency)}",
                    color = tint,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(entry.type.label, style = MaterialTheme.typography.labelSmall, color = Muted)
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "حذف الحركة",
                    tint = Muted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyMonthState(onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(Mint.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Green)
            }
            Spacer(Modifier.height(12.dp))
            Text("لا توجد حركات لهذا الشهر", fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.height(4.dp))
            Text(
                "سجّل أول دخل أو مصروف لتبدأ المتابعة.",
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onAdd) {
                Text("إضافة أول حركة")
            }
        }
    }
}

@Composable
private fun EntryEditorDialog(
    isEditing: Boolean,
    type: EntryType,
    onTypeChange: (EntryType) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    amountError: String?,
    category: String,
    onCategoryChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .heightIn(max = 720.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    if (isEditing) "تعديل الحركة" else "حركة جديدة",
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = type == EntryType.EXPENSE,
                        onClick = { onTypeChange(EntryType.EXPENSE) },
                        label = { Text("مصروف") },
                    )
                    FilterChip(
                        selected = type == EntryType.INCOME,
                        onClick = { onTypeChange(EntryType.INCOME) },
                        label = { Text("دخل") },
                    )
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("المبلغ") },
                    singleLine = true,
                    isError = amountError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = {
                        amountError?.let { Text(it) }
                    },
                )
                Text("التصنيف", color = Ink, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    categoriesFor(type).forEach { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = { onCategoryChange(option) },
                            label = { Text(option) },
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ملاحظة (اختياري)") },
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء")
                    }
                    androidx.compose.material3.Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("حفظ الحركة")
                    }
                }
            }
        }
    }
}

private fun categoriesFor(type: EntryType): List<String> =
    if (type == EntryType.INCOME) incomeCategories else expenseCategories

private fun shiftMonth(monthMillis: Long, amount: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = monthMillis
        add(Calendar.MONTH, amount)
        set(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis

private fun parseAmount(input: String): BigDecimal? {
    val normalized = buildString(input.length) {
        input.trim().forEach { character ->
            when (character) {
                in '٠'..'٩' -> append(('0'.code + character.code - '٠'.code).toChar())
                in '۰'..'۹' -> append(('0'.code + character.code - '۰'.code).toChar())
                '٫', '،', ',' -> append('.')
                '٬', ' ' -> Unit
                else -> append(character)
            }
        }
    }
    return normalized.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }
}

private fun formatMoney(amount: BigDecimal, currency: CurrencyOption): String {
    val formatter = NumberFormat.getNumberInstance(Locale("ar")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }
    return "${formatter.format(amount)} ${currency.symbol}"
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("d MMMM", Locale("ar")).format(Date(timestamp))
