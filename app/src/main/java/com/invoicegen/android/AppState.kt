package com.invoicegen.android

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Serializable
data class BusinessProfile(
    val businessName: String = "",
    val ownerName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val currency: String = "PKR",
    val invoicePrefix: String = "INV-",
    val numberFormat: String = "INV-%04d",
    val defaultTaxRate: Double = 0.0,
    val paymentTerms: String = "Payment due within 30 days.",
    val logoUri: String = "",
    val themePreference: String = "System",
    val bankName: String = "",
    val accountNumber: String = "",
    val customHtmlTemplate: String = "",
    val templateId: String = "premium_v2"
)

@Serializable
data class Client(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = ""
)

@Serializable
data class InvoiceLine(
    val id: String = UUID.randomUUID().toString(),
    val description: String = "",
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0
) {
    val total: Double get() = quantity * unitPrice
}

@Serializable
data class Invoice(
    val id: String = UUID.randomUUID().toString(),
    val number: String = "INV-0001",
    val clientId: String = "",
    val issueDate: String = today(),
    val dueDate: String = today(),
    val lines: List<InvoiceLine> = listOf(InvoiceLine()),
    val taxRate: Double = 0.0,
    val discount: Double = 0.0,
    val notes: String = "",
    val bankName: String = "",
    val accountNumber: String = ""
) {
    val subtotal: Double get() = lines.sumOf { it.total }
    val taxable: Double get() = (subtotal - discount).coerceAtLeast(0.0)
    val tax: Double get() = taxable * taxRate / 100.0
    val total: Double get() = taxable + tax
}

data class InvoiceTemplate(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconName: String,
    val notes: String,
    val lines: List<InvoiceLine>
)

val invoiceTemplates = listOf(
    InvoiceTemplate("freelance", "Freelance service", "Creative or professional work", "draw", "Thank you for your business. Payment is due within 30 days.", listOf(InvoiceLine())),
    InvoiceTemplate("consulting", "Consulting session", "Advisory work by session or hour", "lightbulb", "Thank you for choosing our consulting services. Payment is due within 14 days.", listOf(InvoiceLine(), InvoiceLine())),
    InvoiceTemplate("products", "Product order", "Goods, products, and merchandise", "inventory", "Thank you for your order. Please keep this invoice for your records.", listOf(InvoiceLine(), InvoiceLine())),
    InvoiceTemplate("retainer", "Monthly retainer", "Recurring monthly service", "sync", "This invoice covers the current monthly retainer period.", listOf(InvoiceLine())),
    InvoiceTemplate("milestone", "Project milestone", "Completed phase or deliverable", "flag", "Payment is requested for the completed project milestone described above.", listOf(InvoiceLine()))
)

class InvoiceGenViewModel(private val context: Context) : ViewModel() {
    private val prefs = context.getSharedPreferences("invoicegen_workspace", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    var profile by mutableStateOf(read("profile", BusinessProfile.serializer(), BusinessProfile()))
        private set
    var clients by mutableStateOf(read("clients", kotlinx.serialization.builtins.ListSerializer(Client.serializer()), emptyList()))
        private set
    var invoices by mutableStateOf(read("invoices", kotlinx.serialization.builtins.ListSerializer(Invoice.serializer()), emptyList()))
        private set

    fun saveProfile(value: BusinessProfile) { profile = value; write("profile", BusinessProfile.serializer(), value) }
    fun saveClient(value: Client) { clients = (clients.filterNot { it.id == value.id } + value).sortedBy { it.name.lowercase() }; write("clients", kotlinx.serialization.builtins.ListSerializer(Client.serializer()), clients) }
    fun deleteClient(id: String) { clients = clients.filterNot { it.id == id }; write("clients", kotlinx.serialization.builtins.ListSerializer(Client.serializer()), clients) }
    fun saveInvoice(value: Invoice) { invoices = (invoices.filterNot { it.id == value.id } + value).sortedByDescending { it.issueDate }; write("invoices", kotlinx.serialization.builtins.ListSerializer(Invoice.serializer()), invoices) }
    fun deleteInvoice(id: String) { invoices = invoices.filterNot { it.id == id }; write("invoices", kotlinx.serialization.builtins.ListSerializer(Invoice.serializer()), invoices) }
    fun createInvoice(): Invoice {
        val index = invoices.size + 1
        val num = try { String.format(profile.numberFormat, index) } catch (e: Exception) { "${profile.invoicePrefix}${index.toString().padStart(4, '0')}" }
        return Invoice(
            number = num,
            taxRate = profile.defaultTaxRate,
            notes = profile.paymentTerms,
            lines = listOf(InvoiceLine()),
            bankName = profile.bankName,
            accountNumber = profile.accountNumber
        )
    }

    private fun <T> read(key: String, serializer: kotlinx.serialization.KSerializer<T>, fallback: T): T = try {
        prefs.getString(key, null)?.let { json.decodeFromString(serializer, it) } ?: fallback
    } catch (_: Exception) { fallback }
    private fun <T> write(key: String, serializer: kotlinx.serialization.KSerializer<T>, value: T) { prefs.edit().putString(key, json.encodeToString(serializer, value)).apply() }
}

fun currency(value: Double, code: String): String = try { NumberFormat.getCurrencyInstance().apply { currency = java.util.Currency.getInstance(code.ifBlank { "PKR" }) }.format(value) } catch (e: Exception) { "$code $value" }
fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
