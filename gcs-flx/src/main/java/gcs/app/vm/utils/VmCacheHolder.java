package gcs.app.vm.utils;

import gcs.app.vm.bean.VmClass;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class VmCacheHolder {

    private static final ConcurrentMap<String, AtomicBoolean> vmClassCacheMark = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, VmClass> vmClassCacheMap = new ConcurrentHashMap<>();

    /**
     * 判断缓存是否存在
     */
    public static Boolean isCached(String camelDomainName) {
        if (camelDomainName == null) {
            return false;
        }
        AtomicBoolean mark = vmClassCacheMark.get(camelDomainName);
        return mark != null && mark.get();
    }

    /**
     * 读取缓存条目
     * @return 可能为null
     */
    public static VmClass getCachedEntry(String camelDomainName) {
        if (camelDomainName == null) {
            return null;
        }
        return vmClassCacheMap.get(camelDomainName);
    }

    /**
     * 缓存条目（支持null值，表示删除缓存）
     */
    public static void cacheEntry(String camelDomainName, VmClass vmClass) {
        if (camelDomainName == null) {
            return;
        }
        if (vmClass != null) {
            vmClassCacheMap.put(camelDomainName, vmClass);
        }
        vmClassCacheMark.computeIfAbsent(camelDomainName, k -> new AtomicBoolean()).set(true);
    }

    /**
     * 清理所有缓存
     */
    public static void cleanAllCache() {
        vmClassCacheMap.clear();
        vmClassCacheMark.clear();
    }

    /**
     * 清理指定缓存
     */
    public static void cleanVmClassCache(String domainName) {
        String camelDomainName = VmUtils.toUpperCamel(domainName);
        if (camelDomainName == null) {
            return;
        }
        // 原子性地移除缓存条目和标记
        vmClassCacheMap.remove(camelDomainName);
        vmClassCacheMark.remove(camelDomainName);
    }
}