package ge.hamamlo.pentaho.di.trans.steps.scraper.cfe.scraping.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Industry {
    private String name;
    private List<String> sizeCriteria = new ArrayList<>();
    private String link;
    private String normalName;

    public Industry() {
    }

    public Industry(String name, String normalName, String... sizeCriteria) {
        this.name = name;
        this.normalName = normalName;
        this.sizeCriteria.addAll(Arrays.asList(sizeCriteria));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getSizeCriteria() {
        return sizeCriteria;
    }

    public void setSizeCriteria(List<String> sizeCriteria) {
        this.sizeCriteria = sizeCriteria;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getNormalName() {
        return normalName;
    }
}
