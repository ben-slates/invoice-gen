package com.invoicegen.android

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.os.Build
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.provider.MediaStore
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object InvoiceExporter {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    private fun currency(amount: Double, code: String): String {
        val format = NumberFormat.getCurrencyInstance(Locale.US)
        format.currency = java.util.Currency.getInstance(code)
        return format.format(amount)
    }

    private fun escapeHtml(value: String): String =
        value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun getBase64Logo(context: Context, uriString: String): String {
        if (uriString.isBlank()) {
            return try {
                val bitmap = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.logo)
                val out = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                "<img src=\"data:image/png;base64,\$base64\" style=\"max-height:52px; max-width:200px; object-fit: contain;\" />"
            } catch (e: Exception) { "" }
        }
        return try {
            val uri = android.net.Uri.parse(uriString)
            val bitmap = if (Build.VERSION.SDK_INT >= 28) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri).copy(Bitmap.Config.ARGB_8888, true)
            }
            val outputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            "<img src=\"data:image/png;base64,$base64\" style=\"max-height:64px; max-width:200px; object-fit:contain;\" />"
        } catch (e: Exception) {
            ""
        }
    }

    private const val TEMPLATE_PREMIUM_V2 = """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
  body { font-family: Arial, Helvetica, sans-serif; color: #4A5568; background-color: #FAF9F6; margin: 0; padding: 40px 50px; font-size: 13px; line-height: 1.5; }
  .display { font-family: Georgia, "Times New Roman", serif; }
  .header-table { width: 100%; border-collapse: collapse; border-bottom: 2px solid #1B2430; margin-bottom: 30px; }
  .header-table td { vertical-align: top; padding-bottom: 24px; }
  .brand-name { font-size: 22px; color: #1B2430; margin: 0 0 6px 0; }
  .brand-contact { margin: 0; font-size: 12px; color: #4A5568; }
  .invoice-tag { font-size: 11px; letter-spacing: 2px; color: #A9812F; font-weight: bold; }
  .invoice-number { font-size: 26px; color: #1B2430; margin: 4px 0 8px 0; }
  .invoice-dates { margin: 0; font-size: 12px; line-height: 1.7; }
  .invoice-dates strong { color: #1B2430; }
  .bill-to-label { font-size: 10px; letter-spacing: 1.5px; text-transform: uppercase; color: #A9812F; font-weight: bold; margin-bottom: 6px; }
  .bill-to-name { font-size: 15px; color: #1B2430; font-weight: bold; margin-bottom: 2px; }
  .items-table { width: 100%; border-collapse: collapse; margin-top: 24px; margin-bottom: 26px; }
  .items-table th { text-align: left; padding: 10px; font-size: 10px; letter-spacing: 1px; text-transform: uppercase; color: #1B2430; background-color: #F1E9D6; border-top: 1px solid #1B2430; border-bottom: 1px solid #1B2430; }
  .items-table td { padding: 10px; font-size: 12.5px; border-bottom: 1px solid #E4E1D8; color: #4A5568; }
  .col-right { text-align: right; }
  .col-center { text-align: center; }
  .totals-table { width: 260px; border-collapse: collapse; margin-left: auto; margin-bottom: 36px; }
  .totals-table td { padding: 6px 0; font-size: 12.5px; }
  .totals-table .divider td { border-top: 1px solid #E4E1D8; padding-top: 12px; }
  .grand-total-table { width: 260px; border-collapse: collapse; margin-left: auto; background-color: #1B2430; }
  .grand-total-table td { padding: 12px 16px; color: #FFFFFF; }
  .grand-label { font-size: 10px; letter-spacing: 1.5px; text-transform: uppercase; color: #F1E9D6; }
  .grand-value { font-family: Georgia, "Times New Roman", serif; font-size: 19px; font-weight: bold; color: #FFFFFF; text-align: right; }
  .footer-table { width: 100%; border-collapse: collapse; border-top: 1px solid #E4E1D8; margin-top: 10px; }
  .footer-table td { vertical-align: top; padding-top: 18px; width: 50%; font-size: 12px; }
  .footer-label { font-size: 10px; letter-spacing: 1.5px; text-transform: uppercase; color: #A9812F; font-weight: bold; margin-bottom: 6px; }
</style>
</head>
<body>
  <table class="header-table">
    <tr>
      <td style="width: 60%;">
        <div>{{logo}}</div>
        <h1 class="brand-name display">{{businessName}}</h1>
        <p class="brand-contact">{{businessEmail}}<br>{{businessPhone}}</p>
      </td>
      <td style="width: 40%; text-align: right;">
        <div class="invoice-tag">INVOICE</div>
        <div class="invoice-number display">#{{invoiceNumber}}</div>
        <p class="invoice-dates"><strong>Issued</strong> &nbsp;{{issueDate}}<br><strong>Due</strong> &nbsp;&nbsp;&nbsp;{{dueDate}}</p>
      </td>
    </tr>
  </table>
  <div class="bill-to-label">Bill To</div>
  <div class="bill-to-name">{{clientName}}</div>
  <div>{{clientEmail}}</div>
  <table class="items-table">
    <thead>
      <tr><th>Description</th><th class="col-center">Qty</th><th class="col-right">Price</th><th class="col-right">Total</th></tr>
    </thead>
    <tbody>{{items}}</tbody>
  </table>
  <table class="totals-table">
    <tr><td>Subtotal</td><td class="col-right">{{subtotal}}</td></tr>
    <tr><td>Tax</td><td class="col-right">{{tax}}</td></tr>
    <tr class="divider"><td>Discount</td><td class="col-right">-{{discount}}</td></tr>
  </table>
  <table class="grand-total-table">
    <tr><td class="grand-label">Total Due</td><td class="grand-value">{{total}}</td></tr>
  </table>
  <table class="footer-table">
    <tr>
      <td><div class="footer-label">Payment Details</div><p>{{bankName}}<br>{{accountNumber}}</p></td>
      <td><div class="footer-label">Notes</div><p>{{notes}}</p></td>
    </tr>
  </table>
</body>
</html>
"""

    private fun formatQty(qty: Double): String = if (qty == qty.toLong().toDouble()) qty.toLong().toString() else qty.toString()

    private const val TEMPLATE_MINIMAL_MONO = """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
  body { font-family: Arial, Helvetica, sans-serif; color: #333333; background-color: #FFFFFF; margin: 0; padding: 46px 54px; font-size: 13px; line-height: 1.5; }
  .mono { font-family: "Courier New", Courier, monospace; }
  .wordmark { font-size: 46px; font-weight: bold; letter-spacing: -1px; color: #111111; margin: 0 0 4px 0; }
  .wordmark-sub { font-size: 11px; letter-spacing: 3px; text-transform: uppercase; color: #767676; margin: 0 0 26px 0; }
  .top-rule { border-top: 3px solid #111111; margin-bottom: 24px; }
  .meta-table { width: 100%; border-collapse: collapse; margin-bottom: 34px; }
  .meta-table td { vertical-align: top; padding-right: 20px; }
  .meta-label { font-size: 10px; letter-spacing: 1.5px; text-transform: uppercase; color: #767676; margin-bottom: 6px; }
  .meta-value { font-size: 13.5px; color: #111111; }
  .meta-value.mono { font-size: 13px; }
  .biz-name { font-size: 16px; font-weight: bold; color: #111111; margin-bottom: 3px; }
  .chip { display: inline-block; border: 1px solid #111111; padding: 3px 10px; font-size: 12px; }
  .items-table { width: 100%; border-collapse: collapse; margin-bottom: 26px; }
  .items-table th { text-align: left; padding: 8px 4px; font-size: 10px; letter-spacing: 1.5px; text-transform: uppercase; color: #111111; border-bottom: 2px solid #111111; }
  .items-table td { padding: 10px 4px; font-size: 13px; border-bottom: 1px solid #DDDDDD; color: #333333; }
  .col-right { text-align: right; }
  .col-center { text-align: center; }
  .totals-table { width: 260px; border-collapse: collapse; margin-left: auto; margin-bottom: 6px; }
  .totals-table td { padding: 5px 0; font-size: 13px; }
  .total-box { width: 260px; margin-left: auto; border: 2px solid #111111; padding: 14px 16px; margin-top: 10px; }
  .total-box .label { font-size: 10px; letter-spacing: 2px; text-transform: uppercase; color: #767676; }
  .total-box .value { font-size: 24px; font-weight: bold; color: #111111; text-align: right; }
  .footer-table { width: 100%; border-collapse: collapse; border-top: 1px solid #DDDDDD; margin-top: 36px; }
  .footer-table td { vertical-align: top; padding-top: 16px; width: 50%; font-size: 12px; color: #555555; }
  .footer-label { font-size: 10px; letter-spacing: 1.5px; text-transform: uppercase; color: #767676; margin-bottom: 5px; }
</style>
</head>
<body>
  <div>{{logo}}</div>
  <div class="wordmark">INVOICE</div>
  <div class="wordmark-sub">{{businessName}}</div>
  <div class="top-rule"></div>
  <table class="meta-table">
    <tr>
      <td style="width: 34%;">
        <div class="meta-label">From</div>
        <div class="biz-name">{{businessName}}</div>
        <div class="meta-value">{{businessEmail}}<br>{{businessPhone}}</div>
      </td>
      <td style="width: 33%;">
        <div class="meta-label">Bill To</div>
        <div class="biz-name">{{clientName}}</div>
        <div class="meta-value">{{clientEmail}}</div>
      </td>
      <td style="width: 33%;">
        <div class="meta-label">Invoice No.</div>
        <div class="meta-value mono chip">{{invoiceNumber}}</div>
        <div class="meta-label" style="margin-top: 14px;">Issued / Due</div>
        <div class="meta-value mono">{{issueDate}} &rarr; {{dueDate}}</div>
      </td>
    </tr>
  </table>
  <table class="items-table">
    <thead><tr><th>Description</th><th class="col-center">Qty</th><th class="col-right">Price</th><th class="col-right">Total</th></tr></thead>
    <tbody>{{items}}</tbody>
  </table>
  <table class="totals-table">
    <tr><td>Subtotal</td><td class="col-right">{{subtotal}}</td></tr>
    <tr><td>Tax</td><td class="col-right">{{tax}}</td></tr>
    <tr><td>Discount</td><td class="col-right">-{{discount}}</td></tr>
  </table>
  <div class="total-box">
    <table style="width:100%;"><tr><td class="label">Total Due</td><td class="value">{{total}}</td></tr></table>
  </div>
  <table class="footer-table">
    <tr>
      <td><div class="footer-label">Payment Details</div>{{bankName}}<br>{{accountNumber}}</td>
      <td><div class="footer-label">Notes</div>{{notes}}</td>
    </tr>
  </table>
</body>
</html>
"""

    private const val TEMPLATE_BOLD_SLATE = """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
  body { font-family: Arial, Helvetica, sans-serif; color: #3D4450; background-color: #F7F5F1; margin: 0; padding: 0; font-size: 13px; line-height: 1.5; }
  .content { padding: 0 50px 40px 50px; }
  .header-band { background-color: #223142; padding: 36px 50px; margin-bottom: 30px; }
  .header-table { width: 100%; border-collapse: collapse; }
  .header-table td { vertical-align: top; }
  .biz-name { font-size: 22px; font-weight: bold; color: #FFFFFF; margin: 0 0 6px 0; }
  .biz-contact { margin: 0; font-size: 12px; color: #C7CDD6; }
  .invoice-chip { display: inline-block; background-color: #E2725B; color: #FFFFFF; font-size: 11px; letter-spacing: 2px; font-weight: bold; padding: 5px 12px; border-radius: 2px; margin-bottom: 8px; }
  .invoice-number { font-size: 24px; font-weight: bold; color: #FFFFFF; margin: 0 0 8px 0; }
  .invoice-dates { margin: 0; font-size: 12px; color: #C7CDD6; line-height: 1.7; }
  .invoice-dates strong { color: #FFFFFF; }
  .bill-to-label { font-size: 10px; letter-spacing: 1.5px; text-transform: uppercase; color: #E2725B; font-weight: bold; margin-bottom: 6px; }
  .bill-to-name { font-size: 15px; color: #223142; font-weight: bold; margin-bottom: 2px; }
  .items-table { width: 100%; border-collapse: collapse; margin-top: 22px; margin-bottom: 26px; }
  .items-table th { text-align: left; padding: 11px 10px; font-size: 10px; letter-spacing: 1px; text-transform: uppercase; color: #FFFFFF; background-color: #E2725B; }
  .items-table td { padding: 11px 10px; font-size: 12.5px; border-bottom: 1px solid #E6E2D8; color: #3D4450; }
  .col-right { text-align: right; }
  .col-center { text-align: center; }
  .totals-table { width: 260px; border-collapse: collapse; margin-left: auto; margin-bottom: 6px; }
  .totals-table td { padding: 6px 0; font-size: 12.5px; }
  .grand-total-table { width: 260px; border-collapse: collapse; margin-left: auto; background-color: #223142; margin-top: 10px; }
  .grand-total-table td { padding: 13px 16px; color: #FFFFFF; }
  .grand-label { font-size: 10px; letter-spacing: 1.5px; text-transform: uppercase; color: #E2725B; }
  .grand-value { font-size: 20px; font-weight: bold; color: #FFFFFF; text-align: right; }
  .footer-table { width: 100%; border-collapse: collapse; border-top: 1px solid #E6E2D8; margin-top: 34px; }
  .footer-table td { vertical-align: top; padding-top: 18px; width: 50%; font-size: 12px; }
  .footer-label { font-size: 10px; letter-spacing: 1.5px; text-transform: uppercase; color: #E2725B; font-weight: bold; margin-bottom: 6px; }
</style>
</head>
<body>
  <div class="header-band">
    <table class="header-table">
      <tr>
        <td style="width: 60%;">
          <div style="margin-bottom:10px;">{{logo}}</div>
          <div class="biz-name">{{businessName}}</div>
          <p class="biz-contact">{{businessEmail}}<br>{{businessPhone}}</p>
        </td>
        <td style="width: 40%; text-align: right;">
          <div class="invoice-chip">INVOICE</div>
          <div class="invoice-number">#{{invoiceNumber}}</div>
          <p class="invoice-dates"><strong>Issued</strong> &nbsp;{{issueDate}}<br><strong>Due</strong> &nbsp;&nbsp;&nbsp;{{dueDate}}</p>
        </td>
      </tr>
    </table>
  </div>
  <div class="content">
    <div class="bill-to-label">Bill To</div>
    <div class="bill-to-name">{{clientName}}</div>
    <div>{{clientEmail}}</div>
    <table class="items-table">
      <thead><tr><th>Description</th><th class="col-center">Qty</th><th class="col-right">Price</th><th class="col-right">Total</th></tr></thead>
      <tbody>{{items}}</tbody>
    </table>
    <table class="totals-table">
      <tr><td>Subtotal</td><td class="col-right">{{subtotal}}</td></tr>
      <tr><td>Tax</td><td class="col-right">{{tax}}</td></tr>
      <tr><td>Discount</td><td class="col-right">-{{discount}}</td></tr>
    </table>
    <table class="grand-total-table">
      <tr><td class="grand-label">Total Due</td><td class="grand-value">{{total}}</td></tr>
    </table>
    <table class="footer-table">
      <tr>
        <td><div class="footer-label">Payment Details</div><p>{{bankName}}<br>{{accountNumber}}</p></td>
        <td><div class="footer-label">Notes</div><p>{{notes}}</p></td>
      </tr>
    </table>
  </div>
</body>
</html>
"""

    private const val TEMPLATE_CLASSIC_LEDGER = """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
  body { font-family: Georgia, "Times New Roman", serif; color: #2B2B26; background-color: #FBF7EE; margin: 0; padding: 18px; font-size: 13px; line-height: 1.5; }
  .frame { border: 2px solid #2F4538; padding: 4px; }
  .frame-inner { border: 1px solid #2F4538; padding: 40px 48px; }
  .center { text-align: center; }
  .biz-name { font-size: 24px; color: #2F4538; margin: 0 0 4px 0; letter-spacing: 0.5px; }
  .biz-contact { font-size: 11.5px; color: #6B6559; margin: 0 0 14px 0; }
  .divider { text-align: center; color: #B8892B; font-size: 14px; letter-spacing: 8px; margin: 14px 0 22px 0; }
  .invoice-title { text-align: center; font-size: 12px; letter-spacing: 4px; text-transform: uppercase; color: #B8892B; font-weight: bold; margin-bottom: 4px; }
  .invoice-number { text-align: center; font-size: 15px; color: #2B2B26; margin-bottom: 22px; }
  .meta-table { width: 100%; border-collapse: collapse; margin-bottom: 26px; }
  .meta-table td { vertical-align: top; font-size: 12.5px; padding-bottom: 4px; }
  .meta-label { font-size: 10px; letter-spacing: 1.5px; text-transform: uppercase; color: #B8892B; margin-bottom: 5px; }
  .meta-name { font-size: 14px; color: #2F4538; font-weight: bold; }
  .items-table { width: 100%; border-collapse: collapse; margin-bottom: 24px; }
  .items-table th { text-align: left; padding: 9px 6px; font-size: 10.5px; letter-spacing: 1px; text-transform: uppercase; color: #2F4538; border-top: 1px solid #2F4538; border-bottom: 1px solid #2F4538; }
  .items-table td { padding: 10px 6px; font-size: 12.5px; border-bottom: 1px solid #DCD3BC; color: #2B2B26; }
  .col-right { text-align: right; }
  .col-center { text-align: center; }
  .totals-table { width: 260px; border-collapse: collapse; margin-left: auto; margin-bottom: 6px; }
  .totals-table td { padding: 5px 0; font-size: 12.5px; }
  .total-box { width: 260px; margin-left: auto; border: 1px double #B8892B; padding: 12px 16px; margin-top: 8px; }
  .total-box .label { font-size: 10px; letter-spacing: 2px; text-transform: uppercase; color: #B8892B; }
  .total-box .value { font-size: 19px; color: #2F4538; font-weight: bold; text-align: right; }
  .footer-table { width: 100%; border-collapse: collapse; border-top: 1px solid #DCD3BC; margin-top: 30px; }
  .footer-table td { vertical-align: top; padding-top: 16px; width: 50%; font-size: 12px; color: #6B6559; }
  .footer-label { font-size: 10px; letter-spacing: 1.5px; text-transform: uppercase; color: #B8892B; font-weight: bold; margin-bottom: 5px; }
</style>
</head>
<body>
<div class="frame"><div class="frame-inner">
  <div class="center" style="margin-bottom:10px;">{{logo}}</div>
  <div class="center biz-name">{{businessName}}</div>
  <div class="center biz-contact">{{businessEmail}} &nbsp;&middot;&nbsp; {{businessPhone}}</div>
  <div class="divider">&diams; &diams; &diams;</div>
  <div class="invoice-title">Invoice</div>
  <div class="invoice-number">No. {{invoiceNumber}} &nbsp;&mdash;&nbsp; Issued {{issueDate}} &nbsp;&mdash;&nbsp; Due {{dueDate}}</div>
  <table class="meta-table">
    <tr>
      <td style="width: 50%;">
        <div class="meta-label">Bill To</div>
        <div class="meta-name">{{clientName}}</div>
        <div>{{clientEmail}}</div>
      </td>
    </tr>
  </table>
  <table class="items-table">
    <thead><tr><th>Description</th><th class="col-center">Qty</th><th class="col-right">Price</th><th class="col-right">Total</th></tr></thead>
    <tbody>{{items}}</tbody>
  </table>
  <table class="totals-table">
    <tr><td>Subtotal</td><td class="col-right">{{subtotal}}</td></tr>
    <tr><td>Tax</td><td class="col-right">{{tax}}</td></tr>
    <tr><td>Discount</td><td class="col-right">-{{discount}}</td></tr>
  </table>
  <div class="total-box">
    <table style="width:100%;"><tr><td class="label">Total Due</td><td class="value">{{total}}</td></tr></table>
  </div>
  <table class="footer-table">
    <tr>
      <td><div class="footer-label">Payment Details</div>{{bankName}}<br>{{accountNumber}}</td>
      <td><div class="footer-label">Notes</div>{{notes}}</td>
    </tr>
  </table>
</div></div>
</body>
</html>
"""

    fun generateHtml(context: Context, invoice: Invoice, client: Client?, profile: BusinessProfile): String {
        val logoTag = getBase64Logo(context, profile.logoUri)
        
        val itemsHtml = StringBuilder()
        for (item in invoice.lines) {
            itemsHtml.append("<tr>")
            itemsHtml.append("<td>").append(escapeHtml(item.description)).append("</td>")
            itemsHtml.append("<td class=\"col-center\">").append(formatQty(item.quantity)).append("</td>")
            itemsHtml.append("<td class=\"col-right\">").append(currency(item.unitPrice, profile.currency)).append("</td>")
            itemsHtml.append("<td class=\"col-right\">").append(currency(item.total, profile.currency)).append("</td>")
            itemsHtml.append("</tr>")
        }

        val textReplacements = mapOf(
            "{{businessName}}" to profile.businessName,
            "{{businessEmail}}" to profile.email,
            "{{businessPhone}}" to profile.phone,
            "{{invoiceNumber}}" to invoice.number,
            "{{issueDate}}" to invoice.issueDate,
            "{{dueDate}}" to invoice.dueDate,
            "{{clientName}}" to (client?.name ?: ""),
            "{{clientEmail}}" to (client?.email ?: ""),
            "{{subtotal}}" to currency(invoice.subtotal, profile.currency),
            "{{tax}}" to currency(invoice.tax, profile.currency),
            "{{discount}}" to currency(invoice.discount, profile.currency),
            "{{total}}" to currency(invoice.total, profile.currency),
            "{{bankName}}" to invoice.bankName,
            "{{accountNumber}}" to invoice.accountNumber,
            "{{notes}}" to invoice.notes
        )

        var html = when(profile.templateId) {
            "minimal_mono" -> TEMPLATE_MINIMAL_MONO
            "bold_slate" -> TEMPLATE_BOLD_SLATE
            "classic_ledger" -> TEMPLATE_CLASSIC_LEDGER
            else -> TEMPLATE_PREMIUM_V2
        }
        for ((token, value) in textReplacements) {
            html = html.replace(token, escapeHtml(value))
        }
        
        html = html.replace("{{logo}}", logoTag)
        html = html.replace("{{items}}", itemsHtml.toString())
        
        return html
    }

    

    suspend fun savePdf(context: Context, invoice: Invoice, client: Client?, profile: BusinessProfile): Result<String> = runCatching {
        suspendCancellableCoroutine<String> { continuation ->
            val webView = WebView(context)
        webView.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
            webView.settings.javaScriptEnabled = false
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    val w = 2380; val h = 3368
                    view.measure(android.view.View.MeasureSpec.makeMeasureSpec(w, android.view.View.MeasureSpec.EXACTLY), android.view.View.MeasureSpec.makeMeasureSpec(h, android.view.View.MeasureSpec.EXACTLY))
                    view.layout(0, 0, w, h)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        val c = Canvas(bitmap)
                        view.draw(c)
                        
                        try {
                            val pdfDocument = android.graphics.pdf.PdfDocument()
                            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(w, h, 1).create()
                            val page = pdfDocument.startPage(pageInfo)
                            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                            pdfDocument.finishPage(page)
                            
                            val fileName = "Invoice_${invoice.number}.pdf"
                            if (Build.VERSION.SDK_INT >= 29) {
                                val values = ContentValues().apply {
                                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                                }
                                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("Unable to create PDF entry")
                                context.contentResolver.openOutputStream(uri)?.use { pdfDocument.writeTo(it) } ?: error("Unable to write PDF")
                            } else {
                                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                dir.mkdirs()
                                val file = File(dir, fileName)
                                pdfDocument.writeTo(FileOutputStream(file))
                            }
                            pdfDocument.close()
                            
                            if (continuation.isActive) continuation.resume("Saved PDF to Downloads/$fileName")
                        } catch(e: Exception) {
                            if (continuation.isActive) continuation.resumeWithException(e)
                        }
                    }, 1500)
                }
            }
            val html = generateHtml(context, invoice, client, profile)
            
            val w = 2380; val h = 3368
            webView.measure(android.view.View.MeasureSpec.makeMeasureSpec(w, android.view.View.MeasureSpec.EXACTLY), android.view.View.MeasureSpec.makeMeasureSpec(h, android.view.View.MeasureSpec.EXACTLY))
            webView.layout(0, 0, w, h)
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            continuation.invokeOnCancellation { webView.destroy() }
        }
    }

    suspend fun saveImage(context: Context, invoice: Invoice, client: Client?, profile: BusinessProfile): Result<String> = runCatching {
        // Fallback for image generation since PrintDocumentAdapter creates PDF only.
        // We will do an offscreen WebView snapshot to a Bitmap just for image export.
        suspendCancellableCoroutine<String> { continuation ->
            val webView = WebView(context)
        webView.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
            webView.settings.javaScriptEnabled = false
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    val w = 2380; val h = 3368
                    view.measure(android.view.View.MeasureSpec.makeMeasureSpec(w, android.view.View.MeasureSpec.EXACTLY), android.view.View.MeasureSpec.makeMeasureSpec(h, android.view.View.MeasureSpec.EXACTLY))
                    view.layout(0, 0, w, h)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        val c = Canvas(bitmap)
                        view.draw(c)
                        
                        try {
                            val fileName = "${invoice.number}.png"
                            if (Build.VERSION.SDK_INT >= 29) {
                                val values = ContentValues().apply {
                                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                                }
                                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("Unable to create image entry")
                                context.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } ?: error("Unable to write image")
                            } else {
                                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                dir.mkdirs()
                                val file = java.io.File(dir, fileName)
                                java.io.FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                            }
                            if (continuation.isActive) continuation.resume("Saved image to Downloads/$fileName")
                        } catch(e: Exception) {
                            if (continuation.isActive) continuation.resumeWithException(e)
                        }
                    }, 1500)
                }
            }
            val html = generateHtml(context, invoice, client, profile)
            
            val w = 2380; val h = 3368
            webView.measure(android.view.View.MeasureSpec.makeMeasureSpec(w, android.view.View.MeasureSpec.EXACTLY), android.view.View.MeasureSpec.makeMeasureSpec(h, android.view.View.MeasureSpec.EXACTLY))
            webView.layout(0, 0, w, h)
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            continuation.invokeOnCancellation { webView.destroy() }
        }
    }

    suspend fun printInvoice(context: Context, invoice: Invoice, client: Client?, profile: BusinessProfile): Result<String> = runCatching {
        suspendCancellableCoroutine<String> { continuation ->
            val webView = WebView(context)
        webView.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
            webView.settings.javaScriptEnabled = false
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    val adapter = view.createPrintDocumentAdapter("Invoice_${invoice.number}")
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                    printManager.print("Invoice_${invoice.number}", adapter, PrintAttributes.Builder().build())
                    if (continuation.isActive) continuation.resume("Sent to printer")
                }
            }
            val html = generateHtml(context, invoice, client, profile)
            
            val w = 2380; val h = 3368
            webView.measure(android.view.View.MeasureSpec.makeMeasureSpec(w, android.view.View.MeasureSpec.EXACTLY), android.view.View.MeasureSpec.makeMeasureSpec(h, android.view.View.MeasureSpec.EXACTLY))
            webView.layout(0, 0, w, h)
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            continuation.invokeOnCancellation { webView.destroy() }
        }
    }
}
