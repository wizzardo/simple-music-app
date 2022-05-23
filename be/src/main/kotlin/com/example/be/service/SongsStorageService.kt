package com.example.be.service

import com.example.be.db.dto.AlbumDto
import com.example.be.db.dto.ArtistDto
import com.wizzardo.tools.security.AES
import com.wizzardo.tools.security.Base64
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec

@Component
class SongsStorageService(
    @Value("\${storage.useIdAsName:false}")
    private val useIdAsName: Boolean,
    @Value("\${storage.encryption:false}")
    val encryption: Boolean,
    val storageService: StorageService,
) {

    private val keyGenerator: KeyGenerator

    init {
        keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(128)
    }

    private fun ArtistDto.path(): String {
        if (useIdAsName)
            return this.id.toString()
        return this.path
    }

    private fun AlbumDto.path(): String {
        if (useIdAsName)
            return this.id
        return this.path
    }

    private fun AlbumDto.coverPath(): String? {
        if (useIdAsName)
            return this.id + ".bin"
        return this.coverPath
    }

    private fun AlbumDto.Song.path(): String {
        if (useIdAsName)
            return this.id + ".bin"
        return this.path
    }

    fun createFolder(artist: ArtistDto, album: AlbumDto) = storageService.createFolder("${artist.path()}/${album.path()}")

    fun createFolder(artist: ArtistDto) = storageService.createFolder(artist.path())

    fun move(from: ArtistDto, to: ArtistDto, album: AlbumDto, song: AlbumDto.Song) {
        storageService.move("${from.path()}/${album.path()}/${song.path()}", "${to.path()}/${album.path()}/${song.path()}")
    }

    fun move(from: ArtistDto, albumFrom: AlbumDto, to: ArtistDto, albumTo: AlbumDto, song: AlbumDto.Song) {
        storageService.move("${from.path()}/${albumFrom.path()}/${song.path()}", "${to.path()}/${albumTo.path()}/${song.path()}")
    }

    fun move(from: ArtistDto, albumFrom: AlbumDto, songFrom: AlbumDto.Song, to: ArtistDto, albumTo: AlbumDto, songTo: AlbumDto.Song) {
        storageService.move("${from.path()}/${albumFrom.path()}/${songFrom.path()}", "${to.path()}/${albumTo.path()}/${songTo.path()}")
    }

    fun moveCover(from: ArtistDto, to: ArtistDto, album: AlbumDto) {
        storageService.move("${from.path()}/${album.path()}/${album.coverPath()}", "${to.path()}/${album.path()}/${album.coverPath()}")
    }

    fun moveCover(from: ArtistDto, albumFrom: AlbumDto, to: ArtistDto, albumTo: AlbumDto) {
        storageService.move("${from.path()}/${albumFrom.path()}/${albumFrom.coverPath()}", "${to.path()}/${albumTo.path()}/${albumTo.coverPath()}")
    }

    fun delete(artist: ArtistDto) {
        artist.albums.forEach {
            delete(artist, it)
        }
        storageService.delete(artist.path())
    }

    fun delete(artist: ArtistDto, album: AlbumDto) {
        album.songs.forEach {
            delete(artist, album, it)
        }
        
        if (album.coverPath != null)
            storageService.delete("${artist.path()}/${album.path()}/${album.coverPath()}")

        storageService.delete("${artist.path()}/${album.path()}")
    }

    fun delete(artist: ArtistDto, album: AlbumDto, song: AlbumDto.Song) {
        storageService.delete("${artist.path()}/${album.path()}/${song.path()}")
    }

    fun getStream(artist: ArtistDto, album: AlbumDto, song: AlbumDto.Song): InputStream {
        val stream = storageService.getStream("${artist.path()}/${album.path()}/${song.path()}")
        if (song.encryptionKey.isEmpty())
            return stream

        return decrypt(song.encryptionKey, stream)
    }

    fun getCoverAsStream(artist: ArtistDto, album: AlbumDto): InputStream {
        val stream = storageService.getStream("${artist.path()}/${album.path()}/${album.coverPath()}")
        if (album.coverEncryptionKey.isNullOrEmpty())
            return stream

        return decrypt(album.coverEncryptionKey!!, stream)
    }

    fun put(artist: ArtistDto, album: AlbumDto, song: AlbumDto.Song, file: File) {
        if (song.encryptionKey.isNotEmpty()) {
            val tempFile = File.createTempFile("enc", "file")

            try {
                FileInputStream(file).use { input ->
                    FileOutputStream(tempFile).use { output ->
                        encrypt(song.encryptionKey, input, output)
                    }
                }

                storageService.put("${artist.path()}/${album.path()}/${song.path()}", tempFile)
            } finally {
                tempFile.delete()
            }
        } else
            storageService.put("${artist.path()}/${album.path()}/${song.path()}", file)
    }

    fun putCover(artist: ArtistDto, album: AlbumDto, bytes: ByteArray) {
        if (!album.coverEncryptionKey.isNullOrEmpty()) {
            val aes = AES(Base64.decodeFast(album.coverEncryptionKey, true))
            val encrypted = aes.encrypt(bytes)
            storageService.put("${artist.path()}/${album.path()}/${album.coverPath()}", encrypted)
        } else
            storageService.put("${artist.path()}/${album.path()}/${album.coverPath()}", bytes)
    }

    @Synchronized
    fun createEncryptionKey(): String {
        val key = keyGenerator.generateKey()
        return Base64.encodeToString(key.encoded, false, true)
    }

    private fun decrypt(encryptionKey: String, inputStream: InputStream): InputStream {
        val key = AES.generateKey(Base64.decode(encryptionKey, true))
        val iv = key.encoded
        val paramSpec: AlgorithmParameterSpec = IvParameterSpec(iv)
        val cipher: Cipher = Cipher.getInstance("AES/CFB8/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, paramSpec)

        val inputBuffer = ByteArray(1024)

        return object : InputStream() {
            override fun read(): Int {
                TODO("Not yet implemented")
            }

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                val read = inputStream.read(inputBuffer, 0, Math.min(inputBuffer.size, len))
                if (read == -1)
                    return -1

                return cipher.update(inputBuffer, 0, read, b, off)
            }
        }
    }

    private fun encrypt(encryptionKey: String, inputStream: InputStream, outputStream: OutputStream) {
        val key = AES.generateKey(Base64.decode(encryptionKey, true))
        val iv = key.encoded
        val paramSpec: AlgorithmParameterSpec = IvParameterSpec(iv)
        val cipher: Cipher = Cipher.getInstance("AES/CFB8/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, paramSpec)


        val inputBuffer = ByteArray(1024)
        val outputBuffer = ByteArray(1024)
        var r: Int
        var encrypted: Int

        while (true) {
            r = inputStream.read(inputBuffer)
            if (r == -1)
                break

            encrypted = cipher.update(inputBuffer, 0, r, outputBuffer, 0)
            outputStream.write(outputBuffer, 0, encrypted)
        }
        encrypted = cipher.doFinal(outputBuffer, 0)
        if (encrypted > 0)
            outputStream.write(outputBuffer, 0, encrypted)
        outputStream.close()
    }

}