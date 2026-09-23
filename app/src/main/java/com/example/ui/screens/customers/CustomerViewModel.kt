package com.example.ui.screens.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CustomerEntity
import com.example.data.repository.BillKaroRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CustomerViewModel(private val repository: BillKaroRepository) : ViewModel() {

    val allCustomers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCustomer(name: String, phone: String, gstin: String?, address: String?, email: String?) {
        viewModelScope.launch {
            repository.insertCustomer(
                CustomerEntity(
                    name = name,
                    phone = phone,
                    gstin = if (gstin.isNull_or_blank()) null else gstin,
                    address = if (address.isNull_or_blank()) null else address,
                    email = if (email.isNull_or_blank()) null else email
                )
            )
        }
    }
}

class CustomerViewModelFactory(private val repository: BillKaroRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CustomerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()
