package org.dmlc.tracker

import java.io.File

import akka.actor.Actor
import akka.actor.Actor.Receive
import com.typesafe.config.Config
import org.dmlc.tracker.utils.FileAppender

import scala.collection.mutable


class JobTrackerActor(conf: Config) extends Actor {

  private val registeredSlaves = new mutable.HashSet[Int]

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

  /**
    * start tracker in driver side and
    */
  private def startRabitTracker(): Boolean = {
    //TODO: start the rabit tracker
    val rabitTracker = buildProcess(s"$trackerScriptPath/$trackerScriptName -n $numSlaves").start()
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

  private def checkTrackerHasStarted(): Boolean = {
    //TODO: we have a fixed length of sleeping interval for now, because we directly run tracker as the external script
    //an ideal way to do this is to implement tracker as well as rabit proxy  in scala
    Thread.sleep(100000)
    true
  }

  private def startXGBoost(): Unit = {
    //TODO: start XGBoost
    val rabitTrackerThread = new Thread(new Runnable {
      override def run(): Unit = {
        startRabitTracker()
      }
    })
    rabitTrackerThread.start()
    //TODO: run rabit task via spark
    checkTrackerHasStarted()
  }

  override def receive: Receive = {
    case RegisterTaskProxy(taskProxyID: Int) =>
      registeredSlaves += taskProxyID
      if (registeredSlaves.size == numSlaves) {
        startXGBoost()
      }
  }

  bootstrap()
}
