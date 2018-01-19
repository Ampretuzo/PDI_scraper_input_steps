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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ScraperWorkerImpl implements ScraperWorker {
    @Override
    public String scrapeUrl(final String url, final Scraper.LoggerForScraper logger) {
        String projectsJsonFromApi = null;
        try {
            URLConnection connection = new URL(url).openConnection();
            connection
                    .setRequestProperty("User-Agent",
                            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
            connection.connect();

            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), Charset.forName("UTF-8")));

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            projectsJsonFromApi = sb.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (projectsJsonFromApi == null) return null;   // safety first TODO: log what happened

        JsonObject apiResponse = Json.parse(projectsJsonFromApi).asObject();
//        List<String> projectUrls = new ArrayList<String>();
        JsonArray targetJson = new JsonArray();
        Iterator<JsonValue> projectsIterator = apiResponse.get("projects").asArray().iterator();
        int counter = 0;
        while (projectsIterator.hasNext() && counter < 5 ) {
            counter ++;
            JsonObject next = (JsonObject) projectsIterator.next();
//            projectUrls.add("https://ec.europa.eu/eipp/desktop/en/projects/project-" + next.get("id") + ".html");
            String submitDateStr = next.getString("submitDateStr", "");
            String updateDateStr = next.getString("updateDateStr", "");
            String projectUrl = "https://ec.europa.eu/eipp/desktop/en/projects/project-" + next.get("id") + ".html";
            JsonObject targetJsonProject = createProjectJson(projectUrl, submitDateStr, updateDateStr);
            if (targetJsonProject == null) continue;
            targetJson.add(targetJsonProject);
        }
//        return getAll(projectUrls);
        return targetJson.toString();
    }

    private JsonObject createProjectJson(String projectUrl, String submitDateStr, String updateDateStr) {
        JsonObject timeline = new JsonObject()
                .add("list", submitDateStr)
                .add("last_update", updateDateStr);
        JsonObject sourceUrl = new JsonObject()
                .add("page_url", projectUrl);
        JsonObject targetJsonProject = new JsonObject()
                .add("timeline", timeline)
                .add("source_url", sourceUrl)
                .add("price", new JsonObject().add("ori", new JsonArray() ) )
                .add("project", new JsonObject().add("language", new JsonArray() ) )
                .add("location", new JsonObject().add("origin", new JsonObject().add("location", new JsonArray() ) ) )
                .add("industry", new JsonObject().add("ori_industry", new JsonArray() ) )
                .add("descrip", new JsonArray() )
                .add("publisher", new JsonObject().add("role", new JsonArray() ) );
        // this is dirty
        scrapeSpecificProject(targetJsonProject, projectUrl);
        return targetJsonProject;
    }

    private void scrapeSpecificProject(JsonObject targetJsonProject, String projectUrl) {
        Document doc = null;
        try {
            doc = Jsoup.connect(projectUrl)
                    .userAgent("Mozilla")
                    .timeout(3000)
                    .get();
        } catch (IOException e) {
            // TODO: adapter is needed for log
//                logBasic("Could not connect to " + projectUrls.get(i) );
            return;
        }

        targetJsonProject.add("pro_name", doc.body().getElementsByTag("h1").get(0).text() );

        String from = getFrom(doc);
        ( (JsonObject) targetJsonProject.get("timeline") ).add("from", from);

        String ori1 = getOri(doc);
        JsonObject price = (JsonObject) targetJsonProject.get("price");
        ( (JsonArray) price.get("ori") ).add(ori1);

        String status = getStatus(doc);
        ( (JsonObject) targetJsonProject.get("project") ).add("status", status);

        List<String> originLocations = getOriginLocations(doc);
        for (int i = 0; i < originLocations.size(); i++) {
            ( (JsonArray) ( (JsonObject) ( (JsonObject) targetJsonProject.get("location") ).get("origin") ).get("location") ).add(originLocations.get(i) );
        }

        List<String> oriIndustries = getOriIndustries(doc);
        for (int i = 0; i < oriIndustries.size(); i++) {
            ( (JsonArray) ( (JsonObject) targetJsonProject.get("industry") ).get("ori_industry") ).add(oriIndustries.get(i) );
        }

        if (status != null) {
            targetJsonProject.add(
                    "requirement_type",
                    (status.equals("planning") ||
                            status.equals("(pre)feasibility") ||
                            status.equals("structuring") ||
                            status.equals("procurement")) ? "attraction" : "financing"
            );
        }

        String type = getType(doc);
        ( (JsonObject) targetJsonProject.get("project") ).add("type", type);

        ( (JsonArray) ( (JsonObject) targetJsonProject.get("project") ).get("language") ).add("en");    // hard coded

        List<String> descrip = getDescrip(doc);
        for (int i = 0; i < descrip.size(); i++) {
            ( (JsonArray) targetJsonProject.get("descrip") ).add(descrip.get(i) );
        }

        String publisherType = "Project promoter";
        ( (JsonObject) targetJsonProject.get("publisher") ).add("type", publisherType);

        Element projetPromoterContact = doc.body().getElementById("project-promoter-contact");
        if (projetPromoterContact != null) {
            // unsafe for the moment
            String publisherName = projetPromoterContact.child(1).text();
            ( (JsonObject) targetJsonProject.get("publisher") ).add("name", publisherName);

            String publisherSingleRole = projetPromoterContact.child(2).text();
            ((JsonArray) ((JsonObject) targetJsonProject.get("publisher") ).get("role") ).add(publisherSingleRole);

            String publisherDescription = projetPromoterContact.child(3).text();
            ( (JsonObject) targetJsonProject.get("publisher") ).add("descrip", publisherDescription);
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
        if (shortFacts == null) return null;
        try {   // its nice to have some safety around here, otherwise the thread would be killed with runtime exception
            return shortFacts.child(coord1).child(coord2).child(2).text();
        } catch (IndexOutOfBoundsException ioobe) {
            return null;
        }
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
        if (shortFacts == null) return null;
        try {   // its nice to have some safety around here, otherwise the thread would be killed with runtime exception
            return shortFacts.child(2).child(0).child(2).text();
        } catch (IndexOutOfBoundsException ioobe) {
            return null;
        }
    }

    private String getOri(Document doc) {
        Element shortFacts = doc.body().getElementById("short-facts");
        if (shortFacts == null) return null;
        try {   // its nice to have some safety around here, otherwise the thread would be killed with runtime exception
            return shortFacts.child(0).child(2).child(2).text();
        } catch (IndexOutOfBoundsException ioobe) {
            return null;
        }
    }

    private String getFrom(Document doc) {
        Element shortFacts = doc.body().getElementById("short-facts");
        if (shortFacts == null) return null;
        try {   // its nice to have some safety around here, otherwise the thread would be killed with runtime exception
            return shortFacts.child(0).child(1).child(2).text();
        } catch (IndexOutOfBoundsException ioobe) {
            return null;
        }
    }

    /*private String getAll(List<String> projectUrls) {
        JsonArray projectsJson = new JsonArray();
        for (int i = 0; i < projectUrls.size() && i < 10 *//*this is to reduce redeploy iteration*//* ; i++) {
            JsonObject projectjson = new JsonObject();
            Document doc = null;
            try {
                doc = Jsoup.connect(projectUrls.get(i))
                        .userAgent("Mozilla")
                        .timeout(3000)
                        .get();
            } catch (IOException e) {
//                logBasic("Could not connect to " + projectUrls.get(i) );
            }
            if (doc == null) continue;
            projectjson.add("pro_name", doc.body().getElementsByTag("h1").get(0).text() );

            projectsJson.add(projectjson);
        }
        return projectsJson.toString();
    }*/
}
