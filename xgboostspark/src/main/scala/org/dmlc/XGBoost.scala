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

  /**
    * save the weights of the model
    * @return the array of the weights
    */
  def getWeights(): List[Double] = {
    //TODO:
    null
  }

  /**
    * train the model and synchronize the watched metrics for every monitorInterval iterations
    * @param round the total number of iterations we run
    * @param monitorInterval the number of iterations for watched metric synchronization
    * @param watches the data for watches
    */
  def train(round: Int, monitorInterval: Int, watches: Option[Iterable[(String, Array[Byte])]] = None): Unit = {
    //TODO:
  }

  /**
    * predict based on the given test set
    * @param outPutMargin whether to output the raw untransformed margin value. (default false)
    * @param treeLimit Limit number of trees in the prediction; defaults to 0 (use all trees).
    * @param predLeaf When this option is on, the output will be a matrix of (nsample, ntrees), nsample = data.numRow
            with each record indicating the predicted leaf index of each sample in each tree.
            Note that the leaf index of a tree is unique per tree, so you may find leaf 1
            in both tree 1 and tree 0.
    * @return the prediction results
    */
  def predict(outPutMargin: Boolean = false, treeLimit: Long = 0, predLeaf: Boolean = false): Array[Array[Double]] = {
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
    new XGBoost(XGBoostLibrary.INSTANCE)
  }
}
