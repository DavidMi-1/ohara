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

package com.island.ohara.streams;

import com.island.ohara.common.data.Row;
import com.island.ohara.common.exception.OharaException;
import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.streams.config.StreamDefinitions;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;

// TODO: the streamapp requires many arguments from env variables.
// This tests do not care the rules required by streamapp.
// Fortunately (or unfortunately), streamapp lacks of enough checks to variables so the
// non-completed settings to streamapp works well in this test ... by chia
public class TestStreamApp extends SmallTest {

  @Test
  public void testCanFindCustomClassEntryFromInnerClass() {
    CustomStreamApp app = new CustomStreamApp();
    StreamApp.runStreamApp(app.getClass());
  }

  @Test
  public void testCanDownloadJar() {
    File file = CommonUtils.createTempJar("streamApp");

    try {
      File downloadedFile = StreamApp.downloadJarByUrl(file.toURI().toURL().toString());
      Assert.assertTrue(downloadedFile.isFile());
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }

    file.deleteOnExit();
  }

  @Test(expected = OharaException.class)
  public void testWrongURLJar() {
    File file = CommonUtils.createTempJar("streamApp");
    // redundant quotes
    StreamApp.downloadJarByUrl("\"" + file.toURI().toString() + "\"");
  }

  @Test
  public void testCanFindJarEntry() {
    String projectPath = System.getProperty("user.dir");
    File file = new File(CommonUtils.path(projectPath, "build", "libs", "test-streamApp.jar"));

    try {
      Map.Entry<String, URLClassLoader> entry = StreamApp.findStreamAppEntry(file);
      Assert.assertEquals("com.island.ohara.streams.SimpleApplicationForOharaEnv", entry.getKey());
    } catch (OharaException e) {
      Assert.fail(e.getMessage());
    }
  }

  public static class CustomStreamApp extends StreamApp {
    final AtomicInteger counter = new AtomicInteger();

    @Override
    public StreamDefinitions config() {
      return StreamDefinitions.create();
    }

    @Override
    public void init() {
      int res = counter.incrementAndGet();
      // StreamApp should call init() first
      Assert.assertEquals(1, res);
    }

    @Override
    public void start(OStream<Row> ostream, StreamDefinitions streamDefinitions) {
      int res = counter.incrementAndGet();
      // StreamApp should call start() after init()
      Assert.assertEquals(2, res);
    }
  }
}
