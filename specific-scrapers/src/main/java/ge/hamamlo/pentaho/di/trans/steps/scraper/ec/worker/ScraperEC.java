package ge.hamamlo.pentaho.di.trans.steps.scraper.ec.worker;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import ge.hamamlo.pentaho.di.trans.steps.scraper.base.FieldDef;
import ge.hamamlo.pentaho.di.trans.steps.scraper.base.Scraper;
import ge.hamamlo.pentaho.di.trans.steps.scraper.base.ScraperBase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import static ge.hamamlo.pentaho.di.trans.steps.scraper.base.ScraperBase.MAX_CONNS_TO_SINGE_SERVER;

public class ScraperEC implements Scraper {
    private final static String[] fields = {
            "timeline_list",
            "timeline_update",
            "timeline_from",
            "page_url",
            "price",
            "project_type",
            "project_status",
            "project_language",
            "origin_location_1",
            "origin_location_2",
            "origin_location_3",
            "origin_location_4",
            "industry1",
            "industry2",
            "industry3",
            "industry4",
            "industry5",
            "requirement_type",
            "descrip1",
            "descrip2",
            "descrip3",
            "descrip4",
            "descrip5",
            "descrip6",
            "descrip7",
            "publisher_type",
            "publisher_name",
            "publisher_role",
            "publisher_descrip",
            "pro_name",
            "timeline_insert"
    };

    // utility method
    static int getIndexForFieldName(String fieldName) {
        for (int i = 0; i < fields.length; i++) {
            String fieldNameOther = fields[i];
            if (fieldNameOther.equalsIgnoreCase(fieldName) ) return i;
        }
        throw new RuntimeException("There is no such field as - " + fieldName);
    }

    // utility method
    public static int getIndexForFieldName(String fieldName, String[] fields) {
        for (int i = 0; i < fields.length; i++) {
            String fieldNameOther = fields[i];
            if (fieldNameOther.equalsIgnoreCase(fieldName) ) return i;
        }
        throw new RuntimeException("There is no such field as - " + fieldName);
    }

    public static FieldDef[] createAllStringFieldDefs(String[] fields) {
        FieldDef[] stringFieldDefs = new FieldDef[fields.length];
        for (int i = 0; i < fields.length; i++) {
            String fieldName = fields[i];
            stringFieldDefs[i] = new FieldDef();
            stringFieldDefs[i].setFieldType(FieldDef.FieldType.STRING);
            stringFieldDefs[i].setName(fieldName);
        }
        return stringFieldDefs;
    }

    @Override
    public FieldDef[] fields() {
        FieldDef[] allStringFieldDefs = createAllStringFieldDefs(fields);
        allStringFieldDefs[getIndexForFieldName("timeline_insert") ].setFieldType(FieldDef.FieldType.DATE);
        return allStringFieldDefs;
    }

    @Override
    public void scrapeUrl(final String url, final ScraperBase.LoggerForScraper logger, ScraperBase.ScraperOutput output) throws IOException {
        JsonObject apiResponse = Json.parse(getJsonFromApi(url, logger) ).asObject();
        Semaphore maxConnSemaphore = new Semaphore(MAX_CONNS_TO_SINGE_SERVER);
        int nProjects = apiResponse.get("projects").asArray().size();
        CountDownLatch latchUntilAllDone = new CountDownLatch(nProjects);

        List<Object[] > allData = new ArrayList<>();
        for (int i = 0; i < nProjects; i++) {
            JsonObject projectI = (JsonObject) apiResponse.get("projects").asArray().get(i);
            // necessary values from api
            String submitDateStr = projectI.getString("submitDateStr", "");
            String updateDateStr = projectI.getString("updateDateStr", "");
            String projectUrl = "https://ec.europa.eu/eipp/desktop/en/projects/project-" + projectI.get("id") + ".html";

            allData.add(new Object[fields.length] );
            // maually populate allData with information from api
            // timeline:list
            if (submitDateStr != null) allData.get(i)[getIndexForFieldName("timeline_list") ] = submitDateStr;

            // timeline:last_update
            if (updateDateStr != null) allData.get(i)[getIndexForFieldName("timeline_update") ] = updateDateStr;

            Thread thread = new Thread(new ScraperECWorker(maxConnSemaphore, latchUntilAllDone, logger, projectUrl, allData.get(i) ) );
            thread.start();
        }

        try {
            // wait for all threads to finish
            latchUntilAllDone.await();
        } catch (InterruptedException e) {
            logger.logBasic("Interrupted before all project urls were processed!");
            return;
        }

        for (Object[] projectData : allData) {
            if (projectData == null) continue;
            // This is how the data is sent to output
            output.yield(projectData);
        }
        // send null at the end to signal finish
        output.yield(null);
    }

    private String getJsonFromApi(String url, ScraperBase.LoggerForScraper logger) throws IOException {
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
