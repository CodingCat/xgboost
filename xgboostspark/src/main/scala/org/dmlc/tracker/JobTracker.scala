package org.dmlc.tracker

import com.typesafe.config.Config


private[dmlc] class JobTracker(conf: Config) {

  def startRabitTracker(): Unit = {
    //TODO: start the rabit tracker
  }


  /**
    * submit the spark job wrapping xgboost
    */
  def run(submitFunc: => Unit): Boolean = {
    //TODO:
    false
  }

  //start the RabitTracker
  startRabitTracker()
}
