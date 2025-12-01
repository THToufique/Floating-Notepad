package com.ripp3r.floatingnotepad.data

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File

object NoteRepository {
    private const val DRAFT_FILE = "draft.txt"
    
    fun saveDraft(context: Context, text: String) {
        context.openFileOutput(DRAFT_FILE, Context.MODE_PRIVATE).use {
            it.write(text.toByteArray())
        }
    }
    
    fun loadDraft(context: Context): String {
        return try {
            context.openFileInput(DRAFT_FILE).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            ""
        }
    }
    
    fun saveToDocuments(fileName: String, text: String): Boolean {
        return try {
            val documentsDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "")
            } else {
                File(Environment.getExternalStorageDirectory(), "Documents")
            }
            
            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }
            
            val file = File(documentsDir, fileName)
            file.writeText(text)
            true
        } catch (e: Exception) {
            false
        }
    }
}
