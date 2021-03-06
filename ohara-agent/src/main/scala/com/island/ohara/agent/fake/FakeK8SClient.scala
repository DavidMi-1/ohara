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

package com.island.ohara.agent.fake

import com.island.ohara.agent.k8s.{K8SClient, K8SJson, K8SStatusInfo, Report}
import com.island.ohara.client.configurator.v0.{BrokerApi, ContainerApi, WorkerApi, ZookeeperApi}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}

class FakeK8SClient(isK8SNode: Boolean, k8sStatusInfo: Option[K8SStatusInfo], containerName: String) extends K8SClient {
  private val LOG = Logger(classOf[FakeK8SClient])

  override def images(nodeName: String)(implicit executionContext: ExecutionContext): Future[Seq[String]] =
    Future.successful {
      Seq(ZookeeperApi.IMAGE_NAME_DEFAULT, BrokerApi.IMAGE_NAME_DEFAULT, WorkerApi.IMAGE_NAME_DEFAULT)
    }

  override def checkNode(nodeName: String)(implicit executionContext: ExecutionContext): Future[Report] =
    Future.successful(
      Report(nodeName, isK8SNode, k8sStatusInfo)
    )

  override def close(): Unit = LOG.info("close client")

  override def containers()(implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] =
    Future.successful {
      Seq(
        ContainerInfo("node1",
                      "0000",
                      "fakeimage",
                      "2019-05-21 00:00:00",
                      "running",
                      "unknow",
                      containerName,
                      "0",
                      Seq.empty,
                      Map.empty,
                      "host1"))
    }

  override def remove(name: String)(implicit executionContext: ExecutionContext): Future[ContainerInfo] =
    throw new UnsupportedOperationException("FakeK8SClient not support remove function")

  override def removeNode(clusterName: String, nodeName: String, serviceName: String)(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerApi.ContainerInfo]] =
    throw new UnsupportedOperationException("FakeK8SClient not support remove node function")

  override def log(name: String)(implicit executionContext: ExecutionContext): Future[String] =
    Future.successful(s"fake k8s log for $name")

  override def nodeNameIPInfo()(implicit executionContext: ExecutionContext): Future[Seq[K8SJson.HostAliases]] =
    Future.successful(Seq.empty)

  override def containerCreator()(implicit executionContext: ExecutionContext): K8SClient.ContainerCreator =
    throw new UnsupportedOperationException("FakeK8SClient not support containerCreator function")
}
