## Understand How XGBoost found Each Other##

We know that XGBoost relies on rabit library for communication. In this essay, we analyze how XGBoost utilizes rabit to discover the other node. 

XGBoost does not include distributed computing logic in its core library. Instead, it utilizes rabit to submit XGBoost as the distributed job and through rabit node, XGBoost can do allreduce, etc.


Next, we will analyze the structure of XGBoost as a rabit program.

When submitting XGBoost via Rabit, we need to have an executable version of XGBoost implemented with Rabit APIs. XGBoost implements the main function in `src/xgboost_main.cpp`.

### XGBoost_Main ###

As the Rabit program, the first thing we need to do is to initialize the rabit environment, `rabit::Init(argc, argv); `

What did we do in this Init? 

<b>subtree/rabit/include/rabit/rabit-inl.h</b>

```c++
105 // intialize the rabit engine                                                                                 
106 inline void Init(int argc, char *argv[]) {                                                                    
107   engine::Init(argc, argv);                                                                                   
108 } 

```

<b> subtree/rabit/src/engine.cc </b>

```c++
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

```c++
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

```c++
  34 void AllreduceRobust::Init(void) {                                                                            
  35   AllreduceBase::Init();                                                                                      
  36   result_buffer_round = std::max(world_size / num_global_replica, 1);                                         
  37 }     
  
```

Most of initialization process is inherited from AllreduceBase::Init() which we will look at it later. AllreduceRobust has an additional variable indicating the version of the results, named `result_buffer_round `.


Now, we go into details of `AllreduceBase:Init(void) `:

<b>subtree/rabit/src/allreduce_base.cc</b>

```c++
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


ReconnectLinks(const char * cmd) is the function implemented in AllReduceBase engine. While the suggestion offered by its name, it is also called for establishing the link between the nodes for the first time. The default value of `cmd` is "start".


<b>subtree/rabit/src/allreduce_base.cc</b>

```c++
227 /*!                                                                                                           
228  * \brief connect to the tracker to fix the the missing links                                                 
229  *   this function is also used when the engine start up                                                      
230  */                                                                                                           
231 void AllreduceBase::ReConnectLinks(const char *cmd) {                                                         
232   // single node mode                                                                                         
233   if (tracker_uri == "NULL") {                                                                                
234     rank = 0; world_size = 1; return;                                                                         
235   }                                                                                                           
236   utils::TCPSocket tracker = this->ConnectTracker();                                                          
237   tracker.SendStr(std::string(cmd));    
```

The above lines connect with the Tracker and start the handshaking process with the tracker. `ConnectTracker()` sends the local configuration to the tracker, 

<b>subtree/rabit/src/allreduce_base.cc</b> 

```c++
214   using utils::Assert;                                                                                        
215   Assert(tracker.SendAll(&magic, sizeof(magic)) == sizeof(magic),                                             
216          "ReConnectLink failure 1");                                                                          
217   Assert(tracker.RecvAll(&magic, sizeof(magic)) == sizeof(magic),                                             
218          "ReConnectLink failure 2");                                                                          
219   utils::Check(magic == kMagic, "sync::Invalid tracker message, init failure");                               
220   Assert(tracker.SendAll(&rank, sizeof(rank)) == sizeof(rank),                                                
221                 "ReConnectLink failure 3");                                                                   
222   Assert(tracker.SendAll(&world_size, sizeof(world_size)) == sizeof(world_size),                              
223          "ReConnectLink failure 3");                                                                          
224   tracker.SendStr(task_id);
```



<b>subtree/rabit/src/allreduce_base.cc</b>

```c++                                                                      
238                                                                                                               
239   // the rank of previous link, next link in ring                                                             
240   int prev_rank, next_rank;                                                                                   
241   // the rank of neighbors                                                                                    
242   std::map<int, int> tree_neighbors;                                                                          
243   using utils::Assert;                                                                                        
244   // get new ranks                                                                                            
245   int newrank, num_neighbors;                                                                                 
246   Assert(tracker.RecvAll(&newrank, sizeof(newrank)) == sizeof(newrank),                                       
247            "ReConnectLink failure 4");                                                                        
248   Assert(tracker.RecvAll(&parent_rank, sizeof(parent_rank)) ==\                                               
249          sizeof(parent_rank), "ReConnectLink failure 4");
250   Assert(tracker.RecvAll(&world_size, sizeof(world_size)) == sizeof(world_size),                              
251          "ReConnectLink failure 4");                                                                          
252   Assert(rank == -1 || newrank == rank,                                                                       
253          "must keep rank to same if the node already have one");                                              
254   rank = newrank;                                                                                             
255   Assert(tracker.RecvAll(&num_neighbors, sizeof(num_neighbors)) ==  \                                         
256          sizeof(num_neighbors), "ReConnectLink failure 4");                                                   
```

After the connection is established, the new node is trying to receive various types of information from Tracker. Briefly, `RecvAll` is a wrapper of socket function which reads all data from remote end in blocking fashion. To understand this code block, we need to see the handshaking protocol between the node and the tracker. We move forward to <b>subtree/rabit/tracker/rabit_tracker.py</b>

The main function is Tracker is implemented as 

```python
311 def submit(nslave, args, fun_submit, verbose, hostIP = 'auto'):                                               
312     master = Tracker(verbose = verbose, hostIP = hostIP)                                                      
313     submit_thread = Thread(target = fun_submit, args = (nslave, args, master.slave_envs()))                   
314     submit_thread.daemon = True                                                                               
315     submit_thread.start()                                                                                     
316     master.accept_slaves(nslave)                                                                              
317     submit_thread.join()      

```

Tracker process calls `accept_slaves(nslave)` to wait for the client connections. 

<b>subtree/rabit/tracker/rabit_tracker.py</b>

```python
245     def accept_slaves(self, nslave):                                                                          
246         # set of nodes that finishs the job                                                                   
247         shutdown = {}                                                                                         
248         # set of nodes that is waiting for connections                                                        
249         wait_conn = {}                                                                                        
250         # maps job id to rank                                                                                 
251         job_map = {}                                                                                          
252         # list of workers that is pending to be assigned rank                                                 
253         pending = []                                                                                          
254         # lazy initialize tree_map                                                                            
255         tree_map = None                                                                                       
256                                                                                                               
257         while len(shutdown) != nslave:                                                                        
258             fd, s_addr = self.sock.accept()                                                                   
259             s = SlaveEntry(fd, s_addr)   
```

The SlaveEntry constructor contains the process of receiving information via socket, the process corresponds to the sending order in ``ConnectTracker()`.

<b>subtree/rabit/tracker/rabit_tracker.py</b>

```python                                                                     
271             # lazily initialize the slaves                                                                    
272             if tree_map == None:                                                                              
273                 assert s.cmd == 'start'                                                                       
274                 if s.world_size > 0:                                                                          
275                     nslave = s.world_size                                                                     
276                 tree_map, parent_map, ring_map = self.get_link_map(nslave)                                    
277                 # set of nodes that is pending for getting up                                                 
278                 todo_nodes = range(nslave)                                                                    
279             else:                                                                                             
280                 assert s.world_size == -1 or s.world_size == nslave                          
```

The magic here is `get_link_map`: (TODO)

<b>subtree/rabit/tracker/rabit_tracker.py</b>

```python                 
281             if s.cmd == 'recover':                                                                            
282                 assert s.rank >= 0                                                                            
283                                                                                                               
284             rank = s.decide_rank(job_map)                                                                     
285             # batch assignment of ranks                                                                       
286             if rank == -1:                                                                                    
287                 assert len(todo_nodes) != 0                                                                   
288                 pending.append(s)                                                                             
289                 if len(pending) == len(todo_nodes):                                                           
290                     pending.sort(key = lambda x : x.host)                                                     
291                     for s in pending:                                                                         
292                         rank = todo_nodes.pop(0)                                                              
293                         if s.jobid != 'NULL':                                                                 
294                             job_map[s.jobid] = rank                                                           
295                         s.assign_rank(rank, wait_conn, tree_map, parent_map, ring_map)                        
296                         if s.wait_accept > 0:                                                                 
297                             wait_conn[rank] = s                                                               
298                         self.log_print('Recieve %s signal from %s; assign rank %d' % (s.cmd, s.host, s.rank), 1)
299                 if len(todo_nodes) == 0:                                                                      
300                     self.log_print('@tracker All of %d nodes getting started' % nslave, 2) 
301                     self.start_time = time.time()                                                             
302             else:                                                                                             
303                 s.assign_rank(rank, wait_conn, tree_map, parent_map, ring_map)                                
304                 self.log_print('Recieve %s signal from %d' % (s.cmd, s.rank), 1)                              
305                 if s.wait_accept > 0:                                                                         
306                     wait_conn[rank] = s                                                                       
307         self.log_print('@tracker All nodes finishes job', 2)                                                  
308         self.end_time = time.time()                                                                           
309         self.log_print('@tracker %s secs between node start and job finish' % str(self.end_time - self.start_time), 2)
```






<b>subtree/rabit/src/allreduce_base.cc</b>

```c++
257   for (int i = 0; i < num_neighbors; ++i) {                                                                   
258     int nrank;                                                                                                
259     Assert(tracker.RecvAll(&nrank, sizeof(nrank)) == sizeof(nrank),                                           
260            "ReConnectLink failure 4");                                                                        
261     tree_neighbors[nrank] = 1;                                                                                
262   }                                                                                                           
263   Assert(tracker.RecvAll(&prev_rank, sizeof(prev_rank)) == sizeof(prev_rank),                                 
264          "ReConnectLink failure 4");                                                                          
265   Assert(tracker.RecvAll(&next_rank, sizeof(next_rank)) == sizeof(next_rank),                                 
266          "ReConnectLink failure 4");
267   // create listening socket                                                                                  
268   utils::TCPSocket sock_listen;                                                                               
269   sock_listen.Create();                                                                                       
270   int port = sock_listen.TryBindHost(slave_port, slave_port + nport_trial);                                   
271   utils::Check(port != -1, "ReConnectLink fail to bind the ports specified");                                 
272   sock_listen.Listen();                                                                                       
273                                                                                                               
274   // get number of to connect and number of to accept nodes from tracker                                      
275   int num_conn, num_accept, num_error = 1;                                                                    
276   do {                                                                                                        
277     // send over good links                                                                                   
278     std::vector<int> good_link;                                                                               
279     for (size_t i = 0; i < all_links.size(); ++i) {                                                           
280       if (!all_links[i].sock.BadSocket()) {                                                                   
281         good_link.push_back(static_cast<int>(all_links[i].rank));                                             
282       } else {                                                                                                
283         if (!all_links[i].sock.IsClosed()) all_links[i].sock.Close();                                         
284       }                                                                                                       
285     }                                                                                                         
286     int ngood = static_cast<int>(good_link.size());                                                           
287     Assert(tracker.SendAll(&ngood, sizeof(ngood)) == sizeof(ngood),                                           
288            "ReConnectLink failure 5");                                                                        
289     for (size_t i = 0; i < good_link.size(); ++i) {                                                           
290       Assert(tracker.SendAll(&good_link[i], sizeof(good_link[i])) == \                                        
291              sizeof(good_link[i]), "ReConnectLink failure 6");                                                
292     }                                                                                                         
293     Assert(tracker.RecvAll(&num_conn, sizeof(num_conn)) == sizeof(num_conn),                                  
294            "ReConnectLink failure 7");                                                                        
295     Assert(tracker.RecvAll(&num_accept, sizeof(num_accept)) ==  \                                             
296            sizeof(num_accept), "ReConnectLink failure 8");                                                    
297     num_error = 0;
298     for (int i = 0; i < num_conn; ++i) {                                                                      
299       LinkRecord r;                                                                                           
300       int hport, hrank;                                                                                       
301       std::string hname;                                                                                      
302       tracker.RecvStr(&hname);                                                                                
303       Assert(tracker.RecvAll(&hport, sizeof(hport)) == sizeof(hport),                                         
304              "ReConnectLink failure 9");                                                                      
305       Assert(tracker.RecvAll(&hrank, sizeof(hrank)) == sizeof(hrank),                                         
306              "ReConnectLink failure 10");                                                                     
307       r.sock.Create();                                                                                        
308       if (!r.sock.Connect(utils::SockAddr(hname.c_str(), hport))) {                                           
309         num_error += 1; r.sock.Close(); continue;                                                             
310       }                                                                                                       
311       Assert(r.sock.SendAll(&rank, sizeof(rank)) == sizeof(rank),                                             
312              "ReConnectLink failure 12");                                                                     
313       Assert(r.sock.RecvAll(&r.rank, sizeof(r.rank)) == sizeof(r.rank),                                       
314              "ReConnectLink failure 13");                                                                     
315       utils::Check(hrank == r.rank,                                                                           
316                    "ReConnectLink failure, link rank inconsistent");                                          
317       bool match = false;                                                                                     
318       for (size_t i = 0; i < all_links.size(); ++i) {                                                         
319         if (all_links[i].rank == hrank) {                                                                     
320           Assert(all_links[i].sock.IsClosed(),                                                                
321                  "Override a link that is active");                                                           
322           all_links[i].sock = r.sock; match = true; break;                                                    
323         }                                                                                                     
324       }                                                                                                       
325       if (!match) all_links.push_back(r);                                                                     
326     } 
327     Assert(tracker.SendAll(&num_error, sizeof(num_error)) == sizeof(num_error),                               
328            "ReConnectLink failure 14");                                                                       
329   } while (num_error != 0);                                                                                   
330   // send back socket listening port to tracker                                                               
331   Assert(tracker.SendAll(&port, sizeof(port)) == sizeof(port),                                                
332          "ReConnectLink failure 14");                                                                         
333   // close connection to tracker                                                                              
334   tracker.Close();                                                                                            
335   // listen to incoming links                                                                                 
336   for (int i = 0; i < num_accept; ++i) {                                                                      
337     LinkRecord r;                                                                                             
338     r.sock = sock_listen.Accept();                                                                            
339     Assert(r.sock.SendAll(&rank, sizeof(rank)) == sizeof(rank),                                               
340            "ReConnectLink failure 15");                                                                       
341     Assert(r.sock.RecvAll(&r.rank, sizeof(r.rank)) == sizeof(r.rank),                                         
342            "ReConnectLink failure 15");                                                                       
343     bool match = false;                                                                                       
344     for (size_t i = 0; i < all_links.size(); ++i) {                                                           
345       if (all_links[i].rank == r.rank) {                                                                      
346         utils::Assert(all_links[i].sock.IsClosed(),                                                           
347                       "Override a link that is active");                                                      
348         all_links[i].sock = r.sock; match = true; break;                                                      
349       }                                                                                                       
350     }                                                                                                         
351     if (!match) all_links.push_back(r);                                                                       
352   }                                                                                                           
353   // close listening sockets                                                                                  
354   sock_listen.Close();                                                                                        
355   this->parent_index = -1;                                                                                    
356   // setup tree links and ring structure                                                                      
357   tree_links.plinks.clear(); 
358   for (size_t i = 0; i < all_links.size(); ++i) {                                                             
359     utils::Assert(!all_links[i].sock.BadSocket(), "ReConnectLink: bad socket");                               
360     // set the socket to non-blocking mode, enable TCP keepalive                                              
361     all_links[i].sock.SetNonBlock(true);                                                                      
362     all_links[i].sock.SetKeepAlive(true);                                                                     
363     if (tree_neighbors.count(all_links[i].rank) != 0) {                                                       
364       if (all_links[i].rank == parent_rank) {                                                                 
365         parent_index = static_cast<int>(tree_links.plinks.size());                                            
366       }                                                                                                       
367       tree_links.plinks.push_back(&all_links[i]);                                                             
368     }                                                                                                         
369     if (all_links[i].rank == prev_rank) ring_prev = &all_links[i];                                            
370     if (all_links[i].rank == next_rank) ring_next = &all_links[i];                                            
371   }                                                                                                           
372   Assert(parent_rank == -1 || parent_index != -1,                                                             
373          "cannot find parent in the link");                                                                   
374   Assert(prev_rank == -1 || ring_prev != NULL,                                                                
375          "cannot find prev ring in the link");                                                                
376   Assert(next_rank == -1 || ring_next != NULL,                                                                
377          "cannot find next ring in the link");                                                                
378 }
```