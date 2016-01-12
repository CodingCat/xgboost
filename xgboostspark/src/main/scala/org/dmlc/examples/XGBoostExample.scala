package org.dmlc.examples

import org.dmlc.XGBoost

import scala.collection.immutable.HashMap

object XGBoostExample {

  //NOTE: just for test for now
  def main(args: Array[String]): Unit = {
    val paramMaps = new HashMap[String, String]
    val booster = XGBoost(paramMaps)

    booster.getWeights()
  }
}
