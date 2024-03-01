package me.constructor

import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarStyle
import net.querz.mca.MCAFile
import net.querz.nbt.*
import java.io.File
import java.security.MessageDigest
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.zip.GZIPOutputStream

fun compound(block: CompoundTag.() -> Unit): CompoundTag {
    val compoundTag = CompoundTag()
    compoundTag.block()
    return compoundTag
}

inline fun listTag(type: Tag.Type, block: ListTag.() -> Unit): ListTag {
    val listTag = ListTag(type)
    listTag.block()
    return listTag
}

fun File.useNBT(compressed: Boolean = false, modify: CompoundTag.() -> Unit) {
    val tag = NBTUtil.read(this) as CompoundTag
    tag.modify()
    NBTUtil.write(this, tag, compressed)
}

fun File.useGzipNBT(modify: CompoundTag.() -> Unit) {
    val tag = NBTUtil.read(this) as CompoundTag
    tag.modify()

    GZIPOutputStream(outputStream()).use { output ->
        NBTUtil.write(output, tag)
    }
}

fun MCAFile.use(use: MCAFile.() -> Unit) {
    load()
    use()
    save()
}

fun File.mapId() = name.substringAfterLast("_").substringBeforeLast(".dat").toInt()

fun ByteArray.digestHex(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(this)
    return hashBytes.joinToString("") { "%02x".format(it) }
}

fun String.toProgress(maxIteration: Long, unit: String = ""): ProgressBar =
    ProgressBar.builder()
        .setInitialMax(maxIteration)
        .setUpdateIntervalMillis(100)
        .clearDisplayOnFinish()
        .continuousUpdate()
        .showSpeed()
        .setTaskName(this)
        .setUnit(unit, 1)
        .setMaxRenderedLength(150)
        .build()

fun Byte.toTag() = ByteTag.valueOf(this)
fun Short.toTag() = ShortTag.valueOf(this)
fun Int.toTag() = IntTag.valueOf(this)
fun Long.toTag() = LongTag.valueOf(this)
fun Float.toTag() = FloatTag.valueOf(this)
fun Double.toTag() = DoubleTag.valueOf(this)
fun ByteArray.toTag() = ByteArrayTag(this)
fun String.toTag() = StringTag.valueOf(this)
fun IntArray.toTag() = IntArrayTag(this)
fun LongArray.toTag() = LongArrayTag(this)