package ge.hamamlo.pentaho.di.trans.steps.scraper.ec.worker;

import ge.hamamlo.pentaho.di.trans.steps.scraper.base.ScraperBase;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import static ge.hamamlo.pentaho.di.trans.steps.scraper.ec.worker.ScraperEC.*;

public class ScraperECWorker implements Runnable {
    private final Semaphore maxConnSemaphore;
    private final CountDownLatch latchUntilAllDone;
    private final ScraperBase.LoggerForScraper logger;
    private final String projectUrl;
    private final Object[] dataToPopulate;

    ScraperECWorker(Semaphore maxConnSemaphore, CountDownLatch latchUntilAllDone, ScraperBase.LoggerForScraper logger, String projectUrl, Object[] dataToPopulate) {
        this.maxConnSemaphore = maxConnSemaphore;
        this.latchUntilAllDone = latchUntilAllDone;
        this.logger = logger;
        this.projectUrl = projectUrl;
        this.dataToPopulate = dataToPopulate;
    }

    @Override
    public void run() {
        try {
            maxConnSemaphore.acquire();
        } catch (InterruptedException e) {
            logger.logBasic("Interrupted... stopped processing " + projectUrl);
            latchUntilAllDone.countDown();
        }
        logger.logBasic("Started processing " + projectUrl + "!");
        logger.logBasic("Available semaphores: " + maxConnSemaphore.availablePermits() );

        getAllData(dataToPopulate, projectUrl, logger);

        latchUntilAllDone.countDown();
    }

    private void getAllData(Object[] result, String projectUrl, ScraperBase.LoggerForScraper logger) {
        Document doc = null;
        try {
            doc = Jsoup.connect(projectUrl)
                    .userAgent("Mozilla")
                    .timeout(3000)
                    .get();
        } catch (IOException e) {
            logger.logBasic("Could not retrieve data from " + projectUrl);
            return;
        } finally {
            maxConnSemaphore.release();
        }

        // publisher:type
        result[getIndexForFieldName("publisher_type") ] = "Project promoter";

        result[getIndexForFieldName("project_language") ] = "en";

        // pro_name
        result[getIndexForFieldName("pro_name") ] = doc.body().getElementsByTag("h1").get(0).text();

        // source_url:page_url
        result[getIndexForFieldName("page_url")] = projectUrl;

        // timeline:from
        String from = getFrom(doc);
        if (from != null) result[getIndexForFieldName("timeline_from") ] = from;

        // price:ori
        String ori1 = getOri(doc);
        if (ori1 != null) result[getIndexForFieldName("price") ] = ori1;

        // project:status
        String status = getStatus(doc);
        result[getIndexForFieldName("project_status") ] = status;

        // requirement_type
        String requirementType = null;
        if ("planning".equalsIgnoreCase(status) ||
                "(pre)feasibility".equalsIgnoreCase(status) ||
                "structuring".equalsIgnoreCase(status) ||
                "procurement".equalsIgnoreCase(status) ) {
            requirementType = "attraction";
        } else {
            requirementType = "financing";
        }
        result[getIndexForFieldName("requirement_type") ] = requirementType;

        // location:origin:location
        List<String> originLocations = getOriginLocations(doc);
        setArrayFieldsWithOffset(result, originLocations, 1, "origin_location_");

        // industry:ori_industry
        List<String> oriIndustry = getOriIndustries(doc);
        setArrayFieldsWithOffset(result, oriIndustry, 1, "industry");

        // project:type
        String type = getType(doc);
        result[getIndexForFieldName("project_type") ] = type;

        // descrip
        List<String> descrip = getDescrip(doc);
        setArrayFieldsWithOffset(result, descrip, 1, "descrip");

        Element projectPromoterContact = doc.body().getElementById("project-promoter-contact");
        if (projectPromoterContact != null) {
            Elements paragraphs = projectPromoterContact.getElementsByTag("p");

            // first paragraph must be promoter name
            String publisherName = paragraphs.get(0).text();
            result[getIndexForFieldName("publisher_name") ] = publisherName;

            // second paragraph must be a promoter role
            String publisherSingleRole = paragraphs.get(1).text();
            result[getIndexForFieldName("publisher_role") ] = publisherSingleRole;

            // if 3rd paragraph is present, then it must be publisher description:
            if (paragraphs.size() > 2) {
                String publisherDescription = paragraphs.get(2).text();
                result[getIndexForFieldName("publisher_descrip") ] = publisherDescription;
            }
        }
    }

    private void setArrayFieldsWithOffset(Object[] result, List<String> values, int offset, String fieldNameBase) {
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            String fieldName = fieldNameBase + (i + offset);
            result[getIndexForFieldName(fieldName) ] = value;
        }
    }

    private List<String> getDescrip(Document doc) {
        String separator = "\r\n\r\n";
        List<String> descriptions = new ArrayList<>();

        Elements first = doc.body().getElementsByClass("description");
        if (first.size() == 1) {
            descriptions.add(first.get(0).text() );
        }

        Element shortFacts = doc.body().getElementById("short-facts");

        String secondDescription = "";
        String revenueType = getShortFactsFieldName(shortFacts, 2, 1);
        String revenueTypeValue = getShortFactsFieldValue(shortFacts, 2, 1);
        if (revenueType != null) {
            secondDescription += revenueType;   // must return 'revenue type'
            secondDescription += separator;
            secondDescription += revenueTypeValue;
            descriptions.add(secondDescription);
        }

        String thirdDescription = "";
        String jobsCreated = getShortFactsFieldName(shortFacts, 2, 2);
        String jobsCreatedValue = getShortFactsFieldValue(shortFacts, 2, 2);
        if (jobsCreated != null) {
            thirdDescription += jobsCreated;
            thirdDescription += separator;
            thirdDescription += jobsCreatedValue;
            descriptions.add(thirdDescription);
        }

        Element contentFacts = doc.body().getElementById("content-facts");
        if (contentFacts != null) {
            Elements contentFactsChildren = contentFacts.children();
            for (int i = 0; i < contentFactsChildren.size(); i++) {
                Element element =  contentFactsChildren.get(i);
                if (element.tagName().equals("h3") ) {
                    descriptions.add(element.text() + separator);
                } else if(element.tagName().equals("p") ) {
                    descriptions.set(
                            descriptions.size() - 1,
                            descriptions.get(descriptions.size() - 1) + element.text() + separator
                    );
                }
            }
        }

        return descriptions;
    }

    private String getShortFactsFieldName(Element shortFacts, int coord1, int coord2) {
        if (shortFacts == null) return null;
        try {   // its nice to have some safety around here, otherwise the thread would be killed with runtime exception
            return shortFacts.child(coord1).child(coord2).getElementsByTag("h3").get(0).text();
        } catch (IndexOutOfBoundsException | NullPointerException bad) {
            return null;
        }
    }

    private String getShortFactsFieldValue(Element shortFacts, int coord1, int coord2) {
        if (shortFacts == null) return null;
        try {   // its nice to have some safety around here, otherwise the thread would be killed with runtime exception
            return shortFacts.child(coord1).child(coord2).getElementsByTag("p").get(0).text();
        } catch (IndexOutOfBoundsException | NullPointerException bad) {
            return null;
        }
    }

    private List<String> getShortFactsListField(Element shortFacts, int coord1, int coord2) {
        List<String> result = new ArrayList<>();
        if (shortFacts == null) return result;
        try {   // its nice to have some safety around here, otherwise the thread would be killed with runtime exception
            Element parent = shortFacts.child(coord1).child(coord2);
            for (int i = 2; i < parent.children().size(); i++) {
                result.add(parent.child(i).text() );
            }
        } catch (IndexOutOfBoundsException ioobe) {
            ioobe.printStackTrace();
        }
        return result;
    }

    private String getType(Document doc) {
        Element shortFacts = doc.body().getElementById("short-facts");
        return getShortFactsFieldValue(shortFacts, 1, 2);
    }

    private List<String> getOriIndustries(Document doc) {
        Element shortFacts = doc.body().getElementById("short-facts");
        List<String> result = new ArrayList<>();
        if (shortFacts == null) return result;
        try {   // its nice to have some safety around here, otherwise the thread would be killed with runtime exception
            Element parent = shortFacts.child(1).child(0);
            for (int i = 2; i < parent.children().size(); i++) {
                result.add(parent.child(i).text() );
            }
        } catch (IndexOutOfBoundsException ioobe) {
            ioobe.printStackTrace();
        }
        return result;
    }

    private List<String> getOriginLocations(Document doc) {
        Element shortFacts = doc.body().getElementById("short-facts");
        List<String> result = new ArrayList<>();
        if (shortFacts == null) return result;
        try {   // its nice to have some safety around here, otherwise the thread would be killed with runtime exception
            Element parent = shortFacts.child(0).child(0);
            for (int i = 2; i < parent.children().size(); i++) {
                result.add(parent.child(i).text() );
            }
        } catch (IndexOutOfBoundsException ioobe) {
            ioobe.printStackTrace();
        }
        return result;
    }

    private String getStatus(Document doc) {
        Element shortFacts = doc.body().getElementById("short-facts");
        return getShortFactsFieldValue(shortFacts, 2, 0);
    }

    private String getOri(Document doc) {
        Element shortFacts = doc.body().getElementById("short-facts");
        return getShortFactsFieldValue(shortFacts, 0, 2);
    }

    private String getFrom(Document doc) {
        Element shortFacts = doc.body().getElementById("short-facts");
        return getShortFactsFieldValue(shortFacts, 0, 1);
    }
}
