package com.wake4v;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.Path;

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
        try {
            this.config = FileConfig.of(dataDirectory);
            this.config.load();
        } catch (Exception e) {
            e.printStackTrace();
        }

        logger.info("Hello there! I made my first plugin with Velocity.");
    }

    @Subscribe(order = PostOrder.NORMAL)
    public void onPlayerPreConnect(ServerPreConnectEvent event) {
        var originalServer = event.getOriginalServer();
        try {
            InetAddress address = originalServer.getServerInfo().getAddress().getAddress();
            String macStr = this.config.get(address.getHostAddress());
            boolean reachable = address.isReachable(10000);
            if (!reachable && macStr != null && !macStr.isEmpty()) {
                WakeOnLan.wake(address, macStr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
    }
}
