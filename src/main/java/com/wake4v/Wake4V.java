package com.wake4v;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;

@Plugin(id = "wake4v", name = "Wakeup for Velocity", version = "0.1.0-SNAPSHOT",
        url = "https://example.org", description = "Wakeup remote servers in Velocity Proxy", authors = {"f8ith"})
public class Wake4V {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private FileConfig config;

    @Inject
    public Wake4V(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        Path configPath = Paths.get(dataDirectory.toString() + "/wake4v.toml");
        try {
            logger.info(String.format("Loading config at: %s", String.valueOf(configPath)));
            this.config = FileConfig.of(configPath.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onPlayerPreConnect(ServerPreConnectEvent event) {
        var serverResult = event.getResult();
        if (serverResult.isAllowed()) {
            var originalServer = event.getOriginalServer();
            try {
                String serverName = originalServer.getServerInfo().getName();
                Config serverNetInfo = this.config.get(serverName);
                String address = serverNetInfo.get("ip");
                String broadcastAddress = serverNetInfo.get("broadcast_ip");
                String macStr = serverNetInfo.get("mac");
                Integer openPort = serverNetInfo.get("open_port");
                boolean reachable = isReachable(address, openPort, 2000);
                if (!reachable && macStr != null && !macStr.isEmpty()) {
                    logger.info(String.format("Waking %s on %s", address, macStr));
                    WakeOnLan.wake(broadcastAddress, macStr);
                    reachable = isReachable(address,openPort, 20000);
                    if (!reachable) {
                        logger.error(String.format("Wake on lan for %s failed.", serverName));
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.config.load();
    }


    private static boolean isReachable(String addr, int openPort, int timeOutMillis) {
        // Any Open port on other machine
        // openPort =  22 - ssh, 80 or 443 - webserver, 25 - mailserver etc.
        try (Socket soc = new Socket()) {
            soc.connect(new InetSocketAddress(addr, openPort), timeOutMillis);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
}