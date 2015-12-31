package org.dmlc;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface XGBoostLibrary extends Library {

  XGBoostLibrary INSTANCE = (XGBoostLibrary) Native.loadLibrary("xgboost_wrapper", XGBoostLibrary.class);

  // extend this to create a callback that will fill a data layer
  interface java_callback_t extends Callback {
    void invoke(Pointer data, int batch_size, int num_dims, Pointer shape);
  }

  int set_train_data_callback(Pointer state, java_callback_t callback);

  int set_test_data_callback(Pointer state, java_callback_t callback);


}
