package com.example.agro.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.agro.ui.components.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(onLogout: () -> Unit, onProfileClick: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    var users by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var machines by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var products by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var userFilter by remember { mutableStateOf("All") }
    
    var showEditDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<Map<String, Any>?>(null) }
    var newRole by remember { mutableStateOf("") }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentUser = auth.currentUser
    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }

    LaunchedEffect(Unit) {
        db.collection("users").addSnapshotListener { value, _ ->
            users = value?.documents?.map { doc ->
                val data = doc.data?.toMutableMap() ?: mutableMapOf()
                data["id"] = doc.id
                data
            } ?: emptyList()
        }
        db.collection("machines").addSnapshotListener { value, _ ->
            machines = value?.documents?.map { it.data ?: emptyMap() } ?: emptyList()
        }
        db.collection("products").addSnapshotListener { value, _ ->
            products = value?.documents?.map { it.data ?: emptyMap() } ?: emptyList()
        }
        
        currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                userData = doc.data
            }
        }
    }

    val drawerNavigationItems = listOf(
        DrawerNavItem("User Management", Icons.Default.People, { selectedTab = 0 }),
        DrawerNavItem("Machine Assets", Icons.Default.Agriculture, { selectedTab = 1 }),
        DrawerNavItem("Product Inventory", Icons.Default.Inventory, { selectedTab = 2 }),
        DrawerNavItem("System Analytics", Icons.Default.Analytics, { selectedTab = 3 })
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                userName = userData?.get("name")?.toString() ?: "Admin",
                userRole = "System Administrator",
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
                    title = { Text("Admin Dashboard", fontWeight = FontWeight.ExtraBold) },
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
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.surface)) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {},
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Users", fontWeight = FontWeight.Bold) })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Machines", fontWeight = FontWeight.Bold) })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Products", fontWeight = FontWeight.Bold) })
                    Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Analytics", fontWeight = FontWeight.Bold) })
                }

                if (selectedTab == 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "Farmer", "Shopkeeper", "Machine Owner").forEach { filter ->
                            FilterChip(
                                selected = userFilter == filter,
                                onClick = { userFilter = filter },
                                label = { Text(filter, fontSize = 10.sp) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            val filteredUsers = if (userFilter == "All") users else users.filter { it["role"] == userFilter }
                            items(filteredUsers) { user ->
                                AdminUserItem(
                                    user = user,
                                    onEdit = {
                                        editingUser = user
                                        newRole = user["role"].toString()
                                        showEditDialog = true
                                    },
                                    onDelete = {
                                        db.collection("users").document(user["id"].toString()).delete()
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "User deleted", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                )
                            }
                        }
                        1 -> {
                            items(machines) { machine ->
                                AdminItemRow(
                                    name = machine["name"].toString(), 
                                    detail = "Owner ID: ${machine["ownerId"]}",
                                    type = "Machine",
                                    imageUrl = machine["image"]?.toString() ?: ""
                                )
                            }
                        }
                        2 -> {
                            items(products) { product ->
                                AdminItemRow(
                                    name = product["name"].toString(), 
                                    detail = "Price: ₹${product["price"]}",
                                    type = "Product",
                                    imageUrl = product["image"]?.toString() ?: ""
                                )
                            }
                        }
                        3 -> {
                            item {
                                AdminAnalyticsView(users, machines, products)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog && editingUser != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Change User Role", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("User: ${editingUser!!["name"]}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newRole,
                        onValueChange = { newRole = it },
                        label = { Text("Enter New Role") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        db.collection("users").document(editingUser!!["id"].toString())
                            .update("role", newRole)
                            .addOnSuccessListener {
                                showEditDialog = false
                                Toast.makeText(context, "Role updated successfully", Toast.LENGTH_SHORT).show()
                            }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AdminUserItem(user: Map<String, Any>, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                val imageUrl = user["profileImage"]?.toString() ?: ""
                if (imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(user["name"].toString().take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user["name"].toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = user["role"].toString(), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun AdminItemRow(name: String, detail: String, type: String, imageUrl: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                if (imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (type == "Machine") Icons.Default.Agriculture else Icons.Default.Inventory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(text = detail, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AdminAnalyticsView(
    users: List<Map<String, Any>>,
    machines: List<Map<String, Any>>,
    products: List<Map<String, Any>>
) {
    val farmers = users.count { it["role"] == "Farmer" }
    val shopkeepers = users.count { it["role"] == "Shopkeeper" }
    val owners = users.count { it["role"] == "Machine Owner" }
    
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        StatGrid(
            listOf(
                StatData("Total Users", users.size.toString(), Icons.Default.Group, MaterialTheme.colorScheme.primary),
                StatData("Assets", (machines.size + products.size).toString(), Icons.Default.Inventory2, MaterialTheme.colorScheme.secondary)
            )
        )
        
        Text("User Roles Distribution", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        AnimatedPieChart(
            values = listOf(farmers.toFloat(), shopkeepers.toFloat(), owners.toFloat()),
            colors = listOf(Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF9800)),
            labels = listOf("Farmers", "Shopkeepers", "Owners")
        )
        
        Text("Platform Inventory", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        AttractiveBarChart(
            data = listOf(machines.size.toDouble(), products.size.toDouble()),
            labels = listOf("Machines", "Products"),
            barColor = MaterialTheme.colorScheme.secondary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

