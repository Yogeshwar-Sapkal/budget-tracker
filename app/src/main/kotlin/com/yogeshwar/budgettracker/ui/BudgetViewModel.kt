package com.yogeshwar.budgettracker.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yogeshwar.budgettracker.BudgetApp
import com.yogeshwar.budgettracker.ledger.Category
import com.yogeshwar.budgettracker.ledger.Expense
import com.yogeshwar.budgettracker.ledger.ExpenseItem
import com.yogeshwar.budgettracker.ledger.Merchant
import com.yogeshwar.budgettracker.ledger.MerchantsOverview
import com.yogeshwar.budgettracker.ledger.ReviewItem
import com.yogeshwar.budgettracker.ledger.presentation.CsvExport
import com.yogeshwar.budgettracker.ledger.presentation.HomeModel
import com.yogeshwar.budgettracker.ledger.presentation.homeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/** Holds what the screens show and runs every ledger call off the main thread. */
class BudgetViewModel(application: Application) : AndroidViewModel(application) {
    private val budgetApp = application as BudgetApp
    private val ledger = budgetApp.ledger

    var month by mutableStateOf(YearMonth.now())
        private set
    var home by mutableStateOf<HomeModel?>(null)
        private set
    var items by mutableStateOf<List<ExpenseItem>>(emptyList())
        private set
    var merchants by mutableStateOf(MerchantsOverview(emptyList(), emptyList()))
        private set
    var categories by mutableStateOf<List<Category>>(emptyList())
        private set
    var reviewItems by mutableStateOf<List<ReviewItem>>(emptyList())
        private set
    var allMerchants by mutableStateOf<List<Merchant>>(emptyList())
        private set
    var lastAlertAt by mutableStateOf<Instant?>(null)
        private set

    private var lastDeleted: Expense? = null

    init {
        // Reload whenever the listener saves something, even while the app is open.
        viewModelScope.launch { budgetApp.changes.collect { refresh() } }
    }

    val canGoNext: Boolean get() = month < YearMonth.now()

    fun previousMonth() { month = month.minusMonths(1); refresh() }
    fun nextMonth() { if (canGoNext) { month = month.plusMonths(1); refresh() } }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) { load() }
    }

    private suspend fun load() {
        val m = month
        val zone = ZoneId.systemDefault()
        val reviews = ledger.reviewItems()
        val overview = ledger.merchantsOverview(m, zone)
        val newHome = homeModel(ledger.monthSummary(m, zone), reviews.size, overview.needCategory.size)
        val newItems = ledger.expenseItems(m, zone)
        val cats = ledger.categories()
        val merchantList = ledger.merchants().sortedBy { it.displayName.lowercase() }
        val lastAlert = ledger.lastAlertAt()
        withContext(Dispatchers.Main) {
            if (m != month) return@withContext // the owner moved to another month meanwhile
            home = newHome
            items = newItems
            merchants = overview
            categories = cats
            reviewItems = reviews
            allMerchants = merchantList
            lastAlertAt = lastAlert
        }
    }

    private fun act(block: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            block()
            load()
        }
    }

    // ---- Expenses ------------------------------------------------------

    fun saveExpense(
        existingId: Long?,
        amountCents: Long,
        isRefund: Boolean,
        merchantName: String,
        categoryId: Long?,
        occurredAt: Instant,
        fromReviewId: Long?,
    ) = act {
        if (existingId != null) {
            ledger.editExpense(existingId, if (isRefund) -amountCents else amountCents, merchantName, occurredAt)
            val merchantId = ledger.expense(existingId)?.merchantId
            if (merchantId != null && categoryId != null) ledger.assignCategory(merchantId, categoryId)
        } else {
            ledger.addManualExpense(amountCents, isRefund, merchantName, occurredAt, categoryId)
        }
        if (fromReviewId != null) ledger.dismissReview(fromReviewId)
    }

    fun deleteExpense(id: Long) = act {
        lastDeleted = ledger.expense(id)
        ledger.deleteExpense(id)
    }

    fun undoDelete() = act { lastDeleted?.let { ledger.restoreExpense(it) }; lastDeleted = null }

    fun keepBoth(id: Long) = act { ledger.keepBoth(id) }
    fun removeDuplicate(id: Long) = act { ledger.removeDuplicate(id) }

    // ---- Merchants -----------------------------------------------------

    fun saveMerchant(merchantId: Long, newName: String, categoryId: Long?, newCategoryName: String) = act {
        if (newName.isNotBlank()) ledger.renameMerchant(merchantId, newName)
        val target = if (newCategoryName.isNotBlank()) ledger.addCategory(newCategoryName).id else categoryId
        ledger.assignCategory(merchantId, target)
    }

    fun mergeMerchants(fromId: Long, intoId: Long) = act { ledger.mergeMerchants(fromId, intoId) }

    // ---- Export --------------------------------------------------------

    /** Writes every expense as CSV to [uri]. Calls [onDone] with the number of expenses, or null if it failed. */
    fun exportCsv(uri: Uri, onDone: (Int?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = try {
                val items = ledger.allExpenseItems()
                val csv = CsvExport.expensesCsv(items, ZoneId.systemDefault())
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
                    ?: throw java.io.IOException("could not open the file")
                items.size
            } catch (e: Exception) {
                null
            }
            withContext(Dispatchers.Main) { onDone(count) }
        }
    }

    // ---- Needs review --------------------------------------------------

    fun dismissReview(id: Long) = act { ledger.dismissReview(id) }
}
