package com.example.myapplication

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDateTime
import java.util.*

// ── Firebase Preparation ───────────────────────────────────────────────────

/**
 * Interface for Firebase Authentication.
 * Implement this using FirebaseAuth.getInstance()
 */
interface AuthManager {
    fun signUp(user: User, password: String, onResult: (Boolean, String?) -> Unit)
    fun login(email: String, password: String, onResult: (User?, String?) -> Unit)
    fun logout()
}

/**
 * Concrete implementation of AuthManager using Firebase
 */
class FirebaseAuthManager : AuthManager {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun signUp(user: User, password: String, onResult: (Boolean, String?) -> Unit) {

        auth.createUserWithEmailAndPassword(user.email, password)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    val firebaseUser = task.result?.user

                    // IMPORTANT:
                    // immediately continue login flow
                    onResult(true, null)

                    // save profile in background
                    if (firebaseUser != null) {
                        db.collection("users")
                            .document(firebaseUser.uid)
                            .set(user)
                    }

                } else {

                    val errorMsg =
                        task.exception?.localizedMessage ?: "Unknown Error"

                    val finalMsg =
                        if (errorMsg.contains("CONFIGURATION_NOT_FOUND")) {
                            "Firebase Auth is not enabled in Firebase Console."
                        } else {
                            errorMsg
                        }

                    onResult(false, finalMsg)
                }
            }
    }

    override fun login(email: String, password: String, onResult: (User?, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result?.user?.uid
                    if (uid != null) {
                        db.collection("users").document(uid).get()
                            .addOnSuccessListener { doc ->
                                val user = doc.toObject(User::class.java)
                                onResult(user ?: User(email.split("@")[0], email), null)
                            }
                            .addOnFailureListener { onResult(User(email.split("@")[0], email), null) }
                    } else {
                        onResult(User(email.split("@")[0], email), null)
                    }
                } else {
                    onResult(null, task.exception?.message)
                }
            }
    }

    override fun logout() {
        auth.signOut()
    }
}

/**
 * Interface for Firestore Medicine storage.
 */
interface MedicineRepository {
    fun getMedicines(onResult: (List<Medicine>) -> Unit)
    fun addOrUpdateMedicine(medicine: Medicine)
    fun deleteMedicine(medicine: Medicine)
}

// ── Translations ─────────────────────────────────────────────────────────────

data class AppStrings(
    val appName: String,
    val subtitle: String,
    val searchPlaceholder: String,
    val home: String,
    val emergency: String,
    val cart: String,
    val login: String,
    val addToCart: String,
    val inStock: String,
    val outOfStock: String,
    val lifeSaving: String,
    val cartTitle: String,
    val clearCart: String,
    val subtotal: String,
    val discount: String,
    val totalAmount: String,
    val checkout: String,
    val emergencyStock: String,
    val emergencySubtitle: String,
    val loading: String,
    val userLogin: String,
    val agentLogin: String,
    val emailLabel: String,
    val passwordLabel: String,
    val loginButton: String,
    val agentDashboard: String,
    val addNewMed: String,
    val editMed: String,
    val nameLabel: String,
    val villageLabel: String,
    val shopLabel: String,
    val priceLabel: String,
    val save: String,
    val cancel: String,
    val langLabel: String,
    val logout: String,
    val loginPrompt: String,
    val emptyCart: String,
    val discountApplied: String,
    val kmAway: String,
    val expires: String,
    val noResults: String,
    val issueDateLabel: String,
    val expiryDateLabel: String,
    val expired: String,
    val signUpPrompt: String,
    val signUpTitle: String,
    val nameHint: String,
    val addressLabel: String,
    val registerButton: String,
    val backToLogin: String
)

val englishStrings = AppStrings(
    appName = "Grama-Sanjeevini",
    subtitle = "Rural Pharmacy Network",
    searchPlaceholder = "Search medicines...",
    home = "Home",
    emergency = "Emergency",
    cart = "Cart",
    login = "Login",
    addToCart = "Add to Cart",
    inStock = "In Stock",
    outOfStock = "Out",
    lifeSaving = "LIFE SAVING",
    cartTitle = "Your Shopping Cart",
    clearCart = "Clear Cart",
    subtotal = "Subtotal",
    discount = "Discount",
    totalAmount = "Total Amount",
    checkout = "Proceed to Checkout",
    emergencyStock = "Emergency Stock",
    emergencySubtitle = "Life-saving drugs across the network",
    loading = "Loading Medicines...",
    userLogin = "User Login",
    agentLogin = "Agent Login",
    emailLabel = "Email / ID",
    passwordLabel = "Password",
    loginButton = "Login",
    agentDashboard = "Agent Dashboard",
    addNewMed = "Add New Medicine",
    editMed = "Edit Medicine",
    nameLabel = "Name",
    villageLabel = "Village",
    shopLabel = "Shop Name",
    priceLabel = "Price",
    save = "Save",
    cancel = "Cancel",
    langLabel = "Lang",
    logout = "Logout",
    loginPrompt = "login here -> ",
    emptyCart = "Your cart is empty",
    discountApplied = "Discount of %d%% has been applied",
    kmAway = "%dkm away",
    expires = "Expires in %d days",
    noResults = "No results for \"%s\"",
    issueDateLabel = "Issue Date",
    expiryDateLabel = "Expiry Date",
    expired = "EXPIRED",
    signUpPrompt = "Don't have an account? Sign up",
    signUpTitle = "User Registration",
    nameHint = "Full Name",
    addressLabel = "Address",
    registerButton = "Register",
    backToLogin = "Back to Login"
)

val kannadaStrings = AppStrings(
    appName = "ಗ್ರಾಮ-ಸಂಜೀವಿನಿ",
    subtitle = "ಗ್ರಾಮೀಣ ಫಾರ್ಮಸಿ ನೆಟ್‌ವರ್ಕ್",
    searchPlaceholder = "ಔಷಧಿಗಳನ್ನು ಹುಡುಕಿ...",
    home = "ಮುಖಪುಟ",
    emergency = "ತುರ್ತು",
    cart = "ಕಾರ್ಟ್",
    login = "ಲಾಗಿನ್",
    addToCart = "ಕಾರ್ಟ್‌ಗೆ ಸೇರಿಸಿ",
    inStock = "ದಾಸ್ತಾನು ಇದೆ",
    outOfStock = "ಖಾಲಿ ಇದೆ",
    lifeSaving = "ಜೀವ ರಕ್ಷಕ",
    cartTitle = "ನಿಮ್ಮ ಶಾಪಿಂಗ್ ಕಾರ್ಟ್",
    clearCart = "ಕಾರ್ಟ್ ಖಾಲಿ ಮಾಡಿ",
    subtotal = "ಉಪಮೊತ್ತ",
    discount = "ರಿಯಾಯಿತಿ",
    totalAmount = "ಒಟ್ಟು ಮೊತ್ತ",
    checkout = "ಚೆಕ್‌ಔಟ್‌ಗೆ ಮುಂದುವರಿಯಿರಿ",
    emergencyStock = "ತುರ್ತು ದಾಸ್ತಾನು",
    emergencySubtitle = "ನೆಟ್‌ವರ್ಕ್‌ನಾದ್ಯಂತ ಜೀವ ರಕ್ಷಕ ಔಷಧಗಳು",
    loading = "ಔಷಧಿಗಳನ್ನು ಲೋಡ್ ಮಾಡಲಾಗುತ್ತಿದೆ...",
    userLogin = "ಬಳಕೆದಾರರ ಲಾಗಿನ್",
    agentLogin = "ಏಜೆಂಟ್ ಲಾಗಿನ್",
    emailLabel = "ಇಮೇಲ್ / ಐಡಿ",
    passwordLabel = "ಪಾಸ್‌ವರ್ಡ್",
    loginButton = "ಲಾಗಿನ್",
    agentDashboard = "ಏಜೆಂಟ್ ಡ್ಯಾಶ್‌ಬೋರ್ಡ್",
    addNewMed = "ಹೊಸ ಔಷಧಿಯನ್ನು ಸೇರಿಸಿ",
    editMed = "ಔಷಧಿಯನ್ನು ಎಡಿಟ್ ಮಾಡಿ",
    nameLabel = "ಹೆಸರು",
    villageLabel = "ಗ್ರಾಮ",
    shopLabel = "ಅಂಗಡಿಯ ಹೆಸರು",
    priceLabel = "ಬೆಲೆ",
    save = "ಉಳಿಸಿ",
    cancel = "ರದ್ದುಮಾಡಿ",
    langLabel = "ಭಾಷೆ",
    logout = "ಲೋಗೌಟ್",
    loginPrompt = "ಇಲ್ಲಿ ಲಾಗಿನ್ ಮಾಡಿ -> ",
    emptyCart = "ನಿಮ್ಮ ಕಾರ್ಟ್ ಖಾಲಿ ಇದೆ",
    discountApplied = "%d%% ರಿಯಾಯಿತಿಯನ್ನು ಅನ್ವಯಿಸಲಾಗಿದೆ",
    kmAway = "%d ಕಿ.ಮೀ ದೂರ",
    expires = "%d ದಿನಗಳಲ್ಲಿ ಅವಧಿ ಮುಗಿಯುತ್ತದೆ",
    noResults = "\"%s\" ಗೆ ಯಾವುದೇ ಫಲಿತಾಂಶಗಳಿಲ್ಲ",
    issueDateLabel = "ಬಿಡುಗಡೆ ದಿನಾಂಕ",
    expiryDateLabel = "ಅವಧಿ ಮುಗಿಯುವ ದಿನಾಂಕ",
    expired = "ಅವಧಿ ಮುಗಿದಿದೆ",
    signUpPrompt = "ಖಾತೆ ಇಲ್ಲವೇ? ಸೈನ್ ಅಪ್ ಮಾಡಿ",
    signUpTitle = "ಬಳಕೆದಾರರ ನೋಂದಣಿ",
    nameHint = "ಪೂರ್ಣ ಹೆಸರು",
    addressLabel = "ವಿಳಾಸ",
    registerButton = "ನೋಂದಾಯಿಸಿ",
    backToLogin = "ಲಾಗಿನ್‌ಗೆ ಹಿಂತಿರುಗಿ"
)

val LocalAppStrings = staticCompositionLocalOf { englishStrings }

// ── Data Model ───────────────────────────────────────────────────────────────

data class Medicine(
    val name: String,
    val village: String,
    val shopName: String,
    val distanceKm: Int,
    val inStock: Boolean,
    val isLifeSaving: Boolean = false,
    val issueDate: LocalDate? = null,
    val expiryDate: LocalDate? = null,
    val price: Int = 0,
    val emoji: String = "💊"
) {
    val expiryDaysLeft: Long?
        get() = expiryDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }

    val isExpired: Boolean
        get() = expiryDate?.isBefore(LocalDate.now()) ?: false
}

data class User(
    val name: String = "",
    val email: String = "",
    val address: String = "",
    val isAgent: Boolean = false
)

data class CartItem(
    val medicine: Medicine,
    val quantity: Int = 1
)

// ── Mock Data ─────────────────────────────────────────────────────────────────

val mockMedicines = listOf(
    Medicine("Paracetamol 500mg", "Village B", "Shop B1", 2,  true,  price = 12,  emoji = "💊"),
    Medicine("Paracetamol 500mg", "Village B", "Shop B2", 3,  true,  price = 12,  emoji = "💊"),
    Medicine("Paracetamol 500mg", "Village B", "Shop B3", 7,  true,  price = 12,  emoji = "💊"),
    Medicine("Paracetamol 500mg", "Village C", "Shop C4", 3,  true,  price = 12,  emoji = "💊"),
    Medicine("Insulin (Rapid)",   "Village A", "Shop A1", 1,  true,  isLifeSaving = true, price = 245, emoji = "💉"),
    Medicine("Insulin (Rapid)",   "Village A", "Shop A2", 2,  true,  isLifeSaving = true, price = 245, emoji = "💉"),
    Medicine("Insulin (Rapid)",   "Village A", "Shop A3", 3,  true,  isLifeSaving = true, price = 245, emoji = "💉"),
    Medicine("Insulin (Rapid)",   "Village B", "Shop A4", 1,  true,  isLifeSaving = true, price = 245, emoji = "💉"),
    Medicine("Snake Venom Serum", "Village C", "Shop C1", 7,  true,  isLifeSaving = true, price = 890, emoji = "🧪"),
    Medicine("Snake Venom Serum", "Village C", "Shop C2", 8,  true,  isLifeSaving = true, price = 890, emoji = "🧪"),
    Medicine("Snake Venom Serum", "Village C", "Shop C3", 9,  true,  isLifeSaving = true, price = 890, emoji = "🧪"),
    Medicine("Snake Venom Serum", "Village D", "Shop D1", 4,  true,  isLifeSaving = true, price = 890, emoji = "🧪"),
    Medicine("Snake Venom Serum", "Village E", "Shop C1", 8,  true,  isLifeSaving = true, price = 890, emoji = "🧪"),
    Medicine("Amoxicillin 250mg", "Village B", "Shop B2", 3,  false, price = 48,  emoji = "💊"),
    Medicine("Amoxicillin 250mg", "Village B", "Shop B3", 4,  false, price = 48,  emoji = "💊"),
    Medicine("Amoxicillin 250mg", "Village B", "Shop B4", 5,  false, price = 48,  emoji = "💊"),
    Medicine("Amoxicillin 250mg", "Village C", "Shop C2", 3,  false, price = 48,  emoji = "💊"),
    Medicine("Metformin 500mg",   "Village D", "Shop D1", 10, true,  expiryDate = LocalDate.now().plusDays(5), price = 22, emoji = "💊"),
    Medicine("Metformin 500mg",   "Village D", "Shop D2", 12, true,  expiryDate = LocalDate.now().plusDays(5), price = 22, emoji = "💊"),
    Medicine("Metformin 500mg",   "Village D", "Shop D3", 13, true,  expiryDate = LocalDate.now().plusDays(5), price = 22, emoji = "💊"),
    Medicine("Metformin 500mg",   "Village E", "Shop E1", 12, true,  expiryDate = LocalDate.now().plusDays(5), price = 22, emoji = "💊"),
    Medicine("ORS Sachets",       "Village A", "Shop A2", 1,  true,  price = 8,   emoji = "🧃"),
    Medicine("ORS Sachets",       "Village B", "Shop B2", 2,  true,  price = 8,   emoji = "🧃"),
    Medicine("ORS Sachets",       "Village A", "Shop A3", 4,  true,  price = 8,   emoji = "🧃"),
    Medicine("ORS Sachets",       "Village A", "Shop A4", 5,  true,  price = 8,   emoji = "🧃"),
    Medicine("Adrenaline Inj.",   "Village C", "Shop C2", 8,  true,  isLifeSaving = true, price = 310, emoji = "💉"),
    Medicine("Adrenaline Inj.",   "Village C", "Shop C3", 9,  true,  isLifeSaving = true, price = 310, emoji = "💉"),
    Medicine("Adrenaline Inj.",   "Village C", "Shop C4", 19,  true,  isLifeSaving = true, price = 310, emoji = "💉"),
    Medicine("Adrenaline Inj.",   "Village D", "Shop D2", 8,  true,  isLifeSaving = true, price = 310, emoji = "💉"),
    Medicine("Cough Syrup 100ml", "Village B", "Shop B1", 2,  true,  expiryDate = LocalDate.now().plusDays(3), price = 65, emoji = "🍶"),
    Medicine("Cough Syrup 100ml", "Village B", "Shop B2", 4,  true,  expiryDate = LocalDate.now().plusDays(3), price = 65, emoji = "🍶"),
    Medicine("Cough Syrup 100ml", "Village B", "Shop B3", 6,  true,  expiryDate = LocalDate.now().plusDays(3), price = 65, emoji = "🍶"),
    Medicine("Cough Syrup 100ml", "Village C", "Shop C1", 3,  true,  expiryDate = LocalDate.now().plusDays(3), price = 65, emoji = "🍶"),
    Medicine("Eye Drops 50ml",    "Village B", "Shop B3", 5,  false, price = 38,  emoji = "👁️"),
    Medicine("Eye Drops 50ml",    "Village B", "Shop B2", 7,  false, price = 38,  emoji = "👁️"),
    Medicine("Eye Drops 50ml",    "Village C", "Shop C3", 15,  false, price = 38,  emoji = "👁️"),
    Medicine("Bandages x5",       "Village D", "Shop D2", 10, true,  price = 15,  emoji = "🩹"),
    Medicine("Bandages x5",       "Village D", "Shop D4", 13, true,  price = 15,  emoji = "🩹"),
    Medicine("Bandages x5",       "Village D", "Shop D3", 15, true,  price = 15,  emoji = "🩹"),
    Medicine("Bandages x5",       "Village E", "Shop E2", 13, true,  price = 15,  emoji = "🩹"),
    Medicine("Expired Test",      "Village Z", "Shop Z1", 1,  true,  expiryDate = LocalDate.now().minusDays(2), price = 50, emoji = "💊"),
)

val catalogueColors = listOf(
    listOf(Color(0xFFFFEBEE), Color(0xFFFFCDD2)),
    listOf(Color(0xFFFCE4EC), Color(0xFFF8BBD0)),
    listOf(Color(0xFFF3E5F5), Color(0xFFE1BEE7)),
    listOf(Color(0xFFFFF9C4), Color(0xFFFFF59D)),
    listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9)),
    listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB)),
)

// ── Navigation ────────────────────────────────────────────────────────────────

enum class AppDestinations(val icon: Int) {
    HOME      (R.drawable.ic_home),
    EMERGENCY (R.drawable.ic_favorite),
    CART      (R.drawable.ic_cart),
    LOGIN     (R.drawable.ic_account_box),
}

fun AppDestinations.getLabel(strings: AppStrings): String = when(this) {
    AppDestinations.HOME -> strings.home
    AppDestinations.EMERGENCY -> strings.emergency
    AppDestinations.CART -> strings.cart
    AppDestinations.LOGIN -> strings.login
}

// ── Activity ──────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                    GramaSanjeeviniApp()
                }
            }
        }
    }
}

@Composable
fun GramaSanjeeviniApp() {
    var userLanguage by rememberSaveable { mutableStateOf<String?>(null) }
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var cartItems by remember { mutableStateOf(listOf<CartItem>()) }
    var showPaymentPage by remember { mutableStateOf(false) }
    
    val strings = if (userLanguage?.contains("Kannada") == true) kannadaStrings else englishStrings
    val authManager = remember { FirebaseAuthManager() }
    val context = LocalContext.current

    CompositionLocalProvider(LocalAppStrings provides strings) {
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        if (showPaymentPage) {
            PaymentWebView(
                cartItems = cartItems,
                onDismiss = { showPaymentPage = false },
                onPaymentSuccess = {
                    showPaymentPage = false
                    cartItems = emptyList()
                    currentDestination = AppDestinations.HOME
                    Toast.makeText(context, "Order Placed Successfully!", Toast.LENGTH_LONG).show()
                }
            )
            return@CompositionLocalProvider
        }

        if (userLanguage == null) {
            LanguageSelectionScreen(onLanguageSelected = { userLanguage = it })
            return@CompositionLocalProvider
        }

        val discountPercent = remember(cartItems) {
            if (cartItems.any { 
                val days = it.medicine.expiryDaysLeft
                days != null && days in 0..7 
            }) {
                (10..25).random()
            } else 0
        }
        
        val medicineList = remember { mutableStateListOf(*mockMedicines.toTypedArray()) }

        var loggedInUser by remember { mutableStateOf<User?>(null) }
        val isLoggedIn = loggedInUser != null
        val isAgentMode = loggedInUser?.isAgent == true

        val onAddToCart: (Medicine) -> Unit = { medicine ->
            val existing = cartItems.find { it.medicine.name == medicine.name }
            cartItems = if (existing != null) {
                cartItems.map {
                    if (it.medicine.name == medicine.name) it.copy(quantity = it.quantity + 1) else it
                }
            } else {
                cartItems + CartItem(medicine)
            }
            scope.launch {
                val addedText = if (userLanguage?.contains("Kannada") == true) "ಕಾರ್ಟ್‌ಗೆ ಸೇರಿಸಲಾಗಿದೆ" else "added to cart"
                snackbarHostState.showSnackbar("✅ ${medicine.name} $addedText")
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            NavigationSuiteScaffold(
                navigationSuiteItems = {
                    AppDestinations.entries.filter { it != AppDestinations.LOGIN }.forEach { dest ->
                        item(
                            icon = {
                                if (dest == AppDestinations.CART && cartItems.isNotEmpty()) {
                                    BadgedBox(badge = { Badge { Text(cartItems.sumOf { it.quantity }.toString()) } }) {
                                        Icon(painterResource(dest.icon), contentDescription = dest.getLabel(strings))
                                    }
                                } else {
                                    Icon(painterResource(dest.icon), contentDescription = dest.getLabel(strings))
                                }
                            },
                            label = { Text(dest.getLabel(strings)) },
                            selected = dest == currentDestination,
                            onClick = { currentDestination = dest }
                        )
                    }
                }
            ) {
                when (currentDestination) {
                    AppDestinations.HOME       -> HomeScreen(
                        medicines = medicineList,
                        onAddToCart = onAddToCart,
                        onUserClick = { currentDestination = AppDestinations.LOGIN },
                        isLoggedIn = isLoggedIn,
                        userName = loggedInUser?.name ?: "",
                        userLanguage = userLanguage!!,
                        onLanguageChange = { userLanguage = null },
                        onLogout = {
                            authManager.logout()
                            loggedInUser = null
                            cartItems = emptyList()
                        }
                    )
                    AppDestinations.EMERGENCY  -> LifeSavingScreen(medicineList, onAddToCart)
                    AppDestinations.CART       -> CartScreen(
                        cartItems = cartItems,
                        discountPercent = discountPercent,
                        onIncrease = { med ->
                            cartItems = cartItems.map { if (it.medicine == med) it.copy(quantity = it.quantity + 1) else it }
                        },
                        onDecrease = { med ->
                            cartItems = cartItems.mapNotNull {
                                if (it.medicine == med) {
                                    if (it.quantity > 1) it.copy(quantity = it.quantity - 1) else null
                                } else it
                            }
                        },
                        onClearCart = { cartItems = emptyList() },
                        onCheckout = { 
                            if (isLoggedIn) {
                                showPaymentPage = true
                            } else {
                                currentDestination = AppDestinations.LOGIN
                                scope.launch { snackbarHostState.showSnackbar("Please login to checkout") }
                            }
                        }
                    )
                    AppDestinations.LOGIN      -> UnifiedLoginScreen(
                        isLoggedIn = isLoggedIn,
                        isAgentMode = isAgentMode,
                        userName = loggedInUser?.name ?: "",
                        medicines = medicineList,
                        authManager = authManager,
                        onLoginSuccess = { user ->
                            loggedInUser = user
                            currentDestination =
                                if (user.isAgent) AppDestinations.LOGIN
                                else AppDestinations.HOME
                        },
                        onLogout = {
                            authManager.logout()
                            loggedInUser = null
                            currentDestination = AppDestinations.HOME
                        }
                    )
                }
            }
            
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
            )
        }
    }
}

@Composable
fun HomeScreen(
    medicines: List<Medicine>,
    onAddToCart: (Medicine) -> Unit,
    onUserClick: () -> Unit,
    isLoggedIn: Boolean,
    userName: String,
    userLanguage: String,
    onLanguageChange: () -> Unit,
    onLogout: () -> Unit
) {
    val strings = LocalAppStrings.current
    var query by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        isLoading = false
    }

    val displayList = remember(query, medicines.size) {
        if (query.isBlank()) medicines
        else medicines.filter { it.name.contains(query, ignoreCase = true) }
    }

    if (isLoading) {
        LoadingAnimation()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFDF5F5))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_home),
                        contentDescription = null,
                        tint = Color(0xFFA52A2A),
                        modifier = Modifier.size(36.dp).padding(end = 8.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            strings.appName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFA52A2A),
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            strings.subtitle,
                            fontSize = 12.sp,
                            color = Color.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                    if (isLoggedIn) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(userName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Row {
                                TextButton(onClick = onLanguageChange, contentPadding = PaddingValues(0.dp)) {
                                    val currentLang = if (userLanguage.contains("Kannada")) "ಕನ್ನಡ" else "Eng"
                                    Text("${strings.langLabel}: $currentLang", fontSize = 11.sp, color = Color.Blue)
                                }
                                Spacer(Modifier.width(8.dp))
                                TextButton(onClick = onLogout, contentPadding = PaddingValues(0.dp)) {
                                    Text(strings.logout, fontSize = 11.sp, color = Color(0xFFD81B60))
                                }
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.End) {
                            TextButton(onClick = onLanguageChange, contentPadding = PaddingValues(0.dp)) {
                                    Text("${strings.langLabel}: $userLanguage", fontSize = 10.sp, color = Color.Gray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    strings.loginPrompt,
                                    fontSize = 11.sp,
                                    color = Color(0xFFA52A2A),
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = onUserClick) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_account_box),
                                        contentDescription = "User Login",
                                        tint = Color(0xFFA52A2A)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(strings.searchPlaceholder, color = Color.Black) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {}),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF800000),
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }

            Spacer(Modifier.height(12.dp))

            if (displayList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(strings.noResults.format(query), color = Color.Black)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayList) { med ->
                        val colorIdx = medicines.indexOf(med).coerceAtLeast(0) % catalogueColors.size
                        CataloguePillCard(med, catalogueColors[colorIdx], onAddToCart)
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingAnimation() {
    val strings = LocalAppStrings.current
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rotation"
    )

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.ic_favorite),
                contentDescription = "Loading",
                modifier = Modifier.size(48.dp).rotate(angle),
                tint = Color(0xFFD81B60)
            )
            Spacer(Modifier.height(16.dp))
            Text(strings.loading, color = Color.Black, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun CataloguePillCard(med: Medicine, gradientColors: List<Color>, onAddToCart: (Medicine) -> Unit) {
    val strings = LocalAppStrings.current
    val isExpired = med.isExpired
    Box(
        modifier = Modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .then(if (isExpired) Modifier.alpha(0.6f) else Modifier)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = if (isExpired) Color(0xFFF5F5F5) else Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(Brush.verticalGradient(colors = if (isExpired) listOf(Color.LightGray, Color.Gray) else gradientColors)),
                    contentAlignment = Alignment.Center
                ) {
                    if (med.isLifeSaving) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .background(Color.Red, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(strings.lifeSaving, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (!med.inStock || isExpired) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
                    }
                    Text(med.emoji, fontSize = 40.sp)
                    if (isExpired) {
                        Text(
                            strings.expired,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .rotate(-15f)
                                .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = med.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1C1C1C),
                        maxLines = 2,
                        textDecoration = if (isExpired) TextDecoration.LineThrough else null
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "₹${med.price}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = if (isExpired) Color.Gray else Color(0xFFA52A2A),
                        textDecoration = if (isExpired) TextDecoration.LineThrough else null
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${med.distanceKm}km · ${med.village} (${med.shopName})",
                        fontSize = 12.sp,
                        color = Color.Black,
                        textDecoration = TextDecoration.Underline
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(text = "show location (work in progress)", fontSize = 8.sp, color = Color(0xFFD81B60), textDecoration = TextDecoration.Underline)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isExpired) Color.Gray.copy(alpha = 0.2f)
                                    else if (med.inStock) Color(0xFFE8F5E9)
                                    else Color(0xFFFFEBEE),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                if (isExpired) strings.expired
                                else if (med.inStock) strings.inStock
                                else strings.outOfStock,
                                fontSize = 10.sp,
                                color = if (isExpired) Color.DarkGray else if (med.inStock) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (med.expiryDaysLeft != null && !isExpired) {
                            Text("⚠️ ${med.expiryDaysLeft}d", fontSize = 10.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { onAddToCart(med) },
                        enabled = med.inStock && !isExpired,
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isExpired) Color.Gray else Color(0xFFA52A2A),
                            disabledContainerColor = if (isExpired) Color.Gray.copy(alpha = 0.5f) else Color(0xFFA52A2A).copy(alpha = 0.5f)
                        )
                    ) {
                        Text(if (isExpired) strings.expired else strings.addToCart, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun LifeSavingScreen(medicines: List<Medicine>, onAddToCart: (Medicine) -> Unit) {
    val strings = LocalAppStrings.current
    val lifeSaving = medicines.filter { it.isLifeSaving }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("🚨 ${strings.emergencyStock}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(strings.emergencySubtitle, color = Color.Black, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(lifeSaving) { med -> MedicineCard(med, onAddToCart) }
        }
    }
}

@Composable
fun CartScreen(
    cartItems: List<CartItem>,
    discountPercent: Int,
    onIncrease: (Medicine) -> Unit,
    onDecrease: (Medicine) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: () -> Unit
) {
    val strings = LocalAppStrings.current
    val subtotal = cartItems.sumOf { it.medicine.price * it.quantity }
    val discountAmount = (subtotal * discountPercent) / 100
    val total = subtotal - discountAmount

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("🛒 ${strings.cartTitle}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (cartItems.isNotEmpty()) {
                TextButton(onClick = onClearCart) {
                    Text(strings.clearCart, color = Color(0xFFD81B60))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (cartItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(strings.emptyCart, color = Color.Black)
            }
        } else {
            Column(Modifier.weight(1f)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(cartItems) { item -> CartItemRow(item, onIncrease, onDecrease) }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (discountPercent > 0) {
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp)).padding(12.dp)) {
                    Text(strings.discountApplied.format(discountPercent), color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(Modifier.padding(16.dp)) {
                    if (discountPercent > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(strings.subtotal, fontSize = 14.sp, color = Color.Black)
                            Text("₹$subtotal", fontSize = 14.sp, color = Color.Black)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(strings.discount, fontSize = 14.sp, color = Color.Black)
                            Text("-₹$discountAmount", fontSize = 14.sp, color = Color.Black)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(strings.totalAmount, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("₹$total", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFA52A2A))
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onCheckout, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA52A2A))) {
                        Text(strings.checkout)
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemRow(item: CartItem, onIncrease: (Medicine) -> Unit, onDecrease: (Medicine) -> Unit) {
    val isExpired = item.medicine.isExpired
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isExpired) 0.6f else 1f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isExpired) Color(0xFFF5F5F5) else Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(item.medicine.emoji, fontSize = 32.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.medicine.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textDecoration = if (isExpired) TextDecoration.LineThrough else null
                )
                Text("₹${item.medicine.price} each", fontSize = 12.sp, color = Color.Black)
                if (isExpired) {
                    Text("EXPIRED", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onDecrease(item.medicine) }) { Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                Text(item.quantity.toString(), fontWeight = FontWeight.Bold)
                IconButton(onClick = { onIncrease(item.medicine) }, enabled = !isExpired) { Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun UnifiedLoginScreen(
    isLoggedIn: Boolean,
    isAgentMode: Boolean,
    userName: String,
    medicines: MutableList<Medicine>,
    authManager: AuthManager,
    onLoginSuccess: (User) -> Unit,
    onLogout: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var isSignUpMode by remember { mutableStateOf(false) }
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    
    var error by remember { mutableStateOf("") }
    var showAgentLogin by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Failed login attempts logic
    var failedAttempts by remember { mutableStateOf(0) }
    var lockoutTime by remember { mutableStateOf<LocalDateTime?>(null) }

    val isLockedOut = lockoutTime?.let {
        java.time.Duration.between(LocalDateTime.now(), it).toMinutes() > 0
    } ?: false

    if (!isLoggedIn) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when {
                    isSignUpMode -> "📝 ${strings.signUpTitle}"
                    showAgentLogin -> "🛡️ ${strings.agentLogin}"
                    else -> "👤 ${strings.userLogin}"
                },
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(24.dp))
            
            if (isSignUpMode) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.nameHint) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading
                )
                Spacer(Modifier.height(12.dp))
            }
            
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(strings.emailLabel) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )
            Spacer(Modifier.height(12.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(strings.passwordLabel) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                enabled = !isLoading
            )
            
            if (isSignUpMode) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(strings.addressLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
            }
            
            if (error.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = Color.Red, fontSize = 13.sp)
            }

            if (isLockedOut) {
                val timeLeft = java.time.Duration.between(LocalDateTime.now(), lockoutTime).toMinutes() + 1
                Text("Locked out. Try again in $timeLeft mins.", color = Color.Red, fontSize = 12.sp)
            }
            
            Spacer(Modifier.height(24.dp))
            
            Button(
                onClick = {
                    if (isLockedOut || isLoading) return@Button
                    
                    isLoading = true
                    error = ""

                    if (isSignUpMode) {
                        if (name.isBlank() || email.isBlank() || password.isBlank()) {
                            error = "Please fill all fields"
                            isLoading = false
                        } else {
                            authManager.signUp(User(name, email, address, false), password) { success, msg ->
                                isLoading = false

                                if (success) {
                                    Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess(User(name, email, address, false))
                                } else {
                                    error = msg ?: "Sign up failed"
                                }
                            }
                        }
                    } else if (showAgentLogin) {
                        if (email == "agent@shop.com" && password == "admin") {
                            onLoginSuccess(User("Agent One", email, "Main Office", true))
                        } else {
                            failedAttempts++
                            if (failedAttempts >= 3) {
                                lockoutTime = LocalDateTime.now().plusMinutes(10)
                                failedAttempts = 0
                                Toast.makeText(context, "Incorrect login attempts exceeded, please try again after 10 minutes.", Toast.LENGTH_LONG).show()
                            } else {
                                error = "Try agent@shop.com / admin (Attempt $failedAttempts/3)"
                            }
                            isLoading = false
                        }
                    } else {
                        authManager.login(email, password) { user, msg ->
                            if (user != null) {
                                onLoginSuccess(user)
                                failedAttempts = 0
                                Toast.makeText(context, "Successfully logged in!", Toast.LENGTH_SHORT).show()
                            } else {
                                failedAttempts++
                                if (failedAttempts >= 3) {
                                    lockoutTime = LocalDateTime.now().plusMinutes(10)
                                    failedAttempts = 0
                                    Toast.makeText(context, "Incorrect login attempts exceeded, please try again after 10 minutes.", Toast.LENGTH_LONG).show()
                                } else {
                                    error = msg ?: "Login failed (Attempt $failedAttempts/3)"
                                }
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA52A2A)),
                enabled = !isLockedOut && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(if (isSignUpMode) strings.registerButton else strings.loginButton)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            if (!showAgentLogin) {
                TextButton(onClick = { isSignUpMode = !isSignUpMode; error = "" }) {
                    Text(if (isSignUpMode) strings.backToLogin else strings.signUpPrompt)
                }
            }
            
            if (!isSignUpMode) {
                TextButton(onClick = { showAgentLogin = !showAgentLogin; error = "" }) {
                    Text(if (showAgentLogin) strings.userLogin else "${strings.agentLogin} 🛡️")
                }
            }
        }
    } else if (isAgentMode) {
        AgentDashboard(medicines, userName, onLogout)
    }
}

@Composable
fun AgentDashboard(medicines: MutableList<Medicine>, userName: String, onLogout: () -> Unit) {
    val strings = LocalAppStrings.current
    var showDialog by remember { mutableStateOf(false) }
    var editingMed by remember { mutableStateOf<Medicine?>(null) }
    if (showDialog) {
        MedicineEditDialog(medicine = editingMed, onDismiss = { showDialog = false; editingMed = null }, onSave = { newMed ->
            if (editingMed != null) {
                val idx = medicines.indexOf(editingMed)
                if (idx != -1) medicines[idx] = newMed
            } else medicines.add(newMed)
            showDialog = false
            editingMed = null
        })
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🛡️ ${strings.agentDashboard}: $userName", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onLogout) { Icon(painterResource(R.drawable.ic_account_box), "Logout") }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { showDialog = true; editingMed = null }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA52A2A))) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text(strings.addNewMed)
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(medicines) { med -> AgentMedicineCard(med = med, onEdit = { editingMed = med; showDialog = true }, onDelete = { medicines.remove(med) }) }
        }
    }
}

@Composable
fun AgentMedicineCard(med: Medicine, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isExpired = med.isExpired
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isExpired) 0.7f else 1f),
        colors = CardDefaults.cardColors(containerColor = if (isExpired) Color(0xFFF5F5F5) else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(med.emoji, fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    med.name,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (isExpired) TextDecoration.LineThrough else null
                )
                Text("₹${med.price} • ${med.village} (${med.shopName})", fontSize = 12.sp, color = Color.Black)
                if (isExpired) {
                    Text("EXPIRED", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit", tint = Color.Blue) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = Color.Red) }
        }
    }
}

@Composable
fun MedicineEditDialog(medicine: Medicine?, onDismiss: () -> Unit, onSave: (Medicine) -> Unit) {
    val strings = LocalAppStrings.current
    var name by remember { mutableStateOf(medicine?.name ?: "") }
    var village by remember { mutableStateOf(medicine?.village ?: "Village A") }
    var shopName by remember { mutableStateOf(medicine?.shopName ?: "Shop A1") }
    var price by remember { mutableStateOf(medicine?.price?.toString() ?: "0") }
    var inStock by remember { mutableStateOf(medicine?.inStock ?: true) }
    var isLifeSaving by remember { mutableStateOf(medicine?.isLifeSaving ?: false) }

    var issueDate by remember { mutableStateOf(medicine?.issueDate ?: LocalDate.now()) }
    var expiryDate by remember { mutableStateOf(medicine?.expiryDate ?: LocalDate.now().plusMonths(6)) }

    var showIssueDatePicker by remember { mutableStateOf(false) }
    var showExpiryDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    if (showIssueDatePicker) {
        MyDatePickerDialog(
            initialDate = issueDate,
            restrictPast = true,
            onDateSelected = { issueDate = it; showIssueDatePicker = false },
            onDismiss = { showIssueDatePicker = false }
        )
    }

    if (showExpiryDatePicker) {
        MyDatePickerDialog(
            initialDate = expiryDate,
            restrictPast = true,
            onDateSelected = { expiryDate = it; showExpiryDatePicker = false },
            onDismiss = { showExpiryDatePicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (medicine == null) strings.addNewMed else strings.editMed) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings.nameLabel) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = village, onValueChange = { village = it }, label = { Text(strings.villageLabel) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = shopName, onValueChange = { shopName = it }, label = { Text(strings.shopLabel) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text(strings.priceLabel) }, modifier = Modifier.fillMaxWidth())

                // Date selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.issueDateLabel, fontWeight = FontWeight.Medium)
                    TextButton(onClick = { showIssueDatePicker = true }) {
                        Text(issueDate.format(dateFormatter))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.expiryDateLabel, fontWeight = FontWeight.Medium)
                    TextButton(onClick = { showExpiryDatePicker = true }) {
                        Text(expiryDate.format(dateFormatter), color = if (expiryDate.isBefore(LocalDate.now())) Color.Red else Color.Unspecified)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = inStock, onCheckedChange = { inStock = it })
                    Text(strings.inStock)
                    Spacer(Modifier.width(16.dp))
                    Checkbox(checked = isLifeSaving, onCheckedChange = { isLifeSaving = it })
                    Text(strings.lifeSaving)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    Medicine(
                        name, village, shopName, 1, inStock,
                        isLifeSaving = isLifeSaving,
                        price = price.toIntOrNull() ?: 0,
                        issueDate = issueDate,
                        expiryDate = expiryDate
                    )
                )
            }) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDatePickerDialog(
    initialDate: LocalDate,
    restrictPast: Boolean = false,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        selectableDates = if (restrictPast) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // Normalize today to UTC midnight to match DatePicker's internal behavior
                    val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    return utcTimeMillis >= today
                }
            }
        } else {
            DatePickerDefaults.AllDates
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let {
                    val date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    onDateSelected(date)
                }
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun PaymentWebView(
    cartItems: List<CartItem>,
    onDismiss: () -> Unit,
    onPaymentSuccess: () -> Unit
) {
    val total = cartItems.sumOf { it.medicine.price * it.quantity }
    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body { font-family: sans-serif; padding: 20px; color: #333; }
                h2 { color: #A52A2A; border-bottom: 2px solid #eee; padding-bottom: 10px; }
                .option { margin-bottom: 15px; padding: 12px; border: 1px solid #ddd; border-radius: 8px; cursor: pointer; }
                .option:hover { background-color: #f9f9f9; }
                .info { font-size: 0.9em; color: #666; margin-top: 10px; background: #fff8f8; padding: 10px; border-radius: 5px; }
                .total { font-size: 1.2em; font-weight: bold; margin: 20px 0; color: #A52A2A; }
                button { background: #A52A2A; color: white; border: none; padding: 12px 20px; border-radius: 5px; width: 100%; font-size: 1em; cursor: pointer; }
                .toggle-container { display: flex; align-items: center; margin-bottom: 20px; gap: 10px; }
                .badge { font-size: 0.7em; background: #eee; padding: 2px 6px; border-radius: 4px; vertical-align: middle; }
            </style>
        </head>
        <body>
            <h2>Grama-Sanjeevini Checkout</h2>
            <div class="total">Total Payable: ₹$total</div>
            
            <div class="toggle-container">
                <input type="checkbox" id="deliveryToggle" onchange="updateInfo()"> 
                <label for="deliveryToggle">Enable Home Delivery</label>
            </div>

            <div class="option">
                <input type="radio" name="payment" id="upi" value="upi" checked onchange="updateInfo()">
                <label for="upi"><b>BHIM / UPI</b> <span class="badge">No extra tax</span></label>
            </div>
            <div class="option">
                <input type="radio" name="payment" id="netbank" value="netbank" onchange="updateInfo()">
                <label for="netbank"><b>Net Banking</b> <span class="badge">+ Taxes</span></label>
            </div>
            <div class="option">
                <input type="radio" name="payment" id="card" value="card" onchange="updateInfo()">
                <label for="card"><b>Credit / Debit Card</b> <span class="badge">+ Taxes</span></label>
            </div>
            <div class="option">
                <input type="radio" name="payment" id="cash" value="cash" onchange="updateInfo()">
                <label for="cash"><b>Cash on Delivery</b></label>
            </div>

            <div id="dynamicInfo" class="info">
                BHIM/UPI payments do not include any extra taxes or charges.
            </div>

            <br>
            <button onclick="processPayment()">Place Order</button>
            <p style="text-align: center;"><a href="#" onclick="window.Android.dismiss()" style="color: #666; font-size: 0.8em;">Cancel & Go Back</a></p>

            <script>
                function updateInfo() {
                    const isDelivery = document.getElementById('deliveryToggle').checked;
                    const payment = document.querySelector('input[name="payment"]:checked').value;
                    let info = "";
                    
                    if (isDelivery) info += "• Delivery charges apply for delivery orders.<br>";
                    
                    if (payment === 'upi') {
                        info += "• BHIM/UPI payments do not include any extra taxes or charges.";
                    } else if (payment === 'netbank' || payment === 'card') {
                        info += "• Additional taxes apply only for Net Banking and Credit/Debit Card payments.";
                    } else {
                        info += "• Pay directly at the time of collection/delivery.";
                    }
                    
                    document.getElementById('dynamicInfo').innerHTML = info;
                }

                function processPayment() {
                    alert("Processing payment... Simulated success!");
                    window.Android.onSuccess();
                }
            </script>
        </body>
        </html>
    """.trimIndent()

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Add, contentDescription = "Back", modifier = Modifier.rotate(45f))
                    }
                }
            )
        }
    ) { padding ->
        AndroidView(
            modifier = Modifier.padding(padding).fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    webViewClient = WebViewClient()
                    addJavascriptInterface(object {
                        @android.webkit.JavascriptInterface
                        fun onSuccess() { onPaymentSuccess() }
                        @android.webkit.JavascriptInterface
                        fun dismiss() { onDismiss() }
                    }, "Android")
                    loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                }
            }
        )
    }
}

@Composable
fun LanguageSelectionScreen(onLanguageSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_home),
            contentDescription = null,
            tint = Color(0xFFA52A2A),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("Grama-Sanjeevini", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA52A2A))
        Spacer(Modifier.height(32.dp))
        Text("Select Language / ಭಾಷೆಯನ್ನು ಆಯ್ಕೆ ಮಾಡಿ", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onLanguageSelected("English") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA52A2A))
        ) {
            Text("English")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { onLanguageSelected("ಕನ್ನಡ (Kannada)") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ಕನ್ನಡ (Kannada)", color = Color(0xFFA52A2A))
        }
    }
}

@Composable
fun MedicineCard(med: Medicine, onAddToCart: (Medicine) -> Unit) {
    val strings = LocalAppStrings.current
    val isExpired = med.isExpired
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isExpired) 0.6f else 1f),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = if (isExpired) Color(0xFFF5F5F5) else MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        med.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        textDecoration = if (isExpired) TextDecoration.LineThrough else null
                    )
                    if (med.isLifeSaving) {
                        Spacer(Modifier.width(6.dp))
                        Box(modifier = Modifier.background(Color.Red, RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                            Text(strings.lifeSaving, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(text = "📍 ${med.village} (${med.shopName}) · ${strings.kmAway.format(med.distanceKm)}", fontSize = 17.sp, color = Color.Black, textDecoration = TextDecoration.Underline)
                Spacer(Modifier.height(4.dp))
                Text(text = "show location (work in progress)", fontSize = 9.sp, color = Color(0xFFD81B60), textDecoration = TextDecoration.Underline)
                if (isExpired) {
                    Text("❌ ${strings.expired}", fontSize = 12.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                } else if (med.expiryDaysLeft != null) {
                    Text("⚠️ ${strings.expires.format(med.expiryDaysLeft)}", fontSize = 12.sp, color = Color(0xFFB71C1C))
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onAddToCart(med) },
                    enabled = med.inStock && !isExpired,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isExpired) Color.Gray else Color(0xFFA52A2A))
                ) {
                    Text(if (isExpired) strings.expired else strings.addToCart, fontSize = 11.sp)
                }
            }
            Box(
                modifier = Modifier
                    .background(
                        if (isExpired) Color.Gray.copy(alpha = 0.2f)
                        else if (med.inStock) Color(0xFFD4EDDA)
                        else Color(0xFFF8D7DA),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    if (isExpired) "❌ ${strings.expired}"
                    else if (med.inStock) "✅ ${strings.inStock}"
                    else "❌ ${strings.outOfStock}",
                    fontSize = 12.sp,
                    color = if (isExpired) Color.DarkGray else if (med.inStock) Color(0xFF155724) else Color(0xFF721C24),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
