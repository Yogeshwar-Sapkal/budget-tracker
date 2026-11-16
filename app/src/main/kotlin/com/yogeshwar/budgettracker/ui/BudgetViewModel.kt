package com.yogeshwar.budgettracker.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yogeshwar.budgettracker.BudgetApp
import com.yogeshwar.budgettracker.ledger.Category
import com.yogeshwar.budgettracker.ledger.Expense
import com.yogeshwar.budgettracker.ledger.ExpenseItem
import com.yogeshwar.budgettracker.ledger.MerchantsOverview
import com.yogeshwar.budgettracker.ledger.ReviewItem
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
    private val ledger = (application as BudgetApp).ledger

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

    private var lastDeleted: Expense? = null

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
        withContext(Dispatchers.Main) {
            if (m != month) return@withContext // the owner moved to another month meanwhile
            home = newHome
            items = newItems
            merchants = overview
            categories = cats
            reviewItems = reviews
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

    // ---- Needs review --------------------------------------------------

    fun dismissReview(id: Long) = act { ledger.dismissReview(id) }
}
