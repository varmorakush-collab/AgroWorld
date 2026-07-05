package com.example.agro.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
fun MachineDetailScreen(
    machineId: String,
    onBack: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    var machineData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var ownerData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showRentDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(machineId) {
        db.collection("machines").document(machineId).get().addOnSuccessListener { doc ->
            machineData = doc.data
            val ownerId = machineData?.get("ownerId")?.toString()
            if (ownerId != null) {
                db.collection("users").document(ownerId).get().addOnSuccessListener { ownerDoc ->
                    ownerData = ownerDoc.data
                    isLoading = false
                }
            } else { isLoading = false }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Machine Details") }, navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            })
        },
        bottomBar = {
            if (machineData != null) {
                Surface(tonalElevation = 8.dp) {
                    Button(
                        onClick = { showRentDialog = true },
                        modifier = Modifier.padding(16.dp).fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Rent Now", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        else if (machineData != null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
                AsyncImage(model = machineData!!["image"], contentDescription = null, modifier = Modifier.fillMaxWidth().height(300.dp), contentScale = ContentScale.Crop)
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = machineData!!["name"].toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(text = "Category: ${machineData!!["type"]}", fontSize = 18.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "₹${machineData!!["price"]}/hour", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Description", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = machineData!!["description"].toString(), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Owner Information", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = ownerData?.get("profileImage"), contentDescription = null, modifier = Modifier.size(60.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(text = ownerData?.get("name")?.toString() ?: "Unknown", fontWeight = FontWeight.Bold)
                                Text(text = "Contact: ${machineData!!["contact"]}", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRentDialog && machineData != null) {
        RentMachineDialog(
            machineName = machineData!!["name"].toString(),
            pricePerHour = machineData!!["price"].toString(),
            onDismiss = { showRentDialog = false },
            onConfirm = { hours, date ->
                val total = (hours.toDoubleOrNull() ?: 0.0) * (machineData!!["price"].toString().toDoubleOrNull() ?: 0.0)
                val orderData = mapOf(
                    "customerId" to auth.currentUser?.uid,
                    "sellerId" to machineData!!["ownerId"],
                    "items" to listOf(mapOf("name" to machineData!!["name"], "image" to machineData!!["image"], "quantity" to 1, "type" to "Machine", "duration" to "$hours Hours", "date" to date)),
                    "totalAmount" to total,
                    "status" to "Pending",
                    "timestamp" to System.currentTimeMillis()
                )
                db.collection("orders").add(orderData).addOnSuccessListener {
                    showRentDialog = false
                    Toast.makeText(context, "Rent request sent successfully!", Toast.LENGTH_LONG).show()
                    onBack()
                }
            }
        )
    }
}

@Composable
fun RentMachineDialog(machineName: String, pricePerHour: String, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var hours by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rent $machineName", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Price: ₹$pricePerHour / hour", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = hours, onValueChange = { if (it.all { c -> c.isDigit() }) hours = it }, label = { Text("How many hours?") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Start Date (DD/MM/YYYY)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { if (hours.isNotEmpty() && date.isNotEmpty()) onConfirm(hours, date) }) { Text("Send Request") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    cartViewModel: CartViewModel,
    onBack: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var productData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var shopData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(productId) {
        db.collection("products").document(productId).get().addOnSuccessListener { doc ->
            productData = doc.data
            val shopId = productData?.get("shopkeeperId")?.toString()
            if (shopId != null) {
                db.collection("users").document(shopId).get().addOnSuccessListener { shopDoc ->
                    shopData = shopDoc.data
                    isLoading = false
                }
            } else { isLoading = false }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Product Details") }, navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            })
        },
        bottomBar = {
            if (productData != null) {
                Surface(tonalElevation = 8.dp) {
                    val stock = productData!!["stock"]?.toString()?.toIntOrNull() ?: 0
                    Button(
                        onClick = {
                            if (stock > 0) {
                                cartViewModel.addToCart(CartItem(id = productId, name = productData!!["name"].toString(), price = productData!!["price"].toString(), image = productData!!["image"].toString(), type = "Product", sellerId = productData!!["shopkeeperId"].toString()))
                                Toast.makeText(context, "Added to Cart", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Out of Stock", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.padding(16.dp).fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = stock > 0
                    ) {
                        Text(if (stock > 0) "Add to Cart" else "Out of Stock", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        else if (productData != null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
                AsyncImage(model = productData!!["image"], contentDescription = null, modifier = Modifier.fillMaxWidth().height(300.dp), contentScale = ContentScale.Crop)
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = productData!!["name"].toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(text = productData!!["category"].toString(), fontSize = 18.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(text = "₹${productData!!["price"]}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "In Stock: ${productData!!["stock"]}", fontSize = 14.sp, color = if ((productData!!["stock"]?.toString()?.toIntOrNull() ?: 0) > 5) Color.Gray else Color.Red)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Description", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = productData!!["description"].toString(), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Shop Information", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = shopData?.get("shopName")?.toString() ?: "Unknown", fontWeight = FontWeight.Bold)
                            Text(text = shopData?.get("address")?.toString() ?: "", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
