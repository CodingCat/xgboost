// A low level C API for XGBoost


extern "C" {

  typedef void (*java_callback_t) (void* matrix, long* indexorcol_pointer, unsigned* indices, float* data, int num_iptr, int num_elem);

	void create_xgboost();
	void destroy_xgboost();

	int set_data_callback(java_callback_t callback);

	int set_param(const char* param_name, const char* param_value);
	
	//main entries
	int train(int steps, float* gradient, float* hess);
	float** predict(bool out_put_margin, long tree_limit, bool pred_leaf);

	float* get_weights();
}
