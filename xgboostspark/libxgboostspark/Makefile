export CC  = $(if $(shell which gcc-5),gcc-5,gcc)
export CXX = $(if $(shell which g++-5),g++-5,g++)

export LDFLAGS= -pthread -lm
export CFLAGS = -I../../src -Wall -O3 -shared  -Wno-unknown-pragmas -funroll-loops

all:
	$(CXX) $(CFLAGS) xgboostspark.cpp -o libxgboostspark.so
