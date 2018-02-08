package ge.hamamlo.pentaho.di.trans.steps.scraper.cfe.scraping.helpers;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JsoupDocument {
    private static Map<String, String> cookies = new HashMap<>();
    private static String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36";

    public static Document connect(String url) {
        return connect(url, Connection.Method.GET, new HashMap<>());
    }

    public static Document connect(String url, Connection.Method method, Map<String, String> data) {
        Document document = null;
        while (document == null) {
            try {
                Connection.Response response = Jsoup.connect(url)
                        .timeout(15000)
                        .userAgent(USER_AGENT)
                        .ignoreContentType(true)
                        .maxBodySize(0)
                        .method(method)
                        .data(data)
                        .cookies(cookies)
                        .execute();

                cookies = response.cookies();

                document = response.parse();
            } catch (IOException e) {
                if (e instanceof HttpStatusException) {
                    int statusCode = ((HttpStatusException) e).getStatusCode();
                    if (statusCode == 404 || statusCode == 500) {
                        System.out.println(statusCode + " " + url);
                        return null;
                    }
                } else {
                    e.printStackTrace();
                }

                fetchAgain(3000, url);
            }
        }

        return document;
    }

    private static void fetchAgain(long milliseconds, String url) {
        System.out.println("fetch again " + url);
        sleep(milliseconds);
    }

    private static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
