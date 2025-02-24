package org.pikaqiu.jraft.basic.echo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.pikaqiu.jraft.basic.handler.NettyEchoHandler;

@Data
@Slf4j
public class EchoServer {
    private final int port;

    private ServerBootstrap server;

    public EchoServer(int port) {
        this.port = port;
        this.server = new ServerBootstrap();
    }

    public void start() {
        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup workers = new NioEventLoopGroup();
        try {
            //1.设置线程组
            server.group(boss, workers);
            //2.设置管道类型
            server.channel(NioServerSocketChannel.class);
            //3.设置监听端口
            server.localAddress(9999);
            //4.设置管道参数
            server.option(ChannelOption.SO_KEEPALIVE, true);
            server.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
            //5.装备子通道流水线
            server.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new NettyEchoHandler());
                }
            });
            //6.开始绑定服务器
            // 通过调用sync同步方法阻塞直到绑定成功
            ChannelFuture channelFuture = server.bind().sync();
            log.info("服务器启动成功，监听端口为: {}", channelFuture.channel().localAddress());
            //7.等待通道关闭的异步任务结束
            ChannelFuture closeFuture = channelFuture.channel().closeFuture();
            closeFuture.sync();
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            workers.shutdownGracefully();
            boss.shutdownGracefully();
        }

    }

    public static void main(String[] args) {
        EchoServer echoServer = new EchoServer(9999);
        echoServer.start();
    }
}
