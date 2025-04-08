package com.stupm.message.tcp.server;

import com.stupm.message.codec.MessageDecoder;
import com.stupm.message.codec.MessageEncoder;
import com.stupm.message.codec.config.BootstrapConfig;
import com.stupm.message.tcp.handler.HeartBeatHandler;
import com.stupm.message.tcp.handler.LogoutHandler;
import com.stupm.message.tcp.handler.NettyServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@NoArgsConstructor
public class ImServer {
    private static final Logger logger = LoggerFactory.getLogger(ImServer.class);


    private BootstrapConfig.TcpConfig config;

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;
    private ServerBootstrap bootstrap;


    public ImServer(BootstrapConfig.TcpConfig config) {
        this.config = config;
        bossGroup = new NioEventLoopGroup(config.getBossThreadSize());
        workerGroup = new NioEventLoopGroup(config.getWorkThreadSize());
        bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 10240) // 服务端可连接队列大小
                .option(ChannelOption.SO_REUSEADDR, true) // 参数表示允许重复使用本地地址和端口
                .childOption(ChannelOption.TCP_NODELAY, true)// 是否禁用Nagle算法 简单点说是否批量发送数据 true关闭 false开启。 开启的话可以减少一定的网络开销，但影响消息实时性
                .childOption(ChannelOption.SO_KEEPALIVE, true) // 保活开关2h没有数据服务端会发送心跳包
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception {
                        channel.pipeline().addLast(new MessageDecoder());
                        channel.pipeline().addLast(new MessageEncoder());
                        channel.pipeline().addLast(new IdleStateHandler(0 , 0 ,10));
                        channel.pipeline().addLast(new HeartBeatHandler(config.getHeartBeatTime()));
                        channel.pipeline().addLast(new LogoutHandler());
                        channel.pipeline().addLast(new NettyServerHandler(config.getBrokerId(),config.getLogicUrl()));
                    }
                });
    }

    public void start() {
        this.bootstrap.bind(this.config.getTcpPort());
    }
}
