package com.helesto.infrastructure;

import org.eclipse.microprofile.config.ConfigProvider;
import quickfix.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Simulates FIX exchange acceptor for integration testing.
 */
public class TestFixAcceptor implements Application {

    private final SocketAcceptor acceptor;
    private final BlockingQueue<Message> receivedMessages = new LinkedBlockingQueue<>();
    private SessionID currentSessionId;

    public TestFixAcceptor(int port) throws ConfigError {
        SessionSettings settings = new SessionSettings();
        settings.setString("ConnectionType", "acceptor");
        settings.setString("StartTime", "00:00:00");
        settings.setString("EndTime", "00:00:00");
        settings.setLong("HeartBtInt", 30);
        settings.setString("FileStorePath", "target/data/test/store");
        settings.setString("FileLogPath", "target/data/test/log");

        String beginString = ConfigProvider.getConfig().getOptionalValue("BeginString", String.class).orElse("FIX.4.4");
        String senderCompID = ConfigProvider.getConfig().getOptionalValue("SenderCompID", String.class).orElse("BANZAI");
        String targetCompID = ConfigProvider.getConfig().getOptionalValue("TargetCompID", String.class).orElse("EXEC");

        // Swaps Sender and Target identifiers to configure this Acceptor as the counterparty to the Broker
        SessionID sessionID = new SessionID(beginString, targetCompID, senderCompID);
        settings.setString(sessionID, "SocketAcceptPort", String.valueOf(port));

        MessageStoreFactory storeFactory = new MemoryStoreFactory();
        LogFactory logFactory = new ScreenLogFactory(false, false, false);
        MessageFactory messageFactory = new DefaultMessageFactory();

        this.acceptor = new SocketAcceptor(this, storeFactory, settings, logFactory, messageFactory);
    }

    public void start() throws ConfigError {
        acceptor.start();
    }

    public void stop() {
        acceptor.stop();
    }

    public BlockingQueue<Message> getReceivedMessages() {
        return receivedMessages;
    }

    public void sendToBroker(Message message) throws SessionNotFound {
        if (currentSessionId != null) {
            Session.sendToTarget(message, currentSessionId);
        }
    }

    public void clearMessages() {
        receivedMessages.clear();
    }

    @Override
    public void onCreate(SessionID sessionID) {
    }

    @Override
    public void onLogon(SessionID sessionID) {
        this.currentSessionId = sessionID;
    }

    @Override
    public void onLogout(SessionID sessionID) {
        this.currentSessionId = null;
    }

    @Override
    public void toAdmin(Message message, SessionID sessionID) {
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat,
            IncorrectTagValue, RejectLogon {
    }

    @Override
    public void toApp(Message message, SessionID sessionID) throws DoNotSend {
    }

    @Override
    public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat,
            IncorrectTagValue, UnsupportedMessageType {
        receivedMessages.add(message);
    }
}