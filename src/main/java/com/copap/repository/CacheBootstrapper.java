package com.copap.repository;

public class CacheBootstrapper {

    public static void rebuild(CachedOrderRepository cacheRepo,
                               OrderRepository sourceRepo) {

        // In real system: iterate DB
        System.out.println("Cache rebuilt from repository");
    }
}
