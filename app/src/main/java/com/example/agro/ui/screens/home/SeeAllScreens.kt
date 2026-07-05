package com.example.agro.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MachineListScreen(
    onBack: () -> Unit,
    onMachineClick: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var machines by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("Default") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("machines").get().addOnSuccessListener { result ->
            machines = result.documents.map { it.data?.plus("id" to it.id) ?: emptyMap() }
            isLoading = false
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

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Rent Machines", fontWeight = FontWeight.Bold) }, navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            SearchAndFilterBar(searchQuery, { searchQuery = it }, { showFilterDialog = true })
            if (isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else {
                LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredMachines) { machine ->
                        FarmerItemCard(machine, "Machine", Modifier.fillMaxWidth()) { onMachineClick(machine["id"].toString()) }
                    }
                }
            }
        }
    }
    if (showFilterDialog) SortDialog(sortBy, { sortBy = it }, { showFilterDialog = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    onBack: () -> Unit,
    onProductClick: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var products by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("Default") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("products").get().addOnSuccessListener { result ->
            products = result.documents.map { it.data?.plus("id" to it.id) ?: emptyMap() }
            isLoading = false
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

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Farming Products", fontWeight = FontWeight.Bold) }, navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            SearchAndFilterBar(searchQuery, { searchQuery = it }, { showFilterDialog = true })
            if (isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else {
                LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredProducts) { product ->
                        FarmerItemCard(product, "Product", Modifier.fillMaxWidth()) { onProductClick(product["id"].toString()) }
                    }
                }
            }
        }
    }
    if (showFilterDialog) SortDialog(sortBy, { sortBy = it }, { showFilterDialog = false })
}

@Composable
fun SortDialog(currentSort: String, onSortSelected: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort By Price") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = currentSort == "Default", onClick = { onSortSelected("Default") })
                    Text("Default")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = currentSort == "PriceLowToHigh", onClick = { onSortSelected("PriceLowToHigh") })
                    Text("Low to High")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = currentSort == "PriceHighToLow", onClick = { onSortSelected("PriceHighToLow") })
                    Text("High to Low")
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Apply") } }
    )
}
