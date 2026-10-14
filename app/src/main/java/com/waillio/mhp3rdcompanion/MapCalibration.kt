package com.waillio.mhp3rdcompanion

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale

internal data class MapNodeCalibration(
    val dxSteps: Int = 0,
    val dySteps: Int = 0,
    val exists: Boolean = true,
    val note: String = ""
) {
    val isDefault: Boolean get() = dxSteps == 0 && dySteps == 0 && exists && note.isBlank()
}

internal sealed interface MapCalibrationDecodeResult {
    data object Absent : MapCalibrationDecodeResult
    data class Valid(val values: Map<String, MapNodeCalibration>) : MapCalibrationDecodeResult
    /** Valid rows remain usable; malformed rows are skipped as the old decoder did. */
    data class Malformed(val values: Map<String, MapNodeCalibration>, val skippedRows: Int) : MapCalibrationDecodeResult

    fun safeValues(): Map<String, MapNodeCalibration> = when (this) {
        Absent -> emptyMap()
        is Valid -> values
        is Malformed -> values
    }
}

internal object MapCalibrationCodec {
    fun encode(values: Map<String, MapNodeCalibration>): String = values
        .filterValues { !it.isDefault }
        .toSortedMap()
        .entries
        .joinToString("\n") { (nodeId, value) ->
            listOf(
                nodeId,
                value.dxSteps,
                value.dySteps,
                value.exists,
                Base64.getUrlEncoder().withoutPadding().encodeToString(value.note.toByteArray(Charsets.UTF_8))
            ).joinToString("\t")
        }

    fun decode(raw: String?): Map<String, MapNodeCalibration> = decodeResult(raw).safeValues()

    fun decodeResult(raw: String?): MapCalibrationDecodeResult {
        if (raw == null) return MapCalibrationDecodeResult.Absent
        val parsed = linkedMapOf<String, MapNodeCalibration>()
        var skipped = 0
        raw.lineSequence().filter(String::isNotBlank).forEach { line ->
            val parts = line.split('\t')
            val value = runCatching {
                require(parts.size == 5)
                require(parts[0].isNotBlank())
                MapNodeCalibration(
                    dxSteps = parts[1].toInt(),
                    dySteps = parts[2].toInt(),
                    exists = parts[3].toBooleanStrict(),
                    note = String(Base64.getUrlDecoder().decode(parts[4]), Charsets.UTF_8)
                )
            }.getOrNull()
            if (value == null) skipped++ else parsed[parts[0]] = value
        }
        return if (skipped == 0) MapCalibrationDecodeResult.Valid(parsed)
        else MapCalibrationDecodeResult.Malformed(parsed, skipped)
    }

    fun export(map: MapDefinition, values: Map<String, MapNodeCalibration>): String {
        val changed = map.nodes.mapIndexedNotNull { index, node ->
            val value = values[node.nodeId]?.takeUnless { it.isDefault } ?: return@mapIndexedNotNull null
            val actions = buildList {
                if (!value.exists) add("DOES NOT EXIST")
                if (value.dxSteps < 0) add("LEFT ${-value.dxSteps}")
                if (value.dxSteps > 0) add("RIGHT ${value.dxSteps}")
                if (value.dySteps < 0) add("UP ${-value.dySteps}")
                if (value.dySteps > 0) add("DOWN ${value.dySteps}")
                if (value.note.isNotBlank()) add("NOTE: ${value.note.trim()}")
            }
            "#${index + 1} ${node.nodeId} [${node.label ?: node.category.displayName}${node.areaNumber?.let { ", Area $it" }.orEmpty()}]: ${actions.joinToString("; ")}"
        }
        return buildString {
            appendLine("MHP3rd map calibration: ${map.displayName}")
            appendLine("Step = 1% of rendered map width/height")
            if (changed.isEmpty()) append("No changes marked.") else append(changed.joinToString("\n"))
        }
    }
}

internal class MapCalibrationStore(context: Context) {
    private val preferences = context.getSharedPreferences("map_calibration_v1", Context.MODE_PRIVATE)

    fun loadResult(mapId: String): MapCalibrationDecodeResult = MapCalibrationCodec.decodeResult(preferences.getString(mapId, null))

    fun load(mapId: String): Map<String, MapNodeCalibration> = loadResult(mapId).safeValues()

    fun save(mapId: String, values: Map<String, MapNodeCalibration>) {
        preferences.edit().putString(mapId, MapCalibrationCodec.encode(values)).apply()
    }

    fun clear(mapId: String) {
        preferences.edit().remove(mapId).apply()
    }
}

internal object MapCalibrationReportWriter {
    fun fileName(map: MapDefinition, timestamp: Date = Date()): String {
        val suffix = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(timestamp)
        return "mhp3rd-${map.mapId}-calibration-$suffix.txt"
    }

    fun save(context: Context, map: MapDefinition, values: Map<String, MapNodeCalibration>): String {
        val name = fileName(map)
        val report = MapCalibrationCodec.export(map, values)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val relativeDirectory = "${Environment.DIRECTORY_DOWNLOADS}/MHP3rd Companion"
            val metadata = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDirectory)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = requireNotNull(context.contentResolver.insert(collection, metadata)) {
                "Android could not create the calibration report"
            }
            try {
                context.contentResolver.openOutputStream(uri, "w")!!.bufferedWriter().use { it.write(report) }
                context.contentResolver.update(uri, ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }, null, null)
            } catch (error: Throwable) {
                context.contentResolver.delete(uri, null, null)
                throw error
            }
            return "$relativeDirectory/$name"
        }

        val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "MHP3rd Companion")
        check(directory.exists() || directory.mkdirs()) { "Could not create calibration report directory" }
        return File(directory, name).apply { writeText(report) }.absolutePath
    }
}
