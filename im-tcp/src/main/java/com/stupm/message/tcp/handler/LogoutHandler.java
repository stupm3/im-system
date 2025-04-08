package com.stupm.message.tcp.handler;

import com.stupm.message.tcp.utils.SessionSocketHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;

public class LogoutHandler extends ChannelInboundHandlerAdapter {

    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        SessionSocketHolder.removeUserSession((NioSocketChannel) ctx.channel());
        ctx.channel().close();
    }
}
