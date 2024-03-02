package me.constructor

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    var inputDir = "save"
    var mappingFile = "hash-mapping.json"
    var outputDir = "remapped"
    var dumpIDs = false

    if (args.contains("--help")) {
        printHelp()
        exitProcess(0)
    }

    args.forEachIndexed { index, arg ->
        when (arg) {
            "--input" -> inputDir = args.getOrElse(index + 1) { inputDir }
            "--mapping" -> mappingFile = args.getOrElse(index + 1) { mappingFile }
            "--output" -> outputDir = args.getOrElse(index + 1) { outputDir }
            "--dumpIDs" -> dumpIDs = true
        }
    }

    Compendium(inputDir, mappingFile, outputDir, dumpIDs)
}

fun printHelp() {
    println("Usage: java -jar compendium.jar [OPTIONS]")
    println("Options:")
    println("  --input <inputDir>: Specifies the input directory containing the files to be remapped. (default: save)")
    println("  --mapping <mappingFile>: Specifies the JSON mapping file used for remapping. (default: hash-mapping.json)")
    println("  --output <outputDir>: Specifies the output directory where the remapped files will be saved. (default: remapped)")
    println("  --dumpIDs: Dumps the found and not found map IDs into separate text files in the output directory. (default: false)")
    println("  --help: Prints this help message")
}
