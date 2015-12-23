## Understand How XGBoost found Each Other##

We know that XGBoost relies on rabit library for communication. In this essay, we analyze how XGBoost utilizes rabit to discover the other node. 

XGBoost does not include distributed computing logic in its core library. Instead, it utilizes rabit to submit XGBoost as the distributed job and through rabit node, XGBoost can do allreduce, etc.


Next, we will analyze the structure of XGBoost as a rabit program.

When submitting XGBoost via Rabit, we need to have an executable version of XGBoost implemented with Rabit APIs. XGBoost implements the main function in `src/xgboost_main.cpp`.

### XGBoost_Main ###

As the Rabit program, the first thing we need to do is to initialize the rabit environment, `rabit::Init(argc, argv); `

What did we do in this Init? 

<b>subtree/rabit/include/rabit/rabit-inl.h</b>

``` 
105 // intialize the rabit engine                                                                                 
106 inline void Init(int argc, char *argv[]) {                                                                    
107   engine::Init(argc, argv);                                                                                   
108 } 

```

<b> subtree/rabit/src/engine.cc </b>

```
 30 /*! \brief intiialize the synchronization module */                                                           
 31 void Init(int argc, char *argv[]) {                                                                           
 32   for (int i = 1; i < argc; ++i) {                                                                            
 33     char name[256], val[256];                                                                                 
 34     if (sscanf(argv[i], "%[^=]=%s", name, val) == 2) {                                                        
 35       manager.SetParam(name, val);                                                                            
 36     }                                                                                                         
 37   }                                                                                                           
 38   manager.Init();                                                                                             
 39 }                
```


`manager ` is the instance of one of the three types of engine, which is defined in the same file:

```
 19 // singleton sync manager                                                                                     
 20 #ifndef RABIT_USE_BASE                                                                                        
 21 #ifndef RABIT_USE_MOCK                                                                                        
 22 AllreduceRobust manager;                                                                                      
 23 #else                                                                                                         
 24 AllreduceMock manager;                                                                                        
 25 #endif                                                                                                        
 26 #else                                                                                                         
 27 AllreduceBase manager;                                                                                        
 28 #endif                
```

Let's focus on the `AllreduceRobust`. AllreduceRobust implements the `Init()` as the following:

<b>subtree/rabit/src/allreduce_robust.cc</b>

```
  34 void AllreduceRobust::Init(void) {                                                                            
  35   AllreduceBase::Init();                                                                                      
  36   result_buffer_round = std::max(world_size / num_global_replica, 1);                                         
  37 }     
  
```

Most of initialization process is inherited from AllreduceBase::Init() which we will look at it later. AllreduceRobust has an additional variable indicating the version of the results, named `result_buffer_round `.


Now, we go into details of `AllreduceBase:Init(void) `:

<b>subtree/rabit/src/allreduce_base.cc</b>

```
 53 // initialization function                                                                                    
 54 void AllreduceBase::Init(void) {                                                                              
 55   // setup from enviroment variables                                                                          
 56   // handler to get variables from env                                                                        
 57   for (size_t i = 0; i < env_vars.size(); ++i) {                                                              
 58     const char *value = getenv(env_vars[i].c_str());                                                          
 59     if (value != NULL) {                                                                                      
 60       this->SetParam(env_vars[i].c_str(), value);                                                             
 61     }                                                                                                         
 62   }                                                                                                           
 63   {                                                                                                           
 64     // handling for hadoop                                                                                    
 65     const char *task_id = getenv("mapred_tip_id");                                                            
 66     if (task_id == NULL) {                                                                                    
 67       task_id = getenv("mapreduce_task_id");                                                                  
 68     }                                                                                                         
 69     if (hadoop_mode != 0) {                                                                                   
 70       utils::Check(task_id != NULL,                                                                           
 71                    "hadoop_mode is set but cannot find mapred_task_id");                                      
 72     }                                                                                                         
 73     if (task_id != NULL) {                                                                                    
 74       this->SetParam("rabit_task_id", task_id);                                                               
 75       this->SetParam("rabit_hadoop_mode", "1");                                                               
 76     }                                                                                                         
 77     const char *attempt_id = getenv("mapred_task_id");      
 78     if (attempt_id != 0) {                                                                                    
 79       const char *att = strrchr(attempt_id, '_');                                                             
 80       int num_trial;                                                                                          
 81       if (att != NULL && sscanf(att + 1, "%d", &num_trial) == 1) {                                            
 82         this->SetParam("rabit_num_trial", att + 1);                                                           
 83       }                                                                                                       
 84     }                                                                                                         
 85     // handling for hadoop                                                                                    
 86     const char *num_task = getenv("mapred_map_tasks");                                                        
 87     if (num_task == NULL) {                                                                                   
 88       num_task = getenv("mapreduce_job_maps");                                                                
 89     }                                                                                                         
 90     if (hadoop_mode != 0) {                                                                                   
 91       utils::Check(num_task != NULL,                                                                          
 92                    "hadoop_mode is set but cannot find mapred_map_tasks");                                    
 93     }                                                                                                         
 94     if (num_task != NULL) {                                                                                   
 95       this->SetParam("rabit_world_size", num_task);                                                           
 96     }                                                                                                         
 97   }                                                                                                           
 98   if (dmlc_role != "worker") {                                                                                
 99     fprintf(stderr, "Rabit Module currently only work with dmlc worker"\                                      
100             ", quit this program by exit 0\n");                                                               
101     exit(0);                                                                                                  
102   }                                                                                                           
103   // clear the setting before start reconnection                                                              
104   this->rank = -1;                                                                                            
105   //---------------------                                                                                     
106   // start socket                                                                                             
107   utils::Socket::Startup();                                                                                   
108   utils::Assert(all_links.size() == 0, "can only call Init once");                                            
109   this->host_uri = utils::SockAddr::GetHostName();                                                            
110   // get information from tracker                                                                             
111   this->ReConnectLinks();
112 }
```

Line 63 - 97 are all about how to use rabit in Hadoop framework, we can just skip it for now. The critical part is in Line 107 - 111. `utils:Socket:Startup()` is essentially creating a new socket. The most important code setuping the connection between the nodes in XGBoost is in `this->ReconnectLinks()`.


