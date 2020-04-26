package com.yurii.youtubemusic.utilities

import java.io.File
import java.io.FileOutputStream
import java.lang.IllegalStateException
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties

private typealias Frame = Pair<String?, Pair<String, String>>

/**
 * This is beta version
 *
 * Class represents a tag content which will be written in a given file(music file)
 * To declare frames(variables which should be in a music file), need to declare class member properties,
 * where name must start with 'p' and its type must be [Frame]. This property has to contain [Pair] with assigned property
 * and second [Pair] that describes name of the assigned property and alias(the key of the frame in the music file)
 * For example you want to add new frame 'test' to the tag.
 * First declare non-private property:
 *      var pTest: Frame = Pair(test, Pair("test", "H"))
 * Notice, the property's name starts with 'p' it's important
 * "test" - It is name of the property in the constructor
 * "H" - The key of frame in the music file
 */
class Tag(var title: String? = null, var authorChannel: String? = null) {

    var pTitle: Frame = Pair(title, Pair("title", "T"))
    var pAuthorChannel: Frame = Pair(authorChannel, Pair("authorChannel", "C"))

    override fun toString(): String {
        return "title: $title, authorChannel: $authorChannel"
    }


    /**
     * Returns calculated length(bytes) of the [Tag]
     */
    @Suppress("UNCHECKED_CAST")
    fun getTagLength(): Int {
        var size = TAG.length
        for (property in Tag::class.declaredMemberProperties) {
            if (!property.name.startsWith("p"))
                continue
            val p = property.get(this) as Frame
            p.first?.let { size += N_BYTES_FOR_FRAME_NAME + N_BYTES_FOR_FRAME_LENGTH + it.toByteArray().size}
        }
        return size
    }


    /**
     * Returns converted [Tag] into [ByteArray] which need to add to a music file
     */
    @Suppress("UNCHECKED_CAST")
    fun getTagByteArray(): ByteArray {
        val byteArray = ByteArray(getTagLength())
        var index = 0

        TAG.toByteArray().forEach { byteArray[index++] = it }

        for (property in Tag::class.declaredMemberProperties) {
            if (!property.name.startsWith("p"))
                continue
            val p = property.get(this) as Frame
            p.first?.let { frame ->
                p.second.second.toByteArray().forEach { byteArray[index++] = it }
                byteArray[index++] = frame.toByteArray().size.toByte()

                frame.toByteArray().forEach {
                    byteArray[index++] = it
                }
            }
        }

        return byteArray
    }

    companion object {
        const val TAG: String = "MTAG"
        const val N_BYTES_FOR_FRAME_LENGTH: Int = 1
        const val N_BYTES_FOR_FRAME_NAME: Int = 1


        /**
         * Parses [ByteArray], converts into [Tag] then returns [Tag] instance
         * @param byteArray byte array which need to parse
         * @exception IllegalStateException if byte array does not start with [TAG] keyword
         */
        @Suppress("UNCHECKED_CAST")
        fun parse(byteArray: ByteArray): Tag {
            val tag = Tag()
            var index = 0
            TAG.forEach {
                check(it == byteArray[index++].toChar()) { "Byte array does not start with $TAG" }
            }

            return signTag(byteArray, index, tag)
        }

        /**
         * Parses given byte array and create filled [Tag] instance.
         * @param byteArray that need to parse
         * @param startFrom the index in [byteArray]] where starts a tag
         * @param tag empty [Tag] instance which need to fill
         * @return filled [Tag] instance
         */
        @Suppress("UNCHECKED_CAST")
        private fun signTag(byteArray: ByteArray, startFrom: Int = 0, tag: Tag): Tag {
            var index: Int = startFrom
            val propertiesList = Tag::class.declaredMemberProperties.filter { it.name.startsWith('p') }
            while (index < byteArray.size) {
                var frameName = ""
                var frameLength = 1
                var frameContent: String

                for (i in 1..N_BYTES_FOR_FRAME_NAME) {
                    frameName += byteArray[index].toChar()
                    index += i
                }

                for (i in 1..N_BYTES_FOR_FRAME_LENGTH) {
                    frameLength *= byteArray[index].toInt()
                    index += i
                }
                frameContent = String(byteArray.copyOfRange(index, index + frameLength))
                index += frameLength

                val prop = propertiesList.find { property ->
                    val p = property.get(tag) as Frame
                    p.second.second == frameName
                }?.get(tag) as? Frame

                val classProperty = Tag::class.declaredMemberProperties.find { it.name == prop?.second?.first }
                if (classProperty is KMutableProperty<*>)
                    classProperty.setter.call(tag, frameContent)
            }

            return tag
        }

        /**
         * Returns max possible length of [Tag]
         */
        @Suppress("UNCHECKED_CAST")
        fun getMaxTagLength(): Int {
            var size = TAG.length
            for (property in Tag::class.declaredMemberProperties) {
                if (!property.name.startsWith("p"))
                    continue
                val p = property.get(Tag()) as Frame
                size += p.second.second.length + N_BYTES_FOR_FRAME_LENGTH + N_BYTES_FOR_FRAME_LENGTH * 256
            }
            return size
        }

    }
}

/**
 * Class represents a helper that can parse file to get a [Tag]. Writes a [Tag] to a music file as well
 * @param file to a target music file
 */
class TaggerV1(private val file: File) {
    /**
     * Checks whether the [file] has a valid tag
     * @return Pair<Boolean, Int> where first field is boolean(true is the file has valid tag, otherwise false),
     * second field is integer which points where the tag starts in the byte array
     */
    private fun hasValidTag(): Pair<Boolean, Int> {
        val fileInputStream = file.inputStream()
        val bytes = fileInputStream.readBytes()
        outerLoop@ for (i in (bytes.size - Tag.getMaxTagLength())..bytes.lastIndex) {
            if (bytes[i].toChar() == Tag.TAG[0]) {
                for (index in 0..Tag.TAG.lastIndex)
                    if (bytes[i + index].toChar() != Tag.TAG[index])
                        continue@outerLoop

                fileInputStream.close()
                return Pair(true, i)
            }
        }
        fileInputStream.close()
        return Pair(false, -1)
    }

    /**
     * Returns a parsed [Tag]
     * @exception IllegalStateException if the file does not have a valid tag
     * @return [Tag]
     */
    fun readTag(): Tag {
        val hasTag: Pair<Boolean, Int> = hasValidTag()

        check(hasTag.first) { "This file does not have a valid tag" }

        val bytes = file.readBytes()

        return Tag.parse(bytes.copyOfRange(hasTag.second, bytes.size))
    }

    /**
     * Adds or overwrites a given [tag]
     * @param overWrite true if need to overwrite the tag, otherwise false
     * @exception IllegalStateException if the file already has a tag and [overWrite] is false
     */
    fun writeTag(tag: Tag, overWrite: Boolean = false) {
        val hasAlreadyTag: Pair<Boolean, Int> = hasValidTag()

        check(!(!overWrite && hasAlreadyTag.first)) { "This file already has a tag. Set 'overWrite' to overwrite this tag" }

        if (hasAlreadyTag.first)
            overWrite(tag, hasAlreadyTag.second)
        else
            addTag(tag)

    }

    /**
     * Overwrites the tag
     * @param tag is new tag which need to overwrite into the music file
     * @param startIndex position where old tag starts
     */
    private fun overWrite(tag: Tag, startIndex: Int) {
        val fileInputStream = file.inputStream()
        var bytes = fileInputStream.readBytes()
        fileInputStream.close()
        bytes = bytes.copyOfRange(0, startIndex)
        val outputStream = FileOutputStream(file)
        outputStream.write(bytes + tag.getTagByteArray())
        outputStream.close()

        fileInputStream.close()
    }

    /**
     * Adds a tag to the music file
     * @param tag new tag which need to add the music file
     */
    private fun addTag(tag: Tag) {
        val outputStream = FileOutputStream(file, true)
        outputStream.write(tag.getTagByteArray())
        outputStream.close()
    }

}