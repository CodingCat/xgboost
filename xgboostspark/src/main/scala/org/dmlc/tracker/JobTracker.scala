package org.dmlc.tracker

import java.io.File

import akka.actor.{ActorSystem, ActorRef, Address, Props}
import com.typesafe.config.Config
import org.apache.spark.{Logging, SparkConf, SparkEnv, SparkContext}
import org.apache.spark.rdd.RDD
import org.dmlc.tracker.utils.FileAppender

private[dmlc] class JobTracker(conf: Config) extends Serializable with Logging {

  @transient var sc: SparkContext = null
  @transient var system: ActorSystem = null

  private def init(): Unit = {
    val sparkConf = new SparkConf()
    //we prefer the configuration with one task per executor
    sparkConf.set("spark.task.cpus", conf.getInt("spark.task.cpus").toString)
    sparkConf.set("spark.executor.cores", conf.getInt("spark.executor.cores").toString)
    sc = new SparkContext(sparkConf)
    system = ActorSystem("JobTracker", conf)
  }

  private def runTrainingTask[T](dataRDD: RDD[T], jobTrackerActor: String): Unit = {

    /**
      * this function is to be executed by the distributed tasks
      * note: we have to block the thread here so that we need to ensure all distributed trees running simultaneously
      */
    def executionFunc(rddPartition: Iterator[T]): Unit = {
      // block the thread until all workers are available
      val sparkSystem = SparkEnv.get.actorSystem
      val taskProxyActor = sparkSystem.actorOf(Props(new TaskProxy(jobTrackerActor)))
      sparkSystem.awaitTermination()
    }

    dataRDD.sparkContext.runJob(dataRDD, executionFunc _)
    system.awaitTermination()
  }

  private def composeJobTrackerAddress: String = {
    s"akka.tcp://JobTracker@${conf.getString("xgboostspark.distributed.tracker.ip")}:3000/user/JobTracker}"
  }

  /**
    * submit the spark job wrapping xgboost
    */
  def run[T](dataRDD: RDD[T], rabitTaskString: String): Unit = {
    //start JobTracker actor
    val jtActorRef = system.actorOf(Props(new JobTrackerActor(conf)), name = "JobTracker")
    logInfo(s"started JobTracker ${jtActorRef.path}")
    //start tasks
    runTrainingTask(dataRDD, composeJobTrackerAddress)
  }

  init()
}
