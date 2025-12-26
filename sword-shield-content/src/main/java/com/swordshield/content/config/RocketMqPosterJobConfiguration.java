package com.swordshield.content.config;

import com.swordshield.content.mq.PosterJobDispatcher;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Configuration
@ConditionalOnProperty(prefix = "app.poster.mq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RocketMqPosterJobConfiguration {
    private static final Logger log = LoggerFactory.getLogger(RocketMqPosterJobConfiguration.class);

    @Bean(destroyMethod = "shutdown")
    public DefaultMQProducer posterJobProducer(
            @Value("${app.poster.mq.producer-group}") String producerGroup,
            @Value("${app.poster.mq.name-server}") String nameServer,
            @Value("${app.poster.mq.send-timeout-ms:5000}") int sendTimeoutMs,
            @Value("${app.poster.mq.send-retry-times:5}") int retryTimes) throws MQClientException {
        if (!StringUtils.hasText(nameServer)) {
            throw new IllegalArgumentException("app.poster.mq.name-server 不能为空");
        }
        DefaultMQProducer producer = new DefaultMQProducer(producerGroup);
        producer.setNamesrvAddr(nameServer.trim());
        // Docker / port-mapping environments often don't expose VIP channel ports reliably.
        producer.setVipChannelEnabled(false);
        producer.setSendMsgTimeout(Math.max(1000, sendTimeoutMs));
        producer.setRetryTimesWhenSendFailed(Math.max(0, retryTimes));
        producer.start();
        log.info("RocketMQ poster producer started, group={}, nameServer={}", producerGroup, nameServer);
        return producer;
    }

    @Bean
    public PosterJobDispatcher rocketMqPosterJobDispatcher(
            DefaultMQProducer producer,
            @Value("${app.poster.mq.topic}") String topic) {
        return jobId -> {
            if (!StringUtils.hasText(jobId)) return;
            try {
                Message msg = new Message(topic, jobId.getBytes(StandardCharsets.UTF_8));
                producer.send(msg);
            } catch (Exception e) {
                log.error("RocketMQ poster dispatch failed, topic={}, jobId={}", topic, jobId, e);
                throw new IllegalStateException("RocketMQ dispatch failed: " + summarize(e), e);
            }
        };
    }

    private static String summarize(Throwable t) {
        if (t == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        appendThrowable(sb, t);
        Throwable cause = t.getCause();
        int depth = 0;
        while (cause != null && cause != t && depth++ < 3) {
            sb.append("; cause=");
            appendThrowable(sb, cause);
            cause = cause.getCause();
        }

        String s = sb.toString();
        if (s.length() > 512) {
            return s.substring(0, 512) + "...";
        }
        return s;
    }

    private static void appendThrowable(StringBuilder sb, Throwable t) {
        sb.append(t.getClass().getSimpleName());
        String msg = t.getMessage();
        if (StringUtils.hasText(msg)) {
            sb.append(": ").append(msg.replace('\n', ' ').replace('\r', ' '));
        }
    }
}
