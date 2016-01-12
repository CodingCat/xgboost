package org.dmlc.api

trait ObjectFunc {
  /**
    * user define objective function, return gradient and second order gradient
    * @param predicts untransformed margin predicts
    * @param dtrain training data
    * @return Tuple with two float array, correspond to first order grad and second order grad
    */
  def getGradient(predicts: Array[Array[Float]], dtrain: Array[Byte]): (Array[Float], Array[Float])
}
