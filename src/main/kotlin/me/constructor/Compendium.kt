package me.constructor

import net.querz.nbt.CompoundTag
import net.querz.nbt.ListTag
import net.querz.nbt.io.snbt.SNBTWriter
import java.io.File
import kotlin.system.exitProcess

class Compendium(
    inputDir: String = "save",
    mappingFile: String = "hash-mapping.json",
    private val outputDir: String = "remapped",
    private val dumpIDs: Boolean = false
) {
    private val splash = """
        ┏┓            ┓•     
        ┃ ┏┓┏┳┓┏┓┏┓┏┓┏┫┓┓┏┏┳┓
        ┗┛┗┛┛┗┗┣┛┗ ┛┗┗┻┗┗┻┛┗┗
          v1.3 ┛ by Constructor
    """.trimIndent()
    private val dbHandler = DatabaseHandler(mappingFile)
    private val inputFile = File(inputDir)
    private val outputFile = File(outputDir)
    private val mapFile = File(inputDir, "data")
    private val levelDat = File(outputDir, "level.dat")
    private val playerData = File(outputDir, "playerdata")
    private val notFoundFile = File(outputDir, "ids-not-found.txt")
    private val foundFile = File(outputDir, "ids-found.txt")
    private val mapDatFiles = mapFile.listFiles { file ->
        file.name.startsWith("map_") && file.extension == "dat"
    } ?: throw IllegalStateException("No map files found in given directory.")
    private val mcaFiles: List<File> by lazy {
        outputFile.walk().filter { it.isFile && it.extension == "mca" }.toList()
        // ToDo: Respect mcc files
    }
    private val writer = SNBTWriter()
    private val mapMapping = mutableMapOf<Int, Int>()
    private var remapped = 0

    init {
        println(splash + "\n")
        writer.indent("  ")

        copySave()
        updateMapHashDatabase()

        if (mapMapping.isEmpty()) {
            println("No map data found in given directory. Exiting...")
            exitProcess(0)
        }

        remapMapFiles()
        remapRegionFiles()
        remapPlayerData()
        remapLevelDat()

        println("Successfully remapped $remapped map items from ${inputFile.name} to ${outputFile.name}!")
    }

    private fun copySave() {
        inputFile.copyRecursively(outputFile, true)

        File(outputFile, "data").listFiles { it ->
            it.name.startsWith("map_") && it.extension == "dat"
        }?.forEach { it.delete() }
    }

    private fun updateMapHashDatabase() {
        println("Found ${mapDatFiles.size} maps in given directory.")

        "Updating map hash database".toProgress(mapDatFiles.size.toLong()).use { progressBar ->
            mapDatFiles.forEach { mapDat ->
                progressBar.step()
                val currentMapId = mapDat.mapId()

                mapDat.readNBT {
                    val data = getCompoundTag("data") ?: return@readNBT
                    val mapHash = data.getByteArray("colors").digestHex()

                    dbHandler.registerMapAllocation(mapHash, currentMapId)
                    mapMapping[currentMapId] = dbHandler.getMapId(mapHash)
                }
            }
        }

        println("Generated ${mapMapping.size} map ID mappings.")
    }

    private fun remapMapFiles() {
        "Remapping map files".toProgress(mapDatFiles.size.toLong()).use { progressBar ->
            mapDatFiles.forEach { mapDat ->
                progressBar.step()
                val currentMapId = mapDat.mapId()
                val newMapId = mapMapping[currentMapId] ?: throw IllegalStateException("No mapping found for map ID $currentMapId")

                val newMapDat = File(outputDir, "data/map_$newMapId.dat")
                mapDat.copyTo(newMapDat, true)
            }
        }
        println("\nRemapped ${mapDatFiles.size} map files.")
    }

    private fun remapRegionFiles() {
        println("Found ${mcaFiles.size} region files in given directory.")
        "Remapping region files".toProgress(mcaFiles.size.toLong(), " chunks").use { progressBar ->
            mcaFiles.forEach { mcaFile ->
                progressBar.step()
                progressBar.extraMessage = " ${mcaFile.name}"
                if (mcaFile.parentFile.name in listOf("poi")) return@forEach
                if (mcaFile.length() == 0L) return@forEach

                mcaFile.toMCA().use {
                    when (mcaFile.parentFile.name) {
                        "region" -> {
                            filterNotNull().forEach { chunk ->
                                chunk.data.remapChunk()
                            }
                        }
                        "entities" -> {
                            filterNotNull().forEach { chunk ->
                                chunk.data.getListTag("Entities")?.remapEntities()
                            }
                        }
                    }
                }
            }
        }
        println("Remapped map data of ${mcaFiles.size} region files.")
    }

    private fun CompoundTag.remapChunk() {
        getCompoundTag("Level")?.let { level ->
            level.getListTag("TileEntities")?.remapEntities()
            level.getListTag("Entities")?.remapEntities()
        }
        getListTag("block_entities")?.remapEntities()
    }

    private fun remapPlayerData() {
        val playerFiles = playerData.listFiles { file ->
            file.extension == "dat"
        } ?: return
        "Remapping player data".toProgress(playerFiles.size.toLong(), " players").use { progressBar ->
            playerFiles.forEach { playerDat ->
                progressBar.step()
                playerDat.useNBT {
                    remapInventory()
                }
            }
        }
        println("Remapped map data of ${playerFiles.size} player data files.")
    }

    private fun remapLevelDat() {
        levelDat.useGzipNBT {
            getCompoundTag("Data")?.let { data ->
                data.getCompoundTag("Player")?.remapInventory()
            }
        }
        println("Remapped map data of level.dat.")
    }

    private fun CompoundTag.remapInventory() {
        getListTag("Inventory")?.let { inventory ->
            inventory.filterIsInstance<CompoundTag>().forEach { item ->
                item.remapItem()
            }
        }
    }

    private fun ListTag.remapEntities() {
        filterIsInstance<CompoundTag>().forEach { entity ->
            entity.remapEntity()
        }
    }

    private fun CompoundTag.remapEntity() {
        // Item lying on the ground
        getCompoundTag("Item")?.remapItem()

        // Lockable Container
        getListTag("Items")?.remapItems()

        // Items in hands
        getCompoundTag("HandItems")?.remapItems()

        // Armor items
        getCompoundTag("ArmorItems")?.remapItems()
    }

    private fun ListTag.remapItems() {
        filterIsInstance<CompoundTag>().forEach { item ->
            item.remapItem()
        }
    }

    private fun CompoundTag.remapItems() {
        filterIsInstance<CompoundTag>().forEach { item ->
            item.remapItem()
        }
    }

    private fun CompoundTag.remapItem() {
        getStringTag("id")?.let { id ->
            if (id.value != "minecraft:filled_map") return

            getShortTag("Damage")?.let {
                remapOld(it.asShort())
            }
            getCompoundTag("tag")?.let { tag ->
                tag.getIntTag("map")?.let {
                    tag.remapNew(it.asInt())
                }
            }
        }
    }

    private fun CompoundTag.remapOld(mapId: Short): Any {
        val newID = mapMapping[mapId.toInt()]?.toShort()

        return if (newID == null) {
            if (dumpIDs) {
                notFoundFile.appendText(mapId.toString() + "\n")
            }
            println("Map ID: $mapId not found in mapping database. Flipping to negative value.")
            putShort("Damage", (-mapId).toShort())
        } else {
            putShort("Damage", newID)
            //                        println("Remapped map item $mapId -> $newMapId")

            if (dumpIDs) {
                foundFile.appendText("$mapId -> $newID\n")
            }
            remapped++
        }
    }

    private fun CompoundTag.remapNew(mapId: Int): Any {
        val newID = mapMapping[mapId]

        return if (newID == null) {
            if (dumpIDs) {
                notFoundFile.appendText(mapId.toString() + "\n")
            }
            println("Map ID: $mapId not found in mapping database. Flipping to negative value.")
            putInt("map", -mapId)
        } else {
            putInt("map", newID)
    //                        println("Remapped map item $mapId -> $newMapId")

            if (dumpIDs) {
                foundFile.appendText("$mapId -> $newID\n")
            }
            remapped++
        }
    }
}