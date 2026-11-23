package br.org.cria.splinkerapp.services.implementations;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import static org.junit.Assert.*;

public class MetadataSyncServiceTest {
    @Test public void sharesPendingRequestsAndExpiresEachCollectionIndependently() {
        var queue = new ArrayDeque<Runnable>();
        var clock = new AtomicLong();
        var calls = new ArrayList<String>();
        var service = new MetadataSyncService(queue::add, clock::get, calls::add);
        var first = service.refresh("A", false);
        assertFalse(first.isDone());
        assertSame(first, service.refresh("A", false));
        assertSame(first, service.refresh("A", true));
        var second = service.refresh("B", false);
        assertNotSame(first, second);
        assertEquals(2, queue.size());
        queue.remove().run();
        clock.set(Duration.ofMinutes(1).toNanos());
        queue.remove().run();
        assertTrue(service.refresh("A", false).isDone());
        assertTrue(queue.isEmpty());
        clock.set(Duration.ofMinutes(5).toNanos());
        assertFalse(service.refresh("A", false).isDone());
        assertTrue(service.refresh("B", false).isDone());
        queue.remove().run();
        assertEquals(java.util.List.of("A", "B", "A"), calls);
    }

    @Test public void manualRefreshBypassesFreshnessAndFailureCanBeRetriedImmediately() {
        var queue = new ArrayDeque<Runnable>();
        var attempts = new AtomicLong();
        var service = new MetadataSyncService(queue::add, () -> 0, token -> {
            if (attempts.incrementAndGet() == 2) throw new IOException("offline");
        });
        var first = service.refresh("A", false);
        queue.remove().run();
        first.join();
        var forced = service.refresh("A", true);
        queue.remove().run();
        assertThrows(CompletionException.class, forced::join);
        var retry = service.refresh("A", false);
        assertFalse(retry.isDone());
        queue.remove().run();
        retry.join();
        assertEquals(3, attempts.get());
    }
}
