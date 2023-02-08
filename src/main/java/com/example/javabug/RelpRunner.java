package com.example.javabug;

import com.cloudbees.syslog.Facility;
import com.cloudbees.syslog.Severity;
import com.cloudbees.syslog.SyslogMessage;
import com.teragrep.rlp_01.RelpBatch;
import com.teragrep.rlp_01.RelpConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class RelpRunner implements Runnable {
    private final String serverHostname;
    private final int serverPort;
    private final AtomicInteger eventCounter;
    private final int msgCount;
    private static final Logger logger = LoggerFactory.getLogger(RelpRunner.class);


    public RelpRunner(String serverHostname, int serverPort, AtomicInteger eventCounter, int msgCount) {
        this.serverHostname = serverHostname;
        this.serverPort = serverPort;
        this.eventCounter = eventCounter;
        this.msgCount = msgCount;
    }

    public void run() {
        RelpConnection relpConnection = new RelpConnection();
        try {
            relpConnection.setRxBufferSize(5120);
            relpConnection.setTxBufferSize(2621440);
            relpConnection.connect(serverHostname, serverPort);
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
            relpConnection.tearDown();
        }
        SyslogMessage syslog = new SyslogMessage()
                .withTimestamp(new Date().getTime())
                .withSeverity(Severity.WARNING)
                .withAppName("localhost")
                .withHostname("SlowRelpis")
                .withFacility(Facility.USER)
                .withMsg("Hello ");
        byte[] syslogMsg = syslog.toRfc5424SyslogMessage().getBytes(StandardCharsets.UTF_8);
        while(true) {
            RelpBatch relpBatch = new RelpBatch();
            for(int i=0; i<msgCount; i++) {
                relpBatch.insert(syslogMsg);
            }

            try {
                relpConnection.commit(relpBatch);
            } catch (IOException | TimeoutException e) {
                e.printStackTrace();
                break;
            }
            if (!relpBatch.verifyTransactionAll()) {
                logger.error("Failed to verify batch");
                break;
            }

            eventCounter.getAndAdd(msgCount);
        }
        logger.info("Exiting");
    }
}
