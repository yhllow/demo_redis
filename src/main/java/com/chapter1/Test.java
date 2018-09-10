package com.chapter1;

import redis.clients.jedis.Jedis;

import com.chapter1.service.VotoService;
import com.main.RedisClient;

public class Test {
	
	public static void main(String[] args) {
		RedisClient client = new RedisClient();
		Jedis jedis = client.getJedis();
		jedis.flushDB();
		
		VotoService service = new VotoService();
		String article1 = service.postArticle(jedis, "张苏磊", "我的第一篇文章");
		String article2 = service.postArticle(jedis, "莉莉", "钢铁是怎么练成的");
		String article3 = service.postArticle(jedis, "node", "nodejs深入浅出");
		
		service.articleVote(jedis, "u001", article1);
		service.articleVote(jedis, "u001", article2);
		
		service.articleVote(jedis, "u002", article1);
		service.articleVote(jedis, "u002", article2);
		service.articleVote(jedis, "u002", article3);
		
		service.articleVote(jedis, "u003", article1);
		service.articleVote(jedis, "u003", article2);
		service.articleVote(jedis, "u003", article3);
		service.articleVote(jedis, "u003", article3);
		
		service.articleVote(jedis, "u004", article2);
		
		service.getArticles(jedis);
	}

}
