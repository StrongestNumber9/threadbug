package com.example.javabug;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class JavaBug {
    private static final int parallel = 20;
    private static final String serverHostname = "127.0.0.1";
    private static final int serverPort = 1601;
    private static final AtomicInteger eventCounter = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        Logger logger = LoggerFactory.getLogger(JavaBug.class);
        String action = System.getProperty("action");
        if(action == null) {
            System.out.println("Usage: ");
            System.out.println("Server: java -Daction=server -jar my.jar");
            System.out.println("Fast: java -Daction=spam -DmsgCount=2 -jar my.jar");
            System.out.println("Slow: java -Daction=spam -DmsgCount=3 -jar my.jar");
            System.exit(0);
        }
        if(action.equals("server")) {
            Thread server = new Thread(new RelpServer());
            server.start();
            Thread.sleep(Integer.MAX_VALUE);
        }
        else {
            int msgCount = Integer.parseInt(System.getProperty("msgCount", "1"));
            logger.info("Target is " + serverHostname + ":" + serverPort + " with " + parallel + " threads and " + msgCount + " messages per batch");

            Thread[] threads = new Thread[parallel];
            long start = System.currentTimeMillis();
            for (int i = 0; i < parallel; i++) {
                Thread thread = new Thread(new RelpRunner(serverHostname, serverPort, eventCounter, msgCount));
                thread.start();
                threads[i] = thread;
            }
            int last = 0;
            stats:
            while (true) {
                for (int i = 0; i < parallel; i++) {
                    if (!threads[i].isAlive()) {
                        break stats;
                    }
                }
                Thread.sleep(1000);
                long now = System.currentTimeMillis();
                long elapsed = (now - start) / 1000;
                int count = eventCounter.get();
                int diff = count - last;
                last = count;
                System.out.println("Events sent: " + diff + ", total " + eventCounter + " in " + elapsed + "s (" + count / elapsed + " eps)");
            }
            System.out.println("Done");
        }
    }
}
