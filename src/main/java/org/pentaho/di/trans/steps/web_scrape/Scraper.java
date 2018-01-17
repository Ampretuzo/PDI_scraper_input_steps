package org.pentaho.di.trans.steps.web_scrape;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;

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

public class Scraper extends BaseStep implements StepInterface {

    public Scraper(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
        if (!super.init(smi, sdi) ) return false;
        // set up data interface
        ScraperMeta meta = (ScraperMeta) smi;
        ScraperData data = (ScraperData) sdi;
        /*RowMetaInterface inputRowMeta = getInputRowMeta();
        try {
            // inputRowMeta is not initialized
            meta.getFields( inputRowMeta, getStepname(), null, null, this, getRepository(), getMetaStore() );
        } catch (KettleStepException e) {
            e.printStackTrace();
            return false;
        }
        data.setOutputRowInterface(inputRowMeta);*/
        return true;
    }

    @Override
    public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
        ScraperData scraperData = (ScraperData) sdi;
        ScraperMeta scraperMeta = (ScraperMeta) smi;

        Object[] r = null;
        r = getRow();

        if ( r == null ) { // no more rows to be expected from the previous step(s)
            setOutputDone();
            return false;
        }

        if (first) {
            first = false;
            scraperData.setOutputRowInterface(getInputRowMeta().clone() );
            scraperData.getOutputRowInterface().addValueMeta(scraperMeta.getOutputFieldMetaInterface() );
        }

        String urlFieldName = scraperMeta.getUrlFieldName();
        String url = getInputRowMeta().getString(r, getInputRowMeta().indexOfValue(urlFieldName) );

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
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }


        JsonObject apiResponse = Json.parse(projectsJsonFromApi).asObject();
        List<String> projectUrls = new ArrayList<String>();
        Iterator<JsonValue> projectsIterator = apiResponse.get("projects").asArray().iterator();
        while (projectsIterator.hasNext() ) {
            JsonObject next = (JsonObject) projectsIterator.next();
            projectUrls.add("https://ec.europa.eu/eipp/desktop/en/projects/project-" + next.get("id") + ".html");
        }



        /*Document doc;
        try {
            doc = Jsoup.connect(url)
                    .userAgent("Mozilla")
                    .timeout(3000)
                    .get();
        } catch (IOException e) {
            logBasic("Could not connect to " + url);
            return false;
        }

        JsonArray projectsJson = new JsonArray();

        Elements projectNodes = doc.body().getElementById("projects").children();

        while (projectNodes.iterator().hasNext() ) {
            JsonObject projectJson = new JsonObject();
            Element projectNode = projectNodes.iterator().next();
            // timeline:list
            JsonObject timeline = new JsonObject();
            timeline.add("list", projectNode.child(5).child(0).text() );
            timeline.add("last_update", projectNode.child(6).child(0).text() );
            projectJson.add("timeline", timeline);
            projectsJson.add(projectJson);
        }

        Object[] rc = getInputRowMeta().cloneRow(r);
        RowDataUtil.resizeArray(rc, scraperData.getOutputRowInterface().size() );
        rc[scraperData.getOutputRowInterface().size() - 1] = projectsJson.toString();
        putRow(scraperData.getOutputRowInterface(), rc);
*/
        RowDataUtil.resizeArray(r, scraperData.getOutputRowInterface().size() );
        r[scraperData.getOutputRowInterface().size() - 1] = getAll(projectUrls);
        putRow(scraperData.getOutputRowInterface(), r);

        return true;
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
                logBasic("Could not connect to " + projectUrls.get(i) );
            }
            if (doc == null) continue;
            projectjson.add("pro_name", doc.body().getElementsByTag("h1").get(0).text() );

            projectsJson.add(projectjson);
        }
        return projectsJson.toString();
    }

}
