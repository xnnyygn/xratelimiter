# xratelimiter

A prototype of distribtued rate limiter

## Introduction

It's easy to apply rate limiter in single-server-based service. But when your service grows up,
serveral servers are used, it becomes difficult.

Available approaches to handle such situtation

* redis + lua, centralized
* 1/n, even but you cannot add or remove servers

An ideal distributed approach should be

* not centralized
* able to add or remove servers

`xratelimiter` provides a prototype of such solution, allowing dynamic weight evluation and adjustment of limiters.

## Demostration

Global limiter(token bucket)

capacity: 60, refill amount: 60, refill time, 1000ms, initial tokens: 0

* limiter 1, weight 1/3
* limiter 2, weight 1/3
* limiter 3, weight 1/3

```
$ mvn exec:java -Dexec.mainClass="in.xnnyygn.xratelimiter.StaticMemberDistributedTokenBucketRateLimiterLauncher" -Dexec.args=5302
```

```
$ mvn exec:java -Dexec.mainClass="in.xnnyygn.xratelimiter.StaticMemberDistributedTokenBucketRateLimiterLauncher" -Dexec.args=5303
```

```
$ mvn exec:java -Dexec.mainClass="in.xnnyygn.xratelimiter.StaticMemberDistributedTokenBucketRateLimiterLauncher" -Dexec.args=5304
```

to simulate clients

```
$ mvn exec:java -Dexec.mainClass="in.xnnyygn.xratelimiter.DistributedTokenBucketRateLimiterClient" -Dexec.args=6302
```

```
$ mvn exec:java -Dexec.mainClass="in.xnnyygn.xratelimiter.DistributedTokenBucketRateLimiterClient" -Dexec.args=6303
```

```
$ mvn exec:java -Dexec.mainClass="in.xnnyygn.xratelimiter.DistributedTokenBucketRateLimiterClient" -Dexec.args=6304
```

When you change the rate of clients, you can see the change of limiters.

## Build

xratelimiter uses [maven|https://maven.apache.org/] as the build system.

```
$ mvn clean compile install
```
## License

This project is licensed under the Apache 2.0 License.