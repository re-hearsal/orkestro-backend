package io.github.Romariok.orkestro.config;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

   @Bean
   public Queue telegramRegistrationQueue(
         @Value("${orkestro.telegram.queue-name:telegram_notification_registrations}") String queueName) {
      return new Queue(queueName, true);
   }

   @Bean
   public Queue telegramBotMessageQueue(
         @Value("${orkestro.telegram.bot-message-queue-name:telegram_bot_messages}") String queueName) {
      return new Queue(queueName, true);
   }
}
