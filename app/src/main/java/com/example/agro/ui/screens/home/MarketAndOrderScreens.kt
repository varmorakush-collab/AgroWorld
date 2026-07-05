package com.example.agro.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(onBack: () -> Unit) {
    val marketData = listOf(
        Triple("Wheat", "₹2,100 / Quintal", true),
        Triple("Rice (Basmati)", "₹4,500 / Quintal", false),
        Triple("Cotton", "₹7,200 / Quintal", true),
        Triple("Potato", "₹1,200 / Quintal", true),
        Triple("Onion", "₹1,800 / Quintal", false),
        Triple("Soybean", "₹5,400 / Quintal", false),
        Triple("Mustard", "₹6,100 / Quintal", true),
        Triple("Sugarcane", "₹350 / Quintal", true)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Market Prices", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Current Mandi Rates (Gujarat)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(marketData) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Eco, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(item.first, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(item.second, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (item.third) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = if (item.third) Color(0xFF4CAF50) else Color(0xFFE57373),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (item.third) "+1.2%" else "-0.5%",
                                    fontSize = 12.sp,
                                    color = if (item.third) Color(0xFF4CAF50) else Color(0xFFE57373),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    var orders by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val userId = auth.currentUser?.uid ?: return@LaunchedEffect
        db.collection("orders")
            .whereEqualTo("customerId", userId)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }
                orders = value?.documents?.map { it.data ?: emptyMap() }
                    ?.sortedByDescending { it["timestamp"] as? Long ?: 0L } ?: emptyList()
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Orders", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                items(5) { ShimmerItem() }
            }
        } else if (orders.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyStateView(
                    message = "You haven't placed any orders yet. Start your farming journey today!",
                    icon = Icons.Default.History
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(orders) { order ->
                    OrderCard(order)
                }
            }
        }
    }
}

@Composable
fun OrderCard(order: Map<String, Any>) {
    val status = order["status"].toString()
    val total = order["totalAmount"]
    val timestamp = order["timestamp"] as? Long ?: 0L
    val date = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))
    val rawItems = order["items"] as? List<*>
    val items = rawItems?.filterIsInstance<Map<String, Any>>() ?: emptyList()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Order #"+timestamp.toString().takeLast(6), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                StatusChip(status)
            }
            Text(text = date, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(16.dp))
            OrderStatusStepper(status)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            
            items.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    AsyncImage(
                        model = item["image"]?.toString(),
                        contentDescription = null,
                        modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = item["name"].toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "${item["quantity"]} x ₹${item["price"]}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Total Amount:", fontWeight = FontWeight.Medium)
                Text(text = "₹$total", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun OrderStatusStepper(currentStatus: String) {
    val stages = listOf("Pending", "Confirmed", "Completed")
    val currentIndex = stages.indexOf(currentStatus).coerceAtLeast(0)
    
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        stages.forEachIndexed { index, stage ->
            val isCompleted = index <= currentIndex
            val color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape,
                    color = color
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (index < currentIndex) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = Color.White)
                        } else {
                            Text("${index + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isCompleted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(stage, fontSize = 10.sp, fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal, color = color)
            }
            
            if (index < stages.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.weight(0.5f).padding(bottom = 14.dp),
                    color = if (index < currentIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    thickness = 2.dp
                )
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val color = when (status) {
        "Pending" -> Color(0xFFFFA000)
        "Confirmed" -> Color(0xFF1976D2)
        "Delivered", "Completed" -> Color(0xFF388E3C)
        else -> Color.Gray
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
