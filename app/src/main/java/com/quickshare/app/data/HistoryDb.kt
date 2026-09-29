package com.quickshare.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class HistoryItem(
    val id: Long,
    val name: String,
    val link: String,
    val createdAt: Long
)

class HistoryDb(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "link TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    fun insert(name: String, link: String) {
        val values = ContentValues().apply {
            put("name", name)
            put("link", link)
            put("created_at", System.currentTimeMillis())
        }
        writableDatabase.insert(TABLE, null, values)
    }

    fun all(): List<HistoryItem> {
        val result = ArrayList<HistoryItem>()
        readableDatabase.rawQuery(
            "SELECT id, name, link, created_at FROM $TABLE ORDER BY id DESC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    HistoryItem(
                        id = cursor.getLong(0),
                        name = cursor.getString(1),
                        link = cursor.getString(2),
                        createdAt = cursor.getLong(3)
                    )
                )
            }
        }
        return result
    }

    fun delete(id: Long) {
        writableDatabase.delete(TABLE, "id=?", arrayOf(id.toString()))
    }

    fun clear() {
        writableDatabase.delete(TABLE, null, null)
    }

    companion object {
        private const val DB_NAME = "quickshare.db"
        private const val DB_VERSION = 1
        private const val TABLE = "history"
    }
}
