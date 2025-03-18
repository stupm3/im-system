package com.stupm.service.user.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.rabbitmq.client.Channel;
import com.stupm.common.constant.Constants;
import com.stupm.common.enums.command.GroupEventCommand;
import com.stupm.common.enums.command.UserEventCommand;
import com.stupm.common.model.message.GroupChatMessageContent;
import com.stupm.common.model.message.MessageReadContent;
import com.stupm.common.model.message.UserStatusModifyContent;
import com.stupm.service.user.service.UserStatusService;
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
public class UserOnlineStatusReceiver {

    @Autowired
    UserStatusService userStatusService;

    private static final Logger logger = LoggerFactory.getLogger(UserOnlineStatusReceiver.class);
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = Constants.RabbitConstants.Im2UserService,durable = "true"),
                    exchange = @Exchange(value = Constants.RabbitConstants.Im2UserService,durable = "true")
            ),concurrency = "1"
    )
    public void onChatMessage(@Payload Message message,
                              @Headers Map<String,Object> headers,
                              Channel channel) throws Exception {
        String msg = new String(message.getBody(),"utf-8");
        logger.info("CHAT MSG FORM QUEUE ::: {}", msg);
        long start = System.currentTimeMillis();
        Thread thread = Thread.currentThread();
        Long deliveryTag = (Long) headers.get(AmqpHeaders.DELIVERY_TAG);
        try{
            JSONObject jsonObject = JSON.parseObject(msg);
            Integer command = jsonObject.getInteger("command");
            if(command == UserEventCommand.USER_ONLINE_STATUS_CHANGE.getCommand()){
                UserStatusModifyContent content = JSON.parseObject(msg, UserStatusModifyContent.class);
                userStatusService.processUserOnlineStatusModify(content);
            }
        }catch (Exception e){
            logger.error("处理消息异常:{}",e.getMessage());

            // 第一个false 不批量拒绝  第二个false 消息不重回队列
            channel.basicNack(deliveryTag,false,false);
        }
    }
}
