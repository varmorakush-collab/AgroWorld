package com.example.agro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.agro.data.LanguageManager
import com.example.agro.ui.screens.*
import com.example.agro.ui.screens.home.*
import com.example.agro.ui.theme.AgroTheme
import com.example.agro.viewmodel.CartViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val languageManager = LanguageManager(context)
            val savedLanguage by languageManager.getLanguage.collectAsState(initial = "checking")

            LaunchedEffect(savedLanguage) {
                if (savedLanguage != "checking" && savedLanguage != null) {
                    languageManager.setLocale(context, savedLanguage!!)
                }
            }

            AgroTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (savedLanguage != "checking") {
                        AppNavigation(languageManager, savedLanguage)
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(languageManager: LanguageManager, savedLanguage: String?) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val cartViewModel: CartViewModel = viewModel()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen {
                val currentUser = auth.currentUser
                if (savedLanguage == null) {
                    navController.navigate("language_selection") {
                        popUpTo("splash") { inclusive = true }
                    }
                } else if (currentUser != null) {
                    db.collection("users").document(currentUser.uid).get()
                        .addOnSuccessListener { document ->
                            val role = document.getString("role") ?: "Farmer"
                            navController.navigate("home/$role") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                        .addOnFailureListener {
                            navController.navigate("login") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                } else {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            }
        }
        composable("language_selection") {
            LanguageSelectionScreen(onLanguageSelected = { langCode ->
                scope.launch {
                    languageManager.saveLanguage(langCode)
                    navController.navigate("login") {
                        popUpTo("language_selection") { inclusive = true }
                    }
                }
            })
        }
        composable("login") {
            LoginScreen(
                onLoginSuccess = { role ->
                    navController.navigate("home/$role") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onRegisterClick = { navController.navigate("register") },
                onForgotPasswordClick = { navController.navigate("forgot_password") }
            )
        }
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = { role ->
                    navController.navigate("home/$role") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onLoginClick = { navController.navigate("login") }
            )
        }
        composable("home/{role}") { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "Farmer"
            val onLogout = {
                auth.signOut()
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
            val onProfileClick = { navController.navigate("profile") }
            val onOrdersManageClick = { navController.navigate("seller_orders") }
            val onAnalyticsClick = { navController.navigate("analytics") }

            when (role) {
                "Farmer", "ખેડૂત", "किसान" -> FarmerHomeScreen(
                    onLogout = onLogout,
                    onProfileClick = onProfileClick,
                    onCartClick = { navController.navigate("cart") },
                    onSeeAllMachines = { navController.navigate("machine_list") },
                    onSeeAllProducts = { navController.navigate("product_list") },
                    onMachineClick = { id -> navController.navigate("machine_detail/$id") },
                    onProductClick = { id -> navController.navigate("product_detail/$id") },
                    onMarketClick = { navController.navigate("market") },
                    onOrdersClick = { navController.navigate("my_orders") }
                )
                "Machine Owner", "મશીન માલિક", "मશીન માલિક" -> MachineOwnerHomeScreen(
                    onLogout = onLogout,
                    onProfileClick = onProfileClick,
                    onAddMachineClick = { navController.navigate("add_machine") },
                    onEditMachineClick = { id -> navController.navigate("edit_machine/$id") },
                    onManageOrdersClick = onOrdersManageClick,
                    onAnalyticsClick = onAnalyticsClick
                )
                "Shopkeeper", "દુકાનદાર", "દુકાનદાર" -> ShopkeeperHomeScreen(
                    onLogout = onLogout,
                    onProfileClick = onProfileClick,
                    onAddProductClick = { navController.navigate("add_product") },
                    onEditProductClick = { id -> navController.navigate("edit_product/$id") },
                    onManageOrdersClick = onOrdersManageClick,
                    onAnalyticsClick = onAnalyticsClick
                )
                "Admin", "એડમિન", "एडमिन" -> AdminHomeScreen(onLogout, onProfileClick)
                else -> FarmerHomeScreen(
                    onLogout = onLogout,
                    onProfileClick = onProfileClick,
                    onCartClick = { navController.navigate("cart") },
                    onSeeAllMachines = { navController.navigate("machine_list") },
                    onSeeAllProducts = { navController.navigate("product_list") },
                    onMachineClick = { id -> navController.navigate("machine_detail/$id") },
                    onProductClick = { id -> navController.navigate("product_detail/$id") },
                    onMarketClick = { navController.navigate("market") },
                    onOrdersClick = { navController.navigate("my_orders") }
                )
            }
        }
        composable("seller_orders") {
            SellerOrdersScreen(onBack = { navController.popBackStack() })
        }
        composable("analytics") {
            AnalyticsScreen(onBack = { navController.popBackStack() })
        }
        composable("cart") {
            CartScreen(
                cartViewModel = cartViewModel,
                onBack = { navController.popBackStack() },
                onCheckoutSuccess = { navController.popBackStack() }
            )
        }
        composable("machine_list") {
            MachineListScreen(
                onBack = { navController.popBackStack() },
                onMachineClick = { id -> navController.navigate("machine_detail/$id") }
            )
        }
        composable("product_list") {
            ProductListScreen(
                onBack = { navController.popBackStack() },
                onProductClick = { id -> navController.navigate("product_detail/$id") }
            )
        }
        composable("market") {
            MarketScreen(onBack = { navController.popBackStack() })
        }
        composable("my_orders") {
            MyOrdersScreen(onBack = { navController.popBackStack() })
        }
        composable("machine_detail/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            MachineDetailScreen(
                machineId = id,
                onBack = { navController.popBackStack() }
            )
        }
        composable("product_detail/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            ProductDetailScreen(
                productId = id,
                cartViewModel = cartViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("profile") {
            ProfileScreen(onBack = { navController.popBackStack() })
        }
        composable("add_machine") {
            AddMachineScreen(machineId = null, onBack = { navController.popBackStack() })
        }
        composable("edit_machine/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")
            AddMachineScreen(machineId = id, onBack = { navController.popBackStack() })
        }
        composable("add_product") {
            AddProductScreen(productId = null, onBack = { navController.popBackStack() })
        }
        composable("edit_product/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")
            AddProductScreen(productId = id, onBack = { navController.popBackStack() })
        }
        composable("forgot_password") {
            ForgotPasswordScreen(
                onBackClick = { navController.popBackStack() },
                onSendResetLink = { /* Handle reset */ }
            )
        }
    }
}
