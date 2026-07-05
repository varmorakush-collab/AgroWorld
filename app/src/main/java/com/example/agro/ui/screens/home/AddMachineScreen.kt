package com.example.agro.ui.screens.home

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import coil.compose.rememberAsyncImagePainter
import com.example.agro.utils.ImageUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMachineScreen(machineId: String? = null, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    var machineName by remember { mutableStateOf("") }
    var machineType by remember { mutableStateOf("") }
    var pricePerHour by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var existingImageUrl by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var contactInfo by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedImageUri = uri }

    LaunchedEffect(machineId) {
        if (machineId != null) {
            db.collection("machines").document(machineId).get().addOnSuccessListener { doc ->
                machineName = doc.getString("name") ?: ""
                machineType = doc.getString("type") ?: ""
                pricePerHour = doc.getString("price") ?: ""
                existingImageUrl = doc.getString("image") ?: ""
                description = doc.getString("description") ?: ""
                contactInfo = doc.getString("contact") ?: ""
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (machineId == null) "List Your Machine" else "Edit Machine", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)).clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                val painter = if (selectedImageUri != null) rememberAsyncImagePainter(selectedImageUri) else rememberAsyncImagePainter(existingImageUrl)
                if (selectedImageUri != null || existingImageUrl.isNotEmpty()) {
                    Image(painter = painter, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(50.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Add Machine Photo", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(value = machineName, onValueChange = { machineName = it }, label = { Text("Machine Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = machineType, onValueChange = { machineType = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = pricePerHour, onValueChange = { if (it.all { c -> c.isDigit() }) pricePerHour = it }, label = { Text("Price/hr") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), prefix = { Text("₹ ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = contactInfo, onValueChange = { if (it.all { c -> c.isDigit() }) contactInfo = it }, label = { Text("Contact Number") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(16.dp))

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (machineName.isNotEmpty() && pricePerHour.isNotEmpty()) {
                        isLoading = true
                        scope.launch {
                            val finalImageUrl = if (selectedImageUri != null) {
                                ImageUtils.uploadToCloud(context, selectedImageUri!!)
                            } else existingImageUrl

                            val machineMap = mapOf(
                                "ownerId" to (auth.currentUser?.uid ?: ""),
                                "name" to machineName, "type" to machineType, "price" to pricePerHour,
                                "image" to finalImageUrl, "description" to description, "contact" to contactInfo
                            )
                            
                            val task = if (machineId == null) db.collection("machines").add(machineMap) else db.collection("machines").document(machineId).set(machineMap)
                            task.addOnSuccessListener { onBack() }.addOnFailureListener { 
                                isLoading = false
                                Toast.makeText(context, "Error saving data", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White) else Text("Submit Listing")
            }
        }
    }
}
