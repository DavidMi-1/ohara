#!/usr/bin/env bash
#
# Copyright 2019 is-land
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


if [[ "$1" == "-v" ]] || [[ "$1" == "-version" ]]; then
  if [[ -f "$KAFKA_HOME/bin/broker_version" ]]; then
    echo "broker $(cat "$KAFKA_HOME/bin/broker_version")"
  else
    echo "broker: unknown"
  fi
    if [[ -f "$KAFKA_HOME/bin/ohara_version" ]]; then
    echo "ohara $(cat "$KAFKA_HOME/bin/ohara_version")"
  else
    echo "ohara: unknown"
  fi
  exit
fi

if [[ -z "$KAFKA_HOME" ]];then
  echo "$KAFKA_HOME is required!!!"
  exit 2
fi

CONFIG=$KAFKA_HOME/config/broker.config
if [[ -f "$CONFIG" ]]; then
  echo "$CONFIG already exists!!!"
  exit 2
fi

# jmx setting

if [[ -z $JMX_PORT ]]; then
  # Noted the default value should be equal to BrokerApi.JMX_PORT_DEFAULT
  $JMX_PORT="9093"
fi

if [[ -z $JMX_HOSTNAME ]]; then
  echo "JMX_HOSTNAME is required!!!"
  exit 2
fi

# this option will rewrite the default setting in kafka script
export KAFKA_JMX_OPTS="-Dcom.sun.management.jmxremote \
-Dcom.sun.management.jmxremote.authenticate=false \
-Dcom.sun.management.jmxremote.ssl=false \
-Dcom.sun.management.jmxremote.port=$JMX_PORT \
-Dcom.sun.management.jmxremote.rmi.port=$JMX_PORT \
-Djava.rmi.server.hostname=$JMX_HOSTNAME
"

# default setting
echo "num.network.threads=3" >> "$CONFIG"
echo "num.io.threads=8" >> "$CONFIG"
echo "socket.send.buffer.bytes=102400" >> "$CONFIG"
echo "socket.receive.buffer.bytes=102400" >> "$CONFIG"
echo "socket.request.max.bytes=104857600" >> "$CONFIG"
echo "num.partitions=1" >> "$CONFIG"
echo "num.recovery.threads.per.data.dir=1" >> "$CONFIG"
echo "offsets.topic.replication.factor=1" >> "$CONFIG"
echo "transaction.state.log.replication.factor=1" >> "$CONFIG"
echo "transaction.state.log.min.isr=1" >> "$CONFIG"
echo "log.retention.hours=168" >> "$CONFIG"
echo "log.segment.bytes=1073741824" >> "$CONFIG"
echo "log.retention.check.interval.ms=300000" >> "$CONFIG"
echo "zookeeper.connection.timeout.ms=6000" >> "$CONFIG"
echo "group.initial.rebalance.delay.ms=0" >> "$CONFIG"
if [[ -z "${BROKER_ID}" ]]; then
  BROKER_ID="0"
fi
echo "broker.id=$BROKER_ID" >> "$CONFIG"

if [[ -z "${BROKER_CLIENT_PORT}" ]]; then
  BROKER_CLIENT_PORT=9092
fi
echo "listeners=PLAINTEXT://:$BROKER_CLIENT_PORT" >> "$CONFIG"

if [[ -z "${BROKER_DATA_DIR}" ]]; then
  BROKER_DATA_DIR="/tmp/broker/data"
fi
echo "log.dirs=$BROKER_DATA_DIR" >> "$CONFIG"

if [[ -z "${BROKER_ZOOKEEPERS}" ]]; then
  echo "You have to define BROKER_ZOOKEEPERS"
  exit 2
fi
echo "zookeeper.connect=$BROKER_ZOOKEEPERS" >> "$CONFIG"

if [[ -z "$BROKER_ADVERTISED_CLIENT_PORT" ]]; then
  BROKER_ADVERTISED_CLIENT_PORT=$BROKER_CLIENT_PORT
fi

if [[ -n "$BROKER_ADVERTISED_HOSTNAME" ]]; then
  echo "advertised.listeners=PLAINTEXT://$BROKER_ADVERTISED_HOSTNAME:$BROKER_ADVERTISED_CLIENT_PORT" >> "$CONFIG"
fi

if [[ -z "$KAFKA_HOME" ]]; then
  echo "KAFKA_HOME is required!!!"
  exit 2
fi

if [[ ! -z "$PROMETHEUS_EXPORTER" ]]; then
  if [[ ! -f "$PROMETHEUS_EXPORTER" ]]; then
    echo "PROMETHEUS_EXPORTER exporter doesn't exist!!!"
    exit 2
  fi

  if [[ ! -f "$PROMETHEUS_EXPORTER_CONFIG" ]]; then
    echo "PROMETHEUS_EXPORTER_CONFIG exporter config doesn't exist!!!"
    exit 2
  fi

  if [[ -z "$PROMETHEUS_EXPORTER_PORT" ]]; then
    PROMETHEUS_EXPORTER_PORT="7071"
  fi

  export KAFKA_OPTS="-javaagent:$PROMETHEUS_EXPORTER=$PROMETHEUS_EXPORTER_PORT:$PROMETHEUS_EXPORTER_CONFIG"
fi

exec $KAFKA_HOME/bin/kafka-server-start.sh "$CONFIG"
