package br.org.cria.splinkerapp.models;

import java.util.List;

public class DataSource {
    private List<String> fields;
    private DataSourceType type;


    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public DataSourceType getType() {
        return type;
    }

    public void setType(DataSourceType type) {
        this.type = type;
    }
}
