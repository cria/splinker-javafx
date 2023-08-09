package br.org.cria.splinkerapp.models;

import java.util.List;

public class DarwinCoreArchive {
    private List<DarwinCoreRecord> records;
    
    public DarwinCoreArchive(List<DarwinCoreRecord> records) {
        this.records = records;
    }
    
    public List<DarwinCoreRecord> getRecords() {
        return records;
    }
    
    public void addRecords(List<DarwinCoreRecord> records) {
        this.records.addAll(records);
    }
    public void addRecord(DarwinCoreRecord record) { this.records.add(0, record); }

}
