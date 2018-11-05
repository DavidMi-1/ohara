package com.island.ohara.configurator

import com.island.ohara.client.ConfiguratorClient
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.configurator.store.Store
import com.island.ohara.io.CloseOnce
import com.island.ohara.rule.SmallTest
import org.junit.{After, Test}
import org.scalatest.Matchers

class TestOhara764 extends SmallTest with Matchers {

  private[this] val configurator =
    Configurator.builder().hostname("localhost").port(0).store(Store.inMemory[String, Any]).noCluster.build()

  private[this] val client = ConfiguratorClient(configurator.hostname, configurator.port)

  @Test
  def testStartSourceWithoutExistentTopic(): Unit = {
    val source = client.add[SourceRequest, Source](
      SourceRequest(name = "abc",
                    className = "aaa.class",
                    topics = Seq("abc"),
                    numberOfTasks = 1,
                    schema = Seq.empty,
                    configs = Map.empty))

    an[IllegalArgumentException] should be thrownBy client.start[Source](source.uuid)

    val topic = client.add[TopicInfoRequest, TopicInfo](TopicInfoRequest("abc", 1, 1))
    val source2 = client.add[SourceRequest, Source](
      SourceRequest(name = "abc",
                    className = "aaa.class",
                    topics = Seq(topic.uuid),
                    numberOfTasks = 1,
                    schema = Seq.empty,
                    configs = Map.empty))
    client.start[Source](source2.uuid)
  }

  @After
  def tearDown(): Unit = {
    CloseOnce.close(client)
    CloseOnce.close(configurator)
  }
}