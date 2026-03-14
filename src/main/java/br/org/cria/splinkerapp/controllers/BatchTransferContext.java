package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.models.TransferResult;

import java.util.ArrayList;
import java.util.List;

public final class BatchTransferContext {

    private static final List<String> SELECTED_COLLECTIONS = new ArrayList<>();
    private static final List<TransferResult> RESULTS = new ArrayList<>();

    private BatchTransferContext() {
    }

    public static void setSelectedCollections(List<String> collections) {
        SELECTED_COLLECTIONS.clear();
        SELECTED_COLLECTIONS.addAll(collections);
    }

    public static List<String> getSelectedCollections() {
        return new ArrayList<>(SELECTED_COLLECTIONS);
    }

    public static void setResults(List<TransferResult> results) {
        RESULTS.clear();
        RESULTS.addAll(results);
    }

    public static List<TransferResult> getResults() {
        return new ArrayList<>(RESULTS);
    }

    public static void clear() {
        SELECTED_COLLECTIONS.clear();
        RESULTS.clear();
    }
}