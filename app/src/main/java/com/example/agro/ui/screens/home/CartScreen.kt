package com.example.agro.ui.screens.home

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.agro.viewmodel.CartItem
import com.example.agro.viewmodel.CartViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    cartViewModel: CartViewModel,
    onBack: () -> Unit,
    onCheckoutSuccess: () -> Unit
) {
    val cartItems by cartViewModel.cartItems.collectAsState()
    val context = LocalContext.current
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Cart", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                Surface(
                    tonalElevation = 8.dp, 
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Amount:", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "₹${cartViewModel.getTotal()}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showCheckoutDialog = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isProcessing
                        ) {
                            if (isProcessing) CircularProgressIndicator(color = Color.White)
                            else Text("Proceed to Checkout", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (cartItems.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Your cart is empty", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Start Farming")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(cartItems) { item ->
                    CartItemRow(item, cartViewModel)
                }
            }
        }
    }

    if (showCheckoutDialog) {
        CheckoutDialog(
            total = cartViewModel.getTotal(),
            onDismiss = { showCheckoutDialog = false },
            onConfirm = {
                isProcessing = true
                showCheckoutDialog = false
                saveOrdersToFirestore(cartItems, cartViewModel.getTotal()) { success ->
                    isProcessing = false
                    if (success) {
                        cartViewModel.clearCart()
                        Toast.makeText(context, "Order Placed Successfully!", Toast.LENGTH_LONG).show()
                        onCheckoutSuccess()
                    } else {
                        Toast.makeText(context, "Failed to place order. Try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

private fun saveOrdersToFirestore(items: List<CartItem>, total: Double, onComplete: (Boolean) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return onComplete(false)
    
    // Group items by sellerId to create separate orders
    val ordersBySeller = items.groupBy { it.sellerId }
    val timestamp = System.currentTimeMillis()
    
    var completedRequests = 0
    val totalRequests = ordersBySeller.size
    
    ordersBySeller.forEach { (sellerId, sellerItems) ->
        val orderTotal = sellerItems.sumOf { (it.price.toDoubleOrNull() ?: 0.0) * it.quantity }
        val orderData = mapOf(
            "customerId" to userId,
            "sellerId" to sellerId,
            "items" to sellerItems.map { mapOf(
                "id" to it.id,
                "name" to it.name,
                "price" to it.price,
                "image" to it.image,
                "quantity" to it.quantity,
                "type" to it.type
            )},
            "totalAmount" to orderTotal,
            "status" to "Pending",
            "timestamp" to timestamp
        )
        
        db.collection("orders").add(orderData).addOnCompleteListener {
            completedRequests++
            if (completedRequests == totalRequests) {
                onComplete(true)
            }
        }
    }
}

@Composable
fun CartItemRow(item: CartItem, viewModel: CartViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.image,
                contentDescription = null,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "₹${item.price}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(text = item.type, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.updateQuantity(item.id, -1) }) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Text(text = "${item.quantity}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                IconButton(onClick = { viewModel.updateQuantity(item.id, 1) }) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { viewModel.removeFromCart(item.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun CheckoutDialog(
    total: Double,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var paymentMethod by remember { mutableStateOf("Card") }
    var cardNumber by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Checkout - Total: ₹$total", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Select Payment Method:", fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentChip(selected = paymentMethod == "Card", text = "Card", onClick = { paymentMethod = "Card" })
                    PaymentChip(selected = paymentMethod == "UPI", text = "UPI", onClick = { paymentMethod = "UPI" })
                    PaymentChip(selected = paymentMethod == "COD", text = "COD", onClick = { paymentMethod = "COD" })
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                AnimatedContent(targetState = paymentMethod, label = "") { method ->
                    when (method) {
                        "Card" -> {
                            Column {
                                OutlinedTextField(
                                    value = cardNumber,
                                    onValueChange = { if (it.length <= 16 && it.all { char -> char.isDigit() }) cardNumber = it },
                                    label = { Text("Card Number (16 digits)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row {
                                    OutlinedTextField(
                                        value = "", onValueChange = {}, label = { Text("MM/YY") },
                                        modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedTextField(
                                        value = "", onValueChange = {}, label = { Text("CVV") },
                                        modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                            }
                        }
                        "UPI" -> {
                            OutlinedTextField(
                                value = upiId,
                                onValueChange = { upiId = it },
                                label = { Text("Enter UPI ID (e.g. user@okaxis)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "COD" -> {
                            Text("Pay ₹$total in cash on delivery.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = paymentMethod == "COD" || (paymentMethod == "Card" && cardNumber.length >= 12) || (paymentMethod == "UPI" && upiId.contains("@"))) {
                Text("Confirm Order")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PaymentChip(selected: Boolean, text: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        shape = RoundedCornerShape(12.dp)
    )
}
