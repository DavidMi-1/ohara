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

package com.island.ohara.streams.ostream;

import com.island.ohara.common.data.Row;
import com.island.ohara.common.exception.ExceptionHandler;
import com.island.ohara.common.exception.OharaException;
import com.island.ohara.common.setting.TopicKey;
import com.island.ohara.streams.OStream;
import com.island.ohara.streams.StreamApp;
import com.island.ohara.streams.config.StreamDefinitions;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class LaunchImpl {

  private static final AtomicBoolean appCalled = new AtomicBoolean(false);
  private static volatile boolean error = false;
  private static volatile RuntimeException exception = null;

  public static void launchApplication(
      final Class<? extends StreamApp> clz, final Object... params) {

    if (appCalled.getAndSet(true)) {
      throw new IllegalStateException("StreamApp could only be called once in each thread");
    }

    // Create thread and wait for that thread to finish
    final CountDownLatch latch = new CountDownLatch(1);
    Thread thread =
        new Thread(
            () -> {
              try {
                final AtomicReference<StreamApp> app = new AtomicReference<>();
                if (!error) {
                  if (params == null || (params.length == 1 && params[0] instanceof Properties)) {
                    Constructor<? extends StreamApp> cons = clz.getConstructor();
                    app.set(cons.newInstance());
                  } else {
                    Constructor<? extends StreamApp> cons =
                        clz.getConstructor(
                            Stream.of(params).map(Object::getClass).toArray(Class[]::new));
                    app.set(cons.newInstance(params));
                  }
                  final StreamApp theApp = app.get();
                  Method method =
                      clz.getSuperclass()
                          .getDeclaredMethod(StreamsConfig.STREAMAPP_CONFIG_METHOD_NAME);
                  StreamDefinitions streamDefinitions = (StreamDefinitions) method.invoke(theApp);

                  if (params != null && params.length == 1 && params[0] instanceof Properties) {
                    Properties props = (Properties) params[0];
                    if (props.containsKey(StreamsConfig.STREAMAPP_CONFIG_KEY)) {
                      System.out.println(
                          clz.getCanonicalName() + "=" + streamDefinitions.toString());
                      return;
                    }
                  }

                  OStream<Row> ostream =
                      OStream.builder()
                          // There are many tests which don't pass correct arguments to env, and the
                          // origin design
                          // lack of enough checks to avoid incorrect arguments. It needs to be
                          // refactor but not now.
                          // TODO: add enough checks for env variables ... by chia
                          .appid(streamDefinitions.nameOption().orElse(null))
                          // see above comments. fromTopicWith should not accepts null as input ...
                          // TODO: add enough checks for env variables ... by chia
                          .bootstrapServers(
                              streamDefinitions.brokerConnectionPropsOption().orElse(null))
                          // TODO: Currently, the number of from topics must be 1 ... by chia
                          // https://github.com/oharastream/ohara/issues/688
                          // see above comments. fromTopicWith should not accepts null as input ...
                          // TODO: add enough checks for env variables ... by chia
                          .fromTopicWith(
                              streamDefinitions.fromTopicKeys().stream()
                                  .map(TopicKey::topicNameOnKafka)
                                  .findFirst()
                                  .orElse(null),
                              Serdes.ROW,
                              Serdes.BYTES)
                          // TODO: Currently, the number of to topics must be 1
                          // https://github.com/oharastream/ohara/issues/688
                          // see above comments. fromTopicWith should not accepts null as input ...
                          // TODO: add enough checks for env variables ... by chia
                          .toTopicWith(
                              streamDefinitions.toTopicKeys().stream()
                                  .map(TopicKey::topicNameOnKafka)
                                  .findFirst()
                                  .orElse(null),
                              Serdes.ROW,
                              Serdes.BYTES)
                          .build();
                  theApp.init();
                  theApp.start(ostream, streamDefinitions);
                }
              } catch (RuntimeException e) {
                error = true;
                exception = e;
              } catch (Exception e) {
                error = true;
                exception = new RuntimeException("StreamApp exception", e);
              } finally {
                latch.countDown();
              }
            });
    thread.setContextClassLoader(clz.getClassLoader());
    thread.setName("Ohara-StreamApp");
    thread.start();

    ExceptionHandler handler =
        ExceptionHandler.builder().with(InterruptedException.class, OharaException::new).build();

    handler.handle(
        () -> {
          latch.await();
          return null;
        });

    if (exception != null) {
      throw exception;
    }
  }
}
