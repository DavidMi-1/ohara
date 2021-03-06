/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.kafka;

import com.island.ohara.common.annotations.Optional;
import com.island.ohara.common.util.CommonUtils;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.kafka.common.config.TopicConfig;

/**
 * a helper class used to create the kafka topic. all member are protected since we have to
 * implement a do-nothing TopicCreator in testing.
 */
public abstract class TopicCreator implements com.island.ohara.common.pattern.Creator<Void> {
  protected int numberOfPartitions = 1;
  protected short numberOfReplications = 1;
  protected Map<String, String> options = Collections.emptyMap();
  protected Duration timeout = Duration.ofSeconds(10);
  protected String name = null;

  TopicCreator() {
    // do nothing
  }

  @Optional("default value is 1")
  public TopicCreator numberOfPartitions(int numberOfPartitions) {
    this.numberOfPartitions = CommonUtils.requirePositiveInt(numberOfPartitions);
    return this;
  }

  @Optional("default value is 1")
  public TopicCreator numberOfReplications(short numberOfReplications) {
    this.numberOfReplications = CommonUtils.requirePositiveShort(numberOfReplications);
    return this;
  }

  @Optional("default is empty")
  public TopicCreator options(Map<String, String> options) {
    doOptions(options, true);
    return this;
  }

  /**
   * Specify that the topic's data should be compacted. It means the topic will keep the latest
   * value for each key.
   *
   * @return this builder
   */
  @Optional("default is deleted")
  public TopicCreator compacted() {
    doOptions(
        Collections.singletonMap(
            TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT),
        false);
    return this;
  }
  /**
   * Specify that the topic's data should be deleted. It means the topic won't keep any data when
   * cleanup
   *
   * @return this builder
   */
  @Optional("default is deleted")
  public TopicCreator deleted() {
    doOptions(
        Collections.singletonMap(
            TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE),
        false);
    return this;
  }

  private TopicCreator doOptions(Map<String, String> options, boolean overwrite) {
    CommonUtils.requireNonEmpty(options);
    if (this.options == null || this.options.isEmpty() || overwrite) {
      this.options = new HashMap<>(options);
    } else {
      this.options.entrySet().stream()
          .filter(x -> options.containsKey(x.getKey()))
          .forEach(
              x -> {
                if (!options.get(x.getKey()).equals(x.getValue()))
                  throw new IllegalArgumentException(
                      String.format(
                          "conflict options! previous:%s new:%s",
                          x.getValue(), options.get(x.getKey())));
              });

      this.options.putAll(options);
    }
    return this;
  }

  @Optional("default value is 10 seconds")
  public TopicCreator timeout(Duration timeout) {
    this.timeout = Objects.requireNonNull(timeout);
    return this;
  }

  public TopicCreator topicName(String name) {
    this.name = CommonUtils.requireNonEmpty(name);
    return this;
  }
}
