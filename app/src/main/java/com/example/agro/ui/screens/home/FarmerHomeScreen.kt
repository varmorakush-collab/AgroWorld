package com.example.agro.ui.screens.home

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.agro.api.NetworkClient
import com.example.agro.api.WeatherResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmerHomeScreen(
    onLogout: () -> Unit,
    onProfileClick: () -> Unit,
    onCartClick: () -> Unit,
    onSeeAllMachines: () -> Unit,
    onSeeAllProducts: () -> Unit,
    onMachineClick: (String) -> Unit,
    onProductClick: (String) -> Unit,
    onMarketClick: () -> Unit,
    onOrdersClick: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var machines by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var products by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("Default") }
    var showFilterDialog by remember { mutableStateOf(false) }
    
    var weatherData by remember { mutableStateOf<WeatherResponse?>(null) }
    var weatherError by remember { mutableStateOf<String?>(null) }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val currentUser = auth.currentUser
    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }

    LaunchedEffect(Unit) {
        db.collection("machines").addSnapshotListener { value, _ ->
            machines = value?.documents?.map { it.data?.plus("id" to it.id) ?: emptyMap() } ?: emptyList()
        }
        db.collection("products").addSnapshotListener { value, _ ->
            products = value?.documents?.map { it.data?.plus("id" to it.id) ?: emptyMap() } ?: emptyList()
        }
        
        currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                userData = doc.data
                val city = doc.getString("address")?.split(",")?.last()?.trim()?.takeIf { it.isNotBlank() } ?: "Ahmedabad"
                
                scope.launch {
                    try {
                        weatherData = NetworkClient.weatherService.getWeather(city, "3f81e4e7f8c6476dc97b7827c833ebfb")
                    } catch (e: Exception) { 
                        try {
                            weatherData = NetworkClient.weatherService.getWeather("Ahmedabad", "3f81e4e7f8c6476dc97b7827c833ebfb")
                        } catch (e2: Exception) { weatherError = "Offline" }
                    }
                }
            }
        }
    }

    val filteredMachines = machines.filter { 
        it["name"].toString().contains(searchQuery, ignoreCase = true) ||
        it["type"].toString().contains(searchQuery, ignoreCase = true)
    }.let { list ->
        when (sortBy) {
            "PriceLowToHigh" -> list.sortedBy { it["price"].toString().toDoubleOrNull() ?: 0.0 }
            "PriceHighToLow" -> list.sortedByDescending { it["price"].toString().toDoubleOrNull() ?: 0.0 }
            else -> list
        }
    }
    
    val filteredProducts = products.filter {
        it["name"].toString().contains(searchQuery, ignoreCase = true) ||
        it["category"].toString().contains(searchQuery, ignoreCase = true)
    }.let { list ->
        when (sortBy) {
            "PriceLowToHigh" -> list.sortedBy { it["price"].toString().toDoubleOrNull() ?: 0.0 }
            "PriceHighToLow" -> list.sortedByDescending { it["price"].toString().toDoubleOrNull() ?: 0.0 }
            else -> list
        }
    }

    val showResultsOnly = searchQuery.isNotEmpty() || sortBy != "Default"

    val drawerNavigationItems = listOf(
        DrawerNavItem("Rent Machines", Icons.Default.Agriculture, onSeeAllMachines),
        DrawerNavItem("Buy Products", Icons.Default.Storefront, onSeeAllProducts),
        DrawerNavItem("My Orders", Icons.Default.ReceiptLong, onOrdersClick),
        DrawerNavItem("Market Prices", Icons.AutoMirrored.Filled.TrendingUp, onMarketClick),
        DrawerNavItem("My Cart", Icons.Default.ShoppingCart, onCartClick)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                userName = userData?.get("name")?.toString() ?: "Farmer",
                userRole = "Farmer",
                profileImageUrl = userData?.get("profileImage")?.toString() ?: "",
                onLogout = onLogout,
                onProfileClick = onProfileClick,
                onCloseDrawer = { scope.launch { drawerState.close() } },
                navigationItems = drawerNavigationItems
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Agro", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = onCartClick) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary,
                        actionIconContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                item {
                    SearchAndFilterBar(
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        onFilterClick = { showFilterDialog = true }
                    )
                }

                if (!showResultsOnly) {
                    item { WeatherWidget(weatherData, weatherError) }
                    
                    item {
                        RecentActivitySection(onOrdersClick)
                    }

                    item { BannerSlider() }
                    
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                HomeCard("My Farm", Icons.Default.Landscape, Color(0xFF4CAF50), Modifier.weight(1f))
                                HomeCard("My Orders", Icons.Default.ReceiptLong, Color(0xFF607D8B), Modifier.weight(1f), onClick = onOrdersClick)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                HomeCard("Hire Machine", Icons.Default.Agriculture, Color(0xFF2196F3), Modifier.weight(1f), onClick = onSeeAllMachines)
                                HomeCard("Buy Products", Icons.Default.Storefront, Color(0xFFE91E63), Modifier.weight(1f), onClick = onSeeAllProducts)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            HomeCard("Market Prices", Icons.AutoMirrored.Filled.TrendingUp, Color(0xFFFF9800), Modifier.fillMaxWidth(), onClick = onMarketClick)
                        }
                    }
                }

                if (filteredMachines.isNotEmpty()) {
                    item { SectionHeader("Available Machines", onSeeAll = onSeeAllMachines) }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(filteredMachines) { machine ->
                                FarmerItemCard(machine, "Machine", Modifier.width(220.dp)) { onMachineClick(machine["id"].toString()) }
                            }
                        }
                    }
                }

                if (filteredProducts.isNotEmpty()) {
                    item { SectionHeader("Farming Products", onSeeAll = onSeeAllProducts) }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(filteredProducts) { product ->
                                FarmerItemCard(product, "Product", Modifier.width(220.dp)) { onProductClick(product["id"].toString()) }
                            }
                        }
                    }
                }
                
                if (showResultsOnly && filteredMachines.isEmpty() && filteredProducts.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No results found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    if (showFilterDialog) {
        SortDialog(sortBy, { sortBy = it }, { showFilterDialog = false })
    }
}

@Composable
fun RecentActivitySection(onOrdersClick: () -> Unit) {
    var recentOrder by remember { mutableStateOf<Map<String, Any>?>(null) }
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        db.collection("orders")
            .whereEqualTo("customerId", uid)
            .addSnapshotListener { value, _ ->
                // Sort in memory to avoid requiring a composite index in Firestore
                recentOrder = value?.documents
                    ?.map { it.data ?: emptyMap() }
                    ?.maxByOrNull { it["timestamp"] as? Long ?: 0L }
            }
    }

    if (recentOrder != null) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Activity", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                TextButton(onClick = onOrdersClick) { Text("View All") }
            }
            Card(
                onClick = onOrdersClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        val status = recentOrder!!["status"].toString()
                        Text("Order $status", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Total: ₹${recentOrder!!["totalAmount"]}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        TextButton(onClick = onSeeAll) { Text("See All", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun FarmerItemCard(item: Map<String, Any>, type: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier, 
        shape = RoundedCornerShape(16.dp), 
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), 
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column {
            AsyncImage(
                model = item["image"]?.toString(), 
                contentDescription = null, 
                modifier = Modifier.fillMaxWidth().height(135.dp), 
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = item["name"].toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = if (type == "Machine") "₹${item["price"]}/hr" else "₹${item["price"]}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onClick, 
                    modifier = Modifier.fillMaxWidth().height(38.dp), 
                    shape = RoundedCornerShape(8.dp), 
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (type == "Machine") "Rent Now" else "Buy Now", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun WeatherWidget(weather: WeatherResponse?, error: String?) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            weather == null -> MaterialTheme.colorScheme.primaryContainer
            weather.weather.firstOrNull()?.main?.contains("Clear", true) == true -> Color(0xFFFFE082) // Sunny
            weather.weather.firstOrNull()?.main?.contains("Rain", true) == true -> Color(0xFF90CAF9) // Rainy
            weather.weather.firstOrNull()?.main?.contains("Cloud", true) == true -> Color(0xFFCFD8DC) // Cloudy
            else -> MaterialTheme.colorScheme.primaryContainer
        },
        animationSpec = tween(1000),
        label = "weatherColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        if (error != null) {
            Box(Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(error, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        } else if (weather == null) {
            Box(Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Loading local weather...")
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = weather.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = weather.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "Condition unknown",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${weather.main.temp.toInt()}°C",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Humidity: ${weather.main.humidity}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
