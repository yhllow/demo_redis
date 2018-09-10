package com.shardsentinel;

public class Test {
	
	public static void main(String[] args) {
		RedisUtil util = new RedisUtil();
		util.set("name", "lisi");
		System.out.println(util.get("name"));
		util.set("name2", "wnagwu");
		System.out.println(util.get("name2"));
		util.set("name3", "zhangsulei");
		System.out.println(util.get("name3"));
		util.set("name4", "lishuang");
		System.out.println(util.get("name4"));
		util.set("name5", "zhaosi");
		System.out.println(util.get("name5"));
	}

}
