package com.example.artshop.service.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.Optional;

class EntityCacheTest {

    private EntityCache<String> cache;

    @BeforeEach
    void setUp() {
        cache = new EntityCache<>("TestEntity");
    }

    @Test
    void testGet_WhenItemExists_ReturnsItem() {
        cache.put(1, "TestValue");
        Optional<String> result = cache.get(1);
        assertTrue(result.isPresent());
        assertEquals("TestValue", result.get());
    }

    @Test
    void testGet_WhenItemDoesNotExist_ReturnsEmpty() {
        Optional<String> result = cache.get(1);
        assertFalse(result.isPresent());
    }

    @Test
    void testPut_AddsNewItem() {
        cache.put(1, "TestValue");
        assertTrue(cache.get(1).isPresent());
    }

    @Test
    void testPut_DoesNotOverwriteExistingItem() {
        cache.put(1, "TestValue");
        cache.put(1, "NewValue");
        assertEquals("TestValue", cache.get(1).get());
    }

    @Test
    void testUpdate_UpdatesExistingItem() {
        cache.put(1, "TestValue");
        cache.update(1, "UpdatedValue");
        assertEquals("UpdatedValue", cache.get(1).get());
    }

    @Test
    void testUpdate_DoesNothingForNonExistingItem() {
        cache.update(1, "TestValue");
        assertFalse(cache.get(1).isPresent());
    }

    @Test
    void testEvict_RemovesItem() {
        cache.put(1, "TestValue");
        cache.evict(1);
        assertFalse(cache.get(1).isPresent());
    }

    @Test
    void testEvict_DoesNothingForNonExistingItem() {
        cache.evict(1);
        assertFalse(cache.get(1).isPresent());
    }

    @Test
    void testClear_RemovesAllItems() {
        cache.put(1, "TestValue1");
        cache.put(2, "TestValue2");
        cache.clear();
        assertTrue(cache.getAllCachedItems().isEmpty());
    }

    @Test
    void testGetAllCachedItems_ReturnsCopy() {
        cache.put(1, "TestValue");
        Map<Integer, String> items = cache.getAllCachedItems();
        assertEquals(1, items.size());
        items.put(2, "AnotherValue");
        assertEquals(1, cache.getAllCachedItems().size());
    }

    @Test
    void testGetCacheInfo_WhenEmpty() {
        assertEquals("TestEntity cache is empty", cache.getCacheInfo());
    }

    @Test
    void testGetCacheInfo_WhenNotEmpty() {
        cache.put(1, "TestValue");
        String info = cache.getCacheInfo();
        assertTrue(info.contains("TestEntity cache contains 1 items:"));
        assertTrue(info.contains("- ID: 1, Entity: TestValue"));
    }

    @Test
    void testGetName_ReturnsEntityName() {
        assertEquals("TestEntity", cache.getName());
    }

    @Test
    void testCacheSizeLimit() {
        for (int i = 1; i <= 6; i++) {
            cache.put(i, "Value" + i);
        }
        assertEquals(5, cache.getAllCachedItems().size());
        assertFalse(cache.get(1).isPresent());
    }
}