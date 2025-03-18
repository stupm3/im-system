package com.stupm.messageStore.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.Channel;
import com.stupm.common.constant.Constants;
import com.stupm.messageStore.dao.MessageBodyEntity;
import com.stupm.messageStore.model.StoreGroupMessageDTO;
import com.stupm.messageStore.model.StoreP2PMessageDTO;
import com.stupm.messageStore.service.StoreMessageService;
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
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class StoreGroupMessageReceiver {
    private static Logger logger = LoggerFactory.getLogger(StoreP2PMessageReceiver.class);

    @Autowired
    private StoreMessageService storeMessageService;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = Constants.RabbitConstants.StoreGroupMessage,durable = "true"),
                    exchange = @Exchange(value = Constants.RabbitConstants.StoreGroupMessage,durable = "true")
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
            MessageBodyEntity messageBody = jsonObject.getObject("messageBody", MessageBodyEntity.class);

            StoreGroupMessageDTO javaObject = jsonObject.toJavaObject(StoreGroupMessageDTO.class);
            javaObject.setMessageBody(messageBody);
            storeMessageService.doStoreGroupMessage(javaObject);
            channel.basicAck(deliveryTag,false);
        }catch (Exception e){
            logger.error("处理消息异常:{}",e.getMessage());

            // 第一个false 不批量拒绝  第二个false 消息不重回队列
            channel.basicNack(deliveryTag,false,false);
        }
    }
}
