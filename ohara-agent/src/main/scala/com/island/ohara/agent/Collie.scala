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

import java.util.Objects

import com.island.ohara.agent.Collie.ClusterCreator
import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Collie is a cute dog helping us to "manage" a bunch of sheep.
  * @tparam T cluster description
  */
trait Collie[T <: ClusterInfo] {

  /**
    * remove whole cluster by specified key. The process, mostly, has a graceful shutdown
    * which can guarantee the data consistency. However, the graceful downing whole cluster may take some time...
    *
    * @param key cluster key
    * @param executionContext thread pool
    * @return true if it does remove a running cluster. Otherwise, false
    */
  final def remove(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[Boolean] =
    clusterWithAllContainers().flatMap(_.find(_._1.key == key).fold(Future.successful(false)) {
      case (cluster, containerInfos) => doRemove(cluster, containerInfos)
    })

  // TODO: this is a deprecated method and should be removed in #2570
  final def remove(clusterName: String)(implicit executionContext: ExecutionContext): Future[Boolean] =
    clusterWithAllContainers().flatMap(_.find(_._1.name == clusterName).fold(Future.successful(false)) {
      case (cluster, containerInfos) => doRemove(cluster, containerInfos)
    })

  /**
    * remove whole cluster gracefully.
    * @param clusterInfo cluster info
    * @param containerInfos containers info
    * @param executionContext thread pool
    * @return true if success. otherwise false
    */
  protected def doRemove(clusterInfo: T, containerInfos: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[Boolean]

  /**
    * This method open a door to sub class to implement a force remove which kill whole cluster without graceful shutdown.
    * NOTED: The default implementation is reference to graceful remove.
    * @param key cluster key
    * @param executionContext thread pool
    * @return true if it does remove a running cluster. Otherwise, false
    */
  final def forceRemove(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[Boolean] =
    clusterWithAllContainers().flatMap(_.find(_._1.key == key).fold(Future.successful(false)) {
      case (cluster, containerInfos) => doForceRemove(cluster, containerInfos)
    })

  // TODO: this is a deprecated method and should be removed in #2570
  final def forceRemove(clusterName: String)(implicit executionContext: ExecutionContext): Future[Boolean] =
    clusterWithAllContainers().flatMap(_.find(_._1.name == clusterName).fold(Future.successful(false)) {
      case (cluster, containerInfos) => doForceRemove(cluster, containerInfos)
    })

  /**
    * remove whole cluster forcely. the impl, by default, is similar to doRemove().
    * @param clusterInfo cluster info
    * @param containerInfos containers info
    * @param executionContext thread pool
    * @return true if success. otherwise false
    */
  protected def doForceRemove(clusterInfo: T, containerInfos: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[Boolean] = doRemove(clusterInfo, containerInfos)

  /**
    * get logs from all containers.
    * NOTED: It is ok to get logs from a "dead" cluster.
    * @param key cluster key
    * @return all log content from cluster. Each container has a log.
    */
  def logs(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[Map[ContainerInfo, String]]

  // TODO: this is a deprecated method and should be removed in #2570
  def logs(clusterName: String)(implicit executionContext: ExecutionContext): Future[Map[ContainerInfo, String]]

  /**
    * create a cluster creator
    * @return creator of cluster
    */
  def creator: ClusterCreator[T]

  /**
    * get the containers information from cluster
    * @param key cluster key
    * @return containers information
    */
  def containers(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] =
    cluster(key).map(_._2)

  // TODO: this is a deprecated method and should be removed in #2570
  def containers(clusterName: String)(implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] =
    cluster(clusterName).map(_._2)

  /**
    * fetch all clusters and belonging containers from cache.
    * Note: this function will only get running containers
    *
    * @param executionContext execution context
    * @return cluster and containers information
    */
  def clusters()(implicit executionContext: ExecutionContext): Future[Map[T, Seq[ContainerInfo]]] =
    clusterWithAllContainers().map(
      entry =>
        // Currently, both k8s and pure docker have the same context of "RUNNING".
        // It is ok to filter container via RUNNING state.
        // Note: even if all containers are dead, the cluster information should be fetch also.
        entry.map { case (info, containers) => info -> containers.filter(_.state == ContainerState.RUNNING.name) })

  // Collie only care about active containers, but we need to trace the exited "orphan" containers for deleting them.
  // This method intend to fetch all containers of each cluster and we filter out needed containers in other methods.
  protected def clusterWithAllContainers()(
    implicit executionContext: ExecutionContext): Future[Map[T, Seq[ContainerInfo]]]

  /**
    * get the cluster information from a cluster
    * @param key cluster key
    * @return cluster information
    */
  def cluster(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[(T, Seq[ContainerInfo])] =
    clusters().map(
      _.find(_._1.key == key)
        .getOrElse(throw new NoSuchClusterException(s"cluster with objectKey [$key] is not running")))

  // TODO: this is a deprecated method and should be removed in #2570
  def cluster(name: String)(implicit executionContext: ExecutionContext): Future[(T, Seq[ContainerInfo])] =
    clusters().map(_.find(_._1.name == name).getOrElse(throw new NoSuchClusterException(s"$name is not running")))

  /**
    * @param key cluster key
    * @return true if the cluster exists
    */
  def exist(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[Boolean] =
    clusters().map(_.exists(_._1.key == key))

  // TODO: this is a deprecated method and should be removed in #2570
  def exist(clusterName: String)(implicit executionContext: ExecutionContext): Future[Boolean] =
    clusters().map(_.exists(_._1.name == clusterName))

  /**
    * @param key cluster key
    * @return true if the cluster doesn't exist
    */
  def nonExist(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[Boolean] =
    exist(key).map(!_)

  // TODO: this is a deprecated method and should be removed in #2570
  def nonExist(clusterName: String)(implicit executionContext: ExecutionContext): Future[Boolean] =
    exist(clusterName).map(!_)

  /**
    * add a node to a running cluster
    * NOTED: this is a async operation since graceful adding a node to a running service may be slow.
    * @param key cluster key
    * @param nodeName node name
    * @return updated cluster
    */
  final def addNode(key: ObjectKey, nodeName: String)(implicit executionContext: ExecutionContext): Future[T] =
    cluster(key).flatMap {
      case (cluster, containers) =>
        if (Objects.isNull(key))
          Future.failed(new IllegalArgumentException("clusterName can't empty"))
        else if (CommonUtils.isEmpty(nodeName))
          Future.failed(new IllegalArgumentException("nodeName can't empty"))
        else if (CommonUtils.hasUpperCase(nodeName))
          Future.failed(new IllegalArgumentException("Your node name can't uppercase"))
        else if (cluster.nodeNames.contains(nodeName))
          // the new node is running so we don't need to do anything for this method
          Future.successful(cluster)
        else doAddNode(cluster, containers, nodeName)
    }

  // TODO: this is a deprecated method and should be removed in #2570
  final def addNode(clusterName: String, nodeName: String)(implicit executionContext: ExecutionContext): Future[T] =
    cluster(clusterName).flatMap {
      case (cluster, containers) =>
        if (CommonUtils.isEmpty(clusterName))
          Future.failed(new IllegalArgumentException("clusterName can't empty"))
        else if (CommonUtils.isEmpty(nodeName))
          Future.failed(new IllegalArgumentException("nodeName can't empty"))
        else if (CommonUtils.hasUpperCase(nodeName))
          Future.failed(new IllegalArgumentException("Your node name can't uppercase"))
        else if (cluster.nodeNames.contains(nodeName))
          // the new node is running so we don't need to do anything for this method
          Future.successful(cluster)
        else doAddNode(cluster, containers, nodeName)
    }

  /**
    * do the add actually. Normally, the sub-class doesn't need to check the existence of removed node.
    * @param previousCluster previous cluster
    * @param previousContainers previous container
    * @param newNodeName new node
    * @param executionContext thread pool
    * @return true if success. otherwise, false
    */
  protected def doAddNode(previousCluster: T, previousContainers: Seq[ContainerInfo], newNodeName: String)(
    implicit executionContext: ExecutionContext): Future[T]

  /**
    * remove a node from a running cluster.
    * NOTED: this is a async operation since graceful downing a node from a running service may be slow.
    * @param key cluster key
    * @param nodeName node name
    * @return true if it does remove a node from a running cluster. Otherwise, false
    */
  final def removeNode(key: ObjectKey, nodeName: String)(implicit executionContext: ExecutionContext): Future[Boolean] =
    clusters().flatMap(
      _.find(_._1.key == key)
        .filter(_._1.nodeNames.contains(nodeName))
        .filter(_._2.exists(_.nodeName == nodeName))
        .fold(Future.successful(false)) {
          case (cluster, runningContainers) =>
            runningContainers.size match {
              case 1 =>
                Future.failed(new IllegalArgumentException(
                  s"cluster [$key] is a single-node cluster. You can't remove the last node by removeNode(). Please use remove(clusterName) instead"))
              case _ =>
                doRemoveNode(
                  cluster,
                  runningContainers
                    .find(_.nodeName == nodeName)
                    .getOrElse(throw new IllegalArgumentException(
                      s"This should not be happen!!! $nodeName doesn't exist on cluster:$key"))
                )
            }
        })

  // TODO: this is a deprecated method and should be removed in #2570
  final def removeNode(clusterName: String, nodeName: String)(
    implicit executionContext: ExecutionContext): Future[Boolean] = clusters().flatMap(
    _.find(_._1.name == clusterName)
      .filter(_._1.nodeNames.contains(nodeName))
      .filter(_._2.exists(_.nodeName == nodeName))
      .fold(Future.successful(false)) {
        case (cluster, runningContainers) =>
          runningContainers.size match {
            case 1 =>
              Future.failed(new IllegalArgumentException(
                s"$clusterName is a single-node cluster. You can't remove the last node by removeNode(). Please use remove(clusterName) instead"))
            case _ =>
              doRemoveNode(
                cluster,
                runningContainers
                  .find(_.nodeName == nodeName)
                  .getOrElse(throw new IllegalArgumentException(
                    s"This should not be happen!!! $nodeName doesn't exist on cluster:$clusterName"))
              )
          }
      })

  /**
    * do the remove actually. Normally, the sub-class doesn't need to check the existence of removed node.
    * @param previousCluster previous cluster
    * @param beRemovedContainer the container to be removed
    * @param executionContext thread pool
    * @return true if success. otherwise, false
    */
  protected def doRemoveNode(previousCluster: T, beRemovedContainer: ContainerInfo)(
    implicit executionContext: ExecutionContext): Future[Boolean]

  /**
    * Get the cluster state by containers.
    * <p>
    * Note: we should separate the implementation from docker and k8s environment.
    * <p>
    * a cluster state machine:
    *       -----------------       -----------------       --------
    *       | Some(PENDING) |  -->  | Some(RUNNING) |  -->  | None |
    *       -----------------       -----------------       --------
    *                                      |
    *                                      | (terminated failure or running failure)
    *                                      |       ----------------
    *                                      ----->  | Some(FAILED) |
    *                                              ----------------
    * The cluster state rules
    * 1) RUNNING: all of the containers have been created and at least one container is in "running" state
    * 2) FAILED: all of the containers are terminated and at least one container has terminated failure
    * 3) PENDING: one of the containers are in creating phase
    * 4) UNKNOWN: other situations
    * 4) None: no containers
    *
    * @param containers container list
    * @return the cluster state
    */
  protected def toClusterState(containers: Seq[ContainerInfo]): Option[ClusterState]

  /**
    * return the short service name
    * @return service name
    */
  def serviceName: String

  //---------------------------[helper methods]---------------------------//

  /**
    * used to resolve the hostNames.
    * @param hostNames hostNames
    * @return hostname -> ip address
    */
  protected def resolveHostNames(hostNames: Set[String]): Map[String, String] =
    hostNames.map(hostname => hostname -> resolveHostName(hostname)).toMap

  /**
    * used to resolve the hostname.
    * @param hostname hostname
    * @return ip address or hostname (if you do nothing to it)
    */
  protected def resolveHostName(hostname: String): String = CommonUtils.address(hostname)
}

object Collie {

  /**
    * used to distinguish the cluster name and service name
    */
  private[agent] val DIVIDER: String = "-"
  private[agent] val UNKNOWN: String = "unknown"

  private[agent] val LENGTH_OF_CONTAINER_NAME_ID: Int = 7

  /**
    * generate unique name for the container.
    * It can be used in setting container's hostname and name
    * @param prefixKey environment prefix key
    * @param group cluster group
    * @param clusterName cluster name
    * @param serviceName the service type name for current cluster
    * @return a formatted string. form: {prefixKey}-{group}-{clusterName}-{service}-{index}
    */
  def format(prefixKey: String, group: String, clusterName: String, serviceName: String): String =
    Seq(
      prefixKey,
      group,
      clusterName,
      serviceName,
      CommonUtils.randomString(LENGTH_OF_CONTAINER_NAME_ID)
    ).mkString(DIVIDER)

  /**
    * a helper method to fetch the cluster key from container name
    *
    * @param containerName the container runtime name
    */
  private[agent] def objectKeyOfContainerName(containerName: String): ObjectKey =
    // form: PREFIX_KEY-GROUP-CLUSTER_NAME-SERVICE-HASH
    ObjectKey.of(containerName.split(DIVIDER)(1), containerName.split(DIVIDER)(2))

  /**
    * The basic creator that for cluster creation.
    * We define the "required" parameters for a cluster here, and you should fill in each parameter
    * in the individual cluster creation.
    * Note: the checking rules are moved to the api-creation level.
    * @tparam T Cluster information type
    */
  trait ClusterCreator[T <: ClusterInfo] extends com.island.ohara.common.pattern.Creator[Future[T]] {
    protected var imageName: String = _
    protected var group: String = _
    protected var clusterName: String = _
    protected var nodeNames: Set[String] = Set.empty
    protected var executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    /**
      * set the creator according to another cluster info
      * @param clusterInfo another cluster info
      */
    final def copy(clusterInfo: T): ClusterCreator.this.type = {
      imageName(clusterInfo.imageName)
      key(clusterInfo.key)
      nodeNames(clusterInfo.nodeNames)
      doCopy(clusterInfo)
      this
    }

    /**
      * Do more clone from another cluster.
      * @param clusterInfo another cluster
      */
    protected def doCopy(clusterInfo: T): Unit

    /**
      * set the image name used to create cluster's container.
      * Currently, there are three kind of services are supported by community. zk, bk and wk.
      * @param imageName image name
      * @return this creator
      */
    def imageName(imageName: String): ClusterCreator.this.type = {
      this.imageName = CommonUtils.requireNonEmpty(imageName)
      this
    }

    /**
      * a helper method to set the cluster key
      * @param key cluster key
      * @return this creation
      */
    private[this] def key(key: ObjectKey): ClusterCreator.this.type = {
      group(key.group())
      clusterName(key.name())
      this
    }

    /**
      * set the cluster group.
      * @param group cluster group
      * @return this creator
      */
    def group(group: String): ClusterCreator.this.type = {
      this.group = CommonUtils.requireNonEmpty(group)
      this
    }

    /**
      * set the cluster name.
      * @param clusterName cluster name
      * @return this creator
      */
    def clusterName(clusterName: String): ClusterCreator.this.type = {
      this.clusterName = CommonUtils.requireNonEmpty(clusterName)
      this
    }

    /**
      *  create a single-node cluster.
      *  NOTED: this is a async method since starting a cluster is always gradual.
      * @param nodeName node name
      * @return cluster description
      */
    def nodeName(nodeName: String): ClusterCreator.this.type = nodeNames(Set(nodeName))

    /**
      *  create a cluster.
      *  NOTED: this is a async method since starting a cluster is always gradual.
      * @param nodeNames nodes' name
      * @return cluster description
      */
    def nodeNames(nodeNames: Set[String]): ClusterCreator.this.type = {
      this.nodeNames = CommonUtils.requireNonEmpty(nodeNames.asJava).asScala.toSet
      this
    }

    /**
      * set the thread pool used to create cluster by async call
      * @param executionContext thread pool
      * @return this creator
      */
    @Optional("default pool is scala.concurrent.ExecutionContext.Implicits.global")
    def threadPool(executionContext: ExecutionContext): ClusterCreator.this.type = {
      this.executionContext = Objects.requireNonNull(executionContext)
      this
    }

    /**
      * submit a creation progress in background. the implementation should avoid creating duplicate containers on the
      * same nodes. If the pass nodes already have containers, this creation should be viewed as "adding" than creation.
      * for example, the cluster-A exists and it is running on node-01. When user pass a creation to run cluster-A on
      * node-02, the creation progress should be aware of that user tries to add a new node (node-02) to the cluster-A.
      */
    override def create(): Future[T]
  }
}
