package ge.hamamlo.pentaho.di.trans.steps.scraper.base;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;

public class ScraperInformationDb implements ScraperInformation {

    private final MongoCollection collection;

    public ScraperInformationDb(final MongoCollection collection) {
        this.collection = collection;
    }
    @Override
    public boolean alreadyProcessed(String url) {
        BasicDBObject findQuery = new BasicDBObject();
        findQuery.put("source_url.page_url", url);
        FindIterable findIterable = collection.find(findQuery);
        return findIterable.first() != null;
    }
}
