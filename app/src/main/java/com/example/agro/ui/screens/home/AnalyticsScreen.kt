package com.example.agro.ui.screens.home

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.agro.ui.components.AnimatedPieChart
import com.example.agro.ui.components.AttractiveBarChart
import com.example.agro.ui.components.StatData
import com.example.agro.ui.components.StatGrid
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    
    var totalOrders by remember { mutableIntStateOf(0) }
    var totalEarnings by remember { mutableDoubleStateOf(0.0) }
    var dailyData by remember { mutableStateOf(List(7) { 0.0 }) }
    var machineOrders by remember { mutableIntStateOf(0) }
    var productOrders by remember { mutableIntStateOf(0) }
    var statusCounts by remember { mutableStateOf(mapOf("Pending" to 0, "Confirmed" to 0, "Completed" to 0)) }
    
    var isLoading by remember { mutableStateOf(true) }
    var rawOrders by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    LaunchedEffect(Unit) {
        val userId = auth.currentUser?.uid ?: return@LaunchedEffect
        db.collection("orders")
            .whereEqualTo("sellerId", userId)
            .get()
            .addOnSuccessListener { result ->
                val orders = result.documents.map { it.data ?: emptyMap() }
                rawOrders = orders
                totalOrders = orders.size
                totalEarnings = orders.sumOf { it["totalAmount"]?.toString()?.toDoubleOrNull() ?: 0.0 }
                
                val calendar = Calendar.getInstance()
                val today = calendar.timeInMillis
                val stats = DoubleArray(7) { 0.0 }
                var mCount = 0
                var pCount = 0
                val sMap = mutableMapOf("Pending" to 0, "Confirmed" to 0, "Completed" to 0)

                orders.forEach { order ->
                    val timestamp = order["timestamp"] as? Long ?: 0L
                    val diff = today - timestamp
                    val daysAgo = TimeUnit.MILLISECONDS.toDays(diff).toInt()
                    if (daysAgo in 0..6) {
                        stats[6 - daysAgo] += order["totalAmount"]?.toString()?.toDoubleOrNull() ?: 0.0
                    }

                    val items = order["items"] as? List<Map<String, Any>> ?: emptyList()
                    items.forEach { item ->
                        if (item["type"] == "Machine") mCount++ else pCount++
                    }

                    val status = order["status"].toString()
                    sMap[status] = sMap.getOrDefault(status, 0) + 1
                }
                
                dailyData = stats.toList()
                machineOrders = mCount
                productOrders = pCount
                statusCounts = sMap
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business Insights", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Summary Cards
                StatGrid(
                    listOf(
                        StatData("Total Sales", totalOrders.toString(), Icons.Default.ShoppingCart, MaterialTheme.colorScheme.primary),
                        StatData("Revenue", "₹${totalEarnings.toInt()}", Icons.Default.Payments, Color(0xFF4CAF50))
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 1. Animated Revenue Bar Chart
                Text("Weekly Revenue", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))
                AttractiveBarChart(
                    data = dailyData,
                    labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Animated Order Distribution (Pie)
                Text("Order Distribution", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))
                AnimatedPieChart(
                    values = listOf(machineOrders.toFloat(), productOrders.toFloat()),
                    colors = listOf(Color(0xFF1B5E20), Color(0xFFFFB300)),
                    labels = listOf("Machines", "Products")
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Status Overview
                ChartContainer(title = "Order Status", icon = Icons.Default.TrackChanges) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AnimatedStatusProgressBar("Pending", statusCounts["Pending"] ?: 0, totalOrders, Color(0xFFFFA000))
                        AnimatedStatusProgressBar("Confirmed", statusCounts["Confirmed"] ?: 0, totalOrders, Color(0xFF1976D2))
                        AnimatedStatusProgressBar("Completed", statusCounts["Completed"] ?: 0, totalOrders, Color(0xFF388E3C))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { generateAndShareReport(context, rawOrders) },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(4.dp)
                ) {
                    Icon(Icons.Default.Description, null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Export Detailed Report (.CSV)", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun AnimatedStatusProgressBar(label: String, count: Int, total: Int, color: Color) {
    val targetProgress = if (total > 0) count.toFloat() / total.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
        label = "statusProgress"
    )
    
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("$count", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = color,
            trackColor = color.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun ChartContainer(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(20.dp))
            content()
        }
    }
}

private fun generateAndShareReport(context: Context, orders: List<Map<String, Any>>) {
    try {
        val fileName = "Agro_Sales_Report.csv"
        val file = File(context.cacheDir, fileName)
        file.bufferedWriter().use { writer ->
            writer.write("Order ID,Date,Status,Total Amount,Items\n")
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            orders.forEach { order ->
                val id = order["timestamp"].toString().takeLast(6)
                val date = sdf.format(Date(order["timestamp"] as? Long ?: 0L))
                val status = order["status"]
                val amount = order["totalAmount"]
                val rawItems = order["items"] as? List<*>
                val items = rawItems?.filterIsInstance<Map<String, Any>>()?.joinToString(" | ") { it["name"].toString() } ?: ""
                writer.write("$id,$date,$status,$amount,\"$items\"\n")
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Sales Report"))
    } catch (e: Exception) {
        Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
    }
}
