package com.rokid.cxr

/**
 * Stub for Rokid CXR-M SDK's Caps class.
 * When the real SDK is available, this file should be removed.
 */
class Caps {
    private val values = mutableListOf<Any?>()

    fun write(value: String) { values.add(value) }
    fun writeInt32(value: Int) { values.add(value) }
    fun writeDouble(value: Double) { values.add(value) }
    fun writeBinary(value: ByteArray) { values.add(value) }

    fun size(): Int = values.size

    fun at(index: Int): Value? {
        if (index < 0 || index >= values.size) return null
        return Value(values[index])
    }

    class Value(private val raw: Any?) {
        fun getString(): String? = raw as? String
        fun getInt(): Int? = raw as? Int
        fun getLong(): Long? = raw as? Long
        fun getBinary(): Binary? = (raw as? ByteArray)?.let { Binary(it) }
    }

    class Binary(val a: ByteArray? = null)
}
