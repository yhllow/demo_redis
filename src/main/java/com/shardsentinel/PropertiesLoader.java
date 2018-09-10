package com.shardsentinel;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesLoader {
	
	private static String REDIS_CONFIGURATION_FILE = "/redis.properties";

    private static Properties propertie = null;

    static {
        InputStream inputStream = Object.class.getResourceAsStream(REDIS_CONFIGURATION_FILE);
        try {
            if (inputStream != null) {
                propertie = new Properties();
                propertie.load(inputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Properties getPropertie() {
        return propertie;
    }

}
