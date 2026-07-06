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
import androidx.compose.ui.graphics.Brush
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MachineDetailScreen(
    machineId: String,
    onBack: () -> Unit,
    onRentClick: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    var machineData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var ownerData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
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
            TopAppBar(title = { Text("Machine Details", fontWeight = FontWeight.Bold) }, navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            })
        },
        bottomBar = {
            if (machineData != null) {
                Surface(tonalElevation = 8.dp, shadowElevation = 12.dp) {
                    Button(
                        onClick = { onRentClick(machineId) },
                        modifier = Modifier.padding(16.dp).fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Agriculture, null)
                        Spacer(Modifier.width(12.dp))
                        Text("Rent This Machine", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        else if (machineData != null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
                AsyncImage(model = machineData!!["image"], contentDescription = null, modifier = Modifier.fillMaxWidth().height(320.dp), contentScale = ContentScale.Crop)
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = machineData!!["name"].toString(), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            Text(text = machineData!!["type"].toString(), fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
                        }
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)) {
                            Text(text = "₹${machineData!!["price"]}/hr", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("About Machine", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = machineData!!["description"].toString(), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp), lineHeight = 24.sp)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text("Machine Provider", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(60.dp), shape = CircleShape, color = Color.White) {
                                AsyncImage(model = ownerData?.get("profileImage"), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(text = ownerData?.get("name")?.toString() ?: "Agro Partner", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, null, tint = Color(0xFF2196F3), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(text = "Verified Provider", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentMachineFormScreen(
    machineId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    
    var machineData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var selectedHours by remember { mutableFloatStateOf(4f) }
    var deliveryAddress by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateText by remember { mutableStateOf("") }

    LaunchedEffect(machineId) {
        db.collection("machines").document(machineId).get().addOnSuccessListener { doc ->
            machineData = doc.data
        }
        
        // Pre-fill user address if available
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                deliveryAddress = doc.getString("address") ?: ""
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rent Machine", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        if (machineData == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Machine Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = machineData!!["image"], contentDescription = null, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(machineData!!["name"].toString(), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Text("Rate: ₹${machineData!!["price"]}/hour", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 1. Hours Selection with Slider
                Text("How many hours do you need it?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "${selectedHours.toInt()} Hours", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        Slider(
                            value = selectedHours,
                            onValueChange = { selectedHours = it },
                            valueRange = 1f..48f,
                            steps = 47,
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("1 hr", fontSize = 12.sp)
                            Text("48 hrs", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Date Selection
                Text("Select Delivery Date", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = selectedDateText,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Choose Date") },
                    readOnly = true,
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.CalendarMonth, null) } },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Address Input
                Text("Delivery Address", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = deliveryAddress,
                    onValueChange = { deliveryAddress = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("Enter full address where machine is needed") },
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Price Summary
                val hourlyRate = machineData!!["price"].toString().toDoubleOrNull() ?: 0.0
                val totalAmount = hourlyRate * selectedHours.toInt()
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Hourly Rate")
                            Text("₹$hourlyRate")
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Total Duration")
                            Text("${selectedHours.toInt()} Hours")
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Total Payable", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("₹$totalAmount", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (selectedDateText.isEmpty()) {
                            Toast.makeText(context, "Please select a date", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (deliveryAddress.isBlank()) {
                            Toast.makeText(context, "Please provide an address", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        isSubmitting = true
                        val orderData = mapOf(
                            "customerId" to auth.currentUser?.uid,
                            "sellerId" to machineData!!["ownerId"],
                            "items" to listOf(mapOf(
                                "name" to machineData!!["name"],
                                "image" to machineData!!["image"],
                                "price" to machineData!!["price"],
                                "quantity" to 1,
                                "type" to "Machine",
                                "duration" to "${selectedHours.toInt()} Hours",
                                "date" to selectedDateText,
                                "address" to deliveryAddress
                            )),
                            "totalAmount" to totalAmount,
                            "status" to "Pending",
                            "timestamp" to System.currentTimeMillis()
                        )
                        
                        db.collection("orders").add(orderData).addOnSuccessListener {
                            isSubmitting = false
                            Toast.makeText(context, "Booking Request Sent Successfully!", Toast.LENGTH_LONG).show()
                            onSuccess()
                        }.addOnFailureListener {
                            isSubmitting = false
                            Toast.makeText(context, "Booking Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("Confirm Booking", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        selectedDateText = sdf.format(Date(it))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
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
