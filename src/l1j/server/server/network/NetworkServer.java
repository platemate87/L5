package l1j.server.server.network;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import l1j.server.Config;
import l1j.server.server.GeneralThreadPool;
import l1j.server.server.datatables.IpTable;
import l1j.server.server.utils.SystemUtil;

public class NetworkServer implements Runnable {

    Logger _log = LoggerFactory.getLogger(NetworkServer.class);
    private static final NetworkServer instance = new NetworkServer();
    private ArrayBlockingQueue<Client> clientQueue;
    private ArrayList<String> ips = new ArrayList<>();
    
    // Lógica original do GameServer
    private static final int CONNECTION_LIMIT = 20;
    private static final int CACHE_REFRESH = 1000 * 60 * 4; // 4 minutes
    private static final ConcurrentMap<String, Integer> connectionCache = new ConcurrentHashMap<>();
    
    static {
        GeneralThreadPool.getInstance().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    connectionCache.clear();
                } catch (Exception e) {
                    LoggerFactory.getLogger(NetworkServer.class).error("Error clearing connection cache", e);
                }
            }
        }, CACHE_REFRESH, CACHE_REFRESH);
    }

    private NetworkServer() {
        
    }

    public static NetworkServer getInstance() {
        return instance;
    }

    private ConcurrentHashMap<ChannelId, Client> clients = new ConcurrentHashMap<>();

    @Override
    public void run() {
        _log.info("Server started. Memory used: " + SystemUtil.getUsedMemoryMB() + "MB");
        _log.info("Starting networking on port: " + Config.GAME_SERVER_PORT);
        _log.info("Waiting for connections!");
        
        setClientQueue(new ArrayBlockingQueue<Client>(1024));
        ExecutorService packetexecutor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            Runnable worker = new PacketConsumer("PacketConsumer" + i);
            packetexecutor.execute(worker);
        }
        
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     String host = ch.remoteAddress().getAddress().getHostAddress();
                     
                     // Aplicar a lógica original de controle de conexão
                     if (shouldAcceptConnection(ch, host)) {
                         _log.trace("Accepted connection from IP: " + ch.remoteAddress());
                         ch.pipeline().addLast(new ChannelInit());
                         
                         // Incrementar contador de conexões
                         connectionCache.compute(host, (key, value) -> 
                             value == null ? 1 : value + 1);
                     } else {
                         // Fechar conexão se não passou na validação
                         ch.close();
                     }
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128)
             .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(Config.GAME_SERVER_PORT).sync();
            _log.info("NetworkServer started successfully on port: " + Config.GAME_SERVER_PORT);
            
            // Halts here until server is closed
            f.channel().closeFuture().sync();
            
        } catch (InterruptedException e) {
            _log.error("NetworkServer interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            _log.error("Error starting NetworkServer", e);
            throw new RuntimeException("Failed to start NetworkServer", e);
        } finally {
            _log.info("Shutting down NetworkServer");
            packetexecutor.shutdown();
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
    
    /**
     * Aplica a lógica original de controle de conexão do GameServer
     */
    private boolean shouldAcceptConnection(SocketChannel channel, String host) {
        try {
            // Verificar se IP está banido primeiro
            if (IpTable.getInstance().isBannedIp(host)) {
                _log.info("Banned IP(" + host + ") attempted connection");
                return false;
            }
            
            // Inicializar contador se não existir
            connectionCache.putIfAbsent(host, 0);
            
            // Verificar limite de conexões
            Integer currentConnections = connectionCache.get(host);
            if (currentConnections != null) {
                if (currentConnections == CONNECTION_LIMIT) {
                    // Log DOS detection once, but not more than once
                    _log.warn("NetworkServer: " + host + " hit connection limit.");
                    return false;
                } else if (currentConnections > CONNECTION_LIMIT) {
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception ex) {
            _log.error("Error validating connection from " + host, ex);
            return false;
        }
    }
    
    /**
     * Remove conexão do cache quando cliente desconecta
     */
    public void onClientDisconnected(String host) {
        try {
            connectionCache.compute(host, (key, value) -> 
                value == null || value <= 1 ? null : value - 1);
        } catch (Exception e) {
            _log.warn("Error updating connection cache for disconnect: " + host, e);
        }
    }
    
    /**
     * Retorna estatísticas de conexão
     */
    public int getConnectionCount(String host) {
        return connectionCache.getOrDefault(host, 0);
    }
    
    /**
     * Retorna total de IPs únicos conectados
     */
    public int getUniqueIpCount() {
        return connectionCache.size();
    }

    public ConcurrentHashMap<ChannelId, Client> getClients() {
        return clients;
    }

    public void setClients(ConcurrentHashMap<ChannelId, Client> clients) {
        this.clients = clients;
    }

    public ArrayBlockingQueue<Client> getClientQueue() {
        return clientQueue;
    }

    public void setClientQueue(ArrayBlockingQueue<Client> clientQueue) {
        this.clientQueue = clientQueue;
    }

    public synchronized ArrayList<String> getIps() {
        return ips;
    }

    public void setIps(ArrayList<String> ips) {
        this.ips = ips;
    }
    
    /**
     * Shutdown graceful mantendo a lógica original
     */
    public void shutdown() {
        _log.info("Initiating NetworkServer shutdown");
        
        // Disconnect all clients
        for (Client client : clients.values()) {
            try {
                client.close();
            } catch (Exception e) {
                _log.warn("Error closing client connection", e);
            }
        }
        
        clients.clear();
        ips.clear();
        connectionCache.clear();
        
        _log.info("NetworkServer shutdown completed");
    }
}