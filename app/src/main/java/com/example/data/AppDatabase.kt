package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.CategoryDao
import com.example.data.dao.ExcludedPromoProductDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.ProductDao
import com.example.data.dao.StoreSettingsDao
import com.example.data.dao.TransactionDao
import com.example.data.model.CategoryEntity
import com.example.data.model.ExcludedPromoProductEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ProductEntity
import com.example.data.model.StoreSettingsEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionItemEntity
import androidx.room.migration.Migration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        ExpenseEntity::class,
        StoreSettingsEntity::class,
        ExcludedPromoProductEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun storeSettingsDao(): StoreSettingsDao
    abstract fun excludedPromoProductDao(): ExcludedPromoProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `excluded_promo_products` (
                        `productId` INTEGER NOT NULL PRIMARY KEY,
                        `productName` TEXT NOT NULL,
                        `reason` TEXT NOT NULL,
                        `excludedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `store_settings` ADD COLUMN `topSellingLimit` INTEGER NOT NULL DEFAULT 10"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `expenses` ADD COLUMN `isRestock` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "toko_makmur_db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration(true)
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(database: AppDatabase) {
                val settingsDao = database.storeSettingsDao()
                val categoryDao = database.categoryDao()
                val productDao = database.productDao()
                val expenseDao = database.expenseDao()

                // Initial store settings
                settingsDao.insertOrUpdateSettings(
                    StoreSettingsEntity(
                        id = 1,
                        storeName = "TOKO SUBUR",
                        storeAddress = "Jl. Kembang Kuning No.17, Surabaya",
                        storePhone = "0812-3456-7890",
                        receiptFooter = "Terima kasih telah berbelanja di TOKO SUBUR!\nBarang yang sudah dibeli tidak dapat ditukar.",
                        initialCashCapital = 1000000.0
                    )
                )

                // Initial categories
                val cat1Id = categoryDao.insertCategory(CategoryEntity(name = "Sembako & Beras"))
                val cat2Id = categoryDao.insertCategory(CategoryEntity(name = "Minyak & Bumbu"))
                val cat3Id = categoryDao.insertCategory(CategoryEntity(name = "Minuman & Kopi"))
                val cat4Id = categoryDao.insertCategory(CategoryEntity(name = "Makanan Ringan"))
                val cat5Id = categoryDao.insertCategory(CategoryEntity(name = "Kebersihan & Mandi"))

                // Initial products
                val sampleProducts = listOf(
                    ProductEntity(
                        name = "Beras Rojolele 5kg",
                        categoryId = cat1Id,
                        categoryName = "Sembako & Beras",
                        qrCode = "8991001",
                        hargaBeli = 65000.0,
                        hargaJual = 72000.0,
                        stok = 15,
                        minimumStokAlert = 5
                    ),
                    ProductEntity(
                        name = "Gula Pasir Gulaku 1kg",
                        categoryId = cat1Id,
                        categoryName = "Sembako & Beras",
                        qrCode = "8991002",
                        hargaBeli = 15500.0,
                        hargaJual = 18000.0,
                        stok = 4, // Low stock sample!
                        minimumStokAlert = 5
                    ),
                    ProductEntity(
                        name = "Minyak Goreng Bimoli 2L",
                        categoryId = cat2Id,
                        categoryName = "Minyak & Bumbu",
                        qrCode = "8991003",
                        hargaBeli = 32000.0,
                        hargaJual = 36000.0,
                        stok = 12,
                        minimumStokAlert = 5
                    ),
                    ProductEntity(
                        name = "Kecap Manis Bango 520ml",
                        categoryId = cat2Id,
                        categoryName = "Minyak & Bumbu",
                        qrCode = "8991004",
                        hargaBeli = 20000.0,
                        hargaJual = 23500.0,
                        stok = 8,
                        minimumStokAlert = 3
                    ),
                    ProductEntity(
                        name = "Kopi Kapal Api Spesial 165g",
                        categoryId = cat3Id,
                        categoryName = "Minuman & Kopi",
                        qrCode = "8991005",
                        hargaBeli = 12500.0,
                        hargaJual = 15000.0,
                        stok = 2, // Low stock sample!
                        minimumStokAlert = 5
                    ),
                    ProductEntity(
                        name = "Teh Celup Sariwangi 25s",
                        categoryId = cat3Id,
                        categoryName = "Minuman & Kopi",
                        qrCode = "8991006",
                        hargaBeli = 5500.0,
                        hargaJual = 7000.0,
                        stok = 20,
                        minimumStokAlert = 5
                    ),
                    ProductEntity(
                        name = "Indomie Goreng Original",
                        categoryId = cat4Id,
                        categoryName = "Makanan Ringan",
                        qrCode = "8991007",
                        hargaBeli = 2800.0,
                        hargaJual = 3500.0,
                        stok = 60,
                        minimumStokAlert = 10
                    ),
                    ProductEntity(
                        name = "Sabun Mandi Lifebuoy 110g",
                        categoryId = cat5Id,
                        categoryName = "Kebersihan & Mandi",
                        qrCode = "8991008",
                        hargaBeli = 4000.0,
                        hargaJual = 5500.0,
                        stok = 1, // Alert: tinggal 1!
                        minimumStokAlert = 5
                    )
                )

                for (prod in sampleProducts) {
                    productDao.insertProduct(prod)
                }

                // Initial sample expense
                expenseDao.insertExpense(
                    ExpenseEntity(
                        title = "Plastik & Kresek Toko",
                        category = "Operasional",
                        amount = 45000.0,
                        notes = "Beli plastik ukuran sedang dan besar"
                    )
                )
            }
        }
    }
}
