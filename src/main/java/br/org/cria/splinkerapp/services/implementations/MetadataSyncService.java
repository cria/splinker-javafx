package br.org.cria.splinkerapp.services.implementations;

import br.org.cria.splinkerapp.facade.ConfigFacade;
import br.org.cria.splinkerapp.models.DataSourceType;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.LongSupplier;

/** Session-wide freshness tracking; the actual collection data remains in SQLite. */
public final class MetadataSyncService {
    private static final long MAX_AGE = Duration.ofMinutes(5).toNanos();
    private static final MetadataSyncService INSTANCE = new MetadataSyncService(
            Executors.newSingleThreadExecutor(runnable -> {
                var thread = new Thread(runnable, "metadata-sync");
                thread.setDaemon(true);
                return thread;
            }), System::nanoTime, MetadataSyncService::syncMetadata);

    @FunctionalInterface
    interface Synchronizer {
        void sync(String token) throws Exception;
    }

    private final Executor executor;
    private final LongSupplier clock;
    private final Synchronizer synchronizer;
    private final Map<String, Long> lastSuccess = new HashMap<>();
    private final Map<String, CompletableFuture<Void>> pending = new HashMap<>();

    MetadataSyncService(Executor executor, LongSupplier clock, Synchronizer synchronizer) {
        this.executor = executor;
        this.clock = clock;
        this.synchronizer = synchronizer;
    }

    public static MetadataSyncService getInstance() {
        return INSTANCE;
    }

    public synchronized CompletableFuture<Void> refresh(String token, boolean force) {
        // Even a manual refresh shares a request already running for this collection.
        var existing = pending.get(token);
        if (existing != null) return existing;
        var success = lastSuccess.get(token);
        if (!force && success != null && clock.getAsLong() - success < MAX_AGE) {
            return CompletableFuture.completedFuture(null);
        }
        var result = new CompletableFuture<Void>();
        pending.put(token, result);
        try {
            executor.execute(() -> {
                try {
                    synchronizer.sync(token);
                    synchronized (this) {
                        lastSuccess.put(token, clock.getAsLong());
                        pending.remove(token);
                    }
                    result.complete(null);
                } catch (Exception error) {
                    synchronized (this) {
                        lastSuccess.remove(token);
                        pending.remove(token);
                    }
                    result.completeExceptionally(error);
                }
            });
        } catch (RuntimeException error) {
            pending.remove(token);
            result.completeExceptionally(error);
        }
        return result;
    }

    static void syncMetadata(String token) throws Exception {
        var config = DataSetService.getConfigurationDataFromAPI(token);
        var fields = new HashMap<String, String>();
        fields.put("token", token);
        fields.put("id", String.valueOf((int) Double.parseDouble(config.get("dataset_id").toString())));
        fields.put("dataset_name", config.get("dataset_name").toString());
        fields.put("dataset_acronym", config.get("dataset_acronym").toString());
        fields.put("datasource_type", DataSourceType.valueOf(config.get("data_source_type").toString()).name());
        // UPDATE cannot recreate a collection deleted while the request was running.
        if (DataSetService.getDataSet(token) == null) return;
        DataSetService.updateDataSource(fields);
        ConfigFacade.HandleBackendData(token, config);
    }
}
