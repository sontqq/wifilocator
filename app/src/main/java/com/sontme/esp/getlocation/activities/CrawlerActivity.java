package com.sontme.esp.getlocation.activities;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
import com.sontme.esp.getlocation.R;
import com.sontme.esp.getlocation.SontHelper;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class CrawlerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crawler);

        String URL = "http://888.hu";


        Intent iin = getIntent();
        Bundle b = iin.getExtras();
        if (b != null) {
            String j = (String) b.get("site");
            URL = j;
        }

        System.setProperty("http.keepAlive", "false");
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("sun.net.client.defaultConnectTimeout", "1000");
        System.setProperty("sun.net.client.defaultReadTimeout", "1000");

        Crawler c = new Crawler(getApplicationContext());
        c.START_URL = URL;
        c.toVisit2.add(URL);
        c.execute(URL);

        TextView stats = findViewById(R.id.crawlerstats);
        ProgressBar progress = findViewById(R.id.crawler_progress);

        stats.setMovementMethod(new ScrollingMovementMethod());

        Handler handler = new Handler();
        Runnable run = new Runnable() {
            @Override
            public void run() {
                if (c.status != null)
                    stats.setText(Html.fromHtml(c.status));
                progress.setMax(c.toVisit2.size());
                progress.setProgress(c.wasVisited.size());
                handler.postDelayed(this, 200);
            }
        };
        handler.post(run);


    }

}

class Crawler extends AsyncTask<String, Integer, String> {
    Context ctx;
    public String status;

    public Queue<String> toVisit2 = new LinkedList<>();
    public ArrayList<String> wasVisited = new ArrayList<>();
    public ArrayList<String> badUrl = new ArrayList<>();
    public ArrayList<String> found_email = new ArrayList<>();
    public static Map<String, Integer> mimes = new HashMap<String, Integer>();
    public static Map<String, Integer> http_s = new HashMap<String, Integer>();
    public static Map<String, Integer> domains = new HashMap<String, Integer>();


    public Crawler(Context ctx) {
        this.ctx = ctx;
        System.setProperty("http.keepAlive", "false");
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("sun.net.client.defaultConnectTimeout", "1000");
        System.setProperty("sun.net.client.defaultReadTimeout", "1000");
    }



    String currentUrl;
    String currentTitle;
    double currentSize;
    double bandwidth = 0;
    int notMime = 0;


    public String START_URL;

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (values[1] != null) {
            status = "progress: " + values[0] + " / " + values[1];
            status = status + "<br><br>" + "<b>Title:</b> " + currentTitle;
            status = status + "<br><br>" + "<b>URL:</b> " + currentUrl;
            status = status + "<br><br>" + "<b>Size:</b> " + currentSize;
            status = status + "<br><br>" + "<b>Found emails:</b> " + found_email.size();
            status = status + "<br><br>" + "<b>Bandwidth:</b> " + round(bandwidth / 1024 / 1024, 2) + " mb";
            status = status + "<br><br>" + "<b>Bad URLs:</b> " + badUrl.size();
            status = status + "<br>";

            Object[] a = Crawler.mimes.entrySet().toArray();
            Arrays.sort(a, new Comparator() {
                public int compare(Object o1, Object o2) {
                    return ((Map.Entry<String, Integer>) o2).getValue()
                            .compareTo(((Map.Entry<String, Integer>) o1).getValue());
                }
            });
            status = status + "<br><b>Mimes: </b>";
            for (Object e : a) {
                status = status + "<br>" + ((Map.Entry<String, Integer>) e).getKey() + " : "
                        + ((Map.Entry<String, Integer>) e).getValue();
            }
            status = status + "<br><b>Bad Mimes: </b>" + notMime;
            status = status + "<br>";
            status = status + "<br>" + "<b>http:</b> " + http_s.get("http");
            status = status + "<br>" + "<b>https:</b> " + http_s.get("https");

            Object[] b = Crawler.domains.entrySet().toArray();
            Arrays.sort(b, new Comparator() {
                public int compare(Object o1, Object o2) {
                    return ((Map.Entry<String, Integer>) o2).getValue()
                            .compareTo(((Map.Entry<String, Integer>) o1).getValue());
                }
            });
            status = status + "<br><br><b>Domains: </b>";
            for (Object e : b) {
                status = status + "<br>" + ((Map.Entry<String, Integer>) e).getKey() + " : "
                        + ((Map.Entry<String, Integer>) e).getValue();
            }

        } else {
            status = "progress: " + values[0];
        }
    }

    @Override
    protected String doInBackground(String... strings) {
        while (!toVisit2.isEmpty()) {
            String x = toVisit2.poll();
            try {
                handleAll(getHTML_jsoup(x));
                publishProgress(wasVisited.size(), toVisit2.size());
            } catch (Exception e) {
                badUrl.add(x);
                e.printStackTrace();
            }
        }
        Log.d("CRAW_", "tovisit elfogyott (while end)");
        return null;
    }

    //region METHODS
    public double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public boolean recordEmail(String email) {
        try {
            AndroidNetworking.get("http://192.168.0.43/esp_crawler/crawler_email.php?source=" + currentUrl + "&email=" + email)
                    .setUserAgent("sont_wifilocator")
                    .setTag("save_record_http")
                    .setPriority(Priority.LOW)
                    .build()
                    .getAsString(new StringRequestListener() {
                        @Override
                        public void onResponse(String response) {
                            Log.d("CRAWLER_", "email recorded: " + response.length());
                        }

                        @Override
                        public void onError(ANError anError) {
                            Log.d("CRAWLER_", "email not recorded");
                            recordEmail(email);
                        }
                    });

        } catch (Exception e) {
            //e.printStackTrace();
            return false;
        }
        return true;
    }

    public String getHTML_exp(String urlToRead) {
        return null;
    }

    public String getHTML_jsoup(String urlToRead) {
        final Document[] doc = {null};
        try {
            String body = null;
            int redirCount = 0;

            int domaincount = domains.containsKey(SontHelper.getDomainFromURL(urlToRead)) ? domains.get(SontHelper.getDomainFromURL(urlToRead)) : 0;
            domains.put(SontHelper.getDomainFromURL(urlToRead), domaincount + 1);

            if (urlToRead.contains("http://")) {
                URL url = new URL(urlToRead);
                HttpURLConnection.setFollowRedirects(false);
                HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
                HttpURLConnection finalHttpConn = httpConn;

                try {
                    httpConn.setConnectTimeout(1000);
                    httpConn.setReadTimeout(1000);
                    httpConn.setUseCaches(false);
                    httpConn.setRequestMethod("GET");
                    httpConn.setInstanceFollowRedirects(false);
                    httpConn.setRequestProperty("Connection", "close");
                    HttpURLConnection.setFollowRedirects(false);

                    int count_s = http_s.containsKey("http") ? http_s.get("http") : 0;
                    http_s.put("http", count_s + 1);

                    int count = mimes.containsKey(httpConn.getContentType()) ? mimes.get(httpConn.getContentType()) : 0;
                    mimes.put(httpConn.getContentType(), count + 1);
                    if (wasVisited.size() > 5) {
                        if (!httpConn.getContentType().contains("text/html")) {
                            notMime++;
                            httpConn.disconnect();
                            return null;
                        }
                    }

                    boolean redirect = false;

                    int status = httpConn.getResponseCode();
                    if (status != HttpURLConnection.HTTP_OK) {
                        if (status == HttpURLConnection.HTTP_MOVED_TEMP
                                || status == HttpURLConnection.HTTP_MOVED_PERM
                                || status == HttpURLConnection.HTTP_SEE_OTHER)
                            redirect = true;
                    }

                    if (redirect) {
                        redirCount++;
                        String newUrl = httpConn.getHeaderField("Location");
                        String cookies = httpConn.getHeaderField("Set-Cookie");
                        httpConn.disconnect();
                        httpConn = (HttpURLConnection) new URL(newUrl).openConnection();
                        httpConn.setRequestProperty("Cookie", cookies);
                    }

                    InputStream inputStream = httpConn.getInputStream();
                    body = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    e.printStackTrace();
                    httpConn.getErrorStream().close();
                    httpConn.getInputStream().close();
                    httpConn.getOutputStream().close();
                } finally {
                    /*httpConn.disconnect();
                    httpConn.getInputStream().close();
                    httpConn.getOutputStream().close();*/
                }
            } else {
                URL url = new URL(urlToRead);
                HttpsURLConnection.setFollowRedirects(false);
                HttpsURLConnection httpConn = (HttpsURLConnection) url.openConnection();
                try {
                    httpConn.setUseCaches(false);
                    httpConn.setConnectTimeout(1000);
                    httpConn.setReadTimeout(1000);
                    httpConn.setRequestMethod("GET");
                    httpConn.setInstanceFollowRedirects(false);
                    httpConn.setRequestProperty("Connection", "close");
                    HttpsURLConnection.setFollowRedirects(false);

                    int count_s = http_s.containsKey("https") ? http_s.get("https") : 0;
                    http_s.put("https", count_s + 1);

                    int count = mimes.containsKey(httpConn.getContentType()) ? mimes.get(httpConn.getContentType()) : 0;
                    mimes.put(httpConn.getContentType(), count + 1);

                    if (wasVisited.size() > 5) {
                        if (!httpConn.getContentType().contains("text/html")) {
                            notMime++;
                            httpConn.disconnect();
                            return null;
                        }
                    }

                    boolean redirect = false;

                    int status = httpConn.getResponseCode();
                    if (status != HttpsURLConnection.HTTP_OK) {
                        if (status == HttpsURLConnection.HTTP_MOVED_TEMP
                                || status == HttpsURLConnection.HTTP_MOVED_PERM
                                || status == HttpsURLConnection.HTTP_SEE_OTHER)
                            redirect = true;
                    }

                    if (redirect) {
                        redirCount++;
                        String newUrl = httpConn.getHeaderField("Location");
                        String cookies = httpConn.getHeaderField("Set-Cookie");
                        httpConn.disconnect();
                        httpConn = (HttpsURLConnection) new URL(newUrl).openConnection();
                        httpConn.setRequestProperty("Cookie", cookies);
                    }

                    InputStream inputStream = httpConn.getInputStream();
                    body = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    httpConn.disconnect();
                    httpConn.getInputStream().close();
                    httpConn.getOutputStream().close();
                    e.printStackTrace();
                } finally {
                    /*
                    httpConn.disconnect();
                    httpConn.getInputStream().close();
                    httpConn.getOutputStream().close();
                    */
                }
            }

            doc[0] = Jsoup.parse(body);

            currentTitle = doc[0].title();
            currentSize = doc[0].toString().length() / 1024;
            currentSize = round(currentSize, 2);

            wasVisited.add(urlToRead);
            currentUrl = urlToRead;
            Log.d("CRAWLER_", "[title]: " + currentTitle + " [size]: " + currentSize + " [url]:" + urlToRead);
            if (redirCount > 3)
                Log.d("CRAWLER_", "redirect loop detected: " + redirCount + " => " + currentUrl);
        } catch (Exception e) {
            //getHTML_jsoup(urlToRead);
            e.printStackTrace();
        }
        return doc[0].toString();
    }

    public void handleAll(String resp) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        ArrayList<String> innerLinks = new ArrayList<>();
        ArrayList<String> innerMails = new ArrayList<>();
        innerLinks = getUrlsFromString(resp);
        innerMails = findEmails(resp);

        parseMails(innerMails);
        parseLinks(innerLinks);

        //out.println("Page size: " + resp.length() / 1024 + " kb");
        bandwidth += resp.length();
    }

    public void parseMails(ArrayList<String> mailsFound) {
        int found = 0;
        for (String s : mailsFound) {
            if (!s.contains("png")) {
                if (!found_email.contains(s)) {
                    //textfield.appendText(s + " -> [" + currentSize  + " kb]" + " -> [" + currentTitle + "] -> [" + currentUrl + "]\n");
                    //appendFile("emails.txt", s);
                    found_email.add(s);
                    recordEmail(s);
                    found++;
                }
            }
        }
        if (found >= 1)
            System.out.println("Mails found: " + found);
    }

    public void parseLinks(ArrayList<String> linksFound) {
        int found = 0;
        for (String s : linksFound) {
            if (!s.contains("youtube.com")) {
                if (!toVisit2.contains(s) && !wasVisited.contains(s)) {
                    String extension = s.substring(s.lastIndexOf(".") + 1);
                    if (!extension.contains("png") ||
                            !extension.contains(".js") ||
                            !extension.contains("mp4") ||
                            !extension.contains("mp3") ||
                            !extension.contains("m4a")) {
                        toVisit2.add(s); // ADD UNIQUE URL TO TOVISIT LIST
                        found++;
                    } else {
                        //System.out.println("excluded extension found: " + s);
                    }
                }
            } else {
                //System.out.println("youtube excluded: " + s);
            }
        }
        if (found >= 1)
            System.out.println("Found links: " + found);
    }

    public ArrayList<String> getUrlsFromString(String content) {
        ArrayList<String> result = new ArrayList<String>();
        String regex = "(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(content);
        while (m.find()) {
            result.add(m.group());
        }
        //System.out.println("geturlsfromstring: " + content.length());
        return result;
    }

    public ArrayList<String> findEmails(String str) {
        Matcher m = Pattern.compile("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+").matcher(str);
        ArrayList<String> a = new ArrayList<>();
        while (m.find()) {
            if (m.group().length() > 7)
                a.add(m.group());
        }
        return a;
    }

    //endregion
}