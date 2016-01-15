package org.dmlc.examples

import java.io.File

import com.typesafe.config.ConfigFactory
import org.dmlc.XGBoost
import org.dmlc.tracker.JobTracker

import scala.collection.immutable.HashMap

object XGBoostExample {

  //NOTE: just for test for now
  def main(args: Array[String]): Unit = {
    val paramMaps = new HashMap[String, String]
    val booster = XGBoost(paramMaps)
    booster.getWeights()
    val jt = new JobTracker(ConfigFactory.parseFile(new File(args(0))))
    jt.run("rabit_basic.py")
  }
}
