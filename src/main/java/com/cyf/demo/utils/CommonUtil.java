package com.cyf.demo.utils;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;

public class CommonUtil {

    public static List<String> getUAs() {
        String[] uas = {
                "Mozilla/5.0 (Windows NT 10.0; …) Gecko/20100101 Firefox/73.0", // firefox
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.100 Safari/537.36", // chrome
                "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.25 Safari/537.36 Core/1.70.3741.400 QQBrowser/10.5.3863.400", // qq
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.79 Safari/537.36 Edge/14.14393", // edge
                "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-us) AppleWebKit/534.50 (KHTML, like Gecko) Version/5.1 Safari/534.50", // safari
                "Mozilla/5.0 (Windows NT 6.3; Trident/7.0; rv 11.0) like Gecko", // ie 11
                "Mozilla/5.0 (compatible; WOW64; MSIE 10.0; Windows NT 6.2)", // ie 10
                "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)", // ie 9
                "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; Maxthon 2.0)", // 傲游
                "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; TencentTraveler 4.0)", // tt
                "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; The World)", // 世界之窗
                "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; 360SE)" // 360
        };
        return Arrays.asList(uas);
    }

    public static List<String> getProxies(String ua) {
        if (ua == null) {
            throw new IllegalArgumentException("param can not be null");
        }
        List<String> list = new ArrayList<String>();
        try {
            Document doc = Jsoup
                    .connect("https://www.xicidaili.com/nn/")
                    .userAgent(ua)
                    .timeout(10 * 1000)
                    .get();
            Elements elements = doc.select("tr");
            for (Element e : elements) {
                if (e.child(0).is("td")) {
                    String sb = e.child(1).text() + ":" + e.child(2).text();
                    list.add(sb);
                    System.out.println("proxy = " + sb);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static String getRandomOne(List<String> data) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("data can not be null or empty");
        }
        int selectPos = new Random().nextInt(data.size());
        return data.get(selectPos);
    }

    public static Connection.Response handleResponse(Connection.Response res, String ua) throws IOException {
        String flash_url = "http://1.1.1.3:89/cookie/flash.js";

        String referer = res.url().toString();

        Document doc = res.parse();
        Elements elements = doc.select("script");
        String td_cookie = "";
        for (Element e : elements) {
            if (e.html().contains("setURL") && e.html().contains("supFlash")) {
                td_cookie = e.html().substring(
                        e.html().indexOf("supFlash(") + "supFlash(".length(),
                        e.html().lastIndexOf(")"))
                        .replace("\"", "");
                break;
            }
        }
        if (!td_cookie.isEmpty()) {
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Accept", "*/*");
            headers.put("Accept-Encoding", "gzip, deflate");
            headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
            headers.put("Connection", "keep-alive");
            headers.put("Host", "1.1.1.3:89");
            headers.put("Referer", "http://mp.weixin.qq.com/");
            headers.put("User-Agent", ua);

            Connection connection = Jsoup.connect(flash_url)
                    .headers(headers)
                    .timeout(10 * 1000)
                    .method(Connection.Method.GET);
            Connection.Response resp = connection.execute();

            if (resp.statusCode() == 200) {
                headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
                headers.put("Cookie", "td_cookie=" + td_cookie);
                headers.put("Host", "mp.weixin.qq.com");
                headers.put("Referer", referer);
                headers.put("Upgrade-Insecure-Requests", "1");

                connection = Jsoup.connect(referer)
                        .headers(headers)
                        .timeout(10 * 1000)
                        .method(Connection.Method.GET);
                resp = connection.execute();

                res = resp; // 替换结果
            }
        }
        return res;
    }
}
