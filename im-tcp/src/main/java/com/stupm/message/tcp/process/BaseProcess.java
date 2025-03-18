package com.stupm.message.tcp.process;

import com.stupm.common.model.UserClientDTO;
import com.stupm.message.codec.proto.MessagePack;
import com.stupm.message.tcp.utils.SessionSocketHolder;
import io.netty.channel.socket.nio.NioSocketChannel;

public abstract class BaseProcess {
    public abstract void processBefore();

    public void process(MessagePack messagePack){
        processBefore();
        NioSocketChannel nioSocketChannel = SessionSocketHolder.get(new UserClientDTO(messagePack.getAppId(), messagePack.getToId(), messagePack.getClientType(), messagePack.getImei()));
        if(nioSocketChannel != null){
            nioSocketChannel.writeAndFlush(messagePack);
        }
        processAfter();
    }

    public abstract void processAfter();
}
