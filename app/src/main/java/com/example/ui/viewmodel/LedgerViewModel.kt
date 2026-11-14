package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsPreferences
import com.example.data.model.AppSettings
import com.example.data.model.BreakdownBucket
import com.example.data.model.BreakdownGranularity
import com.example.data.model.CurrencyCode
import com.example.data.model.PeriodComparison
import com.example.data.model.PeriodSummary
import com.example.data.model.ReportPeriod
import com.example.data.model.TransactionFilter
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import com.example.data.repository.LedgerRepository
import com.example.domain.DateUtils
import com.example.domain.ProfitCalculator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class ChartBarPoint(
    val label: String,
    val income: BigDecimal,
    val ads: BigDecimal
)

data class LedgerUiState(
    val isLoading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val allTransactions: List<TransactionRecord> = emptyList(),
    val pendingTransactions: List<TransactionRecord> = emptyList(),
    val filteredTransactions: List<TransactionRecord> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: TransactionFilter = TransactionFilter.ALL,
    val allTimeSummary: PeriodSummary = PeriodSummary(),
    val todaySummary: PeriodSummary = PeriodSummary(),
    val thisWeekSummary: PeriodSummary = PeriodSummary(),
    val thisMonthSummary: PeriodSummary = PeriodSummary(),
    val weeklyComparison: PeriodComparison = PeriodComparison(
        current = PeriodSummary(),
        previous = PeriodSummary(),
        profitDelta = BigDecimal.ZERO,
        percentChange = null
    ),
    val monthlyComparison: PeriodComparison = PeriodComparison(
        current = PeriodSummary(),
        previous = PeriodSummary(),
        profitDelta = BigDecimal.ZERO,
        percentChange = null
    ),
    val reportPeriod: ReportPeriod = ReportPeriod.THIS_WEEK,
    val customStartDate: String = DateUtils.startOfMonth(DateUtils.todayDateString()),
    val customEndDate: String = DateUtils.todayDateString(),
    val reportSummary: PeriodSummary = PeriodSummary(),
    val breakdownGranularity: BreakdownGranularity = BreakdownGranularity.WEEK,
    val breakdownBuckets: List<BreakdownBucket> = emptyList(),
    val chartPoints: List<ChartBarPoint> = emptyList(),
    val isMutating: Boolean = false
)

private data class UiControlState(
    val searchQuery: String = "",
    val selectedFilter: TransactionFilter = TransactionFilter.ALL,
    val reportPeriod: ReportPeriod = ReportPeriod.THIS_WEEK,
    val customStartDate: String = DateUtils.startOfMonth(DateUtils.todayDateString()),
    val customEndDate: String = DateUtils.todayDateString(),
    val breakdownGranularity: BreakdownGranularity = BreakdownGranularity.WEEK,
    val isMutating: Boolean = false
)

class LedgerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LedgerRepository = LedgerRepository(
        dao = AppDatabase.getInstance(application).transactionDao(),
        settingsPreferences = SettingsPreferences(application)
    )

    private val controlsFlow = MutableStateFlow(UiControlState())

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    val uiState: StateFlow<LedgerUiState> = combine(
        repository.transactionsFlow,
        repository.settingsFlow,
        controlsFlow
    ) { transactions, settings, controls ->
        val today = DateUtils.todayDateString()
        val weekStart = DateUtils.startOfWeek(today, settings.weekStartsOnMonday)
        val weekEnd = DateUtils.endOfWeek(today, settings.weekStartsOnMonday)
        val prevWeek = DateUtils.previousWeekRange(today, settings.weekStartsOnMonday)

        val monthStart = DateUtils.startOfMonth(today)
        val monthEnd = DateUtils.endOfMonth(today)
        val prevMonth = DateUtils.previousMonthRange(today)

        val yearStart = DateUtils.startOfYear(today)
        val yearEnd = DateUtils.endOfYear(today)

        val allTimeSummary = ProfitCalculator.summarize(transactions)
        val todaySummary = ProfitCalculator.summarizeRange(transactions, today, today)
        val thisWeekSummary = ProfitCalculator.summarizeRange(transactions, weekStart, weekEnd)
        val thisMonthSummary = ProfitCalculator.summarizeRange(transactions, monthStart, monthEnd)
        val weeklyComparison = ProfitCalculator.comparePeriods(
            transactions,
            weekStart to weekEnd,
            prevWeek
        )
        val monthlyComparison = ProfitCalculator.comparePeriods(
            transactions,
            monthStart to monthEnd,
            prevMonth
        )

        val pendingTransactions = transactions.filter {
            it.type == TransactionType.INCOME && it.status == TransactionStatus.PENDING
        }

        val query = controls.searchQuery.trim().lowercase()
        val filteredTransactions = transactions.filter { tx ->
            val matchesFilter = when (controls.selectedFilter) {
                TransactionFilter.ALL -> true
                TransactionFilter.INCOME -> tx.type == TransactionType.INCOME
                TransactionFilter.ADS -> tx.type == TransactionType.AD_EXPENSE
                TransactionFilter.PENDING -> tx.status == TransactionStatus.PENDING
                TransactionFilter.RECEIVED -> tx.type == TransactionType.INCOME && tx.status == TransactionStatus.RECEIVED
            }
            val matchesQuery = if (query.isEmpty()) {
                true
            } else {
                tx.description.lowercase().contains(query) ||
                    tx.dateString.contains(query) ||
                    tx.currency.name.lowercase().contains(query) ||
                    tx.amountMad.toPlainString().contains(query)
            }
            matchesFilter && matchesQuery
        }

        val reportRange = when (controls.reportPeriod) {
            ReportPeriod.TODAY -> today to today
            ReportPeriod.THIS_WEEK -> weekStart to weekEnd
            ReportPeriod.THIS_MONTH -> monthStart to monthEnd
            ReportPeriod.THIS_YEAR -> yearStart to yearEnd
            ReportPeriod.ALL_TIME -> null
            ReportPeriod.CUSTOM -> minOf(controls.customStartDate, controls.customEndDate) to
                maxOf(controls.customStartDate, controls.customEndDate)
        }

        val reportSummary = if (reportRange == null) {
            allTimeSummary
        } else {
            ProfitCalculator.summarizeRange(transactions, reportRange.first, reportRange.second)
        }

        val scopedTransactionsForBreakdown = if (reportRange == null || controls.reportPeriod == ReportPeriod.TODAY) {
            transactions
        } else if (controls.reportPeriod == ReportPeriod.CUSTOM) {
            transactions.filter { it.dateString in reportRange.first..reportRange.second }
        } else {
            transactions
        }

        val breakdownBuckets = ProfitCalculator.buildBreakdown(
            transactions = scopedTransactionsForBreakdown,
            granularity = controls.breakdownGranularity,
            weekStartsOnMonday = settings.weekStartsOnMonday
        )

        val chartPoints = buildChartPoints(
            transactions = transactions,
            period = controls.reportPeriod,
            today = today,
            weekStart = weekStart,
            customRange = controls.customStartDate to controls.customEndDate
        )

        LedgerUiState(
            isLoading = false,
            settings = settings,
            allTransactions = transactions,
            pendingTransactions = pendingTransactions,
            filteredTransactions = filteredTransactions,
            searchQuery = controls.searchQuery,
            selectedFilter = controls.selectedFilter,
            allTimeSummary = allTimeSummary,
            todaySummary = todaySummary,
            thisWeekSummary = thisWeekSummary,
            thisMonthSummary = thisMonthSummary,
            weeklyComparison = weeklyComparison,
            monthlyComparison = monthlyComparison,
            reportPeriod = controls.reportPeriod,
            customStartDate = controls.customStartDate,
            customEndDate = controls.customEndDate,
            reportSummary = reportSummary,
            breakdownGranularity = controls.breakdownGranularity,
            breakdownBuckets = breakdownBuckets,
            chartPoints = chartPoints,
            isMutating = controls.isMutating
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = LedgerUiState()
    )

    private fun buildChartPoints(
        transactions: List<TransactionRecord>,
        period: ReportPeriod,
        today: String,
        weekStart: String,
        customRange: Pair<String, String>
    ): List<ChartBarPoint> {
        return when (period) {
            ReportPeriod.TODAY, ReportPeriod.THIS_WEEK -> {
                (0..6).map { offset ->
                    val day = DateUtils.addDays(weekStart, offset)
                    val summary = ProfitCalculator.summarizeRange(transactions, day, day)
                    ChartBarPoint(
                        label = DateUtils.formatShortDay(day).take(3),
                        income = summary.totalIncome,
                        ads = summary.totalAds
                    )
                }
            }
            ReportPeriod.THIS_MONTH -> {
                val monthPrefix = today.take(7)
                val monthTxs = transactions.filter { it.dateString.startsWith(monthPrefix) }
                val weeks = listOf(1..7, 8..14, 15..21, 22..31)
                weeks.mapIndexed { idx, days ->
                    val chunk = monthTxs.filter { tx ->
                        val dayNum = tx.dateString.takeLast(2).toIntOrNull() ?: 1
                        dayNum in days
                    }
                    val sum = ProfitCalculator.summarize(chunk)
                    ChartBarPoint(
                        label = "W${idx + 1}",
                        income = sum.totalIncome,
                        ads = sum.totalAds
                    )
                }
            }
            ReportPeriod.THIS_YEAR, ReportPeriod.ALL_TIME -> {
                val months = transactions.map { it.dateString.take(7) }
                    .distinct()
                    .sorted()
                    .takeLast(6)
                months.map { ym ->
                    val chunk = transactions.filter { it.dateString.startsWith(ym) }
                    val sum = ProfitCalculator.summarize(chunk)
                    ChartBarPoint(
                        label = DateUtils.formatShortMonth(ym).take(3),
                        income = sum.totalIncome,
                        ads = sum.totalAds
                    )
                }
            }
            ReportPeriod.CUSTOM -> {
                val start = minOf(customRange.first, customRange.second)
                val end = maxOf(customRange.first, customRange.second)
                transactions.filter { it.dateString in start..end }
                    .groupBy { it.dateString }
                    .toSortedMap()
                    .entries
                    .take(7)
                    .map { (date, list) ->
                        val sum = ProfitCalculator.summarize(list)
                        ChartBarPoint(
                            label = date.takeLast(5),
                            income = sum.totalIncome,
                            ads = sum.totalAds
                        )
                    }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        controlsFlow.value = controlsFlow.value.copy(searchQuery = query)
    }

    fun updateTransactionFilter(filter: TransactionFilter) {
        controlsFlow.value = controlsFlow.value.copy(selectedFilter = filter)
    }

    fun selectReportPeriod(period: ReportPeriod) {
        val suggestedGranularity = when (period) {
            ReportPeriod.TODAY -> BreakdownGranularity.DAY
            ReportPeriod.THIS_WEEK -> BreakdownGranularity.DAY
            ReportPeriod.THIS_MONTH -> BreakdownGranularity.WEEK
            ReportPeriod.THIS_YEAR -> BreakdownGranularity.MONTH
            ReportPeriod.ALL_TIME -> BreakdownGranularity.MONTH
            ReportPeriod.CUSTOM -> BreakdownGranularity.DAY
        }
        controlsFlow.value = controlsFlow.value.copy(
            reportPeriod = period,
            breakdownGranularity = suggestedGranularity
        )
    }

    fun updateCustomDateRange(startDate: String, endDate: String) {
        controlsFlow.value = controlsFlow.value.copy(
            reportPeriod = ReportPeriod.CUSTOM,
            customStartDate = minOf(startDate, endDate),
            customEndDate = maxOf(startDate, endDate)
        )
    }

    fun selectBreakdownGranularity(granularity: BreakdownGranularity) {
        controlsFlow.value = controlsFlow.value.copy(breakdownGranularity = granularity)
    }

    fun addIncome(
        amountMad: BigDecimal,
        description: String,
        dateString: String,
        status: TransactionStatus
    ) {
        if (controlsFlow.value.isMutating) return
        viewModelScope.launch {
            controlsFlow.value = controlsFlow.value.copy(isMutating = true)
            try {
                repository.addIncome(amountMad, description, dateString, status)
                _events.tryEmit(
                    if (status == TransactionStatus.PENDING) "Pending income added"
                    else "Received income added"
                )
            } finally {
                controlsFlow.value = controlsFlow.value.copy(isMutating = false)
            }
        }
    }

    fun addAdExpense(
        amountOriginal: BigDecimal,
        currency: CurrencyCode,
        exchangeRate: BigDecimal,
        description: String,
        dateString: String
    ) {
        if (controlsFlow.value.isMutating) return
        viewModelScope.launch {
            controlsFlow.value = controlsFlow.value.copy(isMutating = true)
            try {
                repository.addAdExpense(amountOriginal, currency, exchangeRate, description, dateString)
                _events.tryEmit("Advertising expense saved")
            } finally {
                controlsFlow.value = controlsFlow.value.copy(isMutating = false)
            }
        }
    }

    fun updateTransaction(record: TransactionRecord) {
        if (controlsFlow.value.isMutating) return
        viewModelScope.launch {
            controlsFlow.value = controlsFlow.value.copy(isMutating = true)
            try {
                repository.updateTransaction(record)
                _events.tryEmit("Transaction updated")
            } finally {
                controlsFlow.value = controlsFlow.value.copy(isMutating = false)
            }
        }
    }

    fun markAsReceived(id: Long) {
        viewModelScope.launch {
            repository.markIncomeAsReceived(id)
            _events.tryEmit("Marked as Received • Total Profit unchanged")
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            _events.tryEmit("Transaction deleted")
        }
    }

    fun updateExchangeRates(usdRate: BigDecimal, eurRate: BigDecimal) {
        viewModelScope.launch {
            repository.updateExchangeRates(usdRate, eurRate)
            _events.tryEmit("Default exchange rates updated")
        }
    }

    fun setAlwaysShowDecimals(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAlwaysShowDecimals(enabled)
        }
    }

    fun setWeekStartsOnMonday(enabled: Boolean) {
        viewModelScope.launch {
            repository.setWeekStartsOnMonday(enabled)
        }
    }

    suspend fun generateBackupJson(): String {
        return repository.exportBackupJson()
    }

    fun restoreFromBackupJson(json: String, replaceExisting: Boolean) {
        viewModelScope.launch {
            val result = repository.restoreBackupJson(json, replaceExisting)
            result.onSuccess { count ->
                val verb = if (replaceExisting) "Restored" else "Imported"
                _events.tryEmit("$verb $count transaction(s) successfully")
            }.onFailure { err ->
                _events.tryEmit("Restore failed: ${err.message ?: "Invalid backup file"}")
            }
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            repository.clearAllTransactions()
            _events.tryEmit("All transactions cleared")
        }
    }

    fun notifyMessage(message: String) {
        _events.tryEmit(message)
    }
}
