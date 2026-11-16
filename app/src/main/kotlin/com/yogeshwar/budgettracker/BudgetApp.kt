package com.yogeshwar.budgettracker

import android.app.Application
import com.yogeshwar.budgettracker.data.AppDatabase
import com.yogeshwar.budgettracker.data.RoomLedgerStore
import com.yogeshwar.budgettracker.ledger.Ledger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.Executors

class BudgetApp : Application() {
    /** All database work runs here, one task at a time, never on the main thread. */
    val io = Executors.newSingleThreadExecutor()

    private val changeCounter = MutableStateFlow(0)

    /** Ticks whenever the listener saves something, so open screens can reload. */
    val changes: StateFlow<Int> = changeCounter

    fun notifyChanged() = changeCounter.update { it + 1 }

    val ledger: Ledger by lazy { Ledger(RoomLedgerStore(AppDatabase.create(this))) }

    override fun onCreate() {
        super.onCreate()
        io.execute { ledger.seedIfNeeded() }
    }
}
