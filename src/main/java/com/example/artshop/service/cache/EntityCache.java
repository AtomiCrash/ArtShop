package com.example.artshop.service.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class EntityCache<T> {
    private final Map<Integer, T> cache;
    private final String entityName;

    public static final String CACHE = "[CACHE] ";
    public static final String CACHE_ID = " with id ";

    public EntityCache(String entityName) {
        this.entityName = entityName;
        this.cache = new LinkedHashMap<Integer, T>(5, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, T> eldest) {
                return size() > 5;
            }
        };
    }

    public Optional<T> get(Integer id) {
        Optional<T> entity = Optional.ofNullable(cache.get(id));
        if (entity.isPresent()) {
            System.out.println(CACHE + entityName + CACHE_ID + id + " retrieved from cache");
        }
        return entity;
    }

    public void put(Integer id, T entity) {
        if (!cache.containsKey(id)) {
            cache.put(id, entity);
            System.out.println(CACHE + entityName + CACHE_ID + id + " added to cache");
        }
    }

    public void evict(Integer id) {
        if (cache.containsKey(id)) {
            cache.remove(id);
            System.out.println(CACHE + entityName + CACHE_ID + id + " removed from cache");
        }
    }

    public void update(Integer id, T entity) {
        if (cache.containsKey(id)) {
            cache.put(id, entity);
            System.out.println(CACHE + entityName + CACHE_ID + id + " updated in cache");
        }
    }

    public void clear() {
        cache.clear();
        System.out.println(CACHE + entityName + " cache cleared");
    }

    public Map<Integer, T> getAllCachedItems() {
        return new LinkedHashMap<>(cache);
    }

    public String getCacheInfo() {
        if (cache.isEmpty()) {
            return entityName + " cache is empty";
        }
        StringBuilder info = new StringBuilder(entityName + " cache contains " + cache.size() + " items:\n");
        cache.forEach((id, entity) -> info.append("- ID: ")
                .append(id).append(", Entity: ").append(entity).append("\n"));
        return info.toString();
    }
}