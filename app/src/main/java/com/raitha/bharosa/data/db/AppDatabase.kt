package com.raitha.bharosa.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Single Room database instance for the whole app.
 *
 * Version history
 * ───────────────
 * 1  – initial schema: mandi_prices table (deprecated - kept for data migration)
 * 2  – added Task, SoilData, FarmerProfile entities
 * 3  – added Agricultural Labour Booking System entities (LabourerProfile, Booking, Rating, 
 *      WorkHistory, Attendance, NotificationPreferences, SyncQueueItem)
 *      Removed MandiEntity from active entities (table kept for migration)
 */
@Database(
    entities = [
        com.raitha.bharosa.data.Task::class,
        com.raitha.bharosa.data.SoilData::class,
        com.raitha.bharosa.data.FarmerProfile::class,
        com.raitha.bharosa.data.LabourerProfile::class,
        com.raitha.bharosa.data.Booking::class,
        com.raitha.bharosa.data.Rating::class,
        com.raitha.bharosa.data.WorkHistory::class,
        com.raitha.bharosa.data.Attendance::class,
        com.raitha.bharosa.data.NotificationPreferences::class,
        com.raitha.bharosa.data.SyncQueueItem::class
    ],
    version = 3,
    exportSchema = false
)
@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    // MandiDao removed - Mandi feature replaced with Labour Booking System
    abstract fun userDao(): UserDao
    abstract fun labourDao(): LabourDao
    abstract fun bookingDao(): BookingDao
    abstract fun ratingDao(): RatingDao
    abstract fun workHistoryDao(): WorkHistoryDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun notificationPreferencesDao(): NotificationPreferencesDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        private const val DB_NAME = "raitha_bharosa.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** Thread-safe singleton accessor. */
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()   // safe for development — add proper migrations later
                    .build()
                    .also { INSTANCE = it }
            }
        
        /**
         * Migration from version 2 to version 3
         * Adds Agricultural Labour Booking System tables and updates existing profile table
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create labourer_profiles table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS labourer_profiles (
                        userId TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        phoneNumber TEXT NOT NULL,
                        age INTEGER NOT NULL,
                        gender TEXT NOT NULL,
                        village TEXT NOT NULL,
                        district TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        skills TEXT NOT NULL,
                        experienceYears TEXT NOT NULL,
                        pricingType TEXT NOT NULL,
                        dailyWage INTEGER,
                        hourlyRate INTEGER,
                        profilePhotoUrls TEXT NOT NULL,
                        availabilityStatus TEXT NOT NULL,
                        futureAvailability TEXT,
                        averageRating REAL NOT NULL,
                        totalRatings INTEGER NOT NULL,
                        completedBookings INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        lastAvailabilityUpdate INTEGER NOT NULL,
                        preferredLanguage TEXT NOT NULL
                    )
                """)
                
                // Create bookings table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS bookings (
                        bookingId TEXT PRIMARY KEY NOT NULL,
                        farmerId TEXT NOT NULL,
                        farmerName TEXT NOT NULL,
                        farmerPhone TEXT NOT NULL,
                        farmerLocation TEXT NOT NULL,
                        labourerId TEXT NOT NULL,
                        labourerName TEXT NOT NULL,
                        labourerPhone TEXT NOT NULL,
                        workDate INTEGER NOT NULL,
                        startTime TEXT NOT NULL,
                        estimatedHours INTEGER NOT NULL,
                        workType TEXT NOT NULL,
                        specialInstructions TEXT,
                        isEmergency INTEGER NOT NULL,
                        estimatedPayment INTEGER NOT NULL,
                        actualHours INTEGER,
                        actualPayment INTEGER,
                        status TEXT NOT NULL,
                        paymentStatus TEXT NOT NULL,
                        paymentTransactionId TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        acceptedAt INTEGER,
                        completedAt INTEGER,
                        cancelledAt INTEGER,
                        cancellationReason TEXT
                    )
                """)
                
                // Create ratings table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS ratings (
                        ratingId TEXT PRIMARY KEY NOT NULL,
                        bookingId TEXT NOT NULL,
                        labourerId TEXT NOT NULL,
                        farmerId TEXT NOT NULL,
                        farmerName TEXT NOT NULL,
                        rating INTEGER NOT NULL,
                        review TEXT,
                        createdAt INTEGER NOT NULL
                    )
                """)
                
                // Create work_history table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS work_history (
                        workId TEXT PRIMARY KEY NOT NULL,
                        labourerId TEXT NOT NULL,
                        bookingId TEXT NOT NULL,
                        farmerName TEXT NOT NULL,
                        workDate INTEGER NOT NULL,
                        workType TEXT NOT NULL,
                        actualHours INTEGER NOT NULL,
                        paymentReceived INTEGER NOT NULL,
                        ratingReceived INTEGER,
                        completedAt INTEGER NOT NULL
                    )
                """)
                
                // Create attendance table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS attendance (
                        attendanceId TEXT PRIMARY KEY NOT NULL,
                        bookingId TEXT NOT NULL,
                        labourerId TEXT NOT NULL,
                        checkInTime INTEGER,
                        checkOutTime INTEGER,
                        actualHours INTEGER,
                        notes TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)
                
                // Create notification_preferences table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS notification_preferences (
                        userId TEXT PRIMARY KEY NOT NULL,
                        smsEnabled INTEGER NOT NULL,
                        whatsappEnabled INTEGER NOT NULL,
                        inAppEnabled INTEGER NOT NULL,
                        newBookingsEnabled INTEGER NOT NULL,
                        bookingConfirmationsEnabled INTEGER NOT NULL,
                        paymentConfirmationsEnabled INTEGER NOT NULL,
                        ratingsReceivedEnabled INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)
                
                // Create sync_queue table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_queue (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entityType TEXT NOT NULL,
                        entityId TEXT NOT NULL,
                        operation TEXT NOT NULL,
                        data TEXT NOT NULL,
                        retryCount INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        lastAttempt INTEGER
                    )
                """)
                
                // Update existing profile table with new columns for labour system integration
                // Using ALTER TABLE to add columns with default values
                database.execSQL("ALTER TABLE profile ADD COLUMN phoneNumber TEXT DEFAULT ''")
                database.execSQL("ALTER TABLE profile ADD COLUMN latitude REAL DEFAULT 0.0")
                database.execSQL("ALTER TABLE profile ADD COLUMN longitude REAL DEFAULT 0.0")
                database.execSQL("ALTER TABLE profile ADD COLUMN createdAt INTEGER DEFAULT 0")
                database.execSQL("ALTER TABLE profile ADD COLUMN updatedAt INTEGER DEFAULT 0")
                database.execSQL("ALTER TABLE profile ADD COLUMN preferredLanguage TEXT DEFAULT 'en'")
            }
        }
    }
}
