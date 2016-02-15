#include "xgboostspark.h"

//XGBoost Header Files
#include "learner.h"
#include "c_api.h" 
struct xgboost_state {
	xgboost::Learner* boost_learner;
};

struct xgboost_state global_xgboost_state;

java_callback_t training_data_callback;
java_callback_t test_data_callback;

void create_boost() {
}

void destroy_xgboost() {
}

int set_train_data_callback(java_callback_t callback) {
	return 0;
}

int set_test_data_callback(java_callback_t callback) {
	return 0;
}

int set_param(const char* param_name, const char* param_value) {
	return 0;
}

int train(int steps, float* gradient, float* hess) {
	return 0;
}

float** predict(bool out_put_margin, long tree_limit, bool pred_leaf) {
	return NULL;
}

float* get_weights() {
	return NULL;
}
