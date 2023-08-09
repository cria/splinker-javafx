package br.org.cria.splinkerapp.models;


public class DarwinCoreRecord {
    private String id;
    private String scientificName;
    private String locality;
    private String kingdom;
    private String phylum;
    private String classs;
    private String order;
    private String family;
    private String genus;
    private String specificEpithet;
    private String infraspecificEpithet;
    private String modified;
    private String basisOfRecord;
    // Add more fields as needed
    
    public DarwinCoreRecord(String id, String scientificName, String locality, 
                            String kingdom, String phylum, String classs,
                            String order, String family, String genus,
                            String specificEpithet, String infraspecificEpithet,
                            String modified, String basisOfRecord) {
        this.id = id;
        this.scientificName = scientificName;
        this.locality = locality;
        this.kingdom = kingdom;
        this.phylum = phylum;
        this.classs = classs;
        this.order = order;
        this.family = family;
        this.genus = genus;
        this.specificEpithet = specificEpithet;
        this.infraspecificEpithet = infraspecificEpithet;
        this.modified = modified;
        this.basisOfRecord = basisOfRecord;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getScientificName() {
        return scientificName;
    }
    
    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }
    
    public String getLocality() {
        return locality;
    }
    
    public void setLocality(String locality) {
        this.locality = locality;
    }
    
    public String getKingdom() {
        return kingdom;
    }
    
    public void setKingdom(String kingdom) {
        this.kingdom = kingdom;
    }
    
    public String getPhylum() {
        return phylum;
    }
    
    public void setPhylum(String phylum) {
        this.phylum = phylum;
    }
    
    public String getClasss() {
        return classs;
    }
    
    public void setClasss(String classs) {
        this.classs = classs;
    }
    
    public String getOrder() {
        return order;
    }
    
    public void setOrder(String order) {
        this.order = order;
    }
    
    public String getFamily() {
        return family;
    }
    
    public void setFamily(String family) {
        this.family = family;
    }
    
    public String getGenus() {
        return genus;
    }
    
    public void setGenus(String genus) {
        this.genus = genus;
    }
    
    public String getSpecificEpithet() {
        return specificEpithet;
    }
    
    public void setSpecificEpithet(String specificEpithet) {
        this.specificEpithet = specificEpithet;
    }
    
    public String getInfraspecificEpithet() {
        return infraspecificEpithet;
    }
    
    public void setInfraspecificEpithet(String infraspecificEpithet) {
        this.infraspecificEpithet = infraspecificEpithet;
    }
    
    public String getModified() {
        return modified;
    }
    
    public void setModified(String modified) {
        this.modified = modified;
    }
    
    public String getBasisOfRecord() {
        return basisOfRecord;
    }
    
    public void setBasisOfRecord(String basisOfRecord) {
        this.basisOfRecord = basisOfRecord;
    }
}



