package com.woori.codeshare.socket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocket                // WebSocket 활성화
@EnableWebSocketMessageBroker   // STOMP 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트가 구독할 경로 설정 (브로드캐스트 전송 경로)
        registry.enableSimpleBroker("/topic")
                .setHeartbeatValue(new long[]{10000, 10000}) // 10초마다 클라이언트와 서버 간의 하트비트 메시지 전송
                .setTaskScheduler(heartbeatScheduler());

        // 클라이언트가 서버로 메시지를 보낼 경로 설정
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트가 WebSocket 서버에 연결할 엔드포인트 설정
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "http://localhost:3000",
                        "https://www.wooricodeshare.com",
                        "https://wooricodeshare.vercel.app"
                )
                .withSockJS();  // SockJS 사용 설정 (WebSocket 미지원 브라우저에서도 폴백 가능하도록)
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> converters) {
        ObjectMapper objectMapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        objectMapper.registerModule(javaTimeModule);

        // LocalDateTime을 ISO 문자열로 직렬화하도록 설정
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
        messageConverter.setObjectMapper(objectMapper);

        converters.add(messageConverter);
        return false; // 기본 메시지 컨버터 유지
    }

    /**
     * STOMP 하트비트 기능을 위한 TaskScheduler 빈
     * `SimpleBrokerMessageHandler`가 주기적으로 하트비트 메시지를 보내거나 클라이언트로부터의 하트비트 응답을 모니터링하는 데 사용
     *
     * @return 하트비트 스케줄링을 위한 TaskScheduler 인스턴스
     */
    @Bean
    public TaskScheduler heartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1); // 스케줄러가 사용할 스레드 풀 크기 설정. 하트비트 작업은 단일 스레드로 충분
        scheduler.setThreadNamePrefix("websocket-heartbeat-scheduler-"); // 스레드 이름 접두사 설정
        scheduler.initialize(); // 스케줄러 초기화

        return scheduler; // 생성된 TaskScheduler 인스턴스를 빈으로 반환
    }

}