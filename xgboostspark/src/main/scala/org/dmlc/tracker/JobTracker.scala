package org.dmlc.tracker

import java.io.File

import com.typesafe.config.Config
import org.dmlc.tracker.utils.FileAppender

private[dmlc] class JobTracker(conf: Config) {

  private var trackerScriptPath = ""
  private var trackerScriptName = ""

  private var stdoutAppender: FileAppender = null
  private var stderrAppender: FileAppender = null

  //runtime configuration
  private var verbose = false
  private var hostIP = ""
  private var numSlaves = 0


  private def bootstrap(): Unit = {
    verbose = conf.getBoolean("xgboostspark.distributed.tracker.verbose")
    hostIP = conf.getString("xgboostspark.distributed.tracker.ip")
    trackerScriptPath = conf.getString("xgboostspark.distributed.tracker.path")
    trackerScriptName = conf.getString("xgboostspark.distributed.tracker.scriptname")
    numSlaves = conf.getInt("xgboostspark.distributed.tracker.numSlaves")
  }

  private def buildProcess(command: String): ProcessBuilder = {
    val commandSeq = command.split(" ").toSeq
    new ProcessBuilder(commandSeq: _*)
  }

  def startRabitTracker(rabitTaskString: String): Boolean = {
    //TODO: start the rabit tracker
    val rabitTracker = buildProcess(s"$trackerScriptPath/$trackerScriptName -n $numSlaves $rabitTaskString").start()
    // Redirect its stdout and stderr to files
    val stdout = new File("./", "stdout")
    stdoutAppender = utils.FileAppender(rabitTracker.getInputStream, stdout)
    val stderr = new File("./", "stderr")
    stderrAppender = utils.FileAppender(rabitTracker.getErrorStream, stderr)
    val exitCode = rabitTracker.waitFor()
    val message = "Tracker exited with code " + exitCode
    println(message)
    exitCode == 0
  }

  /**
    * submit the spark job wrapping xgboost
    */
  def run(rabitTaskString: String): Boolean = {
    startRabitTracker(rabitTaskString)
  }

  //configure
  bootstrap()
}
