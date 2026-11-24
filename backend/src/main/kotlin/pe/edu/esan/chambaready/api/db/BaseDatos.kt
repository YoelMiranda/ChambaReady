package pe.edu.esan.chambaready.api.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pe.edu.esan.chambaready.api.config.ConexionBd
import javax.sql.DataSource

/**
 * Conexión a PostgreSQL (Neon): pool HikariCP chico, migraciones con Flyway y Exposed para consultar.
 * Neon suspende la base a los 5 minutos sin uso: la primera conexión tarda unos segundos en despertarla,
 * por eso el tiempo de espera es generoso y el pool no mantiene conexiones ociosas.
 */
class BaseDatos(private val dataSource: DataSource) {

    val db: Database = Database.connect(dataSource)

    /** Aplica las migraciones pendientes de `db/migration` (y `db/demo` si se piden datos de prueba). */
    fun migrar(conDatosDemo: Boolean) {
        val ubicaciones = buildList {
            add("classpath:db/migration")
            if (conDatosDemo) add("classpath:db/demo")
        }
        Flyway.configure()
            .dataSource(dataSource)
            .locations(*ubicaciones.toTypedArray())
            .load()
            .migrate()
    }

    /** Ejecuta [bloque] en una transacción, fuera del hilo de Ktor (JDBC bloquea). */
    suspend fun <T> tx(bloque: JdbcTransaction.() -> T): T = withContext(Dispatchers.IO) {
        transaction(db) { bloque() }
    }

    companion object {
        fun crearPool(conexion: ConexionBd, maxConexiones: Int): HikariDataSource = HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = conexion.jdbcUrl
                conexion.usuario?.let { username = it }
                conexion.clave?.let { password = it }
                maximumPoolSize = maxConexiones
                minimumIdle = 0
                idleTimeout = 60_000
                connectionTimeout = 20_000
                // Neon cierra conexiones ociosas; Hikari las renueva antes.
                maxLifetime = 240_000
            },
        )
    }
}
