package me.constructor

import java.io.File
import org.json.JSONArray

class DatabaseHandler(private val databaseFile: String) {
    private var database: MutableList<String> = mutableListOf()

    init {
        loadDatabase()
    }

    private fun loadDatabase() {
        val file = File(databaseFile)
        if (file.exists()) {
            database = JSONArray(file.readText()).map {
                it.toString()
            }.toMutableList()
            println("Loaded mapping database with ${database.size} entries")
        } else {
            println("Created new mapping database")
        }
    }

    private fun saveDatabase() {
        val jsonArray = JSONArray(database)
        File(databaseFile).writeText(jsonArray.toString())
    }

    fun registerMapAllocation(hashValue: String, oldMapId: Int) {
        if (hashValue !in database) {
            database.add(hashValue)
            println("Registered new map data (current ID: $oldMapId) ${hashValue.shortenHash()} with new ID: ${getMapId(hashValue)}")
            saveDatabase()
        } else {
            val existingIndex = database.indexOf(hashValue)
            println("Duplicate map data ${hashValue.shortenHash()} currently using ID: $oldMapId was already registered with ID: $existingIndex")
        }
    }

    fun getMapId(hashValue: String) =
        database.indexOf(hashValue)

    private fun String.shortenHash(len: Int = 5) =
        substring(0, len) + "..." + substring(length - len)
}
