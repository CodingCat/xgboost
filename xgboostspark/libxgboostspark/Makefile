export CXX = clang++ #$(if $(shell which g++-5),g++-5,g++)

export LDFLAGS= -pthread -lm
export CFLAGS = -fPIC -std=c++11 -L../../lib/ ../../dmlc-core/libdmlc.a  ../../build/cli_main.o -lxgboost -I../../include/xgboost -I../../dmlc-core/include -I../../rabit/include -Wall -O3 -shared  -Wno-unknown-pragmas -funroll-loops

all:
	$(CXX) $(CFLAGS) xgboostspark.cpp -o libxgboostspark.so

clean:
	rm -rf *.so
