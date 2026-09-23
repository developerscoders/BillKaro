package com.example.data.local

import androidx.room.*
import com.example.data.model.InvoiceWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Transaction
    @Query("SELECT * FROM invoices ORDER BY date DESC")
    fun getAllInvoicesWithItems(): Flow<List<InvoiceWithItems>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceWithItemsById(id: Long): InvoiceWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItemEntity>)

    @Query("UPDATE invoices SET isPaid = :isPaid WHERE id = :id")
    suspend fun updatePaymentStatus(id: Long, isPaid: Boolean)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoiceById(id: Long)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteInvoiceItemsByInvoiceId(invoiceId: Long)

    @Query("SELECT COUNT(*) FROM invoices")
    fun getInvoiceCount(): Flow<Int>

    @Query("SELECT SUM(grandTotal) FROM invoices")
    fun getTotalSales(): Flow<Double?>

    @Query("SELECT SUM(grandTotal) FROM invoices WHERE isPaid = 0")
    fun getOutstandingTotal(): Flow<Double?>

    @Query("SELECT SUM(totalTax) FROM invoices")
    fun getTotalGstCollected(): Flow<Double?>
}
