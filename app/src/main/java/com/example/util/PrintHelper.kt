package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.StoreSettingsEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionItemEntity
import java.io.File
import java.io.FileOutputStream

object PrintHelper {

    fun generateReceiptText(
        settings: StoreSettingsEntity,
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ): String {
        val cleanItems = TransactionItemHelper.deduplicateItems(items, transaction.totalAmount)
        val sb = StringBuilder()
        val line = "--------------------------------"
        val doubleLine = "================================"

        sb.append(doubleLine).append("\n")
        sb.append("        ").append(settings.storeName).append("\n")
        sb.append("   ").append(settings.storeAddress).append("\n")
        if (settings.storePhone.isNotBlank()) {
            sb.append("         Telp: ").append(settings.storePhone).append("\n")
        }
        sb.append(doubleLine).append("\n")
        sb.append("No. Struk : ").append(transaction.invoiceNumber).append("\n")
        sb.append("Waktu     : ").append(DateFormatter.formatReceiptDateTime(transaction.timestamp)).append("\n")
        sb.append("Kasir     : ").append(settings.storeName).append("\n")
        sb.append(line).append("\n")

        for (item in cleanItems) {
            sb.append(item.productName).append("\n")
            val qtyPrice = "  ${item.quantity} x ${CurrencyFormatter.formatRupiah(item.unitPrice)}"
            val subtotal = CurrencyFormatter.formatRupiah(item.subtotal)
            val spaces = " ".repeat((32 - qtyPrice.length - subtotal.length).coerceAtLeast(1))
            sb.append(qtyPrice).append(spaces).append(subtotal).append("\n")
        }

        sb.append(line).append("\n")
        val totalLabel = "TOTAL BELANJA:"
        val totalVal = CurrencyFormatter.formatRupiah(transaction.totalAmount)
        val spTotal = " ".repeat((32 - totalLabel.length - totalVal.length).coerceAtLeast(1))
        sb.append(totalLabel).append(spTotal).append(totalVal).append("\n")

        val payMethod = if (transaction.paymentType == "NON_TUNAI") "NON-TUNAI (QRIS/TRANSFER)" else "TUNAI"
        sb.append("Metode    : ").append(payMethod).append("\n")

        val bayarLabel = "Bayar     :"
        val bayarVal = CurrencyFormatter.formatRupiah(transaction.paidAmount)
        val spBayar = " ".repeat((32 - bayarLabel.length - bayarVal.length).coerceAtLeast(1))
        sb.append(bayarLabel).append(spBayar).append(bayarVal).append("\n")

        val kembaliLabel = "Kembalian :"
        val kembaliVal = CurrencyFormatter.formatRupiah(transaction.changeAmount)
        val spKembali = " ".repeat((32 - kembaliLabel.length - kembaliVal.length).coerceAtLeast(1))
        sb.append(kembaliLabel).append(spKembali).append(kembaliVal).append("\n")

        sb.append(doubleLine).append("\n")
        sb.append(settings.receiptFooter).append("\n")
        sb.append(doubleLine).append("\n")

        return sb.toString()
    }

    /**
     * Membagikan struk transaksi dalam format gambar .jpeg
     */
    fun shareReceipt(
        context: Context,
        settings: StoreSettingsEntity,
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ) {
        try {
            val jpegFile = generateReceiptJpegFile(context, settings, transaction, items)
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                jpegFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Struk Pembelian - ${settings.storeName} (${transaction.invoiceNumber})")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Struk Pembelian ${settings.storeName}\nNo. Struk: ${transaction.invoiceNumber}\nTotal: ${CurrencyFormatter.formatRupiah(transaction.totalAmount)}"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Bagikan Struk (.jpeg)")
            if (context !is Activity) {
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membagikan struk gambar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Membuat file gambar .jpeg dari struk transaksi
     */
    fun generateReceiptJpegFile(
        context: Context,
        settings: StoreSettingsEntity,
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ): File {
        val bitmap = createReceiptBitmap(context, settings, transaction, items)
        val imagesDir = File(context.cacheDir, "receipt_images")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }

        val safeInvoice = transaction.invoiceNumber.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val jpegFile = File(imagesDir, "struk_${safeInvoice}.jpeg")

        FileOutputStream(jpegFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            out.flush()
        }

        return jpegFile
    }

    /**
     * Menggambar struk belanja thermal beresolusi tinggi ke dalam Bitmap
     */
    fun createReceiptBitmap(
        context: Context,
        settings: StoreSettingsEntity,
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ): Bitmap {
        val cleanItems = TransactionItemHelper.deduplicateItems(items, transaction.totalAmount)
        val bitmapWidth = 600
        val horizontalPadding = 36f
        val printableWidth = bitmapWidth - (horizontalPadding * 2)

        // Paints
        val storeTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 28f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val storeSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 19f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val normalTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 20f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        val boldTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 21f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val rightNormalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 20f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        val rightBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 21f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val totalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 24f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val totalValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D4ED8")
            textSize = 25f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val changePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#16A34A")
            textSize = 21f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 18f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val solidLinePaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        val dashedLinePaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 2f
            pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
            style = Paint.Style.STROKE
        }

        // Kalkulasi tinggi dinamis struk
        val storeNameLines = wrapText(settings.storeName, storeTitlePaint, printableWidth)
        val storeAddressLines = wrapText(settings.storeAddress, storeSubPaint, printableWidth)
        val storePhoneLines = if (settings.storePhone.isNotBlank()) {
            wrapText("Telp: ${settings.storePhone}", storeSubPaint, printableWidth)
        } else emptyList()

        val footerLines = wrapMultilineText(settings.receiptFooter, footerPaint, printableWidth)

        // Item height calculations
        var itemsTotalHeight = 0
        val preparedItems = cleanItems.map { item ->
            val nameLines = wrapText(item.productName, boldTextPaint, printableWidth)
            val heightForItem = (nameLines.size * 26) + 30 + 10
            itemsTotalHeight += heightForItem
            Pair(item, nameLines)
        }

        var estimatedHeight = 40 // top margin
        // Logo if present
        estimatedHeight += 74 // logo space
        estimatedHeight += storeNameLines.size * 34
        estimatedHeight += storeAddressLines.size * 26
        estimatedHeight += storePhoneLines.size * 26
        estimatedHeight += 20 // spacing
        estimatedHeight += 12 // solid line
        estimatedHeight += 3 * 28 // Invoice, Date, Kasir
        estimatedHeight += 16 // dashed line
        estimatedHeight += itemsTotalHeight
        estimatedHeight += 16 // dashed line
        estimatedHeight += 34 // TOTAL
        estimatedHeight += 28 // Metode
        estimatedHeight += 28 // Bayar
        estimatedHeight += 28 // Kembalian
        estimatedHeight += 16 // solid line
        estimatedHeight += footerLines.size * 26
        estimatedHeight += 50 // bottom margin

        val bitmap = Bitmap.createBitmap(bitmapWidth, estimatedHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // Background putih bersih untuk struk kertas dan kompatibilitas .jpeg
        canvas.drawColor(Color.WHITE)

        val centerX = bitmapWidth / 2f
        val leftX = horizontalPadding
        val rightX = bitmapWidth - horizontalPadding
        var currentY = 36f

        // Gambar Logo Toko Subur jika ada
        try {
            val logoResId = R.drawable.img_toko_subur_logo_1790711721794
            val logoBitmap = BitmapFactory.decodeResource(context.resources, logoResId)
            if (logoBitmap != null) {
                val logoSize = 64
                val logoLeft = (bitmapWidth - logoSize) / 2
                val destRect = Rect(logoLeft, currentY.toInt(), logoLeft + logoSize, (currentY + logoSize).toInt())
                canvas.drawBitmap(logoBitmap, null, destRect, null)
                currentY += logoSize + 12f
            }
        } catch (_: Exception) {
            // Lanjutkan jika logo tidak ditemukan
        }

        // Header Toko
        for (line in storeNameLines) {
            canvas.drawText(line, centerX, currentY + 24f, storeTitlePaint)
            currentY += 32f
        }
        for (line in storeAddressLines) {
            canvas.drawText(line, centerX, currentY + 18f, storeSubPaint)
            currentY += 24f
        }
        for (line in storePhoneLines) {
            canvas.drawText(line, centerX, currentY + 18f, storeSubPaint)
            currentY += 24f
        }

        currentY += 10f
        canvas.drawLine(leftX, currentY, rightX, currentY, solidLinePaint)
        currentY += 20f

        // Data Transaksi
        canvas.drawText("No. Struk : ${transaction.invoiceNumber}", leftX, currentY, normalTextPaint)
        currentY += 26f
        canvas.drawText("Waktu     : ${DateFormatter.formatReceiptDateTime(transaction.timestamp)}", leftX, currentY, normalTextPaint)
        currentY += 26f
        canvas.drawText("Kasir     : ${settings.storeName}", leftX, currentY, normalTextPaint)
        currentY += 14f

        canvas.drawLine(leftX, currentY, rightX, currentY, dashedLinePaint)
        currentY += 22f

        // Daftar Barang Belanjaan
        for ((item, nameLines) in preparedItems) {
            for (nameLine in nameLines) {
                canvas.drawText(nameLine, leftX, currentY, boldTextPaint)
                currentY += 24f
            }
            val qtyUnitPrice = "${item.quantity} x ${CurrencyFormatter.formatRupiah(item.unitPrice)}"
            val subtotal = CurrencyFormatter.formatRupiah(item.subtotal)
            canvas.drawText(qtyUnitPrice, leftX, currentY, normalTextPaint)
            canvas.drawText(subtotal, rightX, currentY, rightNormalPaint)
            currentY += 28f
        }

        canvas.drawLine(leftX, currentY, rightX, currentY, dashedLinePaint)
        currentY += 26f

        // Bagian Total & Pembayaran
        canvas.drawText("TOTAL BELANJA", leftX, currentY, totalLabelPaint)
        canvas.drawText(CurrencyFormatter.formatRupiah(transaction.totalAmount), rightX, currentY, totalValuePaint)
        currentY += 30f

        val payMethod = if (transaction.paymentType == "NON_TUNAI") "NON-TUNAI (QRIS)" else "TUNAI"
        canvas.drawText("Metode Pembayaran", leftX, currentY, normalTextPaint)
        canvas.drawText(payMethod, rightX, currentY, rightBoldPaint)
        currentY += 26f

        canvas.drawText("Nominal Dibayar", leftX, currentY, normalTextPaint)
        canvas.drawText(CurrencyFormatter.formatRupiah(transaction.paidAmount), rightX, currentY, rightNormalPaint)
        currentY += 26f

        canvas.drawText("Kembalian", leftX, currentY, boldTextPaint)
        canvas.drawText(CurrencyFormatter.formatRupiah(transaction.changeAmount), rightX, currentY, changePaint)
        currentY += 16f

        canvas.drawLine(leftX, currentY, rightX, currentY, solidLinePaint)
        currentY += 26f

        // Footer / Pesan Penutup
        for (fLine in footerLines) {
            canvas.drawText(fLine, centerX, currentY, footerPaint)
            currentY += 24f
        }

        return bitmap
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isEmpty()) return emptyList()
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = StringBuilder(testLine)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                } else {
                    var longWord = word
                    while (paint.measureText(longWord) > maxWidth && longWord.isNotEmpty()) {
                        var cutIndex = 1
                        while (cutIndex < longWord.length && paint.measureText(longWord.substring(0, cutIndex + 1)) <= maxWidth) {
                            cutIndex++
                        }
                        lines.add(longWord.substring(0, cutIndex))
                        longWord = longWord.substring(cutIndex)
                    }
                    if (longWord.isNotEmpty()) {
                        currentLine = StringBuilder(longWord)
                    }
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }

    private fun wrapMultilineText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        val paragraphs = text.split("\n")
        for (p in paragraphs) {
            if (p.isBlank()) {
                result.add("")
            } else {
                result.addAll(wrapText(p, paint, maxWidth))
            }
        }
        return result
    }

    fun printReceipt(
        context: Context,
        settings: StoreSettingsEntity,
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ) {
        val htmlContent = buildHtmlReceipt(settings, transaction, items)

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = false

            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val printAdapter = webView.createPrintDocumentAdapter(transaction.invoiceNumber)
                val printAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A6)
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                printManager?.print(
                    "Struk_${transaction.invoiceNumber}",
                    printAdapter,
                    printAttributes
                )
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun buildHtmlReceipt(
        settings: StoreSettingsEntity,
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ): String {
        val cleanItems = TransactionItemHelper.deduplicateItems(items, transaction.totalAmount)
        val itemsHtml = StringBuilder()
        for (item in cleanItems) {
            itemsHtml.append(
                """
                <tr>
                    <td colspan="2" style="font-weight:bold; padding-top:4px;">${item.productName}</td>
                </tr>
                <tr>
                    <td style="color:#555;">${item.quantity} x ${CurrencyFormatter.formatRupiah(item.unitPrice)}</td>
                    <td style="text-align:right;">${CurrencyFormatter.formatRupiah(item.subtotal)}</td>
                </tr>
                """.trimIndent()
            )
        }

        val payMethod = if (transaction.paymentType == "NON_TUNAI") "NON-TUNAI" else "TUNAI"

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <style>
                body {
                    font-family: 'Courier New', Courier, monospace;
                    width: 280px;
                    margin: 0 auto;
                    padding: 10px;
                    font-size: 13px;
                    color: #000;
                }
                .text-center { text-align: center; }
                .store-name { font-size: 16px; font-weight: bold; margin-bottom: 2px; }
                .store-addr { font-size: 11px; margin-bottom: 6px; }
                .divider { border-top: 1px dashed #000; margin: 6px 0; }
                .double-divider { border-top: 2px solid #000; margin: 6px 0; }
                table { width: 100%; border-collapse: collapse; }
                td { padding: 2px 0; }
                .bold { font-weight: bold; }
                .footer { font-size: 11px; text-align: center; margin-top: 8px; white-space: pre-line; }
            </style>
        </head>
        <body>
            <div class="text-center">
                <div class="store-name">${settings.storeName}</div>
                <div class="store-addr">${settings.storeAddress}</div>
                ${if (settings.storePhone.isNotBlank()) "<div class='store-addr'>Telp: " + settings.storePhone + "</div>" else ""}
            </div>
            <div class="double-divider"></div>
            <div>
                <div>No: <b>${transaction.invoiceNumber}</b></div>
                <div>Tgl: ${DateFormatter.formatReceiptDateTime(transaction.timestamp)}</div>
            </div>
            <div class="divider"></div>
            <table>
                ${itemsHtml}
            </table>
            <div class="divider"></div>
            <table>
                <tr class="bold">
                    <td>TOTAL:</td>
                    <td style="text-align:right;">${CurrencyFormatter.formatRupiah(transaction.totalAmount)}</td>
                </tr>
                <tr>
                    <td>Metode:</td>
                    <td style="text-align:right;">${payMethod}</td>
                </tr>
                <tr>
                    <td>Bayar:</td>
                    <td style="text-align:right;">${CurrencyFormatter.formatRupiah(transaction.paidAmount)}</td>
                </tr>
                <tr>
                    <td>Kembalian:</td>
                    <td style="text-align:right;">${CurrencyFormatter.formatRupiah(transaction.changeAmount)}</td>
                </tr>
            </table>
            <div class="double-divider"></div>
            <div class="footer">${settings.receiptFooter}</div>
        </body>
        </html>
        """.trimIndent()
    }
}
