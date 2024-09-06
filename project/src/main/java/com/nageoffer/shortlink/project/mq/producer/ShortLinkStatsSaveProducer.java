

package com.nageoffer.shortlink.project.mq.producer;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;


/**
 * 短链接监控状态保存消息队列生产者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShortLinkStatsSaveProducer {

//    private final StringRedisTemplate stringRedisTemplate;

    private final RocketMQTemplate rocketMQTemplate;

    @Value("${rocketmq.producer.topic}")
    private String statsSaveTopic;
    /**
     * 发送延迟消费短链接统计
     */
//    public void send(Map<String, String> producerMap) {
//        stringRedisTemplate.opsForStream().add(SHORT_LINK_STATS_STREAM_TOPIC_KEY, producerMap);
//    }
    public void send(Map<String, String> producerMap){
        String keys = UUID.randomUUID().toString();
        producerMap.put("keys", keys);
        Message<Map<String, String>> build = MessageBuilder
                .withPayload(producerMap)
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .build();
        SendResult sendResult;
        try {
            sendResult = rocketMQTemplate.syncSend(statsSaveTopic, build, 2000L);
            log.info("[消息访问统计监控] 消息发送结果: {}, 消息ID: {}, 消息Keys: {}", sendResult.getSendStatus(), sendResult.getMsgId(), keys);
            //输出日志包括，业务场景，发送结果，消息ID(RocketMQ返回的独一无二的ID), 自定义的Keys，
        } catch (Throwable ex){
            log.error("[消息访问统计监控] 消息发送失败, 消息体: {}", JSON.toJSONString(producerMap), ex);
            //失败日志要打印异常
            //自定义行为
        }

    }
}
