package com.example.javabug;

import com.teragrep.rlp_03.Server;
import com.teragrep.rlp_03.SyslogFrameProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class RelpServer implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(RelpServer.class);
    @Override
    public void run() {
        logger.info("Starting");
        final Consumer<byte[]> cbFunction;
        AtomicLong events = new AtomicLong();
        cbFunction = (message) -> {
            events.getAndIncrement();
        };
        int port = 1601;
        Server server = new Server(port, new SyslogFrameProcessor(cbFunction));
        server.setNumberOfThreads(20);
        try {
            server.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
