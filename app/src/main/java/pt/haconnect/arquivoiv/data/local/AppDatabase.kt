package pt.haconnect.arquivoiv.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import pt.haconnect.arquivoiv.data.local.dao.FaturaDao
import pt.haconnect.arquivoiv.data.local.dao.FornecedorDao
import pt.haconnect.arquivoiv.data.local.entity.FaturaEntity
import pt.haconnect.arquivoiv.data.local.entity.FornecedorEntity

/**
 * Fase 9.1: adição da tabela de fornecedores.
 * Versão incrementada (7 → 8) com fallbackToDestructiveMigration(): app ainda
 * em desenvolvimento, sem dados de produção a preservar.
 */
@Database(
    entities = [
        FaturaEntity::class,
        FornecedorEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun faturaDao(): FaturaDao
    abstract fun fornecedorDao(): FornecedorDao
}








