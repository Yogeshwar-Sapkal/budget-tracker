package com.yogeshwar.budgettracker

import android.app.Application
import com.yogeshwar.budgettracker.data.AppDatabase
import com.yogeshwar.budgettracker.data.RoomLedgerStore
import com.yogeshwar.budgettracker.ledger.Ledger
import java.util.concurrent.Executors

class BudgetApp : Application() {
    /** All database work runs here, one task at a time, never on the main thread. */
    val io = Executors.newSingleThreadExecutor()

    val ledger: Ledger by lazy { Ledger(RoomLedgerStore(AppDatabase.create(this))) }

    override fun onCreate() {
        super.onCreate()
        io.execute { ledger.seedIfNeeded() }
    }
}
