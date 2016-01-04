package org.dmlc

import scala.collection.immutable.HashMap

class XGBoost(xgBoostLib: XGBoostLibrary) {

  /**
    * set the training data
    * @param trainingData a RDD partition containing training data (with labels)
    */
  def setTrainingData(trainingData: Iterator[(Array[Byte], Array[Int])]): Unit = {

  }

  /**
    * set the test data
    * @param testData a RDD partition containing data (with labels)
    */
  def setTestData(testData: Iterator[(Array[Byte], Array[Int])]): Unit = {

  }

  def getWeights(): List[Double] = {
    //TODO:
    null
  }

  def train(): Unit = {
    //TODO:
  }

  def test(): Array[Double] = {
    //TODO:
    null
  }
}

object XGBoost {

  /**
    * initialize the models with the provided params
    * @param params the name -> value pair of the parameters
    */
  def apply(params: HashMap[String, String]): XGBoost = {
    //TODO:
    null
  }
}
