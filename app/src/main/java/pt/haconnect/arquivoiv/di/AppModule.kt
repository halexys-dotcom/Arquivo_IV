package pt.haconnect.arquivoiv.di

import android.content.Context
import androidx.room.Room
import pt.haconnect.arquivoiv.data.local.AppDatabase
import pt.haconnect.arquivoiv.data.local.dao.FaturaDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

        /**
     * Fase 2: sem Migration — schema antigo (produtos/documentos) descartado por
     * fallbackToDestructiveMigration(); app ainda sem dados de produção.
     * TODO: substituir por migrations Room explícitas antes da primeira release pública
     *       (ver §7 do documento de separação).
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "arquivoiv_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideFaturaDao(database: AppDatabase): FaturaDao {
        return database.faturaDao()
    }

    @Provides
    fun provideFornecedorDao(database: AppDatabase): pt.haconnect.arquivoiv.data.local.dao.FornecedorDao {
        return database.fornecedorDao()
    }
}








