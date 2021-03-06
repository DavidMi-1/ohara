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

package com.island.ohara.agent
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestCollie extends SmallTest with Matchers {
  private[this] val fakeClusterName: String = FakeCollie.clusterName
  private[this] val TIMEOUT = 10 seconds

  private[this] def node(hostname: String): Node = Node(
    hostname = hostname,
    port = Some(22),
    user = Some("user1"),
    password = Some("123456"),
    services = Seq.empty,
    lastModified = CommonUtils.current(),
    validationReport = None,
    tags = Map.empty
  )
  @Test
  def testAddNodeNameUpperCase(): Unit = {
    validErrorNodeName("Node1", "Your node name can't uppercase")
  }

  @Test
  def testAddNodeNameEmpty(): Unit = {
    validErrorNodeName("", "nodeName can't empty")
  }

  @Test
  def testAddNodeNormal(): Unit = {
    val nodeName1 = "node1"
    val nodeName2 = "node2"

    val node1 = node(nodeName1)
    val node2 = node(nodeName2)

    val container1 =
      ContainerInfo(nodeName1, "0", "fakeimage", "", "RUNNING", "", "container1", "0", Seq(), Map(), s"xxx")

    val fakeCollie = new FakeCollie(NodeCollie(Seq(node1, node2)), Seq(container1))
    val cluster = Await.result(fakeCollie.cluster(FakeCollie.clusterName), TIMEOUT)
    cluster._1.nodeNames.size shouldBe 1

    val result: FakeCollieClusterInfo =
      Await.result(fakeCollie.addNode(FakeCollie.clusterName, nodeName2), TIMEOUT)
    result.nodeNames.size shouldBe 2
    result.nodeNames.contains("node1") shouldBe true
    result.nodeNames.contains("node2") shouldBe true
  }

  @Test
  def testRemoveEmptyNode(): Unit = {
    val fakeCollie = new FakeCollie(NodeCollie(Seq.empty), Seq.empty)
    val removeNode: Future[Boolean] = fakeCollie.removeNode(fakeClusterName, "node1")
    Await.result(removeNode, TIMEOUT) shouldBe false
  }

  @Test
  def testRemoveContainerNodeNameNotExists(): Unit = {
    val node1Name = "node1"
    val node1 = node(node1Name)
    val containerInfo =
      ContainerInfo("node2", "0", "fakeimage", "", "RUNNING", "", "container1", "0", Seq.empty, Map.empty, s"xxx")
    val fakeCollie = new FakeCollie(NodeCollie(Seq(node1)), Seq(containerInfo))
    val removeNode: Future[Boolean] = fakeCollie.removeNode(fakeClusterName, node1Name)
    Await.result(removeNode, TIMEOUT) shouldBe false
  }

  @Test
  def testRemoveSingleNode(): Unit = {
    val node1Name = "node1"
    val node1 = node(node1Name)
    val containerInfo =
      ContainerInfo(node1Name,
                    "0",
                    "fakeimage",
                    "",
                    "RUNNING",
                    "",
                    "container1",
                    "0",
                    Seq(),
                    Map(),
                    s"xxx-${node1Name}")
    val fakeCollie = new FakeCollie(NodeCollie(Seq(node1)), Seq(containerInfo))
    val removeNode: Future[Boolean] = fakeCollie.removeNode(fakeClusterName, node1Name)
    intercept[IllegalArgumentException] {
      Await.result(removeNode, TIMEOUT)
    }.getMessage shouldBe s"${FakeCollie.clusterName} is a single-node cluster. You can't remove the last node by removeNode(). Please use remove(clusterName) instead"
  }

  @Test
  def testRemoveNotExistsNode(): Unit = {
    val node1Name = "node1"
    val node1 = node(node1Name)
    val containerInfo =
      ContainerInfo(node1Name,
                    "0",
                    "fakeimage",
                    "",
                    "RUNNING",
                    "",
                    "container1",
                    "0",
                    Seq(),
                    Map(),
                    s"xxx-${node1Name}")
    val fakeCollie = new FakeCollie(NodeCollie(Seq(node1)), Seq(containerInfo))
    val removeNode: Future[Boolean] = fakeCollie.removeNode(fakeClusterName, "node3")
    Await.result(removeNode, TIMEOUT) shouldBe false
  }

  @Test
  def testRemoveNodeNormal(): Unit = {
    val node1Name = "node1"
    val node2Name = "node2"

    val node1 = node(node1Name)
    val node2 = node(node2Name)
    val container1 =
      ContainerInfo(node1Name,
                    "0",
                    "fakeimage",
                    "",
                    "RUNNING",
                    "",
                    "container1",
                    "0",
                    Seq(),
                    Map(),
                    s"xxx-${node1Name}")

    val container2 =
      ContainerInfo(node2Name,
                    "0",
                    "fakeimage",
                    "",
                    "RUNNING",
                    "",
                    "container1",
                    "0",
                    Seq(),
                    Map(),
                    s"xxx-${node1Name}")

    val fakeCollie = new FakeCollie(NodeCollie(Seq(node1, node2)), Seq(container1, container2))
    val removeNode: Future[Boolean] = fakeCollie.removeNode(fakeClusterName, "node1")
    Await.result(removeNode, TIMEOUT) shouldBe true
  }

  private[this] def validErrorNodeName(nodeName: String, expectErrMsg: String): Unit = {
    val node1 = node(nodeName)
    val containerInfo =
      ContainerInfo(node1.name,
                    "0",
                    "fakeimage",
                    "",
                    "RUNNING",
                    "",
                    "container1",
                    "0",
                    Seq(),
                    Map(),
                    s"xxx-${node1.name}")
    val fakeCollie = new FakeCollie(NodeCollie(Seq(node1)), Seq(containerInfo))
    val addNode: Future[FakeCollieClusterInfo] = fakeCollie.addNode(fakeClusterName, nodeName)
    intercept[IllegalArgumentException] {
      Await.result(addNode, TIMEOUT)
    }.getMessage shouldBe expectErrMsg
  }
}
