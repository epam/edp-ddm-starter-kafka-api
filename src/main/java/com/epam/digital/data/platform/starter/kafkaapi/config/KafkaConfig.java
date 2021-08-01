package com.epam.digital.data.platform.starter.kafkaapi.config;

import com.epam.digital.data.platform.starter.kafkaapi.config.properties.KafkaProperties;
import com.epam.digital.data.platform.starter.kafkaapi.config.properties.KafkaProperties.ErrorHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.SeekToCurrentErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaConfig {

  @Autowired
  private KafkaProperties kafkaProperties;

  @Bean
  public Map<String, Object> producerConfigs() {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrap());
    return props;
  }

  @Bean
  public Serializer<String> keySerializer() {
    return new StringSerializer();
  }

  @Bean
  public <I> Serializer<I> valueSerializer() {
    return new JsonSerializer<>();
  }

  @Bean
  @Primary
  public <I> ProducerFactory<String, I> requestProducerFactory() {
    return new DefaultKafkaProducerFactory<>(producerConfigs(), keySerializer(), valueSerializer());
  }

  @Bean
  public Map<String, Object> consumerConfigs() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrap());
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    props.put(JsonDeserializer.TRUSTED_PACKAGES,
        kafkaProperties.getTrustedPackages().stream().collect(Collectors.joining(",")));
    props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.getGroupId());

    return props;
  }

  @Bean
  public <O> ConsumerFactory<String, O> replyConsumerFactory() {
    return new DefaultKafkaConsumerFactory<>(consumerConfigs());
  }

  @Bean
  public <O>
  ConcurrentKafkaListenerContainerFactory<String, O> concurrentKafkaListenerContainerFactory(
      ConsumerFactory<String, O> cf) {
    ConcurrentKafkaListenerContainerFactory<String, O> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(cf);
    return factory;
  }

  @Bean
  public <O> KafkaTemplate<String, O> replyTemplate(
      ProducerFactory<String, O> pf, ConcurrentKafkaListenerContainerFactory<String, O> factory) {
    KafkaTemplate<String, O> kafkaTemplate = new KafkaTemplate<>(pf);
    factory.getContainerProperties().setMissingTopicsFatal(false);
    factory.setReplyTemplate(kafkaTemplate);
    factory.setErrorHandler(deadLetterErrorHandler(kafkaTemplate));
    return kafkaTemplate;
  }

  private SeekToCurrentErrorHandler deadLetterErrorHandler(
      KafkaOperations<String, ?> kafkaTemplate) {
    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
    ErrorHandler errorHandler = kafkaProperties.getErrorHandler();
    ExponentialBackOff backOff = new ExponentialBackOff(errorHandler.getInitialInterval(),
        errorHandler.getMultiplier());
    backOff.setMaxElapsedTime(errorHandler.getMaxElapsedeTime());
    return new SeekToCurrentErrorHandler(recoverer, backOff);
  }
}
