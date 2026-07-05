package com.example.agro.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopkeeperHomeScreen(
    onLogout: () -> Unit,
    onProfileClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onEditProductClick: (String) -> Unit,
    onManageOrdersClick: () -> Unit,
    onAnalyticsClick: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    var products by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentUser = auth.currentUser
    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }

    LaunchedEffect(Unit) {
        val userId = auth.currentUser?.uid ?: ""
        db.collection("products")
            .whereEqualTo("shopkeeperId", userId)
            .addSnapshotListener { value, error ->
                isLoading = false
                if (error != null) {
                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                products = value?.documents?.map { doc ->
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["id"] = doc.id
                    data
                } ?: emptyList()
            }
            
        currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                userData = doc.data
            }
        }
    }

    val drawerNavigationItems = listOf(
        DrawerNavItem("Add Product", Icons.Default.AddBox, onAddProductClick),
        DrawerNavItem("Order History", Icons.AutoMirrored.Filled.ReceiptLong, onManageOrdersClick),
        DrawerNavItem("Sales Analytics", Icons.Default.BarChart, onAnalyticsClick)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                userName = userData?.get("name")?.toString() ?: "Shopkeeper",
                userRole = "Shopkeeper",
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
                    title = { Text("Shopkeeper Panel", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAddProductClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Product")
                }
            }
        ) { padding ->
            if (isLoading) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.surface),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                HomeCard("Order History", Icons.AutoMirrored.Filled.ReceiptLong, MaterialTheme.colorScheme.primary, Modifier.weight(1f), onClick = onManageOrdersClick)
                                HomeCard("Sales Stats", Icons.Default.BarChart, MaterialTheme.colorScheme.secondary, Modifier.weight(1f), onClick = onAnalyticsClick)
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "My Products",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    if (products.isEmpty()) {
                        item {
                            Text("No products in your shop yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        items(products) { product ->
                            ProductItem(
                                product = product,
                                onEdit = { onEditProductClick(product["id"].toString()) },
                                onDelete = {
                                    db.collection("products").document(product["id"].toString()).delete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductItem(product: Map<String, Any>, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column {
            AsyncImage(
                model = product["image"]?.toString(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(160.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(text = product["name"].toString(), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(text = product["category"].toString(), color = MaterialTheme.colorScheme.secondary)
                    }
                    Text(text = "₹${product["price"]}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                val stock = product["stock"]?.toString()?.toIntOrNull() ?: 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Stock: $stock", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (stock < 5) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant)
                    if (stock < 5) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(color = Color.Red.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                            Text("LOW STOCK", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                }
                
                Text(text = product["description"].toString(), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    FilledIconButton(
                        onClick = onEdit,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
