server {
    host = '0.0.0.0'
    port = 8080
    ioWorkersCount = 2
    ttl = 15 * 60 * 1000l
    resources {
        mapping = '/'
        cache {
            enabled = true
        }
    }
    debugOutput = false
    multipart {
        enabled = true
    }
}

spring {
    datasource {
        url = 'jdbc:postgresql://localhost:5435/postgres'
        username = 'postgres'
        password = 'Pass2020!'
    }
}

storage {
    type = 'local'
    path = 'share'
    subpath = 'subfolder'
    encryption = 'true'
    useIdAsName = 'true'
}
auth {
    username = ''
    password = ''
}