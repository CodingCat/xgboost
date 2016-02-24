#include "xgboostspark.h"

//XGBoost Header Files
#include "learner.h"
#include "data.h"
#include "c_api.h"

enum CLITask {
	kTrain = 0,
	kDump2Text = 1,
	kPredict = 2
};

struct CLIParam : public dmlc::Parameter<CLIParam> {
  /*! \brief the task name */
  int task;
  /*! \brief whether silent */
  int silent;
  /*! \brief whether evaluate training statistics */
  bool eval_train;
  /*! \brief number of boosting iterations */
  int num_round;
  /*! \brief the period to save the model, 0 means only save the final round model */
  int save_period;
  /*! \brief the path of training set */
  std::string train_path;
  /*! \brief path of test dataset */
  std::string test_path;
  /*! \brief the path of test model file, or file to restart training */
  std::string model_in;
  /*! \brief the path of final model file, to be saved */
  std::string model_out;
  /*! \brief the path of directory containing the saved models */
  std::string model_dir;
  /*! \brief name of predict file */
  std::string name_pred;
  /*! \brief data split mode */
  int dsplit;
  /*!\brief limit number of trees in prediction */
  int ntree_limit;
  /*!\brief whether to directly output margin value */
  bool pred_margin;
  /*! \brief whether dump statistics along with model */
  int dump_stats;
  /*! \brief name of feature map */
  std::string name_fmap;
  /*! \brief name of dump file */
  std::string name_dump;
  /*! \brief the paths of validation data sets */
  std::vector<std::string> eval_data_paths;
  /*! \brief the names of the evaluation data used in output log */
  std::vector<std::string> eval_data_names;
  /*! \brief all the configurations */
  std::vector<std::pair<std::string, std::string> > cfg;

  // declare parameters
  DMLC_DECLARE_PARAMETER(CLIParam) {
    // NOTE: declare everything except eval_data_paths.
    DMLC_DECLARE_FIELD(task).set_default(kTrain)
        .add_enum("train", kTrain)
        .add_enum("dump", kDump2Text)
        .add_enum("pred", kPredict)
        .describe("Task to be performed by the CLI program.");
    DMLC_DECLARE_FIELD(silent).set_default(0).set_range(0, 2)
        .describe("Silent level during the task.");
    DMLC_DECLARE_FIELD(eval_train).set_default(false)
        .describe("Whether evaluate on training data during training.");
    DMLC_DECLARE_FIELD(num_round).set_default(10).set_lower_bound(1)
        .describe("Number of boosting iterations");
    DMLC_DECLARE_FIELD(save_period).set_default(0).set_lower_bound(0)
        .describe("The period to save the model, 0 means only save final model.");
    DMLC_DECLARE_FIELD(train_path).set_default("NULL")
        .describe("Training data path.");
    DMLC_DECLARE_FIELD(test_path).set_default("NULL")
        .describe("Test data path.");
    DMLC_DECLARE_FIELD(model_in).set_default("NULL")
        .describe("Input model path, if any.");
    DMLC_DECLARE_FIELD(model_out).set_default("NULL")
        .describe("Output model path, if any.");
    DMLC_DECLARE_FIELD(model_dir).set_default("./")
        .describe("Output directory of period checkpoint.");
    DMLC_DECLARE_FIELD(name_pred).set_default("pred.txt")
        .describe("Name of the prediction file.");
    DMLC_DECLARE_FIELD(dsplit).set_default(0)
        .add_enum("auto", 0)
        .add_enum("col", 1)
        .add_enum("row", 2)
        .describe("Data split mode.");
    DMLC_DECLARE_FIELD(ntree_limit).set_default(0).set_lower_bound(0)
        .describe("Number of trees used for prediction, 0 means use all trees.");
    DMLC_DECLARE_FIELD(pred_margin).set_default(false)
        .describe("Whether to predict margin value instead of probability.");
    DMLC_DECLARE_FIELD(dump_stats).set_default(false)
        .describe("Whether dump the model statistics.");
    DMLC_DECLARE_FIELD(name_fmap).set_default("NULL")
        .describe("Name of the feature map file.");
    DMLC_DECLARE_FIELD(name_dump).set_default("dump.txt")
        .describe("Name of the output dump text file.");
    // alias
    DMLC_DECLARE_ALIAS(train_path, data);
    DMLC_DECLARE_ALIAS(test_path, test:data);
    DMLC_DECLARE_ALIAS(name_fmap, fmap);
  }
  // customized configure function of CLIParam
  inline void Configure(const std::vector<std::pair<std::string, std::string> >& cfg) {
    this->cfg = cfg;
    this->InitAllowUnknown(cfg);
    for (const auto& kv : cfg) {
      if (!strncmp("eval[", kv.first.c_str(), 5)) {
        char evname[256];
        CHECK_EQ(sscanf(kv.first.c_str(), "eval[%[^]]", evname), 1)
            << "must specify evaluation name for display";
        eval_data_names.push_back(std::string(evname));
        eval_data_paths.push_back(kv.second);
      }
    }
    // constraint.
    if (name_pred == "stdout") {
      save_period = 0;
      silent = 1;
    }
    if (dsplit == 0 && rabit::IsDistributed()) {
      dsplit = 2;
    }
    if (rabit::GetRank() != 0) {
      silent = 2;
    }
  }
};

DMLC_REGISTER_PARAMETER(CLIParam);

struct xgboost_state {
	xgboost::Learner *boost_learner;
	std::vector<std::pair<std::string, std::string>> cfg;
	bool initialized;
	std::vector<xgboost::DMatrix*> cache_mats;
	std::vector<xgboost::DMatrix*> eval_datasets;
};

struct xgboost_state global_xgboost_state;

java_callback_t training_data_callback;
java_callback_t test_data_callback;

void create_xgboost() {
	using namespace xgboost;
	CLIParam param;
	param.Configure(global_xgboost_state.cfg);
	global_xgboost_state.boost_learner = Learner::Create(global_xgboost_state.cache_mats);
	global_xgboost_state.boost_learner->Configure(param.cfg);
}

void destroy_xgboost() {
	delete global_xgboost_state.boost_learner;
}

int set_train_data_callback(java_callback_t callback) {
	return 0;
}

int set_test_data_callback(java_callback_t callback) {
	return 0;
}

int set_param(const char* param_name, const char* param_value) {
	global_xgboost_state.cfg.push_back(std::make_pair(std::string(param_name), std::string(param_value)));	
	return 0;
}

void init_rabit() {
	std::vector<std::pair<std::string, std::string>> cfg = global_xgboost_state.cfg;
	for (int i = 1; i < cfg.size() + 1; i++) {
		rabit::engine::GetEngine()->SetParam(cfg[i].first.data(), cfg[i].second.data());
	}
}

int train(int steps, float* gradient, float* hess) {
	init_rabit();
	if (!global_xgboost_state.initialized) {
		create_xgboost();
	}

	rabit::Finalize();
	return 0;
}

float** predict(bool out_put_margin, long tree_limit, bool pred_leaf) {
	return NULL;
}

float* get_weights() {
	return NULL;
}
