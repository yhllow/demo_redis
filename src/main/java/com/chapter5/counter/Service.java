package com.chapter5.counter;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

public class Service {
	
	//以秒为单位的计数器精度，分别为1秒，5秒，1分钟，5分钟，1小时，5小时，1天，用户可以按照需要调整这些精度
	private int[] PRECISION = {1, 5, 60, 300, 3600, 18000, 86400};
	//需要保留的样本数量
	private int SAMPLE_COUNT = 120;

	//更新计数器
	public void updateCounter(Jedis jedis, String name){
		//通过取得当前时间来判断对哪个时间片执行自增操作
		Date now = new Date();
		Pipeline pipe = jedis.pipelined();
		//为我们记录的每种精度都创建一个计数器
		for (int prec : PRECISION) {
			//取得当前时间片的开始时间
			long pnow = (((now.getTime())/1000)/prec)*prec;
			String hash = prec + ":" + name;
			//将计数器的引用信息添加到有序集合里面，并将其分值设置为0，以便在之后执行清理操作
			pipe.zadd("know:", 0, hash);
			//对给定名字和精度的计数器进行更新
			pipe.hincrBy("count:" + hash, pnow + "", 1l);
		}
	}
	
	//从指定精度和名字的计数器里获取计数数据
	public Map<String, String> getCounter(Jedis jedis, String name, int prec){
		String hash = prec + ":" + name;
		return jedis.hgetAll("count:" + hash);
	}
	
	//定期清理计数器
	public void cleanCounter(Jedis jedis){
		Pipeline pipe = jedis.pipelined();
		//为了平等的处理更新频率各不相同的多个计数器，程序需要记录清理操作执行的次数
		int passes = 0;
		while(true){
			//记录清理操作开始执行的时间，这个值将被用于计算清理操作的执行时长
			long start = (new Date()).getTime();
			int end = jedis.zcard("know:").intValue();
			//取得被检查计数器的数据
			Set<String> set = jedis.zrange("know:", 0, end);
			if(set != null){
				Iterator<String> it = set.iterator();
				//渐进的遍历所有已知的计数器
				while (it.hasNext()){
					String hash = it.next();
					String[] arr = hash.split(":");
					int prec = Integer.parseInt(arr[0]);//取得计数器的精度
					int bprec = prec/60;//因为清理程序每60秒就会循环一次，所以这里需要根据计数器的更新频率来判断是否真的有必要对计数器进行清理
					//如果这个计数器在这次循环里不需要进行清理，那么检查下一个计数器，例如如果清理程序只循环了3次，而计数器的更新频率为每5分钟一次，那么程序暂时还不需要对这个计数器进行清理
					if(passes % bprec > 0){
						continue;
					}
					
					String hkey = "count:" + hash;
					Set<String> set2 = jedis.hkeys(hkey);
					//根据给定的精度以及需要保留的样本数量，计算出我们需要保留什么时间之前的样本
					int cutoff = (int)start - SAMPLE_COUNT * prec;
					//需要删除的样本
					String[] fields = null;
					List<String> l = new ArrayList<String>();
					
					//找到需要删除的样本
					Iterator<String> it2 = set2.iterator();
					int i = 0;
					while(it2.hasNext()){
						int tmpTime = Integer.parseInt(it2.next());
						if(tmpTime < cutoff){
							l.add(tmpTime + "");
						}
					}
					fields = new String[l.size()];
					for (String string : l) {
						fields[i++] = string;
					}
					
					if(fields != null && fields.length > 0){
						jedis.hdel(hkey, fields);
						//散列里的所有元素都需要被清除，那么这个散列就可能为空,那么可以从记录已知计数器的有序集合里移除它
						if(set2.size() == l.size()){
							try{
								pipe.watch(hkey);
								//如果散列为空，那么从记录已知计数器的有序集合里移除它
								if(pipe.hlen(hkey).get() == 0l){
									pipe.multi();
									pipe.zrem("know:", hash);
									pipe.exec();
								}else{
									jedis.unwatch();
								}
							}catch(Exception e){
								
							}
						}
					}
					
					passes++;
				}
			}
		}
	}
	
}
