package br.org.cria.splinkerapp.models;

import java.util.Map;

public class DataSource {
    private Map<String, String> fields;
    private DataSourceType type;


    public Map<String, String> getFields() {
        return fields;
    }

    public void addField(String darwinCoreField, String dataSourceField) {
        this.fields.put(darwinCoreField,dataSourceField);
    }

    public DataSourceType getType() {
        return type;
    }

    public void setType(DataSourceType type) {
        this.type = type;
    }
}
