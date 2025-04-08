package com.stupm.service.group.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.rabbitmq.client.Channel;
import com.stupm.common.constant.Constants;
import com.stupm.common.enums.command.GroupEventCommand;
import com.stupm.common.model.message.GroupChatMessageContent;
import com.stupm.common.model.message.MessageReadContent;
import com.stupm.service.group.service.GroupMessageService;
import com.stupm.service.message.service.MessageSyncService;
import com.stupm.service.message.service.P2PMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GroupChatOperateReceiver {
    private static final Logger logger = LoggerFactory.getLogger(GroupChatOperateReceiver.class);
    @Autowired
    P2PMessageService p2pMessageService;

    @Autowired
    GroupMessageService service;

    @Autowired
    MessageSyncService messageSyncService;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = Constants.RabbitConstants.Im2GroupService,durable = "true"),
                    exchange = @Exchange(value = Constants.RabbitConstants.Im2GroupService,durable = "true")
            ),concurrency = "1"
    )
    public void onChatMessage(@Payload Message message,
                              @Headers Map<String,Object> headers,
                              Channel channel) throws Exception {
        String msg = new String(message.getBody(),"utf-8");
        logger.info("CHAT MSG FORM QUEUE ::: {}", msg);
        Long deliveryTag = (Long) headers.get(AmqpHeaders.DELIVERY_TAG);
        try{
            JSONObject jsonObject = JSON.parseObject(msg);
            Integer command = jsonObject.getInteger("command");
            if(command.equals(GroupEventCommand.MSG_GROUP.getCommand())){
                GroupChatMessageContent groupChatMessageContent = JSONObject.toJavaObject(jsonObject, GroupChatMessageContent.class);
                service.process(groupChatMessageContent);
            }else if(command.equals(GroupEventCommand.MSG_GROUP_READED.getCommand())){
                MessageReadContent messageReaded = JSON.parseObject(msg, new TypeReference<MessageReadContent>() {
                }.getType());
                messageSyncService.groupReadMark(messageReaded);
            }
            channel.basicAck(deliveryTag, false);
        }catch (Exception e){
            logger.error("处理消息异常:{}",e.getMessage());
            // 第一个false 不批量拒绝  第二个false 消息不重回队列
            channel.basicNack(deliveryTag,false,false);
        }
    }

}
