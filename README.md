# ddm-starter-kafka-api

This starter provides basic configuration, required for kafka-api microservices (registry `kafka-api`, `user-settings-service-persistence`).  Primarily, it is Kafka configuration for implementing the **reply** part of request-reply pattern, and Database configuration.

### Usage
###### Configuration
* `data-platform.kafka` - Kafka configuration (`KafkaProperties` class)
* `data-platform.datasource` - DB datasource configuration (`DatabaseProperties` class)

### License
ddm-starter-kafka-api is Open Source software released under the Apache 2.0 license.
