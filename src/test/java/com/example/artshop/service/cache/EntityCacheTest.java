import com.example.artshop.service.cache.EntityCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EntityCacheTest {

    private EntityCache<String> cache;

    @BeforeEach
    void setUp() {
        cache = new EntityCache<>("TestEntity");
    }

    @Test
    void testPutAndGet() {
        cache.put(1, "Value1");
        Optional<String> result = cache.get(1);
        assertTrue(result.isPresent());
        assertEquals("Value1", result.get());
    }

    @Test
    void testGetNotPresent() {
        Optional<String> result = cache.get(99);
        assertFalse(result.isPresent());
    }

    @Test
    void testPutExistingKeyShouldNotOverwrite() {
        cache.put(1, "Value1");
        cache.put(1, "NewValue");
        Optional<String> result = cache.get(1);
        assertEquals("Value1", result.get()); // Should remain original
    }

    @Test
    void testEvictPresent() {
        cache.put(2, "ToRemove");
        cache.evict(2);
        assertFalse(cache.get(2).isPresent());
    }

    @Test
    void testEvictNotPresent() {
        // Should not throw
        cache.evict(42);
    }

    @Test
    void testUpdatePresent() {
        cache.put(3, "OldValue");
        cache.update(3, "UpdatedValue");
        assertEquals("UpdatedValue", cache.get(3).get());
    }

    @Test
    void testUpdateNotPresent() {
        // Should not insert
        cache.update(99, "WillNotInsert");
        assertFalse(cache.get(99).isPresent());
    }

    @Test
    void testClear() {
        cache.put(1, "A");
        cache.put(2, "B");
        cache.clear();
        assertTrue(cache.getAllCachedItems().isEmpty());
    }

    @Test
    void testCacheEvictionPolicy() {
        for (int i = 1; i <= 6; i++) {
            cache.put(i, "Val" + i);
        }
        Map<Integer, String> current = cache.getAllCachedItems();
        assertEquals(5, current.size());
        assertFalse(current.containsKey(1)); // 1 should have been evicted
        for (int i = 2; i <= 6; i++) {
            assertTrue(current.containsKey(i));
        }
    }
}
