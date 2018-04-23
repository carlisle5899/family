package org.yxyqcy.family.sys.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.yxyqcy.family.common.component.SpringContextHolder;

/**
 * Created with family.
 * author: cy
 * Date: 16/6/1
 * Time: 下午2:35
 * description: 缓存util
 */
public class CacheUtil {
    private static CacheManager cacheManager = ((CacheManager) SpringContextHolder.getBean("cacheManager"));
    private static final String SYS_CACHE = "sysCache";

    public static Object get(String key) {
        return get(SYS_CACHE, key);
    }

    public static void put(String key, Object value) {
        put(SYS_CACHE, key, value);
    }

    public static void remove(String key) {
        remove(SYS_CACHE, key);
    }

    public static Object get(String cacheName, String key) {
        Element element = getCache(cacheName).get(key);
        return element==null?null:element.getObjectValue();
    }

    public static void put(String cacheName, String key, Object value) {
        Element element = new Element(key, value);
        getCache(cacheName).put(element);
    }

    public static void remove(String cacheName, String key) {
        getCache(cacheName).remove(key);
    }

    /**
     * 获得一个Cache，没有则创建一个。
     * @param cacheName
     * @return
     */
    private static Cache getCache(String cacheName){
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null){
            cacheManager.addCache(cacheName);
            cache = cacheManager.getCache(cacheName);
            cache.getCacheConfiguration().setEternal(true);
        }
        return cache;
    }

    public static CacheManager getCacheManager() {
        return cacheManager;
    }

}