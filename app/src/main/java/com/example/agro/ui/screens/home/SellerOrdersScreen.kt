package com.example.agro.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerOrdersScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    var orders by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val userId = auth.currentUser?.uid ?: return@LaunchedEffect
        db.collection("orders")
            .whereEqualTo("sellerId", userId)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }
                orders = value?.documents?.map { it.data?.plus("id" to it.id) ?: emptyMap() }
                    ?.sortedByDescending { it["timestamp"] as? Long ?: 0L } ?: emptyList()
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Orders", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (orders.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No orders received yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(orders) { order ->
                    SellerOrderCard(order) { newStatus ->
                        db.collection("orders").document(order["id"].toString())
                            .update("status", newStatus)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Status updated to $newStatus", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }
        }
    }
}

@Composable
fun SellerOrderCard(order: Map<String, Any>, onStatusChange: (String) -> Unit) {
    val status = order["status"].toString()
    val total = order["totalAmount"]
    val timestamp = order["timestamp"] as? Long ?: 0L
    val date = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(timestamp))
    val rawItems = order["items"] as? List<*>
    val items = rawItems?.filterIsInstance<Map<String, Any>>() ?: emptyList()
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text(text = "Order #"+timestamp.toString().takeLast(6), fontWeight = FontWeight.Bold)
                    Text(text = date, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(status)
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, null)
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text("Confirm Order") }, onClick = { onStatusChange("Confirmed"); showMenu = false })
                            DropdownMenuItem(text = { Text("Mark as Delivered") }, onClick = { onStatusChange("Delivered"); showMenu = false })
                            DropdownMenuItem(text = { Text("Complete Order") }, onClick = { onStatusChange("Completed"); showMenu = false })
                            DropdownMenuItem(text = { Text("Cancel Order") }, onClick = { onStatusChange("Cancelled"); showMenu = false })
                        }
                    }
                }
            }
            
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
            Text(text = "Total Earned: ₹$total", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        }
    }
}
