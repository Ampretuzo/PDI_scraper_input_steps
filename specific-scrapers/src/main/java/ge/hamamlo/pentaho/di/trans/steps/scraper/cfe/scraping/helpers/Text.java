package ge.hamamlo.pentaho.di.trans.steps.scraper.cfe.scraping.helpers;

public class Text {
    public static String clean(String text) {
        text = text.replace("\u00a0", "");

        return text.trim();
    }
}
