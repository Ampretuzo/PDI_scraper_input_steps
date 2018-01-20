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
    private final Map<String, Object> dataToPopulate;

    ScraperECWorker(Semaphore maxConnSemaphore, CountDownLatch latchUntilAllDone, ScraperBase.LoggerForScraper logger, String projectUrl, Map<String, Object> dataToPopulate) {
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

    private void getAllData(Map<String, Object> result, String projectUrl, ScraperBase.LoggerForScraper logger) {
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

        // pro_name
        result.put(PRO_NAME, doc.body().getElementsByTag("h1").get(0).text() );

        // source_url:page_url
        result.put(PAGE_URL, projectUrl);

        // timeline:from
        String from = getFrom(doc);
        if (from != null) result.put(FROM, from);

        // price:ori
        List<String> ori = new ArrayList<>();
        result.put(ORI, ori);
        String ori1 = getOri(doc);
        if (ori1 != null) ori.add(ori1);

        // project
        HashMap<String, Object> project = new HashMap<>();
        result.put(PROJECT, project);

        // project:status
        String status = getStatus(doc);
        if (status != null) project.put(STATUS, status);

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
        result.put(REQUIREMENT_TYPE, requirementType);

        // location:origin:location
        List<String> originLocations = getOriginLocations(doc);
        result.put(LOCATION, originLocations);

        // industry:ori_industry
        List<String> oriIndustry = getOriIndustries(doc);
        result.put(ORI_INDUSTRY, oriIndustry);

        // project:type
        String type = getType(doc);
        project.put(PROJECT_TYPE, type);

        // descrip
        List<String> descrip = getDescrip(doc);
        result.put(DESCRIP, descrip);

        // publisher
        HashMap<String, Object> publisher = new HashMap<>();
        result.put(PUBLISHER, publisher);

        Element projetPromoterContact = doc.body().getElementById("project-promoter-contact");
        if (projetPromoterContact != null) {
            // unsafe! might nullpointer at some point

            String publisherName = projetPromoterContact.child(1).text();
            publisher.put(PUBLISHER_NAME, publisherName);

            String publisherSingleRole = projetPromoterContact.child(2).text();
            List<String> publisherRole = new ArrayList<>();
            publisher.put(PUBLISHER_ROLE, publisherRole);
            publisherRole.add(publisherSingleRole);

            String publisherDescription = projetPromoterContact.child(3).text();
            publisher.put(PUBLISHER_DESCRIP, publisherDescription);
        }
    }

    private List<String> getDescrip(Document doc) {
        String separator = "\r\n\r\n";
        List<String> descriptions = new ArrayList<>();

        Elements first = doc.body().getElementsByClass("description");
        if (first.size() == 1) {
            Elements firstChildren = first.get(0).children();
            String firstDescription = "";
            Iterator<Element> iterator = firstChildren.iterator();
            while (iterator.hasNext() ) {
                firstDescription += descriptions.add(iterator.next().text());
                firstDescription += separator;
            }
            descriptions.add(firstDescription);
        }

        Element shortFacts = doc.body().getElementById("short-facts");

        // TODO: these might be absent from webpage
        String secondDescription = "";
        secondDescription += getShortFactsField(shortFacts, 2, 1, 1);
        secondDescription += separator;
        secondDescription += getShortFactsField(shortFacts, 2, 1, 2);
        descriptions.add(secondDescription);

        String thirdDescription = "";
        thirdDescription += getShortFactsField(shortFacts, 2, 2, 1);
        thirdDescription += separator;
        thirdDescription += getShortFactsField(shortFacts, 2, 2, 2);
        descriptions.add(thirdDescription);

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

    private String getShortFactsField(Element shortFacts, int coord1, int coord2, int coord3) {
        if (shortFacts == null) return null;
        try {   // its nice to have some safety around here, otherwise the thread would be killed with runtime exception
            return shortFacts.child(coord1).child(coord2).child(coord3).text();
        } catch (IndexOutOfBoundsException ioobe) {
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
        return getShortFactsField(shortFacts, 1, 2);
    }

    private String getShortFactsField(Element shortFacts, int coord1, int coord2) {
        return getShortFactsField(shortFacts, coord1, coord2, 2);
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
        return getShortFactsField(shortFacts, 2, 0);
    }

    private String getOri(Document doc) {
        Element shortFacts = doc.body().getElementById("short-facts");
        return getShortFactsField(shortFacts, 0, 2);
    }

    private String getFrom(Document doc) {
        Element shortFacts = doc.body().getElementById("short-facts");
        return getShortFactsField(shortFacts, 0, 1);
    }
}
