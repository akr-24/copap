package com.copap.api;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

public class HttpServerBootstrap {

    public static HttpServer start(int port) throws Exception {
        HttpServer server =
                HttpServer.create(new InetSocketAddress(port), 0);

        server.start();
        System.out.println("Server started on port " + port);

        return server;
    }
}