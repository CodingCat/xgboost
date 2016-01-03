package org.dmlc.tracker

import com.typesafe.config.Config


class JobTracker(conf: Config) {


  private def startRabitTracker(): Unit = {
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
