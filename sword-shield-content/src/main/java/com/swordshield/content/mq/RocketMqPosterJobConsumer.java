package com.swordshield.content.mq;

import com.swordshield.content.service.PosterJobProcessor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * RocketMQ海报任务消费者
 * 负责从RocketMQ中消费海报生成任务消息，并调用处理器处理任务
 */
@Component
@ConditionalOnProperty(prefix = "app.poster.mq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RocketMqPosterJobConsumer {
    private static final Logger log = LoggerFactory.getLogger(RocketMqPosterJobConsumer.class);

    private final PosterJobProcessor processor;
    private final String consumerGroup;
    private final String nameServer;
    private final String topic;

    private DefaultMQPushConsumer consumer;

    /**
     * 构造函数
     * @param processor 海报任务处理器
     * @param consumerGroup 消费者组名
     * @param nameServer NameServer地址
     * @param topic 订阅的主题
     */
    public RocketMqPosterJobConsumer(PosterJobProcessor processor,
                                     @Value("${app.poster.mq.consumer-group}") String consumerGroup,
                                     @Value("${app.poster.mq.name-server}") String nameServer,
                                     @Value("${app.poster.mq.topic}") String topic) {
        this.processor = processor;
        this.consumerGroup = consumerGroup;
        this.nameServer = nameServer;
        this.topic = topic;
    }

    /**
     * 启动RocketMQ消费者
     * 初始化消费者配置，设置消息监听器，订阅主题并启动消费者
     * @throws Exception 启动过程中可能抛出的异常
     */
    @PostConstruct
    public void start() throws Exception {
        if (!StringUtils.hasText(nameServer)) {
            throw new IllegalArgumentException("app.poster.mq.name-server 不能为空");
        }
        if (!StringUtils.hasText(topic)) {
            throw new IllegalArgumentException("app.poster.mq.topic 不能为空");
        }

        DefaultMQPushConsumer c = new DefaultMQPushConsumer(consumerGroup);
        c.setNamesrvAddr(nameServer.trim());
        c.setVipChannelEnabled(false);
        c.subscribe(topic.trim(), "*");
        c.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                try {
                    for (MessageExt msg : msgs) {
                        String jobId = msg == null ? null : new String(msg.getBody(), StandardCharsets.UTF_8);
                        if (!StringUtils.hasText(jobId)) {
                            continue;
                        }
                        processor.process(jobId.trim());
                    }
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                } catch (Exception e) {
                    log.warn("RocketMQ poster consume failed", e);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            }
        });
        c.start();
        this.consumer = c;
        log.info("RocketMQ poster consumer started, group={}, nameServer={}, topic={}", consumerGroup, nameServer, topic);
    }

    /**
     * 停止RocketMQ消费者
     * 在应用关闭时优雅地关闭消费者
     */
    @PreDestroy
    public void stop() {
        if (consumer != null) {
            try {
                consumer.shutdown();
            } catch (Exception ignored) {
                // no-op
            }
        }
    }
}

