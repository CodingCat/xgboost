package org.dmlc;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

interface XGBoostLibrary extends Library {

  XGBoostLibrary INSTANCE = (XGBoostLibrary) Native.loadLibrary("xgboost_wrapper", XGBoostLibrary.class);

  // extend this to create a callback that will fill a data layer
  interface java_callback_t extends Callback {
    void invoke(Pointer matrix, Pointer indptr, Pointer indices, Pointer data);
  }

  int set_train_data_callback(Pointer xgBooster, java_callback_t callback);
  int set_test_data_callback(Pointer xgBooster, java_callback_t callback);

  int set_param(String param_name, String param_value);

  int train(int iterNum, Pointer gradient, Pointer hess);
  int test(int option_mask, long ntree_limit);

  // extensions for using Spark's communication module
  Pointer get_weights();
}
