
package com.nageoffer.shortlink.project.config;

import com.nageoffer.shortlink.project.mq.consumer.ShortLinkStatsSaveConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.nageoffer.shortlink.project.common.constant.RedisKeyConstant.SHORT_LINK_STATS_STREAM_GROUP_KEY;
import static com.nageoffer.shortlink.project.common.constant.RedisKeyConstant.SHORT_LINK_STATS_STREAM_TOPIC_KEY;

/**
 * Redis Stream 消息队列配置
 */
//@Configuration
@RequiredArgsConstructor
public class RedisStreamConfiguration {

    private final RedisConnectionFactory redisConnectionFactory;
    private final ShortLinkStatsSaveConsumer shortLinkStatsSaveConsumer;

    @Bean
    public ExecutorService asyncStreamConsumer() {
        AtomicInteger index = new AtomicInteger();
        int processors = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(processors,
                processors + processors >> 1,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                runnable -> {
                //这是一种使用 Java 8 引入的 Lambda 表达式，表示一个接受 Runnable 对象并返回 Thread 对象的函数。它是 ThreadFactory 的实现，用于创建新线程。
                    Thread thread = new Thread(runnable);
                    thread.setName("stream_consumer_short-link_stats_" + index.incrementAndGet());
                    //设置线程名称为 stream_consumer_short-link_stats_ 加上自增的索引。
                    thread.setDaemon(true);
                    //将线程设置为守护线程（daemon），这意味着 JVM 在没有非守护线程运行时可以退出。
                    return thread;
                }
                //如果不使用 Lambda 表达式，可以使用匿名内部类来实现相同的功能。以下是将这段代码改写为不使用 Lambda 表达式的版本：
                //new ThreadFactory() {
                //    private final AtomicInteger index = new AtomicInteger();
                //
                //    @Override
                //    public Thread newThread(Runnable runnable) {
                //        Thread thread = new Thread(runnable);
                //        thread.setName("stream_consumer_short-link_stats_" + index.incrementAndGet());
                //        // 设置线程名称为 stream_consumer_short-link_stats_ 加上自增的索引。
                //        thread.setDaemon(true);
                //        // 将线程设置为守护线程（daemon），这意味着 JVM 在没有非守护线程运行时可以退出。
                //        return thread;
                //    }
                //};
        );
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(ExecutorService asyncStreamConsumer) {
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        // 一次最多获取多少条消息
                        .batchSize(10)
                        // 执行从 Stream 拉取到消息的任务流程, 这里用自定义的, 如果不设置则为默认
                        .executor(asyncStreamConsumer)
                        // 如果没有拉取到消息，需要阻塞的时间。不能大于redis全局timeout时间 ${spring.data.redis.timeout}，否则会超时
                        .pollTimeout(Duration.ofSeconds(3))
                        .build();
        StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer =
                StreamMessageListenerContainer.create(redisConnectionFactory, options);
        //使用 Redis 连接工厂和配置选项创建 StreamMessageListenerContainer。
//        streamMessageListenerContainer.receiveAutoAck(Consumer.from(SHORT_LINK_STATS_STREAM_GROUP_KEY, "stats-consumer"),
//                StreamOffset.create(SHORT_LINK_STATS_STREAM_TOPIC_KEY, ReadOffset.lastConsumed()), shortLinkStatsSaveConsumer);
        //配置消息监听器，自动确认消息。
        return streamMessageListenerContainer;
    }
}
