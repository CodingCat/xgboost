package org.dmlc.tracker

//messages from TaskProxy to JobTracker
final case class RegisterTaskProxy(workerId: Int)

//messages from JobTracker to TaskProxy
case object StartTraining
