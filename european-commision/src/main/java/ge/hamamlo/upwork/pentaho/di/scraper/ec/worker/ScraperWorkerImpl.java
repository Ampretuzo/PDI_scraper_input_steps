package ge.hamamlo.upwork.pentaho.di.scraper.ec.worker;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import org.pentaho.di.trans.steps.web_scrape.Scraper;
import org.pentaho.di.trans.steps.web_scrape.ScraperWorker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import static org.pentaho.di.trans.steps.web_scrape.Scraper.MAX_CONNS_TO_SINGE_SERVER;

public class ScraperWorkerImpl implements ScraperWorker {

    public static final String PRO_NAME = "pro_name";
    public static final String TIMELINE = "timeline";
    public static final String FROM = "from";
    public static final String LIST = "list";
    public static final String LAST_UPDATE = "last_update";
    public static final String PRICE = "price";
    public static final String ORI = "ori";
    public static final String PROJECT = "project";
    public static final String STATUS = "status";
    public static final String REQUIREMENT_TYPE = "requirement_type";
    public static final String ORIGIN = "origin";
    public static final String LOCATION = "location";
    public static final String LOCATION2 = "location";
    public static final String INDUSTRY = "industry";
    public static final String ORI_INDUSTRY = "ori_industry";
    public static final String PROJECT_TYPE = "project_type";
    public static final String LANGUAGE = "language";
    public static final String DESCRIP = "descrip";
    public static final String PUBLISHER = "publisher";
    public static final String PUBLISHER_TYPE = "type";
    public static final String PUBLISHER_NAME = "name";
    public static final String PUBLISHER_ROLE = "role";
    public static final String PUBLISHER_DESCRIP = "descrip";
    public static final String SOURCE_URL = "source_url";
    public static final String PAGE_URL = "page_url";

    @Override
    public String scrapeUrl(final String url, final Scraper.LoggerForScraper logger) throws IOException {

        JsonObject apiResponse = Json.parse(getJsonFromApi(url, logger) ).asObject();
        Semaphore maxConnSemaphore = new Semaphore(MAX_CONNS_TO_SINGE_SERVER);
        int nProjects = apiResponse.get("projects").asArray().size();
        CountDownLatch latchUntilAllDone = new CountDownLatch(nProjects);

        List<Map<String, Object> > allData = new ArrayList<>();
        for (int i = 0; i < nProjects; i++) {
            JsonObject projectI = (JsonObject) apiResponse.get("projects").asArray().get(i);
            // necessary values from api
            String submitDateStr = projectI.getString("submitDateStr", "");
            String updateDateStr = projectI.getString("updateDateStr", "");
            String projectUrl = "https://ec.europa.eu/eipp/desktop/en/projects/project-" + projectI.get("id") + ".html";

            allData.add(new HashMap<String, Object>() );
            // maually populate allData with information from api
            // timeline:list
            if (submitDateStr != null) allData.get(i).put(LIST, submitDateStr);

            // timeline:last_update
            if (updateDateStr != null) allData.get(i).put(LAST_UPDATE, updateDateStr);

            Thread thread = new Thread(new ScraperWorkerWorker(maxConnSemaphore, latchUntilAllDone, logger, projectUrl, allData.get(i) ) );
            thread.start();
        }

        try {
            latchUntilAllDone.await();
        } catch (InterruptedException e) {
            logger.logBasic("Interrupted before all project urls were processed!");
            return null;
        }

        JsonArray targetJson = new JsonArray();
        for (Map<String, Object> projectData : allData) {
            JsonObject targetJsonProject = buildTargetJsonProject(projectData);
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
}
