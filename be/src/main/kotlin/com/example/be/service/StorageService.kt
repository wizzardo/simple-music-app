package com.example.be.service

import com.wizzardo.cloud.storage.CredentialsProvider
import com.wizzardo.cloud.storage.FileInfo
import com.wizzardo.cloud.storage.S3Storage
import com.wizzardo.cloud.storage.Storage
import com.wizzardo.cloud.storage.fs.LocalStorage
import com.wizzardo.cloud.storage.webdav.WebdavStorage
import java.io.File
import java.io.InputStream

class StorageService(
    private val path: String,
    private val subpath: String?,
    private val type: String,
    private val username: String?,
    private val password: String?,
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
//            "terabox" -> TeraboxStorage() as Storage<FileInfo>
            "s3" -> S3Storage(
                System.getenv("STORAGE_S3_HOST"),
                System.getenv("STORAGE_S3_BUCKET"),
                System.getenv("STORAGE_S3_REGION"),
                CredentialsProvider.createSimpleProvider(System.getenv("STORAGE_S3_KEY_ID"), System.getenv("STORAGE_S3_SECRET"))
            ) as Storage<FileInfo>
            "webdav" -> WebdavStorage(
                System.getenv("STORAGE_WEBDAV_URL"),
                System.getenv("STORAGE_WEBDAV_USERNAME"),
                System.getenv("STORAGE_WEBDAV_PASSWORD")
            ) as Storage<FileInfo>
//            "degoo" -> DegooStorage(username, password).also {
//                it.setTokenGetterSetter({
//                    configRepository.findByName("DEGOO_TOKEN")?.data?.data()?.let {
//                        JsonTools.parse(it).asJsonObject().getAsString("token")
//                    }
//                }, {
//                    val config = configRepository.findByName("DEGOO_TOKEN")
//                    if (config != null) {
//                        config.updated = LocalDateTime.now()
//                        config.data = JSONB.valueOf(JsonObject().append("token", it).toString())
//                        configRepository.update(config)
//                    } else
//                        configRepository.insert(
//                            Config(
//                                0,
//                                LocalDateTime.now(),
//                                LocalDateTime.now(),
//                                "DEGOO_TOKEN",
//                                JSONB.valueOf(JsonObject().append("token", it).toString())
//                            )
//                        )
//                })
//            } as Storage<FileInfo>
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

    override fun delete(file: FileInfo?) {
        if (file != null)
            storage.delete(file)
    }

    override fun put(path: String, bytes: ByteArray) = storage.put(withSubPath(path), bytes)

    override fun put(path: String, file: File) = storage.put(withSubPath(path), file)

    override fun move(file: FileInfo, destination: String) = storage.move(file, withSubPath(destination))

    override fun getStream(path: String): InputStream = storage.getStream(withSubPath(path))

    override fun getStream(file: FileInfo) = storage.getStream(file)
}