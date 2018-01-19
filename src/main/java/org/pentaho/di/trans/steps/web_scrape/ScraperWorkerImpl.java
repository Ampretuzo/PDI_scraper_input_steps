package org.pentaho.di.trans.steps.web_scrape;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.*;

public class ScraperWorkerImpl implements ScraperWorker {

    private static final String PRO_NAME = "pro_name";
    private static final String TIMELINE = "timeline";
    private static final String FROM = "from";
    private static final String LIST = "list";
    private static final String LAST_UPDATE = "last_update";
    private static final String PRICE = "price";
    private static final String ORI = "ori";
    private static final String PROJECT = "project";
    private static final String STATUS = "status";
    private static final String REQUIREMENT_TYPE = "requirement_type";
    private static final String ORIGIN = "origin";
    private static final String LOCATION = "location";
    private static final String LOCATION2 = "location";
    private static final String INDUSTRY = "industry";
    private static final String ORI_INDUSTRY = "ori_industry";
    private static final String PROJECT_TYPE = "project_type";
    private static final String LANGUAGE = "language";
    private static final String DESCRIP = "descrip";
    private static final String PUBLISHER = "publisher";
    private static final String PUBLISHER_TYPE = "type";
    private static final String PUBLISHER_NAME = "name";
    private static final String PUBLISHER_ROLE = "role";
    private static final String PUBLISHER_DESCRIP = "descrip";
    private static final String SOURCE_URL = "source_url";
    private static final String PAGE_URL = "page_url";

    @Override
    public String scrapeUrl(final String url, final Scraper.LoggerForScraper logger) throws IOException {
        String projectsJsonFromApi = getJsonFromApi(url, logger);

        JsonObject apiResponse = Json.parse(projectsJsonFromApi).asObject();
        JsonArray targetJson = new JsonArray();
        Iterator<JsonValue> projectsIterator = apiResponse.get("projects").asArray().iterator();
        int counter = 0;
        while (projectsIterator.hasNext() && counter < 10 ) {
            counter ++;
            JsonObject next = (JsonObject) projectsIterator.next();
            // put in only necessary values
            String submitDateStr = next.getString("submitDateStr", "");
            String updateDateStr = next.getString("updateDateStr", "");
            String projectUrl = "https://ec.europa.eu/eipp/desktop/en/projects/project-" + next.get("id") + ".html";
            Map<String, Object> allData = getAllData(submitDateStr, updateDateStr, projectUrl, logger);
            JsonObject targetJsonProject = buildTargetJsonProject(allData);
            if (targetJsonProject == null) continue;
            targetJson.add(targetJsonProject);
        }
        return targetJson.toString();
    }

    private JsonObject buildTargetJsonProject(Map<String, Object> allData) {
        // NOTE: map structure does not necessarily match object structure
        if (allData == null) return null;

        String projType = (String) ( (Map<String, Object>) allData.get(PROJECT) ).get(PROJECT_TYPE);
        String projStatus = (String) ( (Map<String, Object>) allData.get(PROJECT) ).get(STATUS);
        JsonObject project = new JsonObject()
                .add(PROJECT_TYPE, projType)
                .add(STATUS, projStatus)
                .add(LANGUAGE, new JsonArray().add("en") );

        JsonObject location = new JsonObject()
                .add(ORIGIN, new JsonObject()
                        .add(LOCATION2, createJsonArrayFromList( (List<String>) allData.get(LOCATION) ) )
                );

        JsonObject industry = new JsonObject()
                .add(ORI_INDUSTRY, createJsonArrayFromList((List<String>) allData.get(ORI_INDUSTRY) ) );

        JsonArray descrip = createJsonArrayFromList( (List<String>) allData.get(DESCRIP));

        Map<String, Object> publisherData = (Map<String, Object>) allData.get(PUBLISHER);
        JsonObject publisher = new JsonObject()
                .add(PUBLISHER_TYPE, "Project promoter")
                .add(PUBLISHER_NAME, (String) publisherData.get(PUBLISHER_NAME))
                .add(PUBLISHER_DESCRIP, (String) publisherData.get(PUBLISHER_DESCRIP))
                .add(PUBLISHER_ROLE, createJsonArrayFromList((List<String>) publisherData.get(PUBLISHER_ROLE)) );

        JsonObject targetProject = new JsonObject()
                .add(TIMELINE, new JsonObject()
                        .add(LIST, (String) allData.get(LIST) )
                        .add(LAST_UPDATE, (String) allData.get(LAST_UPDATE) )
                )
                .add(SOURCE_URL, new JsonObject()
                        .add(PAGE_URL, (String) allData.get(PAGE_URL) )
                ).add(PRICE, new JsonObject().add(ORI, createJsonArrayFromList( (List<String>) allData.get(ORI) ) ) )
                .add(PROJECT, project)
                .add(LOCATION, location)
                .add(INDUSTRY, industry)
                .add(REQUIREMENT_TYPE, (String) allData.get(REQUIREMENT_TYPE) )
                .add(DESCRIP, descrip)
                .add(PUBLISHER, publisher)
                ;

        return targetProject;
    }

    private JsonArray createJsonArrayFromList(List<String> strings) {
        if (strings == null) return null;
        JsonArray res = new JsonArray();
        for (int i = 0; i < strings.size(); i++) {
            String s = strings.get(i);
            res.add(s);
        }
        return res;
    }

    private Map<String, Object> getAllData(String submitDateStr, String updateDateStr, String projectUrl, Scraper.LoggerForScraper logger) {
        Document doc = null;
        try {
            doc = Jsoup.connect(projectUrl)
                    .userAgent("Mozilla")
                    .timeout(3000)
                    .get();
        } catch (IOException e) {
            logger.logBasic("Could not retrieve data from " + projectUrl);
            return null;
        }

        Map<String, Object> result = new HashMap<>();

        // pro_name
        result.put(PRO_NAME, doc.body().getElementsByTag("h1").get(0).text() );

        // source_url:page_url
        result.put(PAGE_URL, projectUrl);

        // timeline:from
        String from = getFrom(doc);
        if (from != null) result.put(FROM, from);

        // timeline:list
        if (submitDateStr != null) result.put(LIST, submitDateStr);

        // timeline:last_update
        if (updateDateStr != null) result.put(LAST_UPDATE, updateDateStr);

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
        if ("planning".equals(status) ||
                "(pre)feasibility".equals(status) ||
                "structuring".equals(status) ||
                "procurement".equals(status) ) {
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

        return result;
    }

    private String getJsonFromApi(String url, Scraper.LoggerForScraper logger) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection
                .setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        connection.connect();
        logger.logBasic("Connected to " + url);

        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), Charset.forName("UTF-8")));

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
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
