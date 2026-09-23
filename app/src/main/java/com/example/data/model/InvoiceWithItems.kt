package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.data.local.InvoiceEntity
import com.example.data.local.InvoiceItemEntity

data class InvoiceWithItems(
    @Embedded val invoice: InvoiceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val items: List<InvoiceItemEntity>
)
