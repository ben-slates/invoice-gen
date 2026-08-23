package com.invoicegen.android

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.util.Calendar
import kotlinx.coroutines.delay

// ── Theme colours ────────────────────────────────────────────────────────────
private val Primary = Color(0xFF3F46E5)
private val PrimaryDark = Color(0xFF3035B8)
private val PrimaryLight = Color(0xFF6366F1)
private val SoftBlue = Color(0xFFEEF2FF)
private val Background = Color(0xFFF8FAFC)
private val Surface = Color(0xFFFFFFFF)
private val PrimaryText = Color(0xFF0F172A)
private val SecondaryText = Color(0xFF64748B)
private val MutedText = Color(0xFF94A3B8)
private val Border = Color(0xFFE2E8F0)
private val Success = Color(0xFF16A34A)
private val SuccessBg = Color(0xFFDCFCE7)
private val Warning = Color(0xFFF59E0B)
private val WarningBg = Color(0xFFFEF3C7)
private val Danger = Color(0xFFEF4444)
private val DangerBg = Color(0xFFFEE2E2)

// ── Activity ─────────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Primary, background = Background,
                    surface = Surface, onSurface = PrimaryText,
                    outline = Border
                )
            ) { InvoiceGenApp() }
        }
    }
}

// ── Root composable with navigation ──────────────────────────────────────────
@Composable
private fun InvoiceGenApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { InvoiceGenViewModel(context.applicationContext) }
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route.orEmpty()
    
    var showSplash by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        delay(1500)
        showSplash = false
    }

    if (showSplash) {
        SplashScreen()
        return
    }

    if (state.profile.businessName.isBlank() && route != "onboarding") {
        LaunchedEffect(Unit) { nav.navigate("onboarding") { popUpTo(0) } }
    }

    val showBar = route == "home" || route == "invoices" || route == "clients" || route == "profile"

    Scaffold(
        containerColor = Background,
        bottomBar = {
            if (showBar) NavigationBar(containerColor = Surface, tonalElevation = 8.dp) {
                NavigationBarItem(selected = route == "home", onClick = { nav.navigate("home") { launchSingleTop = true; popUpTo("home") { saveState = true }; restoreState = true } }, icon = { Icon(Icons.Outlined.Home, "Home", tint = if (route == "home") Primary else MutedText) })
                NavigationBarItem(selected = route == "invoices", onClick = { nav.navigate("invoices") { launchSingleTop = true; popUpTo("home") { saveState = true }; restoreState = true } }, icon = { Icon(Icons.Outlined.Description, "Invoices", tint = if (route == "invoices") Primary else MutedText) })
                NavigationBarItem(selected = false, onClick = { nav.navigate("editor?template=&id=") }, icon = { Box(Modifier.size(48.dp).background(Primary, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Add, "Create", tint = Color.White, modifier = Modifier.size(28.dp)) } })
                NavigationBarItem(selected = route == "clients", onClick = { nav.navigate("clients") { launchSingleTop = true; popUpTo("home") { saveState = true }; restoreState = true } }, icon = { Icon(Icons.Outlined.Groups, "Clients", tint = if (route == "clients") Primary else MutedText) })
                NavigationBarItem(selected = route == "profile", onClick = { nav.navigate("profile") { launchSingleTop = true; popUpTo("home") { saveState = true }; restoreState = true } }, icon = { Icon(Icons.Outlined.Menu, "More", tint = if (route == "profile") Primary else MutedText) })
            }
        }
    ) { padding ->
        NavHost(nav, if (state.profile.businessName.isBlank()) "onboarding" else "home", Modifier.padding(padding)) {
            composable("onboarding") { OnboardingScreen(state) { nav.navigate("home") { popUpTo(0) } } }
            composable("home") { HomeScreen(state) { nav.navigate(it) } }
            composable("invoices") { InvoicesScreen(state, { nav.navigate("editor?template=&id=") }) { nav.navigate("preview/$it") } }
            composable("clients") { ClientsScreen(state) }
            composable("profile") { SettingsScreen(nav, state) }
            composable("settings/profile") { BusinessProfileScreen(state) { nav.popBackStack() } }
                        composable("settings/help") { HelpScreen { nav.popBackStack() } }
            
            composable("settings/terms") { TermsScreen { nav.popBackStack() } }
            composable("settings/privacy") { PrivacyScreen { nav.popBackStack() } }
            composable("settings/about") { AboutScreen({ nav.popBackStack() }, { nav.navigate(it) }) }
            composable("settings/template") { TemplateSelectionScreen(state) { nav.popBackStack() } }
            composable("editor?template={template}&id={id}", arguments = listOf(
                navArgument("template") { type = NavType.StringType; defaultValue = "" },
                navArgument("id") { type = NavType.StringType; defaultValue = "" }
            )) { e ->
                EditorScreen(state, e.arguments?.getString("template").orEmpty(), e.arguments?.getString("id").orEmpty(), { nav.popBackStack() }) { nav.navigate("preview/$it") }
            }
            composable("preview/{id}") { e ->
                PreviewScreen(state, e.arguments?.getString("id").orEmpty(), { nav.popBackStack() }, { nav.navigate("editor?id=$it") }) { nav.popBackStack() }
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(Primary), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painter = painterResource(id = R.drawable.logo), contentDescription = "Logo", modifier = Modifier.size(100.dp))
            Spacer(Modifier.height(16.dp))
            Text("InvoiceGen", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text("Professional invoices, made simple.", color = SoftBlue, fontSize = 14.sp)
        }
    }
}

@Composable
private fun OnboardingScreen(state: InvoiceGenViewModel, onComplete: () -> Unit) {
    var name by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Background).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painter = painterResource(id = R.drawable.logo), contentDescription = "Logo", modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(24.dp))
        Text("Welcome to InvoiceGen", color = PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("What should we call your business?", color = SecondaryText, fontSize = 16.sp)
        Spacer(Modifier.height(32.dp))
        Field(name, { name = it }, "Business Name", "e.g. Acme Corp")
        Spacer(Modifier.height(24.dp))
        Button(onClick = { state.saveProfile(state.profile.copy(businessName = name.ifBlank { "My Business" })); onComplete() },
            modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
            Text("Continue", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// ── Home (dashboard) ─────────────────────────────────────────────────────────
@Composable
private fun HomeScreen(state: InvoiceGenViewModel, onNavigate: (String) -> Unit) {
    val totalInvoices = state.invoices.size
                val totalRevenue = state.invoices.sumOf { it.total }

    Column(Modifier.fillMaxSize().background(Background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Hello, ${state.profile.ownerName.ifBlank { "there" }}", color = PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Here's your business overview", color = SecondaryText, fontSize = 14.sp)
            }
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard("Total Invoices", totalInvoices.toString(), Icons.Outlined.Description, Primary, Modifier.weight(1f))
                    SummaryCard("Revenue", currency(totalRevenue, state.profile.currency), Icons.Outlined.AttachMoney, Success, Modifier.weight(1f))
                }
            }


            // Recent invoices
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Recent Invoices", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("See All", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onNavigate("invoices") })
                }
                if (state.invoices.isEmpty()) EmptyState("No invoices yet", "Create your first invoice and start tracking payments.", "templates", onNavigate)
                else state.invoices.take(5).forEach { inv -> InvoiceRow(inv, state) { onNavigate("preview/${inv.id}") } }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Invoices list ────────────────────────────────────────────────────────────
@Composable
private fun InvoicesScreen(state: InvoiceGenViewModel, onCreate: () -> Unit, onPreview: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    
    val visible = state.invoices.filter { inv ->
        val client = state.clients.firstOrNull { it.id == inv.clientId }
        searchQuery.isBlank() || inv.number.contains(searchQuery, ignoreCase = true) || (client?.name?.contains(searchQuery, ignoreCase = true) == true)
    }.sortedByDescending { it.issueDate }

    Column(Modifier.fillMaxSize().background(Background)) {
        TopBar("Invoices")
        
        OutlinedTextField(
            value = searchQuery, onValueChange = { searchQuery = it },
            placeholder = { Text("Search by number or client...", color = MutedText) },
            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = MutedText) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Border, focusedBorderColor = Primary, unfocusedContainerColor = Surface, focusedContainerColor = Surface)
        )
        
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            if (visible.isEmpty()) item { EmptyState("No invoices found", "Try a different search or create one.", "invoices", { _ -> onCreate() }) }
            else items(visible) { inv -> InvoiceRow(inv, state) { onPreview(inv.id) } }
        }
    }
}

// ── Clients list ─────────────────────────────────────────────────────────────
@Composable
private fun ClientsScreen(state: InvoiceGenViewModel) {
    var adding by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(Background)) {
        TopBar("Clients", rightIcon = if (adding) Icons.Outlined.Close else Icons.Outlined.Add, onRightClick = { adding = !adding })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (adding) {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("New Client", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Field(name, { name = it }, "Client Name", "Wayne Enterprises")
                        Field(email, { email = it }, "Email", "billing@wayne.com", keyboardType = KeyboardType.Email)
                        Field(phone, { phone = it }, "Phone", "+1 234 567 8900", keyboardType = KeyboardType.Phone)
                        Button(onClick = {
                            if (name.isNotBlank()) { state.saveClient(Client(name = name.trim(), email = email.trim(), phone = phone.trim())); name = ""; email = ""; phone = ""; adding = false }
                        }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("Save Client", fontWeight = FontWeight.Bold) }
                    }
                }
            }

            if (state.clients.isEmpty()) EmptyState("No clients yet", "Add clients to easily bill them later.", null, null)
            else state.clients.forEach { client ->
                val clientInvoices = state.invoices.filter { it.clientId == client.id }
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(client.name); Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(client.name, color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(client.email.ifBlank { client.phone.ifBlank { "No contact details" } }, color = SecondaryText, fontSize = 13.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${clientInvoices.size} invoices", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { state.deleteClient(client.id) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Outlined.Delete, "Delete", tint = Danger, modifier = Modifier.size(20.dp)) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}




@Composable
private fun TermsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Background)) {
        TopBar("Terms of Service", onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text("Terms of Service", color = PrimaryText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Text("Last updated: Today\n\n1. Acceptance of Terms\nBy accessing and using InvoiceGen, you accept and agree to be bound by the terms and provision of this agreement.\n\n2. Description of Service\nInvoiceGen provides users with tools to generate, manage, and export invoices completely offline on their device.\n\n3. User Data\nAll data entered into InvoiceGen is stored locally on your device. We do not transmit, collect, or store your business or client data on any external servers.", color = SecondaryText, fontSize = 14.sp)
        }
    }
}

@Composable
private fun PrivacyScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Background)) {
        TopBar("Privacy Policy", onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text("Privacy Policy", color = PrimaryText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Text("Last updated: Today\n\n1. Information Collection\nInvoiceGen is an offline-first application. We do not collect, transmit, or store any personal information, business details, or client data on our servers. All information you enter remains strictly on your device.\n\n2. Data Security\nYour data is secured by the Android operating system's sandbox. If you uninstall the app or clear its data, all invoice information will be permanently deleted.\n\n3. Changes to Policy\nWe reserve the right to modify this privacy policy at any time. Changes will be updated within the app.", color = SecondaryText, fontSize = 14.sp)
        }
    }
}


// ── Profile (settings) ──────────────────────────────────────────────
@Composable
private fun SettingsScreen(nav: androidx.navigation.NavController, state: InvoiceGenViewModel) {
    Column(Modifier.fillMaxSize().background(Background)) {
        TopBar("Settings")
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            
            Text("General", color = MutedText, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsRow(Icons.Outlined.Business, "Business Profile", "Logo, Name, Currency, Tax", onClick = { nav.navigate("settings/profile") })
                }
            }
            
            Text("Appearance", color = MutedText, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsRow(Icons.Outlined.Palette, "Invoice Template", "Select default invoice style", onClick = { nav.navigate("settings/template") })
                }
            }
            
            Text("Support", color = MutedText, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsRow(Icons.Outlined.HelpOutline, "Help & Support", "FAQs, Contact Us", onClick = { nav.navigate("settings/help") })
                    HorizontalDivider(color = Border, thickness = 0.5.dp)
                    SettingsRow(Icons.Outlined.Info, "About", "Version info", onClick = { nav.navigate("settings/about") })
                }
            }
            Text("Legal", color = MutedText, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsRow(Icons.Outlined.Description, "Terms of Service", "Read our terms", onClick = { nav.navigate("settings/terms") })
                    HorizontalDivider(color = Border, thickness = 0.5.dp)
                    SettingsRow(Icons.Outlined.Security, "Privacy Policy", "How we handle your data", onClick = { nav.navigate("settings/privacy") })
                }
            }
        }
    }
}

@Composable private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(SoftBlue, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Primary) }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = PrimaryText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = SecondaryText, fontSize = 13.sp)
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = MutedText)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateDropdown(current: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("premium_v2", "minimal_mono", "bold_slate", "classic_ledger")
    val displayNames = mapOf(
        "premium_v2" to "Premium v2",
        "minimal_mono" to "Minimal Mono",
        "bold_slate" to "Bold Slate",
        "classic_ledger" to "Classic Ledger"
    )
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = displayNames[current] ?: current,
            onValueChange = {},
            readOnly = true,
            label = { Text("Invoice Template") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(displayNames[opt] ?: opt) },
                    onClick = { onSelect(opt); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateSelectionScreen(state: InvoiceGenViewModel, onBack: () -> Unit) {
    var selected by remember { mutableStateOf(state.profile.templateId) }
    val context = LocalContext.current
    val templates = listOf(
        Triple("premium_v2", "Premium v2", "Elegant serif-accented design with gold accents"),
        Triple("minimal_mono", "Minimal Mono", "Clean monospace design, black and white"),
        Triple("bold_slate", "Bold Slate", "Dark header with coral accent color"),
        Triple("classic_ledger", "Classic Ledger", "Traditional double-border bookkeeping style")
    )
    Column(Modifier.fillMaxSize().background(Background)) {
        TopBar("Invoice Template", onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            templates.forEach { (id, name, desc) ->
                val isSelected = selected == id
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) SoftBlue else Surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = if (isSelected) BorderStroke(2.dp, Primary) else null,
                    modifier = Modifier.fillMaxWidth().clickable { selected = id }
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(name, color = PrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(desc, color = SecondaryText, fontSize = 13.sp)
                        }
                        if (isSelected) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = "Selected", tint = Primary, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    state.saveProfile(state.profile.copy(templateId = selected))
                    Toast.makeText(context, "Template saved", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Save Template", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BusinessProfileScreen(state: InvoiceGenViewModel, onBack: () -> Unit) {
    var draft by remember { mutableStateOf(state.profile) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}
            draft = draft.copy(logoUri = uri.toString())
        }
    }
    
    Column(Modifier.fillMaxSize().background(Background)) {
        TopBar("Business Profile", onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Business Details", color = PrimaryText, fontWeight = FontWeight.Bold)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BusinessAvatar(draft)
                        Spacer(Modifier.width(16.dp))
                        OutlinedButton(onClick = { launcher.launch("image/*") }) { Text("Upload Logo") }
                        if (draft.logoUri.isNotBlank()) {
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { draft = draft.copy(logoUri = "") }) { Text("Remove", color = Color.Red) }
                        }
                    }
                    Field(draft.businessName, { draft = draft.copy(businessName = it) }, "Business Name", "Your company name")
                    Field(draft.ownerName, { draft = draft.copy(ownerName = it) }, "Owner Name", "John Doe")
                    Field(draft.email, { draft = draft.copy(email = it) }, "Email", "hello@business.com", keyboardType = KeyboardType.Email)
                    Field(draft.phone, { draft = draft.copy(phone = it) }, "Phone", "+1 234 567 8900", keyboardType = KeyboardType.Phone)
                    Field(draft.bankName, { draft = draft.copy(bankName = it) }, "Bank Name (Optional)", "Your Bank")
                    Field(draft.accountNumber, { draft = draft.copy(accountNumber = it.take(32)) }, "Account Number", "1234567890", keyboardType = KeyboardType.NumberPassword)
                }
            }

            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Preferences", color = PrimaryText, fontWeight = FontWeight.Bold)
                    Field(draft.currency, { draft = draft.copy(currency = it.uppercase().take(3)) }, "Currency", "USD")
                    Field(draft.themePreference, { draft = draft.copy(themePreference = it) }, "Theme (Light, Dark, System)", "System")
                    Field(draft.numberFormat, { draft = draft.copy(numberFormat = it) }, "Invoice Number Format", "INV-%04d")
                    Field(if (draft.defaultTaxRate == 0.0) "" else draft.defaultTaxRate.toString(), { draft = draft.copy(defaultTaxRate = it.toDoubleOrNull() ?: 0.0) }, "Default Tax (%)", "0", keyboardType = KeyboardType.Number)
                    Field(draft.paymentTerms, { draft = draft.copy(paymentTerms = it) }, "Payment Terms", "Net 30 days")
                }
            }
            
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Invoice Template", color = PrimaryText, fontWeight = FontWeight.Bold)
                    Text("Choose the default template for all new invoices", color = SecondaryText, fontSize = 12.sp)
                    TemplateDropdown(draft.templateId) { draft = draft.copy(templateId = it) }
                }
            }
            
            Button(onClick = { state.saveProfile(draft); Toast.makeText(context, "Profile saved", Toast.LENGTH_SHORT).show(); onBack() }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun HelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(Background)) {
        TopBar("Help & Support", onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(64.dp).background(SoftBlue, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Email, contentDescription = null, tint = Primary, modifier = Modifier.size(32.dp))
                    }
                    Text("We're here to help", color = PrimaryText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("If you have any questions or need assistance with your invoices, please reach out to our support team.", color = SecondaryText, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { 
                        val intent = Intent(Intent.ACTION_SENDTO).apply { data = android.net.Uri.parse("mailto:umerf2700@gmail.com") }
                        try { context.startActivity(intent) } catch (e: Exception) { Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show() }
                    }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                        Text("Contact Support", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    Column(Modifier.fillMaxSize().background(Background)) {
        TopBar("About", onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(32.dp))
            Image(painter = painterResource(id = R.drawable.logo), contentDescription = "App Logo", modifier = Modifier.size(100.dp).clip(RoundedCornerShape(24.dp)))
            Spacer(Modifier.height(16.dp))
            Text("Invoice Generator", color = PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("Version 1.0.0", color = SecondaryText, fontSize = 14.sp)
            Text("Email: umerf2700@gmail.com", color = SecondaryText, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Text("Created for Android.", color = SecondaryText, fontSize = 14.sp)
            Spacer(Modifier.height(32.dp))
        }
    }
}
// ── Templates ────────────────────────────────────────────────────────────────


// ── Invoice editor ───────────────────────────────────────────────────────────
@Composable
private fun EditorScreen(state: InvoiceGenViewModel, templateId: String, editingId: String, onBack: () -> Unit, onPreview: (String) -> Unit) {
    val source = state.invoices.firstOrNull { it.id == editingId } ?: state.createInvoice()
    var draft by remember(editingId, templateId) { mutableStateOf(source) }
    var clientName by remember(draft.clientId) { mutableStateOf(state.clients.firstOrNull { it.id == draft.clientId }?.name.orEmpty()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(Background)) {
        TopBar(if (editingId.isBlank()) "Create Invoice" else "Edit Invoice", onBack = onBack, rightIcon = Icons.Outlined.Save, onRightClick = { state.saveInvoice(draft); Toast.makeText(context, "Invoice saved successfully", Toast.LENGTH_SHORT).show(); onPreview(draft.id) })
        
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            
            // Bill From
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Bill From", color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BusinessAvatar(state.profile)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(state.profile.businessName.ifBlank { "Your Business" }, color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (state.profile.email.isNotBlank()) Text(state.profile.email, color = SecondaryText, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Bill To
            var expanded by remember { mutableStateOf(false) }
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Bill To", color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    if (state.clients.isEmpty()) {
                        Button(onClick = { Toast.makeText(context, "Please add a client from the Clients tab first", Toast.LENGTH_LONG).show() }, colors = ButtonDefaults.buttonColors(containerColor = SoftBlue, contentColor = Primary), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Text("No Clients - Add Client First", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = clientName.ifBlank { "Select a Client" },
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, "Select Client") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Border, focusedBorderColor = Primary)
                            )
                            Box(Modifier.matchParentSize().clickable { expanded = true }.background(Color.Transparent))
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.85f).background(Surface)) {
                            state.clients.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.name, color = PrimaryText, fontWeight = FontWeight.Medium) },
                                    onClick = { clientName = c.name; draft = draft.copy(clientId = c.id); expanded = false }
                                )
                            }
                        }
                    }
                }
            }

            // Details
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Invoice Details", color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Field(draft.number, { draft = draft.copy(number = it) }, "Invoice Number", "INV-0001")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        DatePickerField(draft.issueDate, { draft = draft.copy(issueDate = it) }, "Issue Date", Modifier.weight(1f))
                        DatePickerField(draft.dueDate, { draft = draft.copy(dueDate = it) }, "Due Date", Modifier.weight(1f))
                    }
                }
            }

            // Items
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Items", color = PrimaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                draft.lines.forEachIndexed { i, line ->
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Item ${i + 1}", color = PrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                if (draft.lines.size > 1) IconButton(onClick = { draft = draft.copy(lines = draft.lines.filterIndexed { idx, _ -> idx != i }) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Outlined.Delete, "Remove", tint = Danger, modifier = Modifier.size(20.dp))
                                }
                            }
                            Field(line.description, { v -> draft = draft.copy(lines = draft.lines.mapIndexed { idx, it -> if (idx == i) it.copy(description = v) else it }) }, "Description", "Item description")
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Field(if (line.quantity == 0.0) "" else line.quantity.toString(),
                                    { v -> draft = draft.copy(lines = draft.lines.mapIndexed { idx, it -> if (idx == i) it.copy(quantity = v.toDoubleOrNull() ?: 0.0) else it }) },
                                    "Qty", "1", Modifier.weight(1f), KeyboardType.Number)
                                Field(if (line.unitPrice == 0.0) "" else line.unitPrice.toString(),
                                    { v -> draft = draft.copy(lines = draft.lines.mapIndexed { idx, it -> if (idx == i) it.copy(unitPrice = v.toDoubleOrNull() ?: 0.0) else it }) },
                                    "Rate", "0.00", Modifier.weight(1f), KeyboardType.Number)
                            }
                        }
                    }
                }
                OutlinedButton(onClick = { draft = draft.copy(lines = draft.lines + InvoiceLine()) }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)) {
                    Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Add Item", fontWeight = FontWeight.Bold)
                }
            }

            // Calculation
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Field(if (draft.taxRate == 0.0) "" else draft.taxRate.toString(), { draft = draft.copy(taxRate = it.toDoubleOrNull() ?: 0.0) }, "Tax (%)", "0", Modifier.weight(1f), KeyboardType.Number)
                        Field(if (draft.discount == 0.0) "" else draft.discount.toString(), { draft = draft.copy(discount = it.toDoubleOrNull() ?: 0.0) }, "Discount", "0.00", Modifier.weight(1f), KeyboardType.Number)
                    }
                    HorizontalDivider(color = Border, modifier = Modifier.padding(vertical = 4.dp))
                    SummaryRow("Subtotal", currency(draft.subtotal, state.profile.currency))
                    if (draft.taxRate > 0) SummaryRow("Tax (${draft.taxRate}%)", currency(draft.tax, state.profile.currency))
                    if (draft.discount > 0) SummaryRow("Discount", "-${currency(draft.discount, state.profile.currency)}")
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(currency(draft.total, state.profile.currency), color = Primary, fontWeight = FontWeight.Black, fontSize = 24.sp)
                    }
                }
            }

            Field(draft.bankName, { draft = draft.copy(bankName = it) }, "Bank Name (Optional)", "E.g. Chase Bank")
            Field(draft.accountNumber, { draft = draft.copy(accountNumber = it) }, "Account Number (Optional)", "E.g. 1234567890", keyboardType = KeyboardType.NumberPassword)
            Field(draft.notes, { draft = draft.copy(notes = it) }, "Notes (Optional)", "Payment terms or instructions")

            Button(onClick = { state.saveInvoice(draft); Toast.makeText(context, "Invoice saved successfully", Toast.LENGTH_SHORT).show(); onPreview(draft.id) },
                colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Save Invoice", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            
            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Invoice preview ──────────────────────────────────────────────────────────
@Composable
private fun PreviewScreen(state: InvoiceGenViewModel, invoiceId: String, onBack: () -> Unit, onEdit: (String) -> Unit, onDeleted: () -> Unit) {
    val invoice = state.invoices.firstOrNull { it.id == invoiceId } ?: return
    val client = state.clients.firstOrNull { it.id == invoice.clientId }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(Background)) {
        TopBar("Preview", onBack = onBack, rightIcon = Icons.Outlined.Edit, onRightClick = { onEdit(invoice.id) })
        
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            
            // Document Card
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        BusinessAvatar(state.profile)
                        Column(horizontalAlignment = Alignment.End) {
                            Text("INVOICE", color = PrimaryText, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                            Text(invoice.number, color = SecondaryText, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("BILL TO", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(client?.name ?: "No client", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (!client?.email.isNullOrBlank()) Text(client!!.email, color = SecondaryText, fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("ISSUED", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text(invoice.issueDate, color = PrimaryText, fontSize = 13.sp) }
                        Column(horizontalAlignment = Alignment.End) { Text("DUE", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text(invoice.dueDate, color = PrimaryText, fontSize = 13.sp) }
                    }
                    Spacer(Modifier.height(24.dp))
                    
                    // Table Header
                    Row(Modifier.fillMaxWidth().background(SoftBlue, RoundedCornerShape(6.dp)).padding(8.dp)) {
                        Text("Description", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        Text("Amount", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.width(60.dp))
                    }
                    
                    // Table Rows
                    invoice.lines.forEach { line ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(line.description.ifBlank { "Item" }, color = PrimaryText, fontSize = 13.sp)
                                Text("${line.quantity} x ${currency(line.unitPrice, state.profile.currency)}", color = SecondaryText, fontSize = 11.sp)
                            }
                            Text(currency(line.total, state.profile.currency), color = PrimaryText, fontWeight = FontWeight.Medium, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.width(60.dp))
                        }
                        HorizontalDivider(color = Border, thickness = 0.5.dp)
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    SummaryRow("Subtotal", currency(invoice.subtotal, state.profile.currency))
                    if (invoice.taxRate > 0) SummaryRow("Tax (${invoice.taxRate}%)", currency(invoice.tax, state.profile.currency))
                    if (invoice.discount > 0) SummaryRow("Discount", "-${currency(invoice.discount, state.profile.currency)}")
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Due", color = PrimaryText, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text(currency(invoice.total, state.profile.currency), color = Primary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }
                    
                    if (invoice.notes.isNotBlank()) {
                        Spacer(Modifier.height(24.dp))
                        Text("NOTES", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(invoice.notes, color = SecondaryText, fontSize = 12.sp)
                    }
                }
            }

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { scope.launch { val res = InvoiceExporter.savePdf(context, invoice, client, state.profile); res.onSuccess { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show() } } },
                    modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                    Text("PDF", fontWeight = FontWeight.Bold)
                }
                Button(onClick = { scope.launch { val res = InvoiceExporter.saveImage(context, invoice, client, state.profile); res.onSuccess { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show() } } },
                    modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                    Text("Image", fontWeight = FontWeight.Bold)
                }
                Button(onClick = { scope.launch { val res = InvoiceExporter.printInvoice(context, invoice, client, state.profile); res.onSuccess { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show() } } },
                    modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                    Text("Print", fontWeight = FontWeight.Bold)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { 
                        val dup = invoice.copy(id = java.util.UUID.randomUUID().toString(), number = "${invoice.number}-COPY", issueDate = today(), dueDate = today(), lines = invoice.lines.map { it.copy(id = java.util.UUID.randomUUID().toString()) })
                        state.saveInvoice(dup)
                        Toast.makeText(context, "Duplicated as ${dup.number}", Toast.LENGTH_SHORT).show()
                        onDeleted() // Navigate back to refresh list
                    }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)) {
                    Text("Duplicate", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = { state.deleteInvoice(invoice.id); onDeleted() },
                    modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Reusable components ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(title: String, onBack: (() -> Unit)? = null, rightIcon: ImageVector? = null, onRightClick: (() -> Unit)? = null) {
    CenterAlignedTopAppBar(
        title = { Text(title, color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        navigationIcon = { if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = PrimaryText) } },
        actions = { if (rightIcon != null && onRightClick != null) IconButton(onClick = onRightClick) { Icon(rightIcon, "Action", tint = PrimaryText) } },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Background))
}

@Composable private fun Field(value: String, onChange: (String) -> Unit, label: String, hint: String, modifier: Modifier = Modifier.fillMaxWidth(), keyboardType: KeyboardType = KeyboardType.Text) {
    Column(modifier) {
        Text(label, color = SecondaryText, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(value, onChange, placeholder = { Text(hint, color = MutedText) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Border, focusedBorderColor = Primary), keyboardOptions = KeyboardOptions(keyboardType = keyboardType))
    }
}

@Composable private fun DatePickerField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context, { _, y, m, d -> onChange(String.format("%04d-%02d-%02d", y, m + 1, d)) },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )
    Column(modifier) {
        Text(label, color = SecondaryText, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(value, {}, readOnly = true, placeholder = { Text("YYYY-MM-DD", color = MutedText) }, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp), trailingIcon = { IconButton(onClick = { datePickerDialog.show() }) { Icon(Icons.Outlined.CalendarToday, null, tint = MutedText) } },
            colors = OutlinedTextFieldDefaults.colors(disabledBorderColor = Border, disabledTextColor = PrimaryText, disabledTrailingIconColor = MutedText))
    }
}

@Composable private fun SummaryCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), border = CardDefaults.outlinedCardBorder(true), modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Box(Modifier.size(36.dp).background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.height(12.dp))
            Text(title, color = SecondaryText, fontSize = 13.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(value, color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

@Composable private fun InvoiceRow(invoice: Invoice, state: InvoiceGenViewModel, onClick: () -> Unit) {
    val client = state.clients.firstOrNull { it.id == invoice.clientId }
    Card(onClick = onClick, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), border = CardDefaults.outlinedCardBorder(true), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(SoftBlue, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Receipt, null, tint = Primary) }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(invoice.number, color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(client?.name ?: "No client", color = SecondaryText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(currency(invoice.total, state.profile.currency), color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}


@Composable private fun BusinessAvatar(profile: BusinessProfile, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    if (profile.logoUri.isNotBlank()) {
        val bmp = remember(profile.logoUri) {
            try {
                val u = android.net.Uri.parse(profile.logoUri)
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    val src = android.graphics.ImageDecoder.createSource(context.contentResolver, u)
                    android.graphics.ImageDecoder.decodeBitmap(src).asImageBitmap()
                } else {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, u).asImageBitmap()
                }
            } catch (e: Exception) { null }
        }
        if (bmp != null) {
            androidx.compose.foundation.Image(bmp, "Logo", modifier = modifier.size(44.dp).clip(RoundedCornerShape(12.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            return
        }
    }
    Avatar(profile.businessName.ifBlank { "B" })
}

@Composable private fun Avatar(name: String) {
    Box(Modifier.size(44.dp).background(PrimaryLight.copy(alpha = 0.2f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Text(name.take(1).uppercase(), color = PrimaryDark, fontWeight = FontWeight.Black, fontSize = 18.sp)
    }
}

@Composable private fun SummaryRow(label: String, value: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = SecondaryText, fontSize = 13.sp); Text(value, color = PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium) } }

@Composable private fun EmptyState(title: String, detail: String, route: String?, onNavigate: ((String) -> Unit)?) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(72.dp).background(SoftBlue, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Inbox, null, tint = PrimaryLight, modifier = Modifier.size(36.dp)) }
        Spacer(Modifier.height(16.dp))
        Text(title, color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text(detail, color = SecondaryText, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        if (route != null && onNavigate != null) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = { onNavigate(route) }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("Get Started") }
        }
    }
}
