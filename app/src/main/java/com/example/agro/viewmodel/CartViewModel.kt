package com.example.agro.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CartItem(
    val id: String,
    val name: String,
    val price: String,
    val image: String,
    val type: String, // "Machine" or "Product"
    val sellerId: String,
    val quantity: Int = 1
)

class CartViewModel : ViewModel() {
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    fun addToCart(item: CartItem) {
        _cartItems.update { currentList ->
            val existingItem = currentList.find { it.id == item.id }
            if (existingItem != null) {
                currentList.map {
                    if (it.id == item.id) it.copy(quantity = it.quantity + 1) else it
                }
            } else {
                currentList + item
            }
        }
    }

    fun removeFromCart(itemId: String) {
        _cartItems.update { currentList ->
            currentList.filterNot { it.id == itemId }
        }
    }

    fun updateQuantity(itemId: String, delta: Int) {
        _cartItems.update { currentList ->
            currentList.map {
                if (it.id == itemId) {
                    val newQty = (it.quantity + delta).coerceAtLeast(1)
                    it.copy(quantity = newQty)
                } else it
            }
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun getTotal(): Double {
        return _cartItems.value.sumOf { 
            val price = it.price.toDoubleOrNull() ?: 0.0
            price * it.quantity 
        }
    }
}
