package com.example.dnd_quest

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json // <--- ESTA ES LA CLAVE QUE FALTABA
import java.io.File

class AdventureStorage(private val context: Context) {

    // Configuración del formateador JSON
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    // Guardar una aventura en un archivo .json único
    fun saveAdventure(adventure: Adventure) {
        val filename = "${adventure.id}.json"
        val jsonString = json.encodeToString(adventure)

        context.openFileOutput(filename, Context.MODE_PRIVATE).use { output ->
            output.write(jsonString.toByteArray())
        }
    }

    // Obtener la lista de todas las aventuras guardadas
    fun getAllAdventures(): List<Adventure> {
        val files = context.filesDir.listFiles() ?: return emptyList()

        return files.filter { it.name.endsWith(".json") }
            .mapNotNull { file ->
                try {
                    val jsonString = file.readText()
                    json.decodeFromString<Adventure>(jsonString)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null // Si un archivo está corrupto, lo ignoramos
                }
            }
    }

    // Cargar una aventura específica por ID
    fun getAdventure(id: String): Adventure? {
        val file = File(context.filesDir, "$id.json")
        if (!file.exists()) return null

        return try {
            val jsonString = file.readText()
            json.decodeFromString<Adventure>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Eliminar aventura
    fun deleteAdventure(id: String) {
        val file = File(context.filesDir, "$id.json")
        if (file.exists()) {
            file.delete()
        }
    }
}