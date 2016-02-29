package org.dmlc.tracker

import akka.actor.{ActorRef, ActorSelection, Actor}
import akka.actor.Actor.Receive

class TaskProxy(jobTrackerAddress: String) extends Actor with Specializable {

  var jobTrackerHandler: ActorSelection = null

  override def preStart(): Unit = {
    self ! "start"
  }

  private def startTraining(): Unit = {
    //TODO:
  }

  override def receive: Receive = {
    case StartTraining =>
      startTraining()
    case "start" =>
      jobTrackerHandler = context.actorSelection(jobTrackerAddress)
      jobTrackerHandler ! RegisterTaskProxy
  }
}
