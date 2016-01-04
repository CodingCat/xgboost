package org.dmlc;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

interface XGBoostLibrary extends Library {

  XGBoostLibrary INSTANCE = (XGBoostLibrary) Native.loadLibrary("xgboost_wrapper", XGBoostLibrary.class);

  // extend this to create a callback that will fill a data layer
  interface java_callback_t extends Callback {
    void invoke(Pointer matrix, long[] indptr, int[] indices, float[] data);
  }

  int set_train_data_callback(Pointer xgBooster, java_callback_t callback);
  int set_test_data_callback(Pointer xgBooster, java_callback_t callback);

  int set_param(String param_name, String param_value);

  int train();
  int test();

  // extensions for using Spark's communication module
  Pointer get_weights();
}
