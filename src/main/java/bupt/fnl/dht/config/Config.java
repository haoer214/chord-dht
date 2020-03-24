package bupt.fnl.dht.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
@ComponentScan("bupt.fnl.dht")
public class Config {
    @Bean("jedisPool")
    public JedisPool jedisPool(){
        return new JedisPool(poolConfig(), "127.0.0.1", 6379);
    }
    @Bean("poolConfig")
    public JedisPoolConfig poolConfig(){
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        // 在指定时刻通过pool能够获取到的最大连接数，默认值8
        poolConfig.setMaxTotal(1000);
        // 控制一个pool最多有多少个状态为idle的实例，默认值8
        poolConfig.setMaxIdle(32);
        // 当连接耗尽时会阻塞，超过了阻塞时间（单位毫秒）时会报错，默认值-1
        poolConfig.setMaxWaitMillis(1000);
        // 在borrow一个实例时，是否提前进行validate操作；如果为true，则得到的实例均是可用的；默认是false
        poolConfig.setTestOnBorrow(true);
        return poolConfig;
    }
}
