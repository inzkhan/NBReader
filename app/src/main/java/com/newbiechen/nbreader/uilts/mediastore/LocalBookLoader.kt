package com.newbiechen.nbreader.uilts.mediastore

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.text.TextUtils
import androidx.annotation.NonNull
import androidx.loader.content.CursorLoader
import com.newbiechen.nbreader.data.entity.LocalBookEntity
import com.newbiechen.nbreader.ui.component.book.type.BookType
import java.io.File
import java.lang.StringBuilder
import java.sql.Blob

/**
 *  author : newbiechen
 *  date : 2019-08-17 15:09
 *  description :书籍加载器
 */

class LocalBookLoader constructor(context: Context) :
    CursorLoader(context) {

    companion object {
        private const val TAG = "LocalBookLoader"
        // 获取系统外置文件数据表
        private val FILE_URI = Uri.parse("content://media/external/file")
        // 需要获取的书籍信息
        private val FILE_PROJECTION = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.TITLE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.SIZE
        )
        // 排序
        private const val FILE_ORDER = MediaStore.Files.FileColumns.TITLE + " ASC"

        private val BOOK_TYPES = BookType.values()
    }

    init {
        // 初始化 cursor 参数
        val selectionBuilder = StringBuilder()
        selectionBuilder.append(MediaStore.Files.FileColumns.DATA)
        for (i in BOOK_TYPES.indices) {
            if (i == 0) {
                selectionBuilder.append(" like ?")
            } else {
                selectionBuilder.append(" or (${MediaStore.Files.FileColumns.DATA} like ?)")
            }
        }

        uri = FILE_URI
        projection = FILE_PROJECTION
        selection = selectionBuilder.toString()
        selectionArgs = BOOK_TYPES.map { "%." + it.name.toLowerCase() }.toTypedArray()
        sortOrder = FILE_ORDER
    }

    fun parseData(cursor: Cursor?): List<LocalBookEntity> {
        val bookInfos = mutableListOf<LocalBookEntity>()
        if (cursor == null) {
            return bookInfos
        }
        // 重复使用Loader时，需要重置cursor的position；
        cursor.moveToPosition(-1)
        while (cursor.moveToNext()) {
            val path = getValueFromCursor(cursor, MediaStore.Files.FileColumns.DATA, "") as String
            // 获取的文件路径无效
            if (TextUtils.isEmpty(path)) {
                continue
            } else {
                val file = File(path)
                if (!file.exists() || file.isDirectory) {
                    continue
                }
            }

            val size = getValueFromCursor(cursor, MediaStore.Files.FileColumns.SIZE, 0L) as Long
            // 如果文件大小为 0，则不加入
            if (size == 0L) {
                continue
            }
            val id = getValueFromCursor(cursor, MediaStore.Files.FileColumns._ID, 0L) as Long
            val name = getValueFromCursor(cursor, MediaStore.Files.FileColumns.TITLE, "") as String
            val type = path.substringAfterLast(".")
            // time 的单位是 s
            val createTime =
                getValueFromCursor(cursor, MediaStore.Files.FileColumns.DATE_MODIFIED, 0L) as Long
            val bookType = BOOK_TYPES.first {
                TextUtils.equals(it.name.toLowerCase(), type)
            }

            // 时间统一转换成 ms
            val bookInfo = LocalBookEntity(id, name, bookType, size, createTime * 1000, path)
            bookInfos.add(bookInfo)
        }
        return bookInfos
    }

    /**
     * 从Cursor中读取对应columnName的值
     *
     * @param cursor
     * @param columnName
     * @param defaultValue
     * @return 当columnName无效时返回默认值；
     */
    private fun getValueFromCursor(
        @NonNull cursor: Cursor, columnName: String,
        defaultValue: Any
    ): Any {
        try {
            val index = cursor.getColumnIndexOrThrow(columnName)
            when (cursor.getType(index)) {
                Cursor.FIELD_TYPE_STRING -> {
                    // TO SOLVE:某些手机的数据库将数值类型存为String类型
                    val value = cursor.getString(index)
                    try {
                        when (defaultValue) {
                            is String -> return value
                            is Long -> return value.toLong()
                            is Int -> return value.toInt()
                            is Double -> return value.toDouble()
                            is Float -> return value.toFloat()
                        }
                    } catch (e: NumberFormatException) {
                        return defaultValue
                    }

                    if (defaultValue is Long) {
                        return cursor.getLong(index)
                    } else if (defaultValue is Int) {
                        return cursor.getInt(index)
                    }
                    if (defaultValue is Float) {
                        return cursor.getFloat(index)
                    } else if (defaultValue is Double) {
                        return cursor.getDouble(index)
                    }
                    return if (defaultValue is Blob) {
                        cursor.getBlob(index)
                    } else defaultValue
                }
                Cursor.FIELD_TYPE_INTEGER -> {
                    if (defaultValue is Long) {
                        return cursor.getLong(index)
                    } else if (defaultValue is Int) {
                        return cursor.getInt(index)
                    }
                    if (defaultValue is Float) {
                        return cursor.getFloat(index)
                    } else if (defaultValue is Double) {
                        return cursor.getDouble(index)
                    }
                    return if (defaultValue is Blob) {
                        cursor.getBlob(index)
                    } else defaultValue
                }
                Cursor.FIELD_TYPE_FLOAT -> {
                    if (defaultValue is Float) {
                        return cursor.getFloat(index)
                    } else if (defaultValue is Double) {
                        return cursor.getDouble(index)
                    }
                    return if (defaultValue is Blob) {
                        cursor.getBlob(index)
                    } else defaultValue
                }
                Cursor.FIELD_TYPE_BLOB -> {
                    return if (defaultValue is Blob) {
                        cursor.getBlob(index)
                    } else defaultValue
                }
                Cursor.FIELD_TYPE_NULL -> return defaultValue
                else -> return defaultValue
            }
        } catch (e: IllegalArgumentException) {
            return defaultValue
        }

    }
}