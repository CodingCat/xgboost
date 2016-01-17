package org.dmlc.examples

import java.io.File

import com.typesafe.config.ConfigFactory
import org.apache.spark.SparkContext
import org.dmlc.XGBoost
import org.dmlc.tracker.JobTracker

import scala.collection.immutable.HashMap

object XGBoostExample {

  //NOTE: just for test for now
  def main(args: Array[String]): Unit = {
    val paramMaps = new HashMap[String, String]
    val booster = XGBoost(paramMaps)
    booster.getWeights()
    val configPath = args(0)
    val rabitTaskStr = args(1)
    val jt = new JobTracker(ConfigFactory.parseFile(new File(configPath)))
    val sc = new SparkContext()
    val dataRDD = sc.parallelize(List(1, 2, 3, 4), numSlices = 4)
    jt.run(dataRDD, rabitTaskStr)
  }
}
