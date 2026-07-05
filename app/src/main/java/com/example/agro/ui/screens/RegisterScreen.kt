package com.example.agro.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.agro.R
import com.example.agro.utils.ImageUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: (String) -> Unit,
    onLoginClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    var address by remember { mutableStateOf("") }
    var landArea by remember { mutableStateOf("") }
    var machineDetails by remember { mutableStateOf("") }
    var shopName by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }

    val roles = listOf(
        Pair(stringResource(R.string.role_farmer), Icons.Default.Agriculture),
        Pair(stringResource(R.string.role_machine_owner), Icons.Default.PrecisionManufacturing),
        Pair(stringResource(R.string.role_shopkeeper), Icons.Default.Storefront)
    )
    var selectedRoleIndex by remember { mutableStateOf(0) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedImageUri = uri }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), MaterialTheme.colorScheme.surface)))) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(40.dp))
            Text(text = "Join Agro", fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                roles.forEachIndexed { index, roleData ->
                    FilterChip(
                        selected = selectedRoleIndex == index,
                        onClick = { selectedRoleIndex = index },
                        label = { Text(roleData.first, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(roleData.second, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }

            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Box(
                    modifier = Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)).clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        Image(painter = rememberAsyncImagePainter(selectedImageUri), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    }
                }
                Text("Profile Photo", fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), leadingIcon = { Icon(Icons.Default.Person, null) })
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), leadingIcon = { Icon(Icons.Default.Email, null) })
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = phone, onValueChange = { if (it.all { c -> c.isDigit() }) phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), leadingIcon = { Icon(Icons.Default.Phone, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                
                Spacer(modifier = Modifier.height(12.dp))
                
                when (selectedRoleIndex) {
                    0 -> {
                        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Farm Address") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(value = landArea, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) landArea = it }, label = { Text("Land Area (Acres)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                    }
                    1 -> {
                        OutlinedTextField(value = machineDetails, onValueChange = { machineDetails = it }, label = { Text("Machine Types") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Base Location") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                    }
                    2 -> {
                        OutlinedTextField(value = shopName, onValueChange = { shopName = it }, label = { Text("Shop Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Shop Address") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null) } }
                )
                
                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = {
                        if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty()) {
                            isLoading = true
                            scope.launch {
                                val imageUrl = if (selectedImageUri != null) {
                                    ImageUtils.uploadToCloud(context, selectedImageUri!!)
                                } else ""
                                
                                registerUser(auth, db, email, password, name, phone, imageUrl, roles[selectedRoleIndex].first, selectedRoleIndex, address, landArea, machineDetails, shopName, onRegisterSuccess) {
                                    isLoading = false
                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading
                ) {
                    Text("Create My Account", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

private fun registerUser(auth: FirebaseAuth, db: FirebaseFirestore, email: String, pass: String, name: String, phone: String, img: String, role: String, roleIdx: Int, addr: String, land: String, mach: String, shop: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
    auth.createUserWithEmailAndPassword(email, pass).addOnSuccessListener { result ->
        val uid = result.user!!.uid
        val userMap = mutableMapOf("uid" to uid, "name" to name, "email" to email, "phone" to phone, "profileImage" to img, "role" to role)
        when (roleIdx) {
            0 -> { userMap["address"] = addr; userMap["landArea"] = land }
            1 -> { userMap["machineDetails"] = mach; userMap["address"] = addr }
            2 -> { userMap["shopName"] = shop; userMap["address"] = addr }
        }
        db.collection("users").document(uid).set(userMap).addOnSuccessListener { onSuccess(role) }.addOnFailureListener { onError(it.message ?: "DB Error") }
    }.addOnFailureListener { onError(it.message ?: "Auth Error") }
}
