/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.streampipes.sinks.brokers.jvm;

import org.apache.streampipes.container.init.DeclarersSingleton;
import org.apache.streampipes.container.standalone.init.StandaloneModelSubmitter;
import org.apache.streampipes.dataformat.cbor.CborDataFormatFactory;
import org.apache.streampipes.dataformat.fst.FstDataFormatFactory;
import org.apache.streampipes.dataformat.json.JsonDataFormatFactory;
import org.apache.streampipes.dataformat.smile.SmileDataFormatFactory;
import org.apache.streampipes.messaging.jms.SpJmsProtocolFactory;
import org.apache.streampipes.messaging.kafka.SpKafkaProtocolFactory;
import org.apache.streampipes.sinks.brokers.jvm.bufferrest.BufferRestController;
import org.apache.streampipes.sinks.brokers.jvm.config.BrokersJvmConfig;
import org.apache.streampipes.sinks.brokers.jvm.jms.JmsController;
import org.apache.streampipes.sinks.brokers.jvm.kafka.KafkaController;
import org.apache.streampipes.sinks.brokers.jvm.mqtt.MqttController;
import org.apache.streampipes.sinks.brokers.jvm.pulsar.PulsarController;
import org.apache.streampipes.sinks.brokers.jvm.rabbitmq.RabbitMqController;
import org.apache.streampipes.sinks.brokers.jvm.rest.RestController;

public class BrokersJvmInit extends StandaloneModelSubmitter {

  public static void main(String[] args) {
    DeclarersSingleton
            .getInstance()
            .add(new KafkaController())
            .add(new JmsController())
            .add(new RestController())
            .add(new BufferRestController())
            .add(new RabbitMqController())
            .add(new MqttController())
            .add(new PulsarController());

    DeclarersSingleton.getInstance().registerDataFormats(new JsonDataFormatFactory(),
            new CborDataFormatFactory(),
            new SmileDataFormatFactory(),
            new FstDataFormatFactory());

    DeclarersSingleton.getInstance().registerProtocols(new SpKafkaProtocolFactory(),
            new SpJmsProtocolFactory());

    new BrokersJvmInit().init(BrokersJvmConfig.INSTANCE);
  }
}
