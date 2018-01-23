package ge.hamamlo.pentaho.di.trans.steps.scraper.base;

public class FieldDef {
    public enum FieldType {
        STRING, NUMBER, BOOLEAN
    }

    private String name;
    private FieldType fieldType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
    }
}
