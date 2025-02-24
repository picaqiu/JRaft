package org.pikaqiu.jraft.basic.echo;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import javafx.application.Platform;

import java.util.concurrent.TimeUnit;

public class NettyEchoUIClient {
    private final String host;
    private final int port;
    private final MessageCallback callback;
    private Channel channel;
    private EventLoopGroup workerGroup;

    public NettyEchoUIClient(String host, int port, MessageCallback callback) {
        this.host = host;
        this.port = port;
        this.callback = callback;
    }

    public void connect() {
        workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast(new StringEncoder())
                                    .addLast(new StringDecoder())
                                    .addLast(new ClientHandler());
                        }
                    });

            ChannelFuture future = bootstrap.connect(host, port).sync();
            channel = future.channel();
            reportStatus("Connected to " + host + ":" + port, false);

            // 心跳检测
            scheduleHeartbeat();

        } catch (InterruptedException e) {
            reportStatus("Connection interrupted: " + e.getMessage(), true);
        } catch (Exception e) {
            reportStatus("Connection failed: " + e.getMessage(), true);
            attemptReconnect();
        }
    }

    private void scheduleHeartbeat() {
        channel.eventLoop().scheduleAtFixedRate(() -> {
            if (channel.isActive()) {
                channel.writeAndFlush("❤HEARTBEAT❤");
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    public void sendMessage(String message) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(message).addListener(future -> {
                if (!future.isSuccess()) {
                    reportStatus("Send failed: " + future.cause().getMessage(), true);
                }
            });
        } else {
            reportStatus("Not connected to server", true);
        }
    }

    public void disconnect() {
        if (channel != null) {
            channel.closeFuture().syncUninterruptibly();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        reportStatus("Disconnected", false);
    }

    private void reportStatus(String message, boolean isError) {
        Platform.runLater(() -> callback.onStatusChange(message, isError));
    }

    private void attemptReconnect() {
        workerGroup.schedule(() -> {
            reportStatus("Attempting reconnect...", false);
            connect();
        }, 5, TimeUnit.SECONDS);
    }

    private class ClientHandler extends SimpleChannelInboundHandler<String> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) {
            if (!msg.equals("❤HEARTBEAT❤")) {
                callback.onMessage(msg);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            reportStatus("Connection error: " + cause.getMessage(), true);
            ctx.close();
            attemptReconnect();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            reportStatus("Connection lost", true);
            attemptReconnect();
        }
    }

    public interface MessageCallback {
        void onMessage(String message);
        void onStatusChange(String status, boolean isError);
    }
}
