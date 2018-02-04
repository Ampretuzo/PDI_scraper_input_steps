package ge.hamamlo.pentaho.di.trans.steps.scraper.base;

public class ScraperInformationDisabled implements ScraperInformation {
    @Override
    public boolean alreadyProcessed(String url) {
        return false;   // never tell something is already processed
    }
}
