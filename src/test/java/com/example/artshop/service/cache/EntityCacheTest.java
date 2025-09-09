package com.example.artshop.service.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EntityCacheTest {

    private EntityCache<String> cache;
    private final String testEntity = "Test Entity";

    @BeforeEach
    void setUp() {
        cache = new EntityCache<>("Test");
    }

    @Test
    void testPutAndGet() {
        cache.put(1, testEntity);
        Optional<String> result = cache.get(1);

        assertTrue(result.isPresent());
        assertEquals(testEntity, result.get());
    }

    @Test
    void testGet_NotFound() {
        Optional<String> result = cache.get(1);
        assertFalse(result.isPresent());
    }

    @Test
    void testEvict() {
        cache.put(1, testEntity);
        cache.evict(1);

        Optional<String> result = cache.get(1);
        assertFalse(result.isPresent());
    }

    @Test
    void testUpdate() {
        cache.put(1, testEntity);
        cache.update(1, "Updated Entity");

        Optional<String> result = cache.get(1);
        assertTrue(result.isPresent());
        assertEquals("Updated Entity", result.get());
    }

    @Test
    void testClear() {
        cache.put(1, testEntity);
        cache.put(2, "Another Entity");
        cache.clear();

        assertTrue(cache.getAllCachedItems().isEmpty());
    }

    @Test
    void testGetAllCachedItems() {
        cache.put(1, testEntity);
        cache.put(2, "Another Entity");

        Map<Integer, String> items = cache.getAllCachedItems();
        assertEquals(2, items.size());
        assertTrue(items.containsKey(1));
        assertTrue(items.containsKey(2));
    }

    @Test
    void testGetCacheInfo_Empty() {
        String info = cache.getCacheInfo();
        assertTrue(info.contains("empty"));
    }

    @Test
    void testGetCacheInfo_WithItems() {
        cache.put(1, testEntity);
        String info = cache.getCacheInfo();

        assertTrue(info.contains("1 items"));
        assertTrue(info.contains("Test Entity"));
    }

    @Test
    void testGetName() {
        assertEquals("Test", cache.getName());
    }

    @Test
    void testCacheEvictionPolicy() {
        // Добавляем больше элементов, чем вмещает кэш (лимит 5)
        for (int i = 1; i <= 6; i++) {
            cache.put(i, "Entity " + i);
        }

        Map<Integer, String> items = cache.getAllCachedItems();
        assertEquals(5, items.size()); // Должно остаться только 5 элементов
        assertFalse(items.containsKey(1)); // Первый элемент должен быть удален
        assertTrue(items.containsKey(6)); // Последний элемент должен остаться
    }
}