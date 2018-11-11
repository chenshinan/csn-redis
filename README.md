# redis学习

## 基础

* redis是线程安全的`（因为只有一个线程）`，采用`非阻塞I/O多路复用`，其所有操作都是原子的，不会因并发产生数据异常

* Redis的速度非常快`（因为使用非阻塞式IO，且大部分命令的算法时间复杂度都是O(1))`

* 使用高耗时的Redis命令是很危险的，会占用唯一的一个线程的大量处理时间，导致所有的请求都被拖慢。`（例如时间复杂度为O(N)的KEYS命令，严格禁止在生产环境中使用）`

* String是Redis的基础数据类型，Redis没有Int、Float、Boolean等数据类型的概念，所有的基本类型在Redis中都以String体现

* Redis的List是链表型的数据结构，可以使用LPUSH/RPUSH/LPOP/RPOP等命令在List的两端执行插入元素和弹出元素的操作。虽然List也支持在特定index上插入和读取元素的功能，但其时间复杂度较高（O(N)），应小心使用

* Hash即哈希表，Redis的Hash和传统的哈希表一样，是一种field-value型的数据结构，可以理解成将HashMap搬入Redis

Hash的优点包括：

        1、可以实现二元查找，如"查找ID为1000的用户的年龄"
        2、比起将整个对象序列化后作为String存储的方法，Hash能够有效地减少网络传输的消耗
        3、当使用Hash维护一个集合时，提供了比List效率高得多的随机访问命令

## 持久化方式

### RDB

建议至少开启RDB方式的数据持久化：RDB方式的持久化几乎不损耗Redis本身的性能，在进行RDB持久化时，Redis主进程唯一需要做的事情就是fork出一个子进程，所有持久化工作都由子进程完成

Redis默认开启RDB快照：

```conf
save 900 1
save 300 10
save 60 10000
```

### AOF

采用AOF持久方式时，Redis会把每一个写请求都记录在一个日志文件里。在Redis重启时，会把AOF文件中记录的所有写操作顺序执行一遍，确保数据恢复到最新

AOF提供了三种fsync配置，always/everysec/no，通过配置项[appendfsync]指定：

        appendfsync no：不进行fsync，将flush文件的时机交给OS决定，速度最快
        appendfsync always：每写入一条日志就进行一次fsync操作，数据安全性最高，但速度最慢
        appendfsync everysec：折中的做法，交由后台线程每秒fsync一次

## 数据淘汰机制

Redis提供了5种数据淘汰策略：

        volatile-lru：使用LRU算法进行数据淘汰（淘汰上次使用时间最早的，且使用次数最少的key），只淘汰设定了有效期的key
        allkeys-lru：使用LRU算法进行数据淘汰，所有的key都可以被淘汰
        volatile-random：随机淘汰数据，只淘汰设定了有效期的key
        allkeys-random：随机淘汰数据，所有的key都可以被淘汰
        volatile-ttl：淘汰剩余有效期最短的key

> 最好为Redis指定一种有效的数据淘汰策略以配合maxmemory设置，避免在内存使用满后发生写入失败的情况

## Pipelining

Redis提供许多批量操作的命令，如MSET/MGET/HMSET/HMGET等等，这些命令存在的意义是减少维护网络连接和传输数据所消耗的资源和时间

如果客户端要连续执行的多次操作无法通过Redis命令组合在一起，此时便可以使用Redis提供的pipelining功能来实现在一次交互中执行多条命令

## Redis性能调优

        1、最初的也是最重要的，确保没有让Redis执行耗时长的命令
        2、使用pipelining将连续执行的命令组合执行
        3、操作系统的Transparent huge pages功能必须关闭
        4、如果在虚拟机中运行Redis，可能天然就有虚拟机环境带来的固有延迟。可以通过./redis-cli --intrinsic-latency 100命令查看固有延迟。同时如果对Redis的性能有较高要求的话，应尽可能在物理机上直接部署Redis。
        6、检查数据持久化策略
        7、考虑引入读写分离机制

## 主从复制

Redis支持一主多从的主从复制架构。一个Master实例负责处理所有的写请求，Master将写操作同步至所有Slave。
借助Redis的主从复制，可以实现读写分离和高可用：

实时性要求不是特别高的读请求，可以在Slave上完成，提升效率。特别是一些周期性执行的统计任务，这些任务可能需要执行一些长耗时的Redis命令，可以专门规划出1个或几个Slave用于服务这些统计任务

> 借助Redis Sentinel可以实现高可用，当Master crash后，Redis Sentinel能够自动将一个Slave晋升为Master，继续提供服务

## Redis Sentinel 哨兵模式

Redis Sentinel是Redis官方开发的监控组件，可以监控Redis实例的状态，通过Master节点自动发现Slave节点，并在监测到Master节点失效时选举出一个新的Master，并向所有Redis实例推送新的主从配置。

Redis Sentinel需要至少部署3个实例才能形成选举关系

## Redis Cluster 集群分片

> 为何要做集群分片？

        1、Redis中存储的数据量大，一台主机的物理内存已经无法容纳
        2、Redis的写请求并发量大，一个Redis实例以无法承载

### Redis Cluster的能力

        1、能够自动将数据分散在多个节点上
        2、当访问的key不在当前分片上时，能够自动将请求转发至正确的分片
        3、当集群中部分节点失效时仍能提供服务

其中第三点是基于主从复制来实现的，Redis Cluster的每个数据分片都采用了主从复制的结构

### Redis Cluster分片原理

Redis Cluster中共有16384个hash slot，Redis会计算每个key的CRC16，将结果与16384取模，来决定该key存储在哪一个hash slot中，同时需要指定Redis Cluster中每个数据分片负责的Slot数。Slot的分配在任何时间点都可以进行重新分配。

客户端在对key进行读写操作时，可以连接Cluster中的任意一个分片，如果操作的key不在此分片负责的Slot范围内，Redis Cluster会自动将请求重定向到正确的分片上

## Redis常见问题

> 单线程的redis为什么这么快?

        1、纯内存操作
        2、单线程操作，避免了频繁的上下文切换
        3、采用了非阻塞I/O多路复用机制

## Redis安装

### 通过homebrew

```命令行
brew install redis //下载安装
brew services start redis //运行方式1：后台启动
redis-server /usr/local/etc/redis.conf //运行方式2：根据配置启动
redis-cli -h 127.0.0.1 -p 6379 //进入控制台
brew services stop redis //退出redis
```

### 通过官方下载源码包

```命令行
wget http://download.redis.io/releases/redis-5.0.0.tar.gz
tar zxvf redis-5.0.0.tar.gz
cd redis-5.0.0
make //编译
redis-cli -c -p 7000 shutdown //退出redis
```

> 包下的README.md有使用教程

```控制台命令
set key value
get key
shutdown //关闭停止
quit //退出客户端
```

### redis.conf配置

```conf
daemonize : 是否后台运行，将其设为no，表示前台运行。
port :redis服务监听的端口。
logfile : 指定日志文件路径。
appendonly : 是否开启appendonlylog，开启的话每次写操作会记一条log，这会提高数据抗风险能力，但影响效率。
cluster-node-timeout : 集群结点超时限制
```

## Redis Cluster集群部署

每个节点对应一个redis.conf和node.conf（自动生成，命名不同）

* 最小的Redis群集配置文件

```redis7000.conf
port 7000
cluster-enabled yes
cluster-config-file node-7000.conf
cluster-node-timeout 5000
appendonly yes
```

* 启动redis

```ml
redis-server ./redis7000.conf
redis-server ./redis7001.conf
redis-server ./redis7002.conf
redis-server ./redis7003.conf
redis-server ./redis7004.conf
redis-server ./redis7005.conf
```

* 将redis节点关联成集群

```ml
redis-cli --cluster create 127.0.0.1:7000 127.0.0.1:7001 \
127.0.0.1:7002 127.0.0.1:7003 127.0.0.1:7004 127.0.0.1:7005 \
--cluster-replicas 1
```

输入yes后，此时搭建集群成功

> 另外可以使用如下命令快速搭建集群

```ml
create-cluster start
create-cluster create
```

## spring boot整合redis

### 引入pom

```pom
<dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### application.yml配置

```yml
server:
  port: 8011
spring:
  redis:
    host: 127.0.0.1
    port: 6679
```

### redis配置文件

```conf
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        initDomainRedisTemplate(redisTemplate, redisConnectionFactory);
        return redisTemplate;
    }
    private void initDomainRedisTemplate(RedisTemplate<String, Object> redisTemplate, RedisConnectionFactory factory) {
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setConnectionFactory(factory);
    }
    @Bean
    public ValueOperations<String, Object> valueOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForValue();
    }
}
```

### RedisUtil

```util
@Component
public class RedisUtil {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private ValueOperations<String, Object> valueOperations;
    /**
     * 查询key,支持模糊查询
     */
    public Set<String> keys(String key) {
        return redisTemplate.keys(key);
    }
    /**
     * 字符串添加信息
     *
     * @param key     key
     * @param obj     可以是单个的值，也可以是任意类型的对象
     * @param timeout 过期时间，单位秒
     */
    public void set(String key, Object obj, Long timeout) {
        valueOperations.set(key, obj, timeout, TimeUnit.SECONDS);
    }
    /**
     * 字符串添加信息
     *
     * @param key key
     * @param obj 可以是单个的值，也可以是任意类型的对象
     */
    public void set(String key, Object obj) {
        valueOperations.set(key, obj);
    }
}
```

> SpringBoot中可以基础redis作为缓存

        配置：spring.cache.type:redis
        启动缓存：@EnableCaching
        实现：CachingConfigurerSupport
        最后使用注解：@Cacheable注解在方法上，该方法的返回结果既被缓存

参考文献：\
I/O多路复用：https://www.jianshu.com/p/db5da880154a \
官方redis集群教程：https://redis.io/topics/cluster-tutorial \
Redis系列九（redis集群高可用）：https://www.cnblogs.com/leeSmall/p/8414687.html \
docker redis 集群（cluster）搭建：https://my.oschina.net/dslcode/blog/1936656 \
springboot整合redis周围缓存：https://www.cnblogs.com/hlhdidi/p/7928074.html \
Spring Data Redis官方教程：https://docs.spring.io/spring-data/data-redis/docs/current/reference/html/#redis:connectors \
Spring Boot使用Spring Data Redis操作Redis（单机/集群）：https://www.cnblogs.com/EasonJim/p/7805665.html \
【以下是复习Redis面试原理相关】
Redis基础、高级特性与性能调优：https://www.jianshu.com/p/2f14bc570563 \
Redis 主从复制 原理与用法：https://blog.csdn.net/Stubborn_Cow/article/details/50442950 \
redis复习精讲【推荐】：https://www.cnblogs.com/rjzheng/p/9096228.htm \
面试中关于Redis的问题看这篇就够了：https://mp.weixin.qq.com/s?__biz=MzU4NDQ4MzU5OA==&mid=2247483867&idx=1&sn=39a06fa3d6d8f09eefaaf3d2b15b40e4&chksm=fd9857bacaefdeaccd7cacf9dba5b702bf6f639377ded5a29fc1e56ae4f1d0a121ad0829c9dc&scene=21#wechat_redirect \
一文轻松搞懂redis集群原理及搭建与使用：https://mp.weixin.qq.com/s?__biz=MzU4NDQ4MzU5OA==&mid=2247483863&idx=1&sn=8a7d08783f45d3af7947b8a2e7cc981e&chksm=fd9857b6caefdea072a7cec992fa1d32316ffdca8eea24e7f5a7871ce189bdd4e5b144619ae8&scene=21#wechat_redirect \