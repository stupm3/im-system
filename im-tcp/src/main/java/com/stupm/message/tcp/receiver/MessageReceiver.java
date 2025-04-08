package com.stupm.message.tcp.receiver;

import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.*;
import com.stupm.common.constant.Constants;
import com.stupm.message.codec.proto.MessagePack;
import com.stupm.message.tcp.process.BaseProcess;
import com.stupm.message.tcp.process.ProcessFactory;
import com.stupm.message.tcp.utils.MqFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

@Slf4j
public class MessageReceiver {

    private static String brokerId;


    private static void startReceiverMessage(){
        try {
            Channel channel = MqFactory
                    .getChannel(Constants.RabbitConstants.MessageService2Im + brokerId);
            channel.exchangeDeclare(Constants.RabbitConstants.MessageService2Im + brokerId, "direct", true);
            channel.queueDeclare(Constants.RabbitConstants.MessageService2Im
                    + brokerId,
                    true,false,false,null);
            channel.queueBind(Constants.RabbitConstants.MessageService2Im + brokerId,
                    Constants.RabbitConstants.MessageService2Im,brokerId);

            channel.basicConsume(Constants.RabbitConstants.MessageService2Im + brokerId,false,
                    new DefaultConsumer(channel){
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                            try {
                                String msgStr = new String(body);
                                log.info(msgStr);
                                MessagePack messagePack =
                                        JSONObject.parseObject(msgStr, MessagePack.class);
                                BaseProcess messageProcess = ProcessFactory
                                        .getMessageProcess(messagePack.getCommand());
                                messageProcess.process(messagePack);

                                channel.basicAck(envelope.getDeliveryTag(),false);

                            }catch (Exception e){
                                e.printStackTrace();
                                channel.basicNack(envelope.getDeliveryTag(),false,false);
                            }
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void init(){
        startReceiverMessage();
    }

    public static void init(String brokerId){
        if(StringUtils.isBlank(MessageReceiver.brokerId)){
            MessageReceiver.brokerId =  brokerId;
        }

        startReceiverMessage();
    }


}
