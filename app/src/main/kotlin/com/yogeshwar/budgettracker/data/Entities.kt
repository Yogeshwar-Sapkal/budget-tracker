package com.yogeshwar.budgettracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories", indices = [Index(value = ["name"], unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Entity(tableName = "merchants")
data class MerchantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val categoryId: Long?,
)

@Entity(tableName = "merchant_aliases")
data class AliasEntity(
    @PrimaryKey val normalizedKey: String,
    val merchantId: Long,
)

@Entity(
    tableName = "expenses",
    indices = [Index("occurredAtMs"), Index("notificationKey")],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchantId: Long,
    val amountCents: Long,
    val occurredAtMs: Long,
    val source: String?,
    val accountLabel: String?,
    val likelyDuplicateOfId: Long?,
    val notificationKey: String?,
)

@Entity(tableName = "review_items", indices = [Index("notificationKey")])
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String,
    val title: String?,
    val body: String,
    val receivedAtMs: Long,
    val reason: String,
    val notificationKey: String?,
)

@Entity(tableName = "ignored_entries", indices = [Index("notificationKey")])
data class IgnoredEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String,
    val description: String,
    val amountCents: Long,
    val receivedAtMs: Long,
    val reason: String,
    val notificationKey: String?,
)
