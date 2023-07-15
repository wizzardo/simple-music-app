package com.example.be.db;

import com.wizzardo.tools.sql.DBTools;
import com.wizzardo.tools.sql.SimpleConnectionPool;

import javax.sql.ConnectionPoolDataSource;
import java.util.concurrent.ForkJoinPool;

public class DBService extends DBTools {

    public DBService(ConnectionPoolDataSource dataSource) {
        this.dataSource = new SimpleConnectionPool(dataSource, 2);
        ForkJoinPool.commonPool().execute(this::init);
    }

    public void init() {
        migrate();
    }

}