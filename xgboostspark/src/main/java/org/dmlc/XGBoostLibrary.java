package org.dmlc;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

interface XGBoostLibrary extends Library {

  XGBoostLibrary INSTANCE = (XGBoostLibrary) Native.loadLibrary("xgboostspark", XGBoostLibrary.class);

  // extend this to create a callback that will fill a data layer
  interface java_callback_t extends Callback {
    void invoke(Pointer matrix, Pointer indptr, Pointer indices, Pointer data, int num_iprt, int num_elem);
  }

  void create_xgboost();
  void destroy_xgboost();

  int set_data_callback(java_callback_t callback);

  int set_param(String param_name, String param_value);

  //move forward with steps iterations, given the gradient and hess matrix
  int train(int steps, Pointer gradient, Pointer hess);
  float[][] predict(boolean outPutMargin, long treeLimit, boolean predLeaf);

  float[] get_weights();
}
