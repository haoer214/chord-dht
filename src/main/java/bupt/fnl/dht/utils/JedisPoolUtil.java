package bupt.fnl.dht.utils;

import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 获取单例的jedisPool对象
 */

public class JedisPoolUtil {

//    private static volatile JedisPool jedisPool;
//
//    private JedisPoolUtil(){}
//
//    public JedisPool getJedisPool(){
//        if (null == jedisPool){
//            synchronized (JedisPoolUtil.class){
//                if (null == jedisPool){
//                    JedisPoolConfig poolConfig = new JedisPoolConfig();
//                    poolConfig.setMaxTotal(1000);
//                    poolConfig.setMaxIdle(32);
//                    poolConfig.setMaxWaitMillis(1000);
//                    poolConfig.setTestOnBorrow(true);
//                    jedisPool = new JedisPool(poolConfig, "127.0.0.1", 6379);
//                }
//            }
//        }
//        return jedisPool;
//    }
}
