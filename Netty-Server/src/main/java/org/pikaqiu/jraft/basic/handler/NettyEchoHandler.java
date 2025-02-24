package org.pikaqiu.jraft.basic.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class NettyEchoHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf in = (ByteBuf) msg;
        int len = in.readableBytes();
        log.info("msg type: " + (in.hasArray() ? "堆内存" : "直接内存"));
        byte[] arr = new byte[len];
        in.getBytes(0, arr);
        String content = new String(arr, StandardCharsets.UTF_8);
        log.info("server received: {}", content);
        //写回数据，异步任务
        log.info("写回前，msg.refCnt:" + (in.refCnt()));

        ChannelFuture f = ctx.writeAndFlush(msg);

        f.addListener((ChannelFuture futureListener) -> {
            log.info("写回后，msg.refCnt:" + in.refCnt());
        });
    }
}
