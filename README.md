## Introduction
Virtual threads are part of Project Loom, they were introduced in Java 19 as preview and in Java 21 TODO.

Generally speaking virtual threads add a new level of abstraction on top of existing platform threads and therefore bring new risks and challenges for your code. In this article we look into some aspects of virtual threads work and learn troubleshooting techniques that can be used. 

## ForkJoinPool
As we know from the introduction - virtual threads use carrier threads from `ForkJoinPool` under the hood. Let's look at some of its implementation details.

**All carrier threads for virtual threads are actually daemon threads.**

Definition:
> A daemon thread is a thread that does not prevent the JVM from exiting when the program finishes but the thread is still running. 

This means that if there are only virtual threads are running - the process will exit. We can demonstrate this on a simple example:
```java

```
If we run the code above - it will exit immediately (probably print couple "Hello!" before exiting), even though virtual thread we've started has an infinite loop inside. This is good to remember, especially while experimenting with small virtual threads examples like the one above.

**Default ForkJoinPool can be configured, but cannot be replaced with another thread pool.**   

There are several JVM options we use to configure the `ForkJoinPool` used for carriers threads:

- jdk.virtualThreadScheduler.parallelism - The number of platform threads available for scheduling virtual threads. It defaults to the number of available processors.
- jdk.virtualThreadScheduler.maxPoolSize - The maximum number of platform threads available to the scheduler. It defaults to 256.             
- jdk.virtualThreadScheduler.minRunnable - The minimum allowed number of core threads not blocked by a join or `ForkJoinPool.ManagedBlocker` in `ForkJoinPool`. It defaults to max(1, parallelism / 2).    

## Stacktrace
Let's look into virtual threads stack trace first. Based on [the virtual threads JEP](https://openjdk.org/jeps/425) one of the goals of Loom project is:

> Enable easy troubleshooting, debugging, and profiling of virtual threads with existing JDK tools.

I.e. we're expecting the correct representation of the thread stacktrace. Let's check some examples example:

```java

```
If we run the code above - it will print the valid stack trace, that includes each method invoked in the virtual thread:
```
```

The same result will be if an exception stacktrace is printed in the virtual thread, fun fact - Java uses an exception stacktrace underhood to print the thread stack trace:
```java
```  

Things became more complicated if we're dealing with threads in waiting state. As we know, virtual threads use carrier native threads from the `ForkJoinPool` and a virtual thread is unmounted from its carrier thread on TODO. We can compare two examples:
```java
```  

```java
```  

Examples above are similar: both have invocations of methods `a`, `b` and `c` in the separate thread and then this thread is suspended with `Thread.sleep` invocation. The difference is that in the first example we use virtual thread while in the second - the standard native thread. If we compare thread dumps for these two examples - we will see the difference:
```
```

```
```
The stacktrace for the native threads looks clear and again contains invocations of methods `a`, `b` and `c`, but for the virtual thread - we can see only `ForkJoinPool` thread waiting. This can be easily explained: native thread is suspended which means that it is unmounted from its carrier native thread, so we cannot see any traces of our virtual thread in the thread dump. The same difference we will see if we compare flame graphs for total time: only flame graph for the native thread example will contain methods a, b and c. 

To improve visibility for virtual threads JVM creators introduced a new type of thread dump. From [the virtual threads JEP](https://openjdk.org/jeps/425):

> The new thread dump format lists virtual threads that are blocked in network I/O operations, and virtual threads that are created by the new-thread-per-task ExecutorService shown above. It does not include object addresses, locks, JNI statistics, heap statistics, and other information that appears in traditional thread dumps.

We can generate a new thread dump using the following command:
```shell
 jcmd <pid> Thread.dump_to_file -format=json <file>
```
To get the PID for our JVM application, we can use the jps command:
```shell
jps -l
```

If we check the new format thread dump we will see our stacktrace in JSON form:
```json
```

## Memory consumption
We know that virtual threads are lithweigt, let's see what this means in terms of memory consumption. To do so we can compare two examples:
```java
```

```java
```  
Our examples are simple: we create 1000 threads and then wait for 60 seconds before the exit (we will use this time to check allocated memory). In the first example we create native threads, while in the second - virtual threads. 
   
Since memory allocated for threads is not part of the heap - we cannot see it on a heap dump, instead we need to enable Native Memory Tracking, this can be done by using JVM flag `-XX:NativeMemoryTracking=summary`, see [Native Memory Tracking in JVM article](https://www.baeldung.com/native-memory-tracking-in-jvm) for more details. After Native Memory Tracking is enabled - we can get the native memory information using the `jcmd` command:
```shell
jcmd <pid> VM.native_memory
```

The commands output will contain information on different parts of our process memory like Metaspace, GC etc., but we are interested in the Thread section. Let's compare an output for the native threads:
```
Thread (reserved=2102390KB, committed=2102390KB)
    (thread #1019)
    (stack: reserved=2099140KB, committed=2099140KB)
    (malloc=2057KB #6181)
    (arena=1193KB #2037)
```
and for virtual threads:
```
 Thread (reserved=57764KB, committed=57764KB)
     (thread #28)
     (stack: reserved=57680KB, committed=57680KB)
     (malloc=52KB #173)
     (arena=32KB #55)
```
As we can see 1019 threads were created for the native threads example: 1000 requested by us plus some service threads, like GC thread, and around 2099 MB were allocated for the stack traces. This seems to be accurate, if we check the default options of our JVM using command `java -XX:+PrintFlagsFinal -version` we will get `ThreadStackSize                          = 2048`, i.e. approximately 2 MB per one native thread. 
**Note: ** `ThreadStackSize` can be adjusted using flag `-Xss`, for example `-Xss1m` will set `ThreadStackSize = 1 MB` for the process.

In the second example we have only 28 threads created and hence much less memory allocated, these threads include `ForkJoinPool` threads and again some service threads.

To demonstrate the difference in the memory consumption without any special tools - we can just increment threads count in each example and try to create 1 million threads - this will work fine for virtual threads, but will cause `OutOfMemoryError` for native threads:
```
Exception in thread "main" java.lang.OutOfMemoryError: unable to create native thread: possibly out of memory or process/resource limits reached
    at java.base/java.lang.Thread.start0(Native Method)
```  
Can we conclude now that virtual threads are superior to native threads in term of memory consumption? Not so fast: virtual threads still need to store their stacktraces  somewhere and they will store them in the JVM heap. With virtual threads we are getting "pay as you go" model, i.e. memory for the stacktrace will be allocated on the go, not at the thread creation time. Let's run the following example to demonstrate this:
```java
```

The example above creates 1000 virtual threads and then starts an endless recursion in each of the threads to emulate a growing stacktrace. If we profile this code and check "own allocation size" per method, we will see that the main cause of memory allocation is `jdk.internal.vm.StackChunk` class that stores information about the virtual thread stacktrace:    

![Image description](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/mqo5t1242yk317eas0c9.png)

As we can see, virtual threads can still consume quite big chunks of memory, moreover they will allocate memory in the heap instead of using a separate space, which can create additional pressure on GC and affect overall performance of the application.   

## CPU consumption
The main rule of thumb for virtual threads and CPU is virtual threads shouldn't be used for compute-intensive tasks. The reason is simple: virtual threads work well for cases with waiting threads, because they allow to release underlining platform threads. In case of CPU bound tasks we are loosing this advantage, since there is no waiting time for a thread. 

Additionally CPU can't be distributed fairly among virtual threads. Let's consider the following examples to prove this statement: 
```java
```    

In this example we start 100 threads and run a cpu intensive task in each of them. For each thread we measure duration between task being submitted and task being finished. We can use a platform threads executor or a virtual threads executor to see the difference:   
```java
var executor = Executors.newThreadPerTaskExecutor(Thread.ofPlatform().factory());
// or 
var executor = Executors.newVirtualThreadPerTaskExecutor();
```
The output example for native threads:
```
Minimal duration: 2175
Maximal duration: 3080
Average duration: 2936.26
```
The output example for virtual threads:
```
Minimal duration: 337
Maximal duration: 2634
Average duration: 1525.2
```
As we can see, for virtual threads we have much bigger gap between minimal and maximal task duration. We can easily understand why if we think about it: when we start 100 native threads - CPU is distributed between them fairly by the underlining OS scheduler, but when we start 100 virtual threads - first N of them will be executed on the carrier threads (where N is `jdk.virtualThreadScheduler.parallelism`), and the rest will be waiting until free carrier threads are available. Now imagine the same behaviour in a more complex application, where virtual threads are started in completely different classes - we can easily get performance problems. This example shows that we need to be careful when decide what logic to run on virtual threads and avoid using virtual threads for long running CPU intensive tasks.

The example above is based on the [Game of Loom 2: life and dead(lock) of a virtual thread](https://www.youtube.com/watch?v=6-6diIzOzJc) presentation by Mario Fusco - check it for other facts about virtual threads. 

## Pinned threads detection
One of the important limitations of virtual threads is pinning. From the [Virtual Threads JEP](https://openjdk.org/jeps/425):

> There are two scenarios in which a virtual thread cannot be unmounted during blocking operations because it is pinned to its carrier:
1. When it executes code inside a synchronized block or method, or
2. When it executes a native method or a foreign function.
Pinning does not make an application incorrect, but it might hinder its scalability. If a virtual thread performs a blocking operation such as I/O or BlockingQueue.take() while it is pinned, then its carrier and the underlying OS thread are blocked for the duration of the operation. Frequent pinning for long durations can harm the scalability of an application by capturing carriers.

From the definition above, we can see that if thread is pinned - we are losing the virtual threads advantage since underlining platform thread is not released anymore in this scenario. The general recommendation to avoid pinning is to replace `synchronized` blocks with Java `ReentrantLock` class usage.  
Unfortunately, finding places where virtual thread can be pinned is tricky since we need to check not only our own code, but also underlining libraries, especially their low level methods. To help with such analysis - JVM provides special option that shows pinned threads cases: `-Djdk.tracePinnedThreads=full/short`. Let's demonstrate how it works on the following example:
```java
```    

If we run this example with `-Djdk.tracePinnedThreads=short`, we will see output like this:
```
```
With `-Djdk.tracePinnedThreads=full` option we will see the whole stack trace:
```
```
## Deadlock detection
We've already checked stacktrace options for the virtual threads, let's look into connected topic - deadlock detection. To demonstrate the case let's run an example causing a deadlock:
```java
``` 

We are running a classical deadlock example with two threads trying to acquire two locks (in our case we use `ReentrantLock`) in different order. When the code is stuck, we can try to identify a deadlock using available tools. Reminder: for the native threads deadlock could be easily detected and we would get something like this in the thread dump output:
```
Found one Java-level deadlock:
=============================
"Thread-0":
  waiting for ownable synchronizer 0x000000070fd1d908, (a java.util.concurrent.locks.ReentrantLock$NonfairSync),
  which is held by "Thread-1"

"Thread-1":
  waiting for ownable synchronizer 0x000000070fd1d8d8, (a java.util.concurrent.locks.ReentrantLock$NonfairSync),
  which is held by "Thread-0"
```
If we trigger a thread dump for the virtual threads scenario, we will see only waiting platform threads from the `ForkJoinPool`:
```
```
In the new thread dump format, our blocked virtual threads and their stacktarces will be visible, but there is no way to identify a deadlock clearly since no locks are shown in this format:
```
```  

## Conclusions
Virtual threads is a powerful tool, that can help us to simplify the codebase and use available resources more efficiently. At the same time it is important to remember  their implementation details and limitations:

1. Virtual threads should not be used for CPU intensive tasks, instead they should be used for tasks that involve waiting.
2. The stacks of virtual threads are stored in the JVM heap, this 
3. Virtual threads unmounted from the carrier threads are not visible in the standard thread dumps, this make possible issues and deadlocks investigations more difficult 
  
