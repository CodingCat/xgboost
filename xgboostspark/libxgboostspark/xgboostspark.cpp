#include "xgboostspark.h"

//XGBoost Header Files
#include "./learner/learner-inl.hpp" 
struct xgboost_state {
	xgboost::learner::BoostLearner* boost_learner;
};

struct xgboost_state global_xgboost_state;

java_callback_t training_data_callback;
java_callback_t test_data_callback;

void create_boost() {
}

void destroy_xgboost() {
}

int set_train_data_callback(java_callback_t callback) {
}

int set_test_data_callback(java_callback_t callback) {
}

int set_param(const char* param_name, const char* param_value) {
}

int train(int steps, float* gradient, float* hess) {
}


float** predict(bool out_put_margin, long tree_limit, bool pred_leaf) {
}

float* get_weights() {
}
