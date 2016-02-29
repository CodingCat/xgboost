## Outline of Design ##

### Overview ###

This documents presents the design of <b>SparkXGBoost</b>, a library running the [XGBoost](https://github.com/dmlc/xgboost) with Spark. 

The goal of the project is to provide a tool for the user so that they can build an unified pipeline including data preprocessing, machine learning model training/prediction, as well as the futher steps in a data analytic task (e.g. saving the model to the database, etc.).

In these stages of the pipeline, Spark is one of the most widely used platform for the first and the last stages. For machine learning components, a significant amount of ML libraries are written in C++. To reuse the existing libraries in JVM-based environment like Spark, there are two approaches:

* Develop JVM-language (for simplicity, we use `Java` in this document to represent the languages in this family) Bindings of C++ libraries: Java Bindings of C++ usually have a one-one mapping from Java API to the C++ library functionality points. For example, the current Java Binding of XGBoost includes APIS ranging from loading data to building a matrix to move one step in the training process, etc. The <b> advantage</b> of this approach is that the user has the fine-grained controlling ability of the program. The <b>down</b> side is that we get the controlling permission at the cost of overhead for calling Native method frequently.

* Take C++ libraries as black-box: This approach divides the training process into three phases: (1) copy data from JVM heap to C++ space; (2) trigger C++ program via JNI/JNA; (3) fetch the results from C++ direct memory space to JVM heap. The interaction between JVM program and the C++ library only happens for 3 times, while we lose the chances to have a fine-grained control of the program.


We adopt the `black-box` approach in the current design.

### Modules ###

<b>XGBoostSpark</b> includes three modules:

##### XGBoostLibrary

XGBoostLibrary has two components, C++-implemented wrapper of XGBoost and the JNA interface providing the interfaces for other modules, `org.dmlc.XGBoostLibrary`. 

`org.dmlc.XGBoostLibrary` is not supposed to be accessible to the user, instead, we shall provide the user interface wrapping this interface. The reason is that we do not expect the user to operate with the obscure parameter type like `com.sun.jna.Pointer`.

##### XGBoost

XGBoost is the user-facing interface which provides the interface to the user to start XGBoost program 

##### JobTracker

JobTracker is the program maintaining the information of the rabit job and connection between the nodes

