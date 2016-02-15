package org.dmlc.tracker

import java.io.File

import akka.actor.Props
import com.typesafe.config.Config
import org.apache.spark.{SparkEnv, SparkContext}
import org.apache.spark.rdd.RDD
import org.dmlc.tracker.utils.FileAppender

private[dmlc] class JobTracker(conf: Config) {

  private def runTrainingTask[T](dataRDD: RDD[T], jobTrackerAddr: String): Unit = {
    /**
      * this function is to be executed by the distributed tasks
      * note: we have to block the thread here so that we need to ensure all distributed trees running simultaneously
      */
    def executionFunc(rddPartition: Iterator[T]): Unit = {
      // block the thread until all workers are available
      // NOTE: use actorSystem for now, for Spark 1.6+, we need to use RpcEndpoint since Akka was removed from Spark
      val system = SparkEnv.get.actorSystem
      val taskProxyActor = system.actorOf(Props(new TaskProxy(jobTrackerAddr)))
      system.awaitTermination()
    }

    dataRDD.sparkContext.runJob(dataRDD, executionFunc _)
    SparkEnv.get.actorSystem.awaitTermination()
  }

  /**
    * submit the spark job wrapping xgboost
    */
  def run[T](dataRDD: RDD[T], rabitTaskString: String): Unit = {
    //start JobTracker actor
    val jtAddress = SparkEnv.get.actorSystem.actorOf(Props(new JobTrackerActor(conf)))
    //start tasks
    runTrainingTask(dataRDD, jtAddress.path.toString)
  }

}
