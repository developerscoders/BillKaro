package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierBillDao {
    @Query("SELECT * FROM supplier_bills ORDER BY date DESC")
    fun getAllSupplierBills(): Flow<List<SupplierBillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplierBill(bill: SupplierBillEntity): Long

    @Delete
    suspend fun deleteSupplierBill(bill: SupplierBillEntity)
}
