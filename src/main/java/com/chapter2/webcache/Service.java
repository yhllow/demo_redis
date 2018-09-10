package com.chapter2.webcache;

import redis.clients.jedis.Jedis;

public class Service {
	
	/**把页面缓存
	 * @param jedis
	 * @param pageKey
	 */
	public void cacheRequest(Jedis jedis, String pageKey){
		String content = jedis.get(pageKey);
		if(content == null){
			content = "查询数据库生成动态页面内容";
			//setex：将值value关联到key，并将key的生存时间设为seconds(以秒为单位)。
			jedis.setex(pageKey, 300, content);
		}
	}

}
