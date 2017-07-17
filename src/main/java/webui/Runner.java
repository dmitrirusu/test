package webui;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tarantool.SocketChannelProvider;
import org.tarantool.TarantoolClient;
import org.tarantool.TarantoolClientConfig;
import org.tarantool.TarantoolClientImpl;
import webui.util.Helper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.util.Properties;


public class Runner {

    static final Logger LOG = LoggerFactory.getLogger("Service");
    private static Properties config;

    public static void main(String[] args) throws Exception {

        try {
            configureLogSettings();
        } catch (Exception e) {
            LOG.error("Could not configure log settings", e);
        }

        LOG.info("Starting.");
        config = Helper.loadConfig("config.properties");

        TarantoolClient tarantoolClient = initTarantoolClient(config);
        APIService apiService = new APIService(config, tarantoolClient);
        apiService.start();
    }

    private static TarantoolClient initTarantoolClient(Properties config) {
        TarantoolClientConfig clientConfig = new TarantoolClientConfig();
        clientConfig.username = config.getProperty("tarantoolUser");
        clientConfig.password = config.getProperty("tarantoolPass");
        SocketChannelProvider socketChannelProvider = (retryNumber, lastError) -> {
            if (lastError != null) {
                LOG.warn(String.format("Tarantool connection retry %1$d", retryNumber), lastError);
            }

            try {
                int tarantoolPort = Integer.parseInt(config.getProperty("tarantoolPort"));
                return SocketChannel.open(new InetSocketAddress(config.getProperty("tarantoolHost"), tarantoolPort));
            } catch (IOException e) {
                LOG.error("Could not connect to tarantool", e);
                return null;
            }
        };

        return new TarantoolClientImpl(socketChannelProvider, clientConfig);
    }

    private static void configureLogSettings() throws JoranException {
        LoggerContext logContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        logContext.reset();
        JoranConfigurator logConfigurator = new JoranConfigurator();
        logConfigurator.setContext(logContext);
        logConfigurator.doConfigure(Paths.get("logback.xml").toAbsolutePath().toString());
    }
}
