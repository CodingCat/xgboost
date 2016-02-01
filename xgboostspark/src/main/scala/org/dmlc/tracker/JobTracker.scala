package org.dmlc.tracker

import java.io.File

import akka.actor.Props
import com.typesafe.config.Config
import org.apache.spark.{SparkEnv, SparkContext}
import org.apache.spark.rdd.RDD
import org.dmlc.tracker.utils.FileAppender

private[dmlc] class JobTracker(conf: Config) {

  /**
    * this function is to be executed by the distributed tasks
    * note: we have to block the thread here so that we need to ensure that all distributed trees are running
    * simultaneously
    */
  private def executionFunc[T](rddPartition: Iterator[T]): Unit = {
    // block the thread until all workers are available
    // NOTE: use actorSystem for now, for Spark 1.6+, we need to use RpcEndpoint since Akka was removed from Spark
    val system = SparkEnv.get.actorSystem
    val taskProxyActor = system.actorOf(Props(new TaskProxy()))
    system.awaitTermination()
  }

  /**
    * submit the spark job wrapping xgboost
    */
  def run[T](dataRDD: RDD[T], rabitTaskString: String): Boolean = {
    //start JobTracker actor
    SparkEnv.get.actorSystem.actorOf(Props(new JobTrackerActor(conf)))
    //start tasks
    dataRDD.sparkContext.runJob(dataRDD, executionFunc _)
    SparkEnv.get.actorSystem.awaitTermination()
    true
  }

}
