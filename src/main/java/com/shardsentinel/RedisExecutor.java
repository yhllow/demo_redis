package com.shardsentinel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.ShardedJedis;

public abstract class RedisExecutor<T> {

    private static ShardedJedisSentinelPool redisPool = null;
    private static String appName = null;

    public RedisExecutor() {
        if (redisPool == null) {
            poolInit();
        }
    }

    /**
     * 在多线程环境同步初始化
     */
    private static synchronized void poolInit() {
        if (redisPool == null) {
            initialPool();
        }
    }

    /**
     * 初始化RedisPool
     */
    private static void initialPool() {
        Properties propertie = PropertiesLoader.getPropertie();
        if (propertie == null)
            throw new RuntimeException("load redis properties failed!");
        String servers = propertie.getProperty("redis.servers");
        String masters = propertie.getProperty("redis.masters");
        String password = propertie.getProperty("redis.password");
        int database = Integer.parseInt(propertie.getProperty("redis.database", "0"));
        int maxTotal = Integer.parseInt(propertie.getProperty("redis.maxTotal", "512"));
        int maxIdle = Integer.parseInt(propertie.getProperty("redis.maxIdle", "512"));
        int minIdle = Integer.parseInt(propertie.getProperty("redis.minIdle", "8"));
        int timeout = Integer.parseInt(propertie.getProperty("redis.timeout", "2000"));
        appName = propertie.getProperty("reids.appName", "default") + ":";

        Set<String> sentinels = new HashSet<String>();
        if (servers != null)
            sentinels.addAll(Arrays.asList(servers.split(" ")));
        List<String> masterList = new ArrayList<String>();
        if (masters != null)
            masterList.addAll(Arrays.asList(masters.split(" ")));
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(maxTotal);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);

        redisPool = new ShardedJedisSentinelPool(masterList, sentinels, poolConfig, timeout, password, database);
    }

    /**
     * 回调
     * 
     * @return 执行结果
     */
    abstract T execute(ShardedJedis jedis, String appName);

    /**
     * 调用{@link #execute(ShardedJedis jedis)}并返回执行结果 它保证在执行{@link #execute(ShardedJedis jedis)}
     * 之后释放数据源
     * 
     * @return 执行结果
     */
    public T getResult() {
        T result = null;
        ShardedJedis jedis = null;
        try {
            jedis = redisPool.getResource();
            result = execute(jedis, appName);
        } catch (Throwable e) {
            throw new RuntimeException("Redis execute exception", e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return result;
    }
    
}