package com.island.ohara.agent
import com.island.ohara.common.util.Releasable

/**
  * This is the top-of-the-range "collie". It maintains and organizes all collies.
  * Each getter should return new instance of collie since each collie has close() method.
  * However, it is ok to keep global instance of collie if they have dump close().
  * Currently, default implementation is based on ssh and docker command. It is simple but slow.
  * TODO: We are looking for k8s implementation...by chia
  */
trait ClusterCollie extends Releasable {

  /**
    * create a collie for zookeeper cluster
    * @return zookeeper collie
    */
  def zookeepersCollie(): ZookeeperCollie

  /**
    * create a collie for broker cluster
    * @return broker collie
    */
  def brokerCollie(): BrokerCollie

  /**
    * create a collie for worker cluster
    * @return worker collie
    */
  def workerCollie(): WorkerCollie
}

/**
  * the default implementation uses ssh and docker command to manage all clusters.
  * Each node running the service has name "${clusterName}-${service}-${index}".
  * For example, there is a worker cluster called "workercluster" and it is run on 3 nodes.
  * node-0 => workercluster-worker-0
  * node-1 => workercluster-worker-1
  * node-2 => workercluster-worker-2
  */
object ClusterCollie {
  def apply(implicit nodeCollie: NodeCollie): ClusterCollie = new ClusterCollieImpl
}