package com.shardsentinel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPipeline;
import redis.clients.jedis.Transaction;

/**
 * 内存数据库REDIS的辅助类,负责对内存数据库的所有操作
 * 
 * @version V1.0
 * @author 俞金贵
 */
public class RedisUtil {

    /**
     * 返回模糊匹配的key,左匹配
     * 
     * @param key 模糊匹配的key
     * @return 匹配到的key列表
     */
    public static List<String> keys(final String key) {
        return new RedisExecutor<List<String>>() {
            @Override
            List<String> execute(ShardedJedis jedis, String appName) {
                Collection<Jedis> jedisC = jedis.getAllShards();
                Iterator<Jedis> iter = jedisC.iterator();
                List<String> keys = new ArrayList<String>();
                while (iter.hasNext()) {
                    Jedis _jedis = iter.next();
                    Set<String> sets = _jedis.keys(appName + key + "*");
                    keys.addAll(sets);
                }
                return keys;
            }
        }.getResult();
    }

    /**
     * 删除单个key
     * 
     * @param key key
     * @return 删除成功的条数
     */
    public static Long delKey(final String key) {
        return new RedisExecutor<Long>() {
            @Override
            Long execute(ShardedJedis jedis, String appName) {
                return jedis.del(appName + key);
            }
        }.getResult();
    }

    /**
     * 删除多个key
     * 
     * @param keys 匹配的key的集合
     * @return 删除成功的条数
     */
    public static Long delKeys(final String[] keys) {
        return new RedisExecutor<Long>() {
            @Override
            Long execute(ShardedJedis jedis, String appName) {
                Collection<Jedis> jedisC = jedis.getAllShards();
                Iterator<Jedis> iter = jedisC.iterator();
                long count = 0;
                while (iter.hasNext()) {
                    Jedis _jedis = iter.next();
                    count += _jedis.del(appName + keys);
                }
                return count;
            }
        }.getResult();
    }

    /**
     * 删除模糊匹配的key,左匹配
     * 
     * @param likeKey 模糊匹配的key
     * @return 删除成功的条数
     */
    public static long delKeysLike(final String likeKey) {
        return new RedisExecutor<Long>() {
            @Override
            Long execute(ShardedJedis jedis, String appName) {
                Collection<Jedis> jedisC = jedis.getAllShards();
                Iterator<Jedis> iter = jedisC.iterator();
                long count = 0;
                while (iter.hasNext()) {
                    Jedis _jedis = iter.next();
                    Set<String> keys = _jedis.keys(appName + likeKey + "*");
                    count += _jedis.del(keys.toArray(new String[keys.size()]));
                }
                return count;
            }
        }.getResult();
    }

    /**
     * 为给定 key设置有效期.
     * 
     * @param key key
     * @param expire 有效期（秒）
     * @return 1:设置成功,0:已经超时或key不存在
     */
    public static Long expire(final String key, final int expire) {
        return new RedisExecutor<Long>() {
            @Override
            Long execute(ShardedJedis jedis, String appName) {
                return jedis.expire(appName + key, expire);
            }
        }.getResult();
    }

    /**
     * 对存储的key的数值执行原子（跨JVM）的加1操作
     * 
     * @param key key
     * @return 执行递增操作后key对应的值
     */
    public static long incr(final String key) {
        return new RedisExecutor<Long>() {
            @Override
            Long execute(ShardedJedis jedis, String appName) {
                long id = jedis.incr(appName + key);
                if ((id + 75807) >= Long.MAX_VALUE) {
                    // 避免溢出,重置,set命令之前允许incr插队,75807就是预留的插队空间
                    jedis.set(appName + key, "0");
                }
                return id;
            }
        }.getResult();
    }

    /* ================================Strings================================ */

    /**
     * 将键key设定为指定的value值 . <br>
     * 如果 key已经持有其他值, 将覆写旧值,无视类型. <br>
     * 对于某个原本带有生存时间（TTL）的键来说,将会清除原有的 TTL.<br>
     * 时间复杂度：O(1)
     * 
     * @param key key
     * @param value value
     * @return 在操作成功完成时返回 OK
     */
    public static String set(final String key, final String value) {
        return new RedisExecutor<String>() {
            @Override
            String execute(ShardedJedis jedis, String appName) {
                return jedis.set(appName + key, value);
            }
        }.getResult();
    }

    /**
     * 将键key设定为指定的value值 ,并将 key的生存时间设为 expire（以秒为单位）.<br>
     * 如果 key 已经存在, 将覆写旧值. <br>
     * 时间复杂度：O(1)
     * 
     * @param key key
     * @param value value
     * @param expire 有效期（秒）
     * @return 在操作成功完成时返回 OK.当 expire参数不合法时,返回一个错误.
     */
    public static String set(final String key, final String value, final int expire) {
        return new RedisExecutor<String>() {
            @Override
            String execute(ShardedJedis jedis, String appName) {
                return jedis.setex(appName + key, expire, value);
            }
        }.getResult();
    }

    /**
     * 将键key设定为指定的value值 ,只有key不存在的时候才会设置key的值. <br>
     * 时间复杂度：O(1)
     * 
     * @param key key
     * @param value value
     * @return 设置成功返回 1,设置失败返回 0
     */
    public static Long setIfNotExists(final String key, final String value) {
        return new RedisExecutor<Long>() {
            @Override
            Long execute(ShardedJedis jedis, String appName) {
                return jedis.setnx(appName + key, value);
            }
        }.getResult();
    }

    /**
     * 返回key的value<br>
     * 时间复杂度: O(1)
     * 
     * @param key key
     * @return 当 key不存在时,返回 nil,否则返回 key 的值.如果 key的value不是string,就返回一个错误.
     */
    public static String get(final String key) {
        return new RedisExecutor<String>() {
            @Override
            String execute(ShardedJedis jedis, String appName) {
                return jedis.get(appName + key);
            }
        }.getResult();
    }

    /**
     * 批量 {@link #set(String, String)}
     * 
     * @param pairs 键值对数组{数组第一个元素为key,第二个元素为value}
     * @return 操作状态的集合
     */
    public static List<Object> batchSet(final List<Pair<String, String>> pairs) {
        return new RedisExecutor<List<Object>>() {
            @Override
            List<Object> execute(ShardedJedis jedis, String appName) {
                ShardedJedisPipeline pipeline = jedis.pipelined();
                for (Pair<String, String> pair : pairs) {
                    pipeline.set(appName + pair.getKey(), pair.getValue());
                }
                return pipeline.syncAndReturnAll();
            }
        }.getResult();
    }

    /**
     * 批量 {@link #get(String)}
     * 
     * @param keys key数组
     * @return value的集合
     */
    public static List<String> batchGet(final String[] keys) {
        return new RedisExecutor<List<String>>() {
            @Override
            List<String> execute(ShardedJedis jedis, String appName) {
                ShardedJedisPipeline pipeline = jedis.pipelined();
                List<String> result = new ArrayList<String>(keys.length);
                List<Response<String>> responses = new ArrayList<Response<String>>(keys.length);
                for (String key : keys) {
                    responses.add(pipeline.get(appName + key));
                }
                pipeline.sync();
                for (Response<String> resp : responses) {
                    result.add(resp.get());
                }
                return result;
            }
        }.getResult();
    }

    /* ================================Hashes================================ */

    /**
     * 将哈希表 key中的域field的值设为 value.<br>
     * 如果key不存在,将新建一个哈希表.<br>
     * 如果域field已经存在于哈希表中,旧值将被覆盖.<br>
     * 时间复杂度: O(1)
     * 
     * @param key key
     * @param field 域
     * @param value value
     * @return 如果field是哈希表中的一个新建域,并且值设置成功,返回 1. 如果哈希表中域field已经存在且旧值已被新值覆盖,返回 0.
     */
    public static Long hashSet(final String key, final String field, final String value) {
        return new RedisExecutor<Long>() {
            @Override
            Long execute(ShardedJedis jedis, String appName) {
                return jedis.hset(appName + key, field, value);
            }
        }.getResult();
    }

    /**
     * 将哈希表 key中的域field的值设为 value,同时设置哈希表key的有效期.<br>
     * 如果key不存在,将新建一个哈希表.<br>
     * 如果域field已经存在于哈希表中,旧值将被覆盖.<br>
     * 时间复杂度: O(1)
     * 
     * @param key key
     * @param field 域
     * @param value value
     * @param expire 有效期（秒）
     * @return 如果field是哈希表中的一个新建域,并且值设置成功,返回 1.如果哈希表中域field已经存在且旧值已被新值覆盖,返回 0.
     */
    public static Long hashSet(final String key, final String field, final String value,
            final int expire) {
        return new RedisExecutor<Long>() {
            @Override
            Long execute(ShardedJedis jedis, String appName) {
                Pipeline pipeline = jedis.getShard(appName + key).pipelined();
                Response<Long> result = pipeline.hset(appName + key, field, value);
                pipeline.expire(appName + key, expire);
                pipeline.sync();
                return result.get();
            }
        }.getResult();
    }

    /**
     * 返回哈希表key中给定域 field的值.<br>
     * 时间复杂度:O(1)
     * 
     * @param key key
     * @param field 域
     * @return 给定域的值.当给定域不存在或是给定 key不存在时,返回 nil .
     */
    public static String hashGet(final String key, final String field) {
        return new RedisExecutor<String>() {
            @Override
            String execute(ShardedJedis jedis, String appName) {
                return jedis.hget(appName + key, field);
            }
        }.getResult();
    }

    /**
     * 返回哈希表key中给定域 field的值. 如果哈希表key存在,同时设置这个key的有效期
     * 
     * @param key key
     * @param field 域
     * @param expire 有效期（秒）
     * @return 给定域的值.当给定域不存在或是给定key不存在时,返回 nil.
     */
    public static String hashGet(final String key, final String field, final int expire) {
        return new RedisExecutor<String>() {
            @Override
            String execute(ShardedJedis jedis, String appName) {
                Pipeline pipeline = jedis.getShard(appName + key).pipelined();
                Response<String> result = pipeline.hget(appName + key, field);
                pipeline.expire(appName + key, expire);
                pipeline.sync();
                return result.get();
            }
        }.getResult();
    }

    /**
     * 删除哈希表key中给定域 fields.<br>
     * 时间复杂度:O(1)
     * 
     * @param key key
     * @param fields 域
     */
    public static Long hashDel(final String key, final String... fields) {
        return new RedisExecutor<Long>() {
            @Override
            Long execute(ShardedJedis jedis, String appName) {
                return jedis.hdel(appName + key, fields);
            }
        }.getResult();
    }

    /**
     * 同时将多个 field-value (域-值)对设置到哈希表key中. <br>
     * 时间复杂度: O(N) (N为fields的数量)
     * 
     * @param key key
     * @param hash field-value的map
     * @return 如果命令执行成功,返回 OK .当 key不是哈希表(hash)类型时,返回一个错误.
     */
    public static String hashMultipleSet(final String key, final Map<String, String> hash) {
        return new RedisExecutor<String>() {
            @Override
            String execute(ShardedJedis jedis, String appName) {
                return jedis.hmset(appName + key, hash);
            }
        }.getResult();
    }

    /**
     * 同时将多个 field-value (域-值)对设置到哈希表 key 中.同时设置这个key的生存时间
     * 
     * @param key key
     * @param hash field-value的map
     * @param expire 生命周期,单位为秒
     * @return 如果命令执行成功,返回 OK .当 key 不是哈希表(hash)类型时,返回一个错误.
     */
    public static String hashMultipleSet(final String key, final Map<String, String> hash,
            final int expire) {
        return new RedisExecutor<String>() {
            @Override
            String execute(ShardedJedis jedis, String appName) {
                Pipeline pipeline = jedis.getShard(appName + key).pipelined();
                Response<String> result = pipeline.hmset(appName + key, hash);
                pipeline.expire(appName + key, expire);
                pipeline.sync();
                return result.get();
            }
        }.getResult();
    }

    /**
     * 返回哈希表 key中,一个或多个给定域的值.如果给定的域不存在于哈希表,那么返回一个 nil值.<br>
     * 时间复杂度: O(N) (N为fields的数量)
     * 
     * @param key key
     * @param fields field的数组
     * @return 一个包含多个给定域的关联值的表,表值的排列顺序和给定域参数的请求顺序一样.
     */
    public static List<String> hashMultipleGet(final String key, final String... fields) {
        return new RedisExecutor<List<String>>() {
            @Override
            List<String> execute(ShardedJedis jedis, String appName) {
                return jedis.hmget(appName + key, fields);
            }
        }.getResult();
    }

    /**
     * 返回哈希表 key中,一个或多个给定域的值.如果给定的域不存在于哈希表,那么返回一个 nil值. 同时设置这个 key的生存时间
     * 
     * @param key key
     * @param fields field的数组
     * @param expire 生命周期,单位为秒
     * @return 一个包含多个给定域的关联值的表,表值的排列顺序和给定域参数的请求顺序一样.
     */
    public static List<String> hashMultipleGet(final String key, final int expire,
            final String... fields) {
        return new RedisExecutor<List<String>>() {

            @Override
            List<String> execute(ShardedJedis jedis, String appName) {
                Pipeline pipeline = jedis.getShard(appName + key).pipelined();
                Response<List<String>> result = pipeline.hmget(appName + key, fields);
                pipeline.expire(appName + key, expire);
                pipeline.sync();
                return result.get();
            }
        }.getResult();
    }

    /**
     * 批量{@link #hashMultipleSet(String, Map)},在管道中执行
     * 
     * @param pairs 多个hash的多个field
     * @return 操作状态的集合
     */
    public static List<Object> batchHashMultipleSet(
            final List<Pair<String, Map<String, String>>> pairs) {
        return new RedisExecutor<List<Object>>() {

            @Override
            List<Object> execute(ShardedJedis jedis, String appName) {
                ShardedJedisPipeline pipeline = jedis.pipelined();
                for (Pair<String, Map<String, String>> pair : pairs) {
                    pipeline.hmset(appName + pair.getKey(), pair.getValue());
                }
                return pipeline.syncAndReturnAll();
            }
        }.getResult();
    }

    /**
     * 批量{@link #hashMultipleSet(String, Map)},在管道中执行
     * 
     * @param data Map<String, Map<String, String>>格式的数据
     * @return 操作状态的集合
     */
    public static List<Object> batchHashMultipleSet(final Map<String, Map<String, String>> data) {
        return new RedisExecutor<List<Object>>() {
            @Override
            List<Object> execute(ShardedJedis jedis, String appName) {
                ShardedJedisPipeline pipeline = jedis.pipelined();
                for (Map.Entry<String, Map<String, String>> iter : data.entrySet()) {
                    pipeline.hmset(appName + iter.getKey(), iter.getValue());
                }
                return pipeline.syncAndReturnAll();
            }
        }.getResult();
    }

    /**
     * 批量{@link #hashMultipleGet(String, String...)},在管道中执行
     * 
     * @param pairs 多个hash的多个field
     * @return 执行结果的集合
     */
    public static List<List<String>> batchHashMultipleGet(final List<Pair<String, String[]>> pairs) {
        return new RedisExecutor<List<List<String>>>() {
            @Override
            List<List<String>> execute(ShardedJedis jedis, String appName) {
                ShardedJedisPipeline pipeline = jedis.pipelined();
                List<List<String>> result = new ArrayList<List<String>>(pairs.size());
                List<Response<List<String>>> responses = new ArrayList<Response<List<String>>>(
                        pairs.size());
                for (Pair<String, String[]> pair : pairs) {
                    responses.add(pipeline.hmget(appName + pair.getKey(), pair.getValue()));
                }
                pipeline.sync();
                for (Response<List<String>> resp : responses) {
                    result.add(resp.get());
                }
                return result;
            }
        }.getResult();

    }

    /**
     * 返回哈希表 key中,所有的域和值.<br>
     * 在返回值里,紧跟每个域名(field name)之后是域的值(value),所以返回值的长度是哈希表大小的两倍. <br>
     * 时间复杂度: O(N)
     * 
     * @param key key
     * @return 以列表形式返回哈希表的域和域的值.若 key不存在,返回空列表.
     */
    public static Map<String, String> hashGetAll(final String key) {
        return new RedisExecutor<Map<String, String>>() {
            @Override
            Map<String, String> execute(ShardedJedis jedis, String appName) {
                return jedis.hgetAll(appName + key);
            }
        }.getResult();
    }

    /**
     * 返回哈希表 key中,所有的域和值,同时设置这个 key 的生存时间.<br>
     * 在返回值里,紧跟每个域名(field name)之后是域的值(value),所以返回值的长度是哈希表大小的两倍.
     * 
     * @param key key
     * @param expire 生命周期,单位为秒
     * @return 以列表形式返回哈希表的域和域的值.若 key 不存在,返回空列表.
     */
    public static Map<String, String> hashGetAll(final String key, final int expire) {
        return new RedisExecutor<Map<String, String>>() {
            @Override
            Map<String, String> execute(ShardedJedis jedis, String appName) {
                Pipeline pipeline = jedis.getShard(appName + key).pipelined();
                Response<Map<String, String>> result = pipeline.hgetAll(appName + key);
                pipeline.expire(appName + key, expire);
                pipeline.sync();
                return result.get();
            }
        }.getResult();
    }

    /**
     * 批量{@link #hashGetAll(String)}
     * 
     * @param keys key的数组
     * @return 执行结果的集合
     */
    public static List<Map<String, String>> batchHashGetAll(final String... keys) {
        return new RedisExecutor<List<Map<String, String>>>() {
            @Override
            List<Map<String, String>> execute(ShardedJedis jedis, String appName) {
                ShardedJedisPipeline pipeline = jedis.pipelined();
                List<Map<String, String>> result = new ArrayList<Map<String, String>>(keys.length);
                List<Response<Map<String, String>>> responses = new ArrayList<Response<Map<String, String>>>(
                        keys.length);
                for (String key : keys) {
                    responses.add(pipeline.hgetAll(appName + key));
                }
                pipeline.sync();
                for (Response<Map<String, String>> resp : responses) {
                    result.add(resp.get());
                }
                return result;
            }
        }.getResult();
    }

    /**
     * 批量{@link #hashMultipleGet(String, String...)},与{@link #batchHashGetAll(String...)}
     * 不同的是,返回值为Map类型
     * 
     * @param keys key的数组
     * @return 多个hash的所有filed和value
     */
    public static Map<String, Map<String, String>> batchHashGetAllForMap(final String... keys) {
        return new RedisExecutor<Map<String, Map<String, String>>>() {
            @Override
            Map<String, Map<String, String>> execute(ShardedJedis jedis, String appName) {
                ShardedJedisPipeline pipeline = jedis.pipelined();
                // 设置map容量防止rehash
                int capacity = 1;
                while ((int) (capacity * 0.75) <= keys.length) {
                    capacity <<= 1;
                }
                Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>(
                        capacity);
                List<Response<Map<String, String>>> responses = new ArrayList<Response<Map<String, String>>>(
                        keys.length);
                for (String key : keys) {
                    responses.add(pipeline.hgetAll(appName + key));
                }
                pipeline.sync();
                for (int i = 0; i < keys.length; ++i) {
                    result.put(keys[i], responses.get(i).get());
                }
                return result;
            }
        }.getResult();
    }

    /* ================================List================================ */

    /**
     * 将一个或多个值value插入到列表 key的表尾(最右边).<br>
     * RPUSH mylist a b c 会返回一个列表，其第一个元素是 a ，第二个元素是 b ，第三个元素是 c
     * 
     * @param key key
     * @param values value的数组
     * @return 执行listPushTail操作后,表的长度
     */
    public static Long listPushTail(final String key, final String... values) {
        return new RedisExecutor<Long>() {
            @Override
            Long execute(ShardedJedis jedis, String appName) {
                return jedis.rpush(appName + key, values);
            }
        }.getResult();
    }

    /**
     * 将一个或多个值 value插入到列表 key的表头(最左边).<br>
     * LPUSH mylist a b c，返回的列表是 c 为第一个元素， b 为第二个元素， a 为第三个元素
     * 
     * @param key key
     * @param values values
     * @return 执行 listPushHead 命令后,列表的长度.
     */
    public static Long listPushHead(final String key, final String... values) {
        return new RedisExecutor<Long>() {
            @Override
            Long execute(ShardedJedis jedis, String appName) {
                return jedis.lpush(appName + key, values);
            }
        }.getResult();
    }

    /**
     * 将一个或多个值 value 插入到列表 key 的表头, 当列表大于指定长度是就对列表进行修剪(trim)
     * 
     * @param key key
     * @param values values
     * @param size 链表超过这个长度就修剪元素
     * @return 执行 listPushHeadAndTrim 命令后,列表的长度.
     */
    public static Long listPushHeadAndTrim(final String key, final long size,
            final String... values) {
        return new RedisExecutor<Long>() {
            @Override
            Long execute(ShardedJedis jedis, String appName) {
                Pipeline pipeline = jedis.getShard(appName + key).pipelined();
                Response<Long> result = pipeline.lpush(appName + key, values);
                // 修剪列表元素, 如果 size - 1 比 end 下标还要大,Redis将 size 的值设置为 end .
                pipeline.ltrim(appName + key, 0, size - 1);
                pipeline.sync();
                return result.get();
            }
        }.getResult();
    }

    /**
     * 批量{@link #listPushTail(String, String...)},以锁的方式实现
     * 
     * @param key key
     * @param values value的数组
     * @param delOld 如果key存在,是否删除它. true:删除；false:不删除,只是在行尾追加
     */
    public static void batchListPushTail(final String key, final String[] values,
            final boolean delOld) {
        new RedisExecutor<Object>() {
            @Override
            Object execute(ShardedJedis jedis, String appName) {
                if (delOld) {
                    RedisLock lock = new RedisLock(appName + key, jedis);
                    lock.lock();
                    try {
                        Pipeline pipeline = jedis.getShard(appName + key).pipelined();
                        pipeline.del(appName + key);
                        // for (String value : values) {
                        // pipeline.rpush(key, value);
                        // }
                        pipeline.rpush(appName + key, values);
                        pipeline.sync();
                    } finally {
                        lock.unlock();
                    }
                } else {
                    jedis.rpush(appName + key, values);
                }
                return null;
            }
        }.getResult();
    }

    /**
     * 同{@link #batchListPushTail(String, String[], boolean)},不同的是利用redis的事务特性来实现
     * 
     * @param key key
     * @param values value的数组
     * @return null
     */
    public static Object updateListInTransaction(final String key, final List<String> values) {
        return new RedisExecutor<Object>() {
            @Override
            Object execute(ShardedJedis jedis, String appName) {
                Transaction transaction = jedis.getShard(appName + key).multi();
                transaction.del(appName + key);
                for (String value : values) {
                    transaction.rpush(appName + key, value);
                }
                transaction.exec();
                return null;
            }
        }.getResult();
    }

    /**
     * 在key对应list的尾部部添加字符串元素,如果key存在,什么也不做
     * 
     * @param key key
     * @param values value的数组
     * @return 执行insertListIfNotExists后,表的长度
     */
    public static Long insertListIfNotExists(final String key, final String[] values) {
        return new RedisExecutor<Long>() {
            @Override
            Long execute(ShardedJedis jedis, String appName) {
                RedisLock lock = new RedisLock(appName + key, jedis);
                lock.lock();
                try {
                    if (!jedis.exists(appName + key)) {
                        return jedis.rpush(appName + key, values);
                    }
                } finally {
                    lock.unlock();
                }
                return 0L;
            }
        }.getResult();
    }

    /**
     * 返回list所有元素,下标从0开始,负值表示从后面计算,-1表示倒数第一个元素,key不存在返回空列表
     * 
     * @param key key
     * @return list所有元素
     */
    public static List<String> listGetAll(final String key) {
        return new RedisExecutor<List<String>>() {
            @Override
            List<String> execute(ShardedJedis jedis, String appName) {
                return jedis.lrange(appName + key, 0, -1);
            }
        }.getResult();
    }

    /**
     * 返回指定区间内的元素,下标从0开始,负值表示从后面计算,-1表示倒数第一个元素,key不存在返回空列表
     * 
     * @param key key
     * @param beginIndex 下标开始索引（包含）
     * @param endIndex 下标结束索引（不包含）
     * @return 指定区间内的元素
     */
    public static List<String> listRange(final String key, final long beginIndex,
            final long endIndex) {
        return new RedisExecutor<List<String>>() {

            @Override
            List<String> execute(ShardedJedis jedis, String appName) {
                return jedis.lrange(appName + key, beginIndex, endIndex - 1);
            }
        }.getResult();
    }

    /**
     * 一次获得多个链表的数据
     * 
     * @param keys key的数组
     * @return 执行结果
     */
    public static Map<String, List<String>> batchGetAllList(final List<String> keys) {
        return new RedisExecutor<Map<String, List<String>>>() {
            @Override
            Map<String, List<String>> execute(ShardedJedis jedis, String appName) {
                ShardedJedisPipeline pipeline = jedis.pipelined();
                Map<String, List<String>> result = new HashMap<String, List<String>>();
                List<Response<List<String>>> responses = new ArrayList<Response<List<String>>>(
                        keys.size());
                for (String key : keys) {
                    responses.add(pipeline.lrange(appName + key, 0, -1));
                }
                pipeline.sync();
                for (int i = 0; i < keys.size(); ++i) {
                    result.put(keys.get(i), responses.get(i).get());
                }
                return result;
            }
        }.getResult();
    }

    /* ======================================Pub/Sub====================================== */

    /**
     * 将信息 message 发送到指定的频道 channel. 时间复杂度：O(N+M),其中 N 是频道 channel 的订阅者数量,而 M 则是使用模式订阅(subscribed
     * patterns)的客户端的数量.
     * 
     * @param channel 频道
     * @param message 信息
     * @return 接收到信息 message 的订阅者数量.
     */
    public static Long publish(final String channel, final String message) {
        return new RedisExecutor<Long>() {
            @Override
            Long execute(ShardedJedis jedis, String appName) {
                Jedis _jedis = jedis.getShard(channel);
                return _jedis.publish(channel, message);
            }

        }.getResult();
    }

    /**
     * 订阅给定的一个频道的信息.
     * 
     * @param jedisPubSub 监听器
     * @param channel 频道
     */
    public static void subscribe(final JedisPubSub jedisPubSub, final String channel) {
        new RedisExecutor<Object>() {
            @Override
            Object execute(ShardedJedis jedis, String appName) {
                Jedis _jedis = jedis.getShard(channel);
                // 注意subscribe是一个阻塞操作,因为当前线程要轮询Redis的响应然后调用subscribe
                _jedis.subscribe(jedisPubSub, channel);
                return null;
            }
        }.getResult();
    }

    /**
     * 取消订阅
     * 
     * @param jedisPubSub 监听器
     */
    public static void unSubscribe(final JedisPubSub jedisPubSub) {
        jedisPubSub.unsubscribe();
    }

    /* ======================================Sorted set================================= */

    /**
     * 将一个 member 元素及其 score 值加入到有序集 key 当中.
     * 
     * @param key key
     * @param score score 值可以是整数值或双精度浮点数.
     * @param member 有序集的成员
     * @return 被成功添加的新成员的数量,不包括那些被更新的、已经存在的成员.
     */
    public static Long addWithSortedSet(final String key, final double score, final String member) {
        return new RedisExecutor<Long>() {
            @Override
            Long execute(ShardedJedis jedis, String appName) {
                return jedis.zadd(appName + key, score, member);
            }
        }.getResult();
    }

    /**
     * 将多个 member 元素及其 score 值加入到有序集 key 当中.
     * 
     * @param key key
     * @param scoreMembers score、member的pair
     * @return 被成功添加的新成员的数量,不包括那些被更新的、已经存在的成员.
     */
    public static Long addWithSortedSet(final String key, final Map<String, Double> scoreMembers) {
        return new RedisExecutor<Long>() {
            @Override
            Long execute(ShardedJedis jedis, String appName) {
                return jedis.zadd(appName + key, scoreMembers);
            }
        }.getResult();
    }

    /**
     * 返回有序集 key 中, score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员. 有序集成员按 score 值递减(从大到小)的次序排列.
     * 
     * @param key key
     * @param max score最大值
     * @param min score最小值
     * @return 指定区间内,带有 score 值(可选)的有序集成员的列表
     */
    public static Set<String> revrangeByScoreWithSortedSet(final String key, final double max,
            final double min) {
        return new RedisExecutor<Set<String>>() {
            @Override
            Set<String> execute(ShardedJedis jedis, String appName) {
                return jedis.zrevrangeByScore(appName + key, max, min);
            }
        }.getResult();
    }
    
}