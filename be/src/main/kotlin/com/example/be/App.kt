package com.example.be

import com.example.be.controller.ArtistController2
import com.example.be.controller.AuthController
import com.example.be.db.DBService
import com.example.be.filter.AuthFilter
import com.example.be.service.*
import com.example.be.service.SongsStorageService
import com.wizzardo.http.FileTreeHandler
import com.wizzardo.http.MultipartHandler
import com.wizzardo.http.RestHandler
import com.wizzardo.http.framework.ControllerHandler
import com.wizzardo.http.framework.Environment
import com.wizzardo.http.framework.WebApplication
import com.wizzardo.http.framework.di.DependencyFactory
import com.wizzardo.http.framework.di.SingletonDependency
import com.wizzardo.http.framework.template.ResourceTools
import com.wizzardo.http.framework.template.Tag
import com.wizzardo.http.request.Header
import com.wizzardo.http.request.Request
import com.wizzardo.http.response.Status
import com.wizzardo.tools.evaluation.Config
import java.lang.IllegalStateException
import javax.sql.ConnectionPoolDataSource

class App(args: Array<out String>?) : WebApplication(args) {

    override fun getBasicTags(): List<Class<out Tag>> = emptyList()

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val dbUrl = System.getenv("DATABASE_URL")
            if (dbUrl != null) {
                val credentials = dbUrl.substringAfter("://").substringBefore("@")
                val url = "jdbc:postgresql://" + dbUrl.substringAfter("@")
                System.setProperty("spring.datasource.url", url)
                System.setProperty("spring.datasource.username", credentials.substringBefore(":"))
                System.setProperty("spring.datasource.password", credentials.substringAfter(":"))
            }

            val port = System.getenv("PORT")
            if (port != null) {
                System.setProperty("server.port", port)
            }

            val app = App(args)
            app.environment = Environment.PRODUCTION
            app.onSetup {
                registerServices(app)

                val resourceTools = DependencyFactory.get(ResourceTools::class.java)
                val indexHtml = resourceTools.getResource("/public/index.html").readAllBytes()

                val fileTreeHandler = DependencyFactory.get(FileTreeHandler::class.java)
                val defaultForbiddenHandler = fileTreeHandler.forbiddenHandler()
                fileTreeHandler.forbiddenHandler { request, response ->
                    if (request.path().length() == 0) {
                        return@forbiddenHandler response.body(indexHtml).appendHeader(Header.KEY_CONTENT_TYPE, Header.VALUE_HTML_UTF8).status(Status._200)
                    }
                    defaultForbiddenHandler.handle(request, response)
                }
                fileTreeHandler.notFoundHandler { request, response ->
                    response.body(indexHtml).appendHeader(Header.KEY_CONTENT_TYPE, Header.VALUE_HTML_UTF8).status(Status._200)
                }

                it.filtersMapping.addBefore("/*", DependencyFactory.get(AuthFilter::class.java))

                it.urlMapping
                    .append("/login", AuthController::class.java, "login", Request.Method.POST)
                    .append("/login/required", AuthController::class.java, "isLoginRequired", Request.Method.GET)
                    .append("/artists", ArtistController2::class.java, "getArtists", Request.Method.GET)
                    .append(
                        "/artists", RestHandler()
                            .get(ControllerHandler(ArtistController2::class.java, "getArtists"))
                            .post(ControllerHandler(ArtistController2::class.java, "createArtist"))
                    )
                    .append(
                        "/artists/\${id}", RestHandler()
                            .get(ControllerHandler(ArtistController2::class.java, "getArtist"))
                            .post(ControllerHandler(ArtistController2::class.java, "updateArtist"))
                            .delete(ControllerHandler(ArtistController2::class.java, "deleteArtist"))
                    )
                    .append("/artists/\${artistIdOrPath}/\${albumIdOrPath}/cover.jpg", ArtistController2::class.java, "getAlbumCover", Request.Method.GET)
                    .append("/artists/\${artistId}/\${albumId}/cover", RestHandler()
                        .post(ControllerHandler(ArtistController2::class.java, "uploadCoverArt"))
                    )
                    .append(
                        "/artists/\${artistId}/\${albumId}", RestHandler()
                            .delete(ControllerHandler(ArtistController2::class.java, "deleteAlbum"))
                            .post(ControllerHandler(ArtistController2::class.java, "mergeAlbums"))
                    )
                    .append(
                        "/artists/\${artistId}/\${albumId}/\${songId}", RestHandler()
                            .get(ControllerHandler(ArtistController2::class.java, "getSong"))
                            .delete(ControllerHandler(ArtistController2::class.java, "deleteSong"))
                    )
                    .append("/artists/\${artistId}/album", ArtistController2::class.java, "createAlbum", Request.Method.POST)
                    .append(
                        "/artists/\${artistId}/\${albumIdOrName}/\${songIdOrTrackNumber}/\${format}/\${bitrate}/stream",
                        ArtistController2::class.java,
                        "getSongConvertedStreamed",
                        Request.Method.GET
                    )
                    .append(
                        "/artists/\${artistId}/\${albumIdOrName}/\${songIdOrTrackNumber}/\${format}/\${bitrate}",
                        ArtistController2::class.java,
                        "getSongConverted",
                        Request.Method.GET
                    )
                    .append("/artists/\${artistId}/\${albumId}/moveTo/\${toArtistId}", ArtistController2::class.java, "moveAlbum", Request.Method.POST)
                    .append("/upload", RestHandler()
                        .post(ControllerHandler(ArtistController2::class.java, "upload"))
                    )

            }
            app.start()
        }

        private fun registerServices(app: App) {
            DependencyFactory.get().register(
                AuthenticationService::class.java, SingletonDependency(
                    AuthenticationService(
                        app.config.config("auth").get("username", ""),
                        app.config.config("auth").get("password", ""),
                    )
                )
            )
            DependencyFactory.get().register(
                DBService::class.java,
                SingletonDependency(DBService(app.createDatasource(app.config)))
            )
            DependencyFactory.get().register(
                RandomIdService::class.java,
                SingletonDependency(RandomIdService())
            )
            DependencyFactory.get().register(
                StorageService::class.java,
                SingletonDependency(
                    StorageService(
                        app.config.config("storage").get("path", ""),
                        app.config.config("storage").get<String?>("subpath", null as String?),
                        app.config.config("storage").get("type", ""),
                        app.config.config("storage").get<String?>("username", null as String?),
                        app.config.config("storage").get<String?>("password", null as String?)
                    )
                )
            )
            DependencyFactory.get().register(
                SongsStorageService::class.java,
                SingletonDependency(
                    SongsStorageService(
                        app.config.config("storage").get("useIdAsName", "false").toBooleanStrict(),
                        app.config.config("storage").get("encryption", "false").toBooleanStrict(),
                        DependencyFactory.get(StorageService::class.java)
                    )
                )
            )
            DependencyFactory.get().register(
                ArtistService::class.java,
                SingletonDependency(
                    ArtistService(
                        DependencyFactory.get(SongsStorageService::class.java),
                        DependencyFactory.get(RandomIdService::class.java),
                        DependencyFactory.get(DBService::class.java),
                    )
                )
            )
            DependencyFactory.get().register(
                SongService::class.java,
                SingletonDependency(
                    SongService(
                        DependencyFactory.get(ArtistService::class.java),
                        DependencyFactory.get(SongsStorageService::class.java),
                    )
                )
            )
            DependencyFactory.get().register(
                FFmpegService::class.java,
                SingletonDependency(
                    FFmpegService(
                        DependencyFactory.get(SongService::class.java),
                    )
                )
            )
            DependencyFactory.get().register(
                UploadService::class.java,
                SingletonDependency(
                    UploadService(
                        DependencyFactory.get(SongsStorageService::class.java),
                        DependencyFactory.get(FFmpegService::class.java),
                        DependencyFactory.get(RandomIdService::class.java),
                        DependencyFactory.get(ArtistService::class.java),
                        DependencyFactory.get(DBService::class.java),
                    )
                )
            )
        }
    }


    private fun createDatasource(config: Config): ConnectionPoolDataSource {
        val datasourceConfig = config.config("spring").config("datasource")
        var username = datasourceConfig.get("username", "")
        var password = datasourceConfig.get("password", "")
        var url = datasourceConfig.get("url", "")

        val dbUrl = System.getenv("DATABASE_URL")
        if (dbUrl != null) {
            val credentials = dbUrl.substringAfter("://").substringBefore("@")
            url = "jdbc:postgresql://" + dbUrl.substringAfter("@")
            username = credentials.substringBefore(":")
            password = credentials.substringAfter(":")
        }

        println("createDatasource: ${url}")

        if (url!!.contains("postgres")) {
            val dbName = url.substringAfterLast("/")
            val host = url.substringAfter("://").substringBefore("/")

            val poolDataSource = org.postgresql.ds.PGConnectionPoolDataSource()
            poolDataSource.databaseName = dbName
            poolDataSource.serverNames = arrayOf(host.substringBefore(":"))
            poolDataSource.portNumbers = intArrayOf(host.substringAfter(":").toIntOrNull() ?: 5432)
            poolDataSource.user = username
            poolDataSource.password = password
            poolDataSource.binaryTransfer = true
//        poolDataSource.ssl = true
//        poolDataSource.sslMode = "require"
            poolDataSource.tcpKeepAlive = true
            poolDataSource.preparedStatementCacheSizeMiB = 1
            poolDataSource.preparedStatementCacheQueries = 32
            poolDataSource.sslMode = "prefer"

            return poolDataSource
        }

        throw IllegalStateException("No datasource!")
    }
}