package com.chapter1.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Jedis;

public class VotoService {
	
	private final int ONE_WEEK_IN_SECONDS = 7 * 86400 * 1000;
	private final int VOTE_SCORE = 432;
	
	/**投票功能
	 * @param jedis
	 * @param user
	 * @param article
	 */
	public void articleVote(Jedis jedis, String user, String article){
		Date date = new Date();
		//计算文章的投票截止时间
		long cutoff = date.getTime() - ONE_WEEK_IN_SECONDS;
		//检查是否还可以对文章进行投票
		if(jedis.zscore("time:", article) < cutoff) {
			return;
		}
		
		//从article:id标识符里面取出文章的id
		String article_id = article.split(":")[1];
		//如果用户是第一次为这票文章投票，那么增加这篇文章的投票数量和评分
		if(jedis.sadd("voted:" + article_id, user) == 1){
			jedis.zincrby("score:", VOTE_SCORE, article);
			jedis.hincrBy(article, "votes", 1);
		}else{
			System.out.println(user + "对" + article + "已经投过票了");
		}
	}
	
	/**发布新文章
	 * @param jedis
	 * @param user
	 * @param title
	 */
	public String postArticle(Jedis jedis, String user, String title){
		//incr：将指定主键key的value值加1
		//生成新文章id
		long article_id = jedis.incr("article:");
		
		String voted = "voted:" + article_id;
		//将发布文章的用户添加到文章的已投票用户名单里，然后将这个名单的过期时间设置为一周
		jedis.sadd(voted, user);
		jedis.expire(voted, ONE_WEEK_IN_SECONDS);
		
		Date date = new Date();
		String article = "article:" + article_id;
		Map hash = new HashMap();
		hash.put("title", title);
		hash.put("poster", user);
		hash.put("time", date.getTime() + "");
		hash.put("votes", "1");
		jedis.hmset(article, hash);
		
		//将文章添加到根据发布时间排序的有序集合里和根据评分排序的有序集合里
		jedis.zadd("score:", date.getTime() + VOTE_SCORE, article);
		jedis.zadd("time:", date.getTime(), article);
		
		return article;
	}
	
	public void getArticles(Jedis jedis){
		//ZREVRANGE命令返回存储在键的排序元素集合在指定的范围。该元素被认为是从最高到最低得分排序。
		Set<String> ids = jedis.zrevrange("score:", 0, -1);
		for (String id : ids) {
			System.out.println(id);
		}
	}

}
