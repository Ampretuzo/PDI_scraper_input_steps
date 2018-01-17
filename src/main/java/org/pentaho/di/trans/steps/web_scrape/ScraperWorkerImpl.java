package org.pentaho.di.trans.steps.web_scrape;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

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
    public String scrapeUrl(String url) {
        String projectsJsonFromApi = null;
        try {
            String a = "https://ec.europa.eu/eipp/desktop/en/data/projects.json";
            URLConnection connection = new URL(a).openConnection();
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


        JsonObject apiResponse = Json.parse(projectsJsonFromApi).asObject();
        List<String> projectUrls = new ArrayList<String>();
        Iterator<JsonValue> projectsIterator = apiResponse.get("projects").asArray().iterator();
        while (projectsIterator.hasNext() ) {
            JsonObject next = (JsonObject) projectsIterator.next();
            projectUrls.add("https://ec.europa.eu/eipp/desktop/en/projects/project-" + next.get("id") + ".html");
        }
        return getAll(projectUrls);
    }

    private String getAll(List<String> projectUrls) {
        JsonArray projectsJson = new JsonArray();
        for (int i = 0; i < projectUrls.size(); i++) {
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
    }
}
