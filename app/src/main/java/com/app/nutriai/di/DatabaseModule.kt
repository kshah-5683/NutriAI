package com.app.nutriai.di

import android.content.Context
import androidx.room.Room
import com.app.nutriai.data.local.NutriAiDatabase
import com.app.nutriai.data.local.migrations.Migrations
import com.app.nutriai.data.local.dao.CatalogDao
import com.app.nutriai.data.local.dao.DailyLogDao
import com.app.nutriai.data.local.dao.FoodItemDao
import com.app.nutriai.data.local.dao.IfctFoodDao
import com.app.nutriai.data.local.dao.LabelPhotoDao
import com.app.nutriai.data.local.dao.UserDao
import com.app.nutriai.data.local.dao.UserPreferencesDao
import com.app.nutriai.data.repository.AiRepositoryImpl
import com.app.nutriai.data.repository.AuthRepositoryImpl
import com.app.nutriai.data.repository.CatalogRepositoryImpl
import com.app.nutriai.data.repository.DailyLogRepositoryImpl
import com.app.nutriai.data.repository.FoodRepositoryImpl
import com.app.nutriai.data.repository.NutritionRepositoryImpl
import com.app.nutriai.data.repository.SyncRepositoryImpl
import com.app.nutriai.data.repository.UserRepositoryImpl
import com.app.nutriai.domain.repository.AiRepository
import com.app.nutriai.domain.repository.AuthRepository
import com.app.nutriai.domain.repository.CatalogRepository
import com.app.nutriai.domain.repository.DailyLogRepository
import com.app.nutriai.domain.repository.FoodRepository
import com.app.nutriai.domain.repository.NutritionRepository
import com.app.nutriai.domain.repository.SyncRepository
import com.app.nutriai.domain.repository.UserRepository
import com.app.nutriai.util.Constants
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Room database, DAOs, and repository bindings.
 *
 * - [DatabaseProviderModule] uses @Provides for concrete instances (Room DB + DAOs).
 * - [RepositoryBindingsModule] uses @Binds for interface → implementation mappings.
 *
 * Migration strategy: explicit migrations from v3 onward via [Migrations.ALL].
 * Destructive fallback is limited to pre-release versions 1 and 2 only.
 *
 * Phase 5: Added [NutritionRepository] binding → [NutritionRepositoryImpl].
 * Phase 5.5: Added [IfctFoodDao] provision for offline IFCT 2017 nutrition data.
 * Phase 6: Added [AuthRepository] → [AuthRepositoryImpl] and [SyncRepository] → [SyncRepositoryImpl].
 * Phase 8 Pre-work II: Replaced fallbackToDestructiveMigration() with explicit migrations.
 *                      SyncPushManager uses @Singleton + @Inject constructor — auto-provided by Hilt.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseProviderModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): NutriAiDatabase {
        return Room.databaseBuilder(
            context,
            NutriAiDatabase::class.java,
            Constants.DATABASE_NAME
        )
            .fallbackToDestructiveMigrationFrom(1, 2) // pre-release versions only
            .addMigrations(*Migrations.ALL)
            .build()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: NutriAiDatabase): UserDao = database.userDao()

    @Provides
    @Singleton
    fun provideCatalogDao(database: NutriAiDatabase): CatalogDao = database.catalogDao()

    @Provides
    @Singleton
    fun provideFoodItemDao(database: NutriAiDatabase): FoodItemDao = database.foodItemDao()

    @Provides
    @Singleton
    fun provideDailyLogDao(database: NutriAiDatabase): DailyLogDao = database.dailyLogDao()

    /**
     * Phase 5.5: Provides [IfctFoodDao] for offline Indian food nutrition lookups.
     * Data is seeded from assets/ifct2017.csv by [com.app.nutriai.util.IfctCsvLoader].
     */
    @Provides
    @Singleton
    fun provideIfctFoodDao(database: NutriAiDatabase): IfctFoodDao = database.ifctFoodDao()

    /**
     * Phase 11: Provides [LabelPhotoDao] for nutrition label photo metadata.
     * Photos are stored as JPEG files in filesDir/label_photos/;
     * this DAO manages the metadata rows used for TTL-based cleanup.
     */
    @Provides
    @Singleton
    fun provideLabelPhotoDao(database: NutriAiDatabase): LabelPhotoDao = database.labelPhotoDao()

    /**
     * Phase 14: Provides [UserPreferencesDao] for macro goals cross-platform sync.
     * Backs the new Room-based [UserPreferences] — replaces DataStore for goal storage.
     */
    @Provides
    @Singleton
    fun provideUserPreferencesDao(database: NutriAiDatabase): UserPreferencesDao = database.userPreferencesDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingsModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindCatalogRepository(impl: CatalogRepositoryImpl): CatalogRepository

    @Binds
    @Singleton
    abstract fun bindFoodRepository(impl: FoodRepositoryImpl): FoodRepository

    @Binds
    @Singleton
    abstract fun bindDailyLogRepository(impl: DailyLogRepositoryImpl): DailyLogRepository

    @Binds
    @Singleton
    abstract fun bindAiRepository(impl: AiRepositoryImpl): AiRepository

    /**
     * Phase 5: Binds [NutritionRepositoryImpl] (backed by USDA FDC + IFCT 2017) to [NutritionRepository].
     */
    @Binds
    @Singleton
    abstract fun bindNutritionRepository(impl: NutritionRepositoryImpl): NutritionRepository

    /**
     * Phase 6: Binds [AuthRepositoryImpl] (GoTrue REST + DataStore) to [AuthRepository].
     */
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    /**
     * Phase 6: Binds [SyncRepositoryImpl] (PostgREST push/pull) to [SyncRepository].
     */
    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository
}
