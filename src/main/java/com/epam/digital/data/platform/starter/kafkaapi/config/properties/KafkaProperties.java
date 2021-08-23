package com.epam.digital.data.platform.starter.kafkaapi.config.properties;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("data-platform.kafka")
public class KafkaProperties {

  private String bootstrap;
  private String groupId;
  private SslProperties ssl;
  private Map<String, String> topics;
  private ErrorHandler errorHandler = new ErrorHandler();
  private List<String> trustedPackages;

  public List<String> getTrustedPackages() {
    return trustedPackages;
  }

  public void setTrustedPackages(List<String> trustedPackages) {
    this.trustedPackages = trustedPackages;
  }

  public String getBootstrap() {
    return bootstrap;
  }

  public void setBootstrap(String bootstrap) {
    this.bootstrap = bootstrap;
  }

  public ErrorHandler getErrorHandler() {
    return errorHandler;
  }

  public void setErrorHandler(ErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public Map<String, String> getTopics() {
    return topics;
  }

  public void setTopics(Map<String, String> topics) {
    this.topics = topics;
  }

  public SslProperties getSsl() {
    return ssl;
  }

  public void setSsl(
      SslProperties ssl) {
    this.ssl = ssl;
  }

  public static class ErrorHandler {

    private Long initialInterval;
    private Long maxElapsedeTime;
    private Double multiplier;

    public Long getInitialInterval() {
      return initialInterval;
    }

    public void setInitialInterval(Long initialInterval) {
      this.initialInterval = initialInterval;
    }

    public Long getMaxElapsedeTime() {
      return maxElapsedeTime;
    }

    public void setMaxElapsedeTime(Long maxElapsedeTime) {
      this.maxElapsedeTime = maxElapsedeTime;
    }

    public Double getMultiplier() {
      return multiplier;
    }

    public void setMultiplier(Double multiplier) {
      this.multiplier = multiplier;
    }
  }

  public static class SslProperties {
    private Boolean enabled;
    private String keystoreKey;
    private String keystoreCertificate;
    private String truststoreCertificate;

    public Boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(Boolean enabled) {
      this.enabled = enabled;
    }

    public String getKeystoreKey() {
      return keystoreKey;
    }

    public void setKeystoreKey(String keystoreKey) {
      this.keystoreKey = keystoreKey;
    }

    public String getKeystoreCertificate() {
      return keystoreCertificate;
    }

    public void setKeystoreCertificate(String keystoreCertificate) {
      this.keystoreCertificate = keystoreCertificate;
    }

    public String getTruststoreCertificate() {
      return truststoreCertificate;
    }

    public void setTruststoreCertificate(String truststoreCertificate) {
      this.truststoreCertificate = truststoreCertificate;
    }
  }
}
