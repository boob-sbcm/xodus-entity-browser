package jetbrains.xodus.browser.web.db

import jetbrains.xodus.browser.web.DBSummary
import jetbrains.xodus.browser.web.NotFoundException
import jetbrains.xodus.browser.web.mapper
import jetbrains.xodus.browser.web.systemOr
import java.io.File
import java.util.*


object Databases {

    internal val file by lazy {
        File("recent.dbs" systemOr "./recent-dbs.json")
    }

    private val dbs by lazy {
        val result = arrayListOf<DBSummary>()
        val canonicalFile = File(file.canonicalPath)
        if (!canonicalFile.exists()) {
            val parent = canonicalFile.parentFile
            if (!parent.exists() && !parent.mkdirs()) {
                throw IllegalStateException("can't create folder ${parent.canonicalPath}")
            }
            if (!canonicalFile.createNewFile()) {
                throw IllegalStateException("can't create file ${canonicalFile.absolutePath}")
            }
        }
        try {
            result.addAll(fileDBs())
        } catch (e: Exception) {
            // ignore
        }
        result
    }

    fun add(dbSummary: DBSummary, tweak: DBSummary.() -> Unit): DBSummary {
        val uuid = UUID.randomUUID().toString()
        saveWith {
            val copy = dbSummary.copy(uuid = uuid, isOpened = false)
            dbs.add(copy)
            copy.tweak()
        }
        return find(uuid) {
            throw NotFoundException("Database on '${dbSummary.location}' is already registered")
        }
    }

    fun applyChange(uuid: String, call: DBSummary.() -> Unit): DBSummary {
        val dbCopy = find(uuid) {
            throw NotFoundException("Database not found by id '$uuid'")
        }
        val location = dbCopy.location
        saveWith {
            dbCopy.call()
        }
        return find(uuid) {
            throw NotFoundException("Database on '$location' can't be modified")
        }
    }

    fun delete(uuid: String) {
        saveWith {
            dbs.removeAll { it.uuid == uuid }
        }
    }

    fun all(): List<DBSummary> {
        return dbs.toList()
    }

    internal fun find(uuid: String, error: () -> Nothing = {
        throw NotFoundException("Database not found by id '$uuid'")
    }) = dbs.firstOrNull { it.uuid == uuid } ?: error()


    private fun saveWith(call: () -> Unit) {
        synchronized(this) {
            try {
                call()
                doSync()
            } catch (e: Exception) {
                revert()
                throw e
            }
        }
    }

    private fun doSync() {
        val copy = dbs.toList()
        val distinct = copy.distinctBy { it.location to it.key }
        mapper.writeValue(file, distinct)
        dbs.clear()
        dbs.addAll(distinct)
    }

    private fun revert() {
        dbs.clear()
        dbs.addAll(fileDBs())
    }

    private fun fileDBs(): List<DBSummary> {
        try {
            val type = mapper.typeFactory.constructCollectionType(List::class.java, DBSummary::class.java)
            return mapper.readValue<List<DBSummary>>(File(file.canonicalPath), type)
        } catch (e: Exception) {
            // ignore
            return arrayListOf<DBSummary>()
        }
    }
}
