package com.criss.wang.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

@Component
@EnableScheduling
public class MessageProducer {

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	/**
     * 定时任务
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Scheduled(cron = "*/1 * * * * ?")
    public void send(){
        String message = UUID.randomUUID().toString();
        System.out.println(message);
        ListenableFuture future = kafkaTemplate.send("criss-test", "criss", "1, a");
        future.addCallback(o -> System.out.println("send-消息发送成功：" + message), throwable -> System.out.println("消息发送失败：" + message));

        ListenableFuture future1 = kafkaTemplate.send("criss-another-test", "criss1", "a, bbbbb");
        future1.addCallback(o -> System.out.println("send-消息发送成功：" + message), throwable -> System.out.println("消息发送失败：" + message));
    }
}
