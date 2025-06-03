package com.example.demo;

public class LabelValue {
    private String label;
    private String tag;
    private Object value;

    public LabelValue(String label, String tag, Object value) {
        this.label = label;
        this.tag = tag;
        this.value = value;
    }

    // Getters and setters
    public String getLabel() { return label; }
    public String getTag() { return tag; }
    public Object getValue() { return value; }

    public void setLabel(String label) { this.label = label; }
    public void setTag(String tag) { this.tag = tag; }
    public void setValue(Object value) { this.value = value; }
}
