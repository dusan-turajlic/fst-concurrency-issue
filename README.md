# fst-concurrency-issue

Demonstrates a problem in the [fst](https://github.com/RuedigerMoeller/fast-serialization/) library.

## Details & background

[#235](https://github.com/RuedigerMoeller/fast-serialization/issues/235) describes an issue related to how the `FSTConfiguration` class in fst is not thread safe.<br>
[#270](https://github.com/RuedigerMoeller/fast-serialization/issues/270) describes another issue with a user experiencing `SIGSEGV` errors in the Java process.

We were seeing the latter in our application while put under load, which led us to start investigating this further. Here are the results of this investigation. This repository illustrates how to reproduce our current bug, which is only displayed when using one of the following:

- Java 11 (without changing GC settings; uses G1GC by default)
- Java 8, with G1GC enabled: `-XX:+UseG1GC`

This repro project does not cause the JVM to crash, but we feel confident enough anyway that our crashes have the same root cause as these (managed) exceptions. When excluding FST from our application, using another serialization library, our SIGSEGV errors go away completely.

## How to run

`$ ./gradlew test` (or load the project in IntelliJ IDEA/some other Java IDE, and use its test runner).

When run multiple times, the problem is exhibited. From my experience, when running it 10 times it will fail about 3-4 times with fst 2.57.

(If running on the command line, the exceptions will be logged to an HTML file; normally, `build/reports/tests/test/index.html` - its location will be printed by Gradle. Full exception details are not printed to stdout.)

I also made it easy to test this with older versions of fst - see [build.gradle](build.gradle) for more details. From what I can tell, the bug seems to be present on all 2.xx versions of fst.

## Disabling parallelization

I experimented with setting `NUMBER_OF_THREADS` to 1, i.e. minimize parallelism, since we were thinking that the root cause here might not be strictly concurrency-related but rather triggered by unexpected GC behavior (= fst semantics only working reliably with ParallelGC, the default GC in Java 8).

Interestingly enough, I could not reproduce the errors this way, which leads to the conclusion that the error is likely to be a concurrency bug after all.

## Example exceptions

Provided for convenience only; these were the exceptions I noted when running the tests now about 10-15 times.

### java.io.IOException: Failed to read the next byte

```
java.lang.RuntimeException: java.util.concurrent.ExecutionException: java.lang.RuntimeException: java.io.IOException: java.io.IOException: java.io.IOException: Failed to read the next byte
	at FSTSerializationTest.tryAndReProduceBug(FSTSerializationTest.java:68)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.testng.internal.MethodInvocationHelper.invokeMethod(MethodInvocationHelper.java:108)
	at org.testng.internal.Invoker.invokeMethod(Invoker.java:661)
	at org.testng.internal.Invoker.invokeTestMethod(Invoker.java:869)
	at org.testng.internal.Invoker.invokeTestMethods(Invoker.java:1193)
	at org.testng.internal.TestMethodWorker.invokeTestMethods(TestMethodWorker.java:126)
	at org.testng.internal.TestMethodWorker.run(TestMethodWorker.java:109)
	at org.testng.TestRunner.privateRun(TestRunner.java:744)
	at org.testng.TestRunner.run(TestRunner.java:602)
	at org.testng.SuiteRunner.runTest(SuiteRunner.java:380)
	at org.testng.SuiteRunner.runSequentially(SuiteRunner.java:375)
	at org.testng.SuiteRunner.privateRun(SuiteRunner.java:340)
	at org.testng.SuiteRunner.run(SuiteRunner.java:289)
	at org.testng.SuiteRunnerWorker.runSuite(SuiteRunnerWorker.java:52)
	at org.testng.SuiteRunnerWorker.run(SuiteRunnerWorker.java:86)
	at org.testng.TestNG.runSuitesSequentially(TestNG.java:1301)
	at org.testng.TestNG.runSuitesLocally(TestNG.java:1226)
	at org.testng.TestNG.runSuites(TestNG.java:1144)
	at org.testng.TestNG.run(TestNG.java:1115)
	at org.gradle.api.internal.tasks.testing.testng.TestNGTestClassProcessor.runTests(TestNGTestClassProcessor.java:139)
	at org.gradle.api.internal.tasks.testing.testng.TestNGTestClassProcessor.stop(TestNGTestClassProcessor.java:89)
	at org.gradle.api.internal.tasks.testing.SuiteTestClassProcessor.stop(SuiteTestClassProcessor.java:61)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:35)
	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:24)
	at org.gradle.internal.dispatch.ContextClassLoaderDispatch.dispatch(ContextClassLoaderDispatch.java:32)
	at org.gradle.internal.dispatch.ProxyDispatchAdapter$DispatchingInvocationHandler.invoke(ProxyDispatchAdapter.java:93)
	at com.sun.proxy.$Proxy2.stop(Unknown Source)
	at org.gradle.api.internal.tasks.testing.worker.TestWorker.stop(TestWorker.java:131)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:35)
	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:24)
	at org.gradle.internal.remote.internal.hub.MessageHubBackedObjectConnection$DispatchWrapper.dispatch(MessageHubBackedObjectConnection.java:155)
	at org.gradle.internal.remote.internal.hub.MessageHubBackedObjectConnection$DispatchWrapper.dispatch(MessageHubBackedObjectConnection.java:137)
	at org.gradle.internal.remote.internal.hub.MessageHub$Handler.run(MessageHub.java:404)
	at org.gradle.internal.concurrent.ExecutorPolicy$CatchAndRecordFailures.onExecute(ExecutorPolicy.java:63)
	at org.gradle.internal.concurrent.ManagedExecutorImpl$1.run(ManagedExecutorImpl.java:46)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	at org.gradle.internal.concurrent.ThreadFactoryImpl$ManagedThreadRunnable.run(ThreadFactoryImpl.java:55)
	at java.lang.Thread.run(Thread.java:748)
Caused by: java.util.concurrent.ExecutionException: java.lang.RuntimeException: java.io.IOException: java.io.IOException: java.io.IOException: Failed to read the next byte
	at java.util.concurrent.FutureTask.report(FutureTask.java:122)
	at java.util.concurrent.FutureTask.get(FutureTask.java:192)
	at FSTSerializationTest.tryAndReProduceBug(FSTSerializationTest.java:64)
	... 50 more
Caused by: java.lang.RuntimeException: java.io.IOException: java.io.IOException: java.io.IOException: Failed to read the next byte
	at FSTSerializationTest.lambda$tryAndReProduceBug$0(FSTSerializationTest.java:55)
	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)
	at java.util.concurrent.FutureTask.run(FutureTask.java:266)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	... 1 more
Caused by: java.io.IOException: java.io.IOException: java.io.IOException: Failed to read the next byte
	at FSTSerializationTest.readObject(FSTSerializationTest.java:78)
	at FSTSerializationTest.lambda$tryAndReProduceBug$0(FSTSerializationTest.java:51)
	... 5 more
Caused by: java.io.IOException: java.io.IOException: Failed to read the next byte
	at org.nustaq.serialization.FSTObjectInput.readObject(FSTObjectInput.java:247)
	at FSTSerializationTest.readObject(FSTSerializationTest.java:75)
	... 6 more
Caused by: java.io.IOException: Failed to read the next byte
	at org.nustaq.serialization.coders.FSTStreamDecoder.readFByte(FSTStreamDecoder.java:294)
	at org.nustaq.serialization.coders.FSTStreamDecoder.readObjectHeaderTag(FSTStreamDecoder.java:98)
	at org.nustaq.serialization.FSTObjectInput.readObjectWithHeader(FSTObjectInput.java:344)
	at org.nustaq.serialization.FSTObjectInput.readObjectFields(FSTObjectInput.java:713)
	at org.nustaq.serialization.FSTObjectInput.instantiateAndReadNoSer(FSTObjectInput.java:566)
	at org.nustaq.serialization.FSTObjectInput.readObjectWithHeader(FSTObjectInput.java:374)
	at org.nustaq.serialization.FSTObjectInput.readObjectInternal(FSTObjectInput.java:331)
	at org.nustaq.serialization.FSTObjectInput.readObject(FSTObjectInput.java:311)
	at org.nustaq.serialization.FSTObjectInput.readObject(FSTObjectInput.java:245)
	... 7 more
```

### java.lang.NullPointerException at FSTObjectInput.java:357

```
java.lang.RuntimeException: java.util.concurrent.ExecutionException: java.lang.RuntimeException: java.io.IOException: java.io.IOException: java.lang.NullPointerException
	at FSTSerializationTest.tryAndReProduceBug(FSTSerializationTest.java:68)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.testng.internal.MethodInvocationHelper.invokeMethod(MethodInvocationHelper.java:108)
	at org.testng.internal.Invoker.invokeMethod(Invoker.java:661)
	at org.testng.internal.Invoker.invokeTestMethod(Invoker.java:869)
	at org.testng.internal.Invoker.invokeTestMethods(Invoker.java:1193)
	at org.testng.internal.TestMethodWorker.invokeTestMethods(TestMethodWorker.java:126)
	at org.testng.internal.TestMethodWorker.run(TestMethodWorker.java:109)
	at org.testng.TestRunner.privateRun(TestRunner.java:744)
	at org.testng.TestRunner.run(TestRunner.java:602)
	at org.testng.SuiteRunner.runTest(SuiteRunner.java:380)
	at org.testng.SuiteRunner.runSequentially(SuiteRunner.java:375)
	at org.testng.SuiteRunner.privateRun(SuiteRunner.java:340)
	at org.testng.SuiteRunner.run(SuiteRunner.java:289)
	at org.testng.SuiteRunnerWorker.runSuite(SuiteRunnerWorker.java:52)
	at org.testng.SuiteRunnerWorker.run(SuiteRunnerWorker.java:86)
	at org.testng.TestNG.runSuitesSequentially(TestNG.java:1301)
	at org.testng.TestNG.runSuitesLocally(TestNG.java:1226)
	at org.testng.TestNG.runSuites(TestNG.java:1144)
	at org.testng.TestNG.run(TestNG.java:1115)
	at org.gradle.api.internal.tasks.testing.testng.TestNGTestClassProcessor.runTests(TestNGTestClassProcessor.java:139)
	at org.gradle.api.internal.tasks.testing.testng.TestNGTestClassProcessor.stop(TestNGTestClassProcessor.java:89)
	at org.gradle.api.internal.tasks.testing.SuiteTestClassProcessor.stop(SuiteTestClassProcessor.java:61)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:35)
	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:24)
	at org.gradle.internal.dispatch.ContextClassLoaderDispatch.dispatch(ContextClassLoaderDispatch.java:32)
	at org.gradle.internal.dispatch.ProxyDispatchAdapter$DispatchingInvocationHandler.invoke(ProxyDispatchAdapter.java:93)
	at com.sun.proxy.$Proxy2.stop(Unknown Source)
	at org.gradle.api.internal.tasks.testing.worker.TestWorker.stop(TestWorker.java:131)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:35)
	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:24)
	at org.gradle.internal.remote.internal.hub.MessageHubBackedObjectConnection$DispatchWrapper.dispatch(MessageHubBackedObjectConnection.java:155)
	at org.gradle.internal.remote.internal.hub.MessageHubBackedObjectConnection$DispatchWrapper.dispatch(MessageHubBackedObjectConnection.java:137)
	at org.gradle.internal.remote.internal.hub.MessageHub$Handler.run(MessageHub.java:404)
	at org.gradle.internal.concurrent.ExecutorPolicy$CatchAndRecordFailures.onExecute(ExecutorPolicy.java:63)
	at org.gradle.internal.concurrent.ManagedExecutorImpl$1.run(ManagedExecutorImpl.java:46)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	at org.gradle.internal.concurrent.ThreadFactoryImpl$ManagedThreadRunnable.run(ThreadFactoryImpl.java:55)
	at java.lang.Thread.run(Thread.java:748)
Caused by: java.util.concurrent.ExecutionException: java.lang.RuntimeException: java.io.IOException: java.io.IOException: java.lang.NullPointerException
	at java.util.concurrent.FutureTask.report(FutureTask.java:122)
	at java.util.concurrent.FutureTask.get(FutureTask.java:192)
	at FSTSerializationTest.tryAndReProduceBug(FSTSerializationTest.java:64)
	... 50 more
Caused by: java.lang.RuntimeException: java.io.IOException: java.io.IOException: java.lang.NullPointerException
	at FSTSerializationTest.lambda$tryAndReProduceBug$0(FSTSerializationTest.java:55)
	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)
	at java.util.concurrent.FutureTask.run(FutureTask.java:266)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	... 1 more
Caused by: java.io.IOException: java.io.IOException: java.lang.NullPointerException
	at FSTSerializationTest.readObject(FSTSerializationTest.java:78)
	at FSTSerializationTest.lambda$tryAndReProduceBug$0(FSTSerializationTest.java:51)
	... 5 more
Caused by: java.io.IOException: java.lang.NullPointerException
	at org.nustaq.serialization.FSTObjectInput.readObject(FSTObjectInput.java:247)
	at FSTSerializationTest.readObject(FSTSerializationTest.java:75)
	... 6 more
Caused by: java.lang.NullPointerException
	at org.nustaq.serialization.FSTObjectInput.readObjectWithHeader(FSTObjectInput.java:357)
	at org.nustaq.serialization.FSTObjectInput.readObjectFields(FSTObjectInput.java:713)
	at org.nustaq.serialization.FSTObjectInput.readObjectCompatibleRecursive(FSTObjectInput.java:635)
	at org.nustaq.serialization.FSTObjectInput.readObjectCompatible(FSTObjectInput.java:574)
	at org.nustaq.serialization.FSTObjectInput.instantiateAndReadNoSer(FSTObjectInput.java:559)
	at org.nustaq.serialization.FSTObjectInput.readObjectWithHeader(FSTObjectInput.java:374)
	at org.nustaq.serialization.FSTObjectInput.readObjectFields(FSTObjectInput.java:713)
	at org.nustaq.serialization.FSTObjectInput.instantiateAndReadNoSer(FSTObjectInput.java:566)
	at org.nustaq.serialization.FSTObjectInput.readObjectWithHeader(FSTObjectInput.java:374)
	at org.nustaq.serialization.FSTObjectInput.readObjectInternal(FSTObjectInput.java:331)
	at org.nustaq.serialization.FSTObjectInput.readObject(FSTObjectInput.java:311)
	at org.nustaq.serialization.FSTObjectInput.readObject(FSTObjectInput.java:245)
	... 7 more
```

### java.lang.RuntimeException: unknown object tag -19

```
java.lang.RuntimeException: java.util.concurrent.ExecutionException: java.lang.RuntimeException: java.io.IOException: java.io.IOException: java.lang.RuntimeException: unknown object tag -19
	at FSTSerializationTest.tryAndReProduceBug(FSTSerializationTest.java:68)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.testng.internal.MethodInvocationHelper.invokeMethod(MethodInvocationHelper.java:108)
	at org.testng.internal.Invoker.invokeMethod(Invoker.java:661)
	at org.testng.internal.Invoker.invokeTestMethod(Invoker.java:869)
	at org.testng.internal.Invoker.invokeTestMethods(Invoker.java:1193)
	at org.testng.internal.TestMethodWorker.invokeTestMethods(TestMethodWorker.java:126)
	at org.testng.internal.TestMethodWorker.run(TestMethodWorker.java:109)
	at org.testng.TestRunner.privateRun(TestRunner.java:744)
	at org.testng.TestRunner.run(TestRunner.java:602)
	at org.testng.SuiteRunner.runTest(SuiteRunner.java:380)
	at org.testng.SuiteRunner.runSequentially(SuiteRunner.java:375)
	at org.testng.SuiteRunner.privateRun(SuiteRunner.java:340)
	at org.testng.SuiteRunner.run(SuiteRunner.java:289)
	at org.testng.SuiteRunnerWorker.runSuite(SuiteRunnerWorker.java:52)
	at org.testng.SuiteRunnerWorker.run(SuiteRunnerWorker.java:86)
	at org.testng.TestNG.runSuitesSequentially(TestNG.java:1301)
	at org.testng.TestNG.runSuitesLocally(TestNG.java:1226)
	at org.testng.TestNG.runSuites(TestNG.java:1144)
	at org.testng.TestNG.run(TestNG.java:1115)
	at org.gradle.api.internal.tasks.testing.testng.TestNGTestClassProcessor.runTests(TestNGTestClassProcessor.java:139)
	at org.gradle.api.internal.tasks.testing.testng.TestNGTestClassProcessor.stop(TestNGTestClassProcessor.java:89)
	at org.gradle.api.internal.tasks.testing.SuiteTestClassProcessor.stop(SuiteTestClassProcessor.java:61)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:35)
	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:24)
	at org.gradle.internal.dispatch.ContextClassLoaderDispatch.dispatch(ContextClassLoaderDispatch.java:32)
	at org.gradle.internal.dispatch.ProxyDispatchAdapter$DispatchingInvocationHandler.invoke(ProxyDispatchAdapter.java:93)
	at com.sun.proxy.$Proxy2.stop(Unknown Source)
	at org.gradle.api.internal.tasks.testing.worker.TestWorker.stop(TestWorker.java:131)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:35)
	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:24)
	at org.gradle.internal.remote.internal.hub.MessageHubBackedObjectConnection$DispatchWrapper.dispatch(MessageHubBackedObjectConnection.java:155)
	at org.gradle.internal.remote.internal.hub.MessageHubBackedObjectConnection$DispatchWrapper.dispatch(MessageHubBackedObjectConnection.java:137)
	at org.gradle.internal.remote.internal.hub.MessageHub$Handler.run(MessageHub.java:404)
	at org.gradle.internal.concurrent.ExecutorPolicy$CatchAndRecordFailures.onExecute(ExecutorPolicy.java:63)
	at org.gradle.internal.concurrent.ManagedExecutorImpl$1.run(ManagedExecutorImpl.java:46)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	at org.gradle.internal.concurrent.ThreadFactoryImpl$ManagedThreadRunnable.run(ThreadFactoryImpl.java:55)
	at java.lang.Thread.run(Thread.java:748)
Caused by: java.util.concurrent.ExecutionException: java.lang.RuntimeException: java.io.IOException: java.io.IOException: java.lang.RuntimeException: unknown object tag -19
	at java.util.concurrent.FutureTask.report(FutureTask.java:122)
	at java.util.concurrent.FutureTask.get(FutureTask.java:192)
	at FSTSerializationTest.tryAndReProduceBug(FSTSerializationTest.java:64)
	... 50 more
Caused by: java.lang.RuntimeException: java.io.IOException: java.io.IOException: java.lang.RuntimeException: unknown object tag -19
	at FSTSerializationTest.lambda$tryAndReProduceBug$0(FSTSerializationTest.java:55)
	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)
	at java.util.concurrent.FutureTask.run(FutureTask.java:266)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	... 1 more
Caused by: java.io.IOException: java.io.IOException: java.lang.RuntimeException: unknown object tag -19
	at FSTSerializationTest.readObject(FSTSerializationTest.java:78)
	at FSTSerializationTest.lambda$tryAndReProduceBug$0(FSTSerializationTest.java:51)
	... 5 more
Caused by: java.io.IOException: java.lang.RuntimeException: unknown object tag -19
	at org.nustaq.serialization.FSTObjectInput.readObject(FSTObjectInput.java:247)
	at FSTSerializationTest.readObject(FSTSerializationTest.java:75)
	... 6 more
Caused by: java.lang.RuntimeException: unknown object tag -19
	at org.nustaq.serialization.FSTObjectInput.instantiateSpecialTag(FSTObjectInput.java:434)
	at org.nustaq.serialization.FSTObjectInput.readObjectWithHeader(FSTObjectInput.java:364)
	at org.nustaq.serialization.FSTObjectInput.readObjectFields(FSTObjectInput.java:713)
	at org.nustaq.serialization.FSTObjectInput.instantiateAndReadNoSer(FSTObjectInput.java:566)
	at org.nustaq.serialization.FSTObjectInput.readObjectWithHeader(FSTObjectInput.java:374)
	at org.nustaq.serialization.FSTObjectInput.readObjectInternal(FSTObjectInput.java:331)
	at org.nustaq.serialization.FSTObjectInput.readObject(FSTObjectInput.java:311)
	at org.nustaq.serialization.FSTObjectInput.readObject(FSTObjectInput.java:245)
	... 7 more
```

