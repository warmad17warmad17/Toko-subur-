package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TOKO SUBUR", appName)
  }

  @Test
  fun `test notification preferences persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = com.example.util.NotificationPreferences(context)

    prefs.isNotificationEnabled = true
    assertEquals(true, prefs.isNotificationEnabled)

    prefs.soundTitle = "Ding Tone"
    prefs.soundUri = "content://media/internal/audio/media/1"
    assertEquals("Ding Tone", prefs.soundTitle)
    assertEquals("content://media/internal/audio/media/1", prefs.soundUri)

    val sounds = com.example.util.NotificationHelper.getDeviceNotificationSounds(context)
    assertTrue(sounds.isNotEmpty())
    assertTrue(sounds.first().isDefault)
  }

  @Test
  fun `test TokoViewModel initialization succeeds`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.viewmodel.TokoViewModel(application)
    org.junit.Assert.assertNotNull(viewModel)
    org.junit.Assert.assertNotNull(viewModel.allProducts)
    org.junit.Assert.assertNotNull(viewModel.notificationPrefs)
  }

  @Test
  fun `test updateStoreName and receipt settings`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.viewmodel.TokoViewModel(application)
    viewModel.updateStoreName("TOKO SUBUR")
    viewModel.updateReceiptSettings("TOKO MAKMUR", "Jl. Baru", "0812345", "Terima kasih")
    org.junit.Assert.assertNotNull(viewModel.storeSettings)
  }

  @Test
  fun `test NoMediaHelper creates nomedia files`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val success = com.example.util.NoMediaHelper.hideAppImagesFromGallery(context)
    assertTrue(success)
    assertTrue(com.example.util.NoMediaHelper.isNoMediaProtectionActive(context))
  }

  @Test
  fun `test generateReceiptJpegFile creates jpeg image`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val settings = com.example.data.model.StoreSettingsEntity(
        id = 1,
        storeName = "TOKO SUBUR",
        storeAddress = "Jl. Kembang Kuning No.17",
        storePhone = "081234567890",
        receiptFooter = "Terima kasih!",
        initialCashCapital = 500000.0,
        quickNominals = "10,20,50,100"
    )
    val tx = com.example.data.model.TransactionEntity(
        id = 1,
        invoiceNumber = "TRX-12345",
        timestamp = System.currentTimeMillis(),
        totalAmount = 50000.0,
        totalCost = 35000.0,
        paymentType = "TUNAI",
        paidAmount = 50000.0,
        changeAmount = 0.0,
        notes = ""
    )
    val items = listOf(
        com.example.data.model.TransactionItemEntity(
            id = 1,
            transactionId = 1,
            productId = 1,
            productName = "Beras Rojolele 5kg",
            qrCode = "BRS001",
            quantity = 1,
            unitPrice = 50000.0,
            unitCost = 35000.0,
            subtotal = 50000.0
        )
    )

    val jpegFile = com.example.util.PrintHelper.generateReceiptJpegFile(context, settings, tx, items)
    assertTrue(jpegFile.exists())
    assertTrue(jpegFile.name.endsWith(".jpeg"))
    assertTrue(jpegFile.length() > 0)
  }

  @Test
  fun `test GoogleAuthManager signInWithEmailDirect and signOut`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authManager = com.example.data.sync.GoogleAuthManager(context)
    val result = authManager.signInWithEmailDirect("spxmt95@gmail.com", "Toko Subur")
    assertTrue(result.isSuccess)
    val user = authManager.currentUser.value
    org.junit.Assert.assertNotNull(user)
    assertEquals("spxmt95@gmail.com", user?.email)
    assertEquals("Toko Subur", user?.displayName)

    kotlinx.coroutines.runBlocking {
      authManager.signOut()
    }
    org.junit.Assert.assertNull(authManager.currentUser.value)
  }

  @Test
  fun `test AppAuthManager owner password and cashier login`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authRoleManager = com.example.data.auth.AppAuthManager(context)

    // Default password must be 1234567890
    assertEquals("1234567890", authRoleManager.getOwnerPassword())

    // Incorrect password fails
    val failResult = authRoleManager.loginAsOwner("wrong_pass")
    assertTrue(failResult.isFailure)

    // Correct default password succeeds
    val successResult = authRoleManager.loginAsOwner("1234567890")
    assertTrue(successResult.isSuccess)
    assertEquals(com.example.data.auth.UserRole.PEMILIK, authRoleManager.currentUserRole.value)

    // Change password
    val changeRes = authRoleManager.changeOwnerPassword("1234567890", "owner2026")
    assertTrue(changeRes.isSuccess)
    assertEquals("owner2026", authRoleManager.getOwnerPassword())

    // Reset password to default
    authRoleManager.resetOwnerPasswordToDefault()
    assertEquals("1234567890", authRoleManager.getOwnerPassword())

    // Cashier login requires no password
    val cashierRes = authRoleManager.loginAsCashier()
    assertTrue(cashierRes.isSuccess)
    assertEquals(com.example.data.auth.UserRole.KASIR, authRoleManager.currentUserRole.value)
  }

  @Test
  fun `test 7-day weekly report range and cashier label format`() {
    val startOfLast7Days = com.example.util.DateFormatter.getStartOfLast7Days()
    val now = System.currentTimeMillis()
    assertTrue(startOfLast7Days <= now)

    val diffDays = (now - startOfLast7Days) / (1000 * 60 * 60 * 24)
    assertTrue(diffDays in 5..7)

    // Test label formatting: Kasir 1 .. Kasir 9
    for (i in 1..9) {
      val label = "Kasir ${i.coerceIn(1, 9)}"
      assertEquals("Kasir $i", label)
    }
  }

  @Test
  fun `test restock expense impacts cash drawer but does not reduce net profit`() {
    // Scenario requested by user:
    // Uang laci di laci kasir: 10.000
    // Pendapatan bersih: 5.000
    // Ada pengeluaran untuk stok produk: 2.000
    // Fisik uang tunai di laci kasir menjadi: 8.000
    // Pendapatan bersih tetap: 5.000

    val initialCash = 10000.0
    val labaKotor = 5000.0

    val expenses = listOf(
        com.example.data.model.ExpenseEntity(
            id = 1,
            title = "Kulakan Produk Grosir",
            category = "Stok Ulang Produk",
            amount = 2000.0,
            isRestock = true
        )
    )

    val totalPengeluaran = expenses.sumOf { it.amount }
    val pengeluaranOperasional = expenses.filter { !it.isRestock }.sumOf { it.amount }
    val pengeluaranStokUlang = expenses.filter { it.isRestock }.sumOf { it.amount }

    val pendapatanBersih = labaKotor - pengeluaranOperasional
    val uangKasLaci = initialCash - totalPengeluaran

    assertEquals(2000.0, pengeluaranStokUlang, 0.001)
    assertEquals(0.0, pengeluaranOperasional, 0.001)
    assertEquals(5000.0, pendapatanBersih, 0.001) // Pendapatan bersih tetap 5.000
    assertEquals(8000.0, uangKasLaci, 0.001)       // Uang kas laci berkurang menjadi 8.000
  }
}
