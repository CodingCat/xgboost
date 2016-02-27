import xgboost as xgb
import numpy as np
from sklearn.cross_validation import KFold
from sklearn.metrics import mean_squared_error
from sklearn.grid_search import GridSearchCV
from sklearn.datasets import load_iris, load_digits, load_boston

rng = np.random.RandomState(1994)

def test_binary_classification():
    digits = load_digits(2)
    y = digits['target']
    X = digits['data']
    kf = KFold(y.shape[0], n_folds=2, shuffle=True, random_state=rng)
    for train_index, test_index in kf:
        xgb_model = xgb.XGBClassifier().fit(X[train_index],y[train_index])
        preds = xgb_model.predict(X[test_index])
        labels = y[test_index]
        err = sum(1 for i in range(len(preds)) if int(preds[i]>0.5)!=labels[i]) / float(len(preds))
    assert err < 0.1

def test_multiclass_classification():
    iris = load_iris()
    y = iris['target']
    X = iris['data']
    kf = KFold(y.shape[0], n_folds=2, shuffle=True, random_state=rng)
    for train_index, test_index in kf:
        xgb_model = xgb.XGBClassifier().fit(X[train_index],y[train_index])
        preds = xgb_model.predict(X[test_index])
        # test other params in XGBClassifier().fit
        preds2 = xgb_model.predict(X[test_index], output_margin=True, ntree_limit=3)
        preds3 = xgb_model.predict(X[test_index], output_margin=True, ntree_limit=0)
        preds4 = xgb_model.predict(X[test_index], output_margin=False, ntree_limit=3)
        labels = y[test_index]
        err = sum(1 for i in range(len(preds)) if int(preds[i]>0.5)!=labels[i]) / float(len(preds))
    assert err < 0.4

def test_boston_housing_regression():
    boston = load_boston()
    y = boston['target']
    X = boston['data']
    kf = KFold(y.shape[0], n_folds=2, shuffle=True, random_state=rng)
    for train_index, test_index in kf:
        xgb_model = xgb.XGBRegressor().fit(X[train_index],y[train_index])
        preds = xgb_model.predict(X[test_index])
        # test other params in XGBRegressor().fit
        preds2 = xgb_model.predict(X[test_index], output_margin=True, ntree_limit=3)
        preds3 = xgb_model.predict(X[test_index], output_margin=True, ntree_limit=0)
        preds4 = xgb_model.predict(X[test_index], output_margin=False, ntree_limit=3)
        labels = y[test_index]
    assert mean_squared_error(preds, labels) < 25

def test_parameter_tuning():
    boston = load_boston()
    y = boston['target']
    X = boston['data']
    xgb_model = xgb.XGBRegressor()
    clf = GridSearchCV(xgb_model,
                       {'max_depth': [2,4,6],
                        'n_estimators': [50,100,200]}, verbose=1)
    clf.fit(X,y)
    assert clf.best_score_ < 0.7
    assert clf.best_params_ == {'n_estimators': 100, 'max_depth': 4}

def test_regression_with_custom_objective():
    def objective_ls(y_true, y_pred):
        grad = (y_pred - y_true)
        hess = np.ones(len(y_true))
        return grad, hess

    boston = load_boston()
    y = boston['target']
    X = boston['data']
    kf = KFold(y.shape[0], n_folds=2, shuffle=True, random_state=rng)
    for train_index, test_index in kf:
        xgb_model = xgb.XGBRegressor(objective=objective_ls).fit(
            X[train_index], y[train_index]
        )
        preds = xgb_model.predict(X[test_index])
        labels = y[test_index]
    assert mean_squared_error(preds, labels) < 25

    # Test that the custom objective function is actually used
    class XGBCustomObjectiveException(Exception):
        pass

    def dummy_objective(y_true, y_pred):
        raise  XGBCustomObjectiveException()

    xgb_model = xgb.XGBRegressor(objective=dummy_objective)
    np.testing.assert_raises(
        XGBCustomObjectiveException,
        xgb_model.fit,
        X, y
    )

def test_classification_with_custom_objective():
    def logregobj(y_true, y_pred):
        y_pred = 1.0 / (1.0 + np.exp(-y_pred))
        grad = y_pred - y_true
        hess = y_pred * (1.0-y_pred)
        return grad, hess

    digits = load_digits(2)
    y = digits['target']
    X = digits['data']
    kf = KFold(y.shape[0], n_folds=2, shuffle=True, random_state=rng)
    for train_index, test_index in kf:
        xgb_model = xgb.XGBClassifier(objective=logregobj).fit(
            X[train_index],y[train_index]
        )
        preds = xgb_model.predict(X[test_index])
        labels = y[test_index]
        err = sum(1 for i in range(len(preds))
                  if int(preds[i]>0.5)!=labels[i]) / float(len(preds))
    assert err < 0.1


    # Test that the custom objective function is actually used
    class XGBCustomObjectiveException(Exception):
        pass

    def dummy_objective(y_true, y_preds):
        raise  XGBCustomObjectiveException()

    xgb_model = xgb.XGBClassifier(objective=dummy_objective)
    np.testing.assert_raises(
        XGBCustomObjectiveException,
        xgb_model.fit,
        X, y
    )

