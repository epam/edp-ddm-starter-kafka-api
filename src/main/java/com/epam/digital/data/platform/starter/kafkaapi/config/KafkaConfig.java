package com.epam.digital.data.platform.starter.kafkaapi.config;

import com.epam.digital.data.platform.starter.kafkaapi.config.properties.KafkaProperties;
import com.epam.digital.data.platform.starter.kafkaapi.config.properties.KafkaProperties.ErrorHandler;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
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

  private static final String CERTIFICATES_TYPE = "PEM";
  private static final String SECURITY_PROTOCOL = "SSL";
  public static final String SSL_TRUSTSTORE_CERTIFICATES = "ssl.truststore.certificates";
  public static final String SSL_KEYSTORE_CERTIFICATE_CHAIN = "ssl.keystore.certificate.chain";
  public static final String SSL_KEYSTORE_KEY = "ssl.keystore.key";

  @Autowired
  private KafkaProperties kafkaProperties;

  @Bean
  public Map<String, Object> producerConfigs() {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrap());
    if (kafkaProperties.getSsl().isEnabled()) {
      props.putAll(createSslProperties());
    }
    props.putAll(kafkaProperties.getProducerConfigs());
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
        String.join(",", kafkaProperties.getTrustedPackages()));
    props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.getGroupId());

    if (kafkaProperties.getSsl().isEnabled()) {
      props.putAll(createSslProperties());
    }
    props.putAll(kafkaProperties.getConsumerConfigs());
    return props;
  }

  private Map<String, Object> createSslProperties() {
    return Map.of(
        CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SECURITY_PROTOCOL,
        SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, CERTIFICATES_TYPE,
        SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, CERTIFICATES_TYPE,
        SSL_TRUSTSTORE_CERTIFICATES, kafkaProperties.getSsl().getTruststoreCertificate(),
        SSL_KEYSTORE_CERTIFICATE_CHAIN, kafkaProperties.getSsl().getKeystoreCertificate(),
        SSL_KEYSTORE_KEY, kafkaProperties.getSsl().getKeystoreKey()
    );
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
