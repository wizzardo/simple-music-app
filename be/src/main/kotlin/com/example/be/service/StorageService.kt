package com.example.be.service

import com.example.be.db.generated.tables.pojos.Config
import com.example.be.db.repository.ConfigRepository
import com.wizzardo.cloud.storage.FileInfo
import com.wizzardo.cloud.storage.Storage
import com.wizzardo.cloud.storage.degoo.DegooStorage
import com.wizzardo.cloud.storage.fs.LocalStorage
import com.wizzardo.tools.json.JsonObject
import com.wizzardo.tools.json.JsonTools
import org.jooq.JSONB
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.io.InputStream
import java.time.LocalDateTime

@Component
class StorageService(
    @Value("\${storage.path:}")
    private val path: String,
    @Value("\${storage.subpath:/}")
    private val subpath: String?,
    @Value("\${storage.type}")
    private val type: String,
    @Value("\${storage.username:}")
    private val username: String?,
    @Value("\${storage.password:}")
    private val password: String?,
    private val configRepository: ConfigRepository,
) : Storage<FileInfo> {

    protected val storage: Storage<FileInfo>
    protected val folder: String?

    init {
        var folder: String? = subpath
        if (!folder.isNullOrBlank()) {
            if (!folder.startsWith("/"))
                folder = "/" + folder
            if (!folder.endsWith("/"))
                folder = folder + "/"
        }
        this.folder = folder

        storage = when (type) {
            "local" -> LocalStorage(File(path)) as Storage<FileInfo>
            "degoo" -> DegooStorage(username, password).also {
                it.setTokenGetterSetter({
                    configRepository.findByName("DEGOO_TOKEN")?.data?.data()?.let {
                        JsonTools.parse(it).asJsonObject().getAsString("token")
                    }
                }, {
                    val config = configRepository.findByName("DEGOO_TOKEN")
                    if (config != null) {
                        config.updated = LocalDateTime.now()
                        config.data = JSONB.valueOf(JsonObject().append("token", it).toString())
                        configRepository.update(config)
                    } else
                        configRepository.insert(
                            Config(
                                0,
                                LocalDateTime.now(),
                                LocalDateTime.now(),
                                "DEGOO_TOKEN",
                                JSONB.valueOf(JsonObject().append("token", it).toString())
                            )
                        )
                })
            } as Storage<FileInfo>
            else -> throw IllegalArgumentException("Unknown storage type: ${type}")
        }

    }

    protected fun withSubPath(path: String): String {
        if (folder.isNullOrBlank() || folder == "/")
            return path
        else
            return folder + path
    }

    override fun list(folder: FileInfo?): MutableList<FileInfo> = storage.list(folder)

    override fun getInfo(path: String): FileInfo? = storage.getInfo(withSubPath(path))

    override fun getData(file: FileInfo, from: Long, to: Long): ByteArray = storage.getData(file, from, to)

    override fun getTotalSpace(): Long = storage.totalSpace

    override fun getUsableSpace(): Long = storage.usableSpace

    override fun createFolder(path: String) = storage.createFolder(withSubPath(path))

    override fun delete(file: FileInfo) = storage.delete(file)

    override fun put(path: String, bytes: ByteArray) = storage.put(withSubPath(path), bytes)

    override fun put(path: String, file: File) = storage.put(withSubPath(path), file)

    override fun move(file: FileInfo, destination: String) = storage.move(file, withSubPath(destination))

    override fun getStream(path: String): InputStream = storage.getStream(withSubPath(path))

    override fun getStream(file: FileInfo) = storage.getStream(file)
}