package com.cyf.demo;

import com.cyf.demo.entity.Article;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Demo {

    private static final int TIME_OUT = 10000;

    public static void main(String[] args) {
        String keywords = "疫情";
        try {
            searchArticle(keywords);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void searchArticle(String keywords) throws IOException, InterruptedException {
        String url = "https://weixin.sogou.com/weixin?type=2&query="
                + URLEncoder.encode(keywords, "UTF-8")
                + "&ie=utf8&s_from=input&_sug_=y&_sug_type_=";

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        headers.put("Connection", "keep-alive");
        headers.put("Host", "weixin.sogou.com");
        headers.put("Upgrade-Insecure-Requests", "1");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:74.0) Gecko/20100101 Firefox/74.0");

        Connection connection = Jsoup.connect(url)
                .headers(headers)
                .timeout(TIME_OUT)
                .method(Connection.Method.GET);

        Connection.Response res = connection.execute();
        Document doc = res.parse();

        System.out.println(doc);

        List<Article> articles = getArticles(doc);
        articles.forEach(System.out::println);

        Map<String, String> params = getUigsPara(doc, false);
        System.out.println(params);

        Map<String, String> cookies = getCookies(res, params);
        System.out.println(cookies);

        String token = getToken(doc);

        approveSearch(res, params, token);

        Thread.sleep(2000); // 延时2000ms，模拟搜索之后等一会再点击标题

        getRealUrl(res, params, articles);
    }

    private static void getRealUrl(Connection.Response res, Map<String, String> params, List<Article> articles) throws IOException, InterruptedException {
        for (Article article : articles) {
            approveOuter(res, params);

            String urlWithSuffix = getKH(article.getOriginalUrl());
            article.setUrlWithSuffix(urlWithSuffix);

            convertUrl(article, res);

            Thread.sleep(5000); // 延时5000ms，模拟打开一篇文章后再点开另一篇
        }
        articles.forEach(System.out::println);
    }

    private static String getToken(Document doc) {
        Elements elements = doc.select("script");
        String token = "";
        for (Element e : elements) {
            if (e.html().contains("var uigs_para =")) {
                String temp = e.html().substring(
                        e.html().indexOf("$.get(") + "$.get(".length(),
                        e.html().lastIndexOf(")"))
                        .replace("+", "")
                        .replace("\'", "")
                        .replace("\"", "");
                token = temp.substring(temp.indexOf("&token=") + "&token=".length(), temp.lastIndexOf("&")).trim();
            }
        }
        return token;
    }

    private static void approveSearch(Connection.Response res, Map<String, String> params, String token) throws IOException {
        String approve_url = "https://weixin.sogou.com/approve?"
                + "uuid=" + params.get("uuid")
                + "&token=" + token
                + "&from=search";

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "*/*");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        headers.put("Connection", "keep-alive");
        headers.put("Cookie", "ABTEST=" + res.cookies().get("ABTEST")
                + ";SNUID=" + res.cookies().get("SNUID")
                + ";IPLOC=" + res.cookies().get("IPLOC")
                + ";SUID=" + res.cookies().get("SUID")
                + ";JSESSIONID=" + res.cookies().get("JSESSIONID"));
        headers.put("Host", "weixin.sogou.com");
        headers.put("Referer", res.url().toString());
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:74.0) Gecko/20100101 Firefox/74.0");
        headers.put("X-Requested-With", "XMLHttpRequest");

        Connection connection = Jsoup.connect(approve_url)
                .headers(headers)
                .timeout(TIME_OUT)
                .method(Connection.Method.GET);

        Connection.Response resp = connection.execute();
        System.out.println("search = " + resp.statusCode());
    }

    private static Map<String, String> getCookies(Connection.Response res, Map<String, String> params) throws IOException {
        getJSESSIONID(res);

        getSUV(res, params);
        return res.cookies();
    }

    private static void getSUV(Connection.Response res, Map<String, String> params) throws IOException {
        String url = "https://pb.sogou.com/pv.gif";

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "image/webp,*/*");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        headers.put("Connection", "keep-alive");
        headers.put("Cookie", "SNUID=" + res.cookies().get("SNUID") + ";IPLOC=" + res.cookies().get("IPLOC") + ";SUID=" + res.cookies().get("SUID"));
        headers.put("Host", "pb.sogou.com");
        headers.put("Referer", "https://weixin.sogou.com/");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:74.0) Gecko/20100101 Firefox/74.0");

        Connection connection = Jsoup.connect(url)
                .headers(headers)
                .data(params)
                .timeout(TIME_OUT)
                .method(Connection.Method.GET);

        Connection.Response resp = connection.execute();

        Map<String, String> respCookies = resp.cookies();
        if (respCookies.containsKey("SUV")) {
            res.cookies().put("SUV", respCookies.get("SUV"));
        }
    }

    private static void getJSESSIONID(Connection.Response res) throws IOException {
        String url = "https://weixin.sogou.com/websearch/wexinurlenc_sogou_profile.jsp";

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "*/*");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        headers.put("Connection", "keep-alive");
        headers.put("Cookie", "ABTEST=" + res.cookies().get("ABTEST") + ";SNUID=" + res.cookies().get("SNUID") + ";IPLOC=" + res.cookies().get("IPLOC") + ";SUID=" + res.cookies().get("SUID"));
        headers.put("Host", "weixin.sogou.com");
        headers.put("Referer", res.url().toString());
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:74.0) Gecko/20100101 Firefox/74.0");

        Connection connection = Jsoup.connect(url)
                .headers(headers)
                .timeout(TIME_OUT)
                .method(Connection.Method.GET);

        Connection.Response resp = connection.execute();

        Map<String, String> respCookies = resp.cookies();
        if (respCookies.containsKey("JSESSIONID")) {
            res.cookies().put("JSESSIONID", respCookies.get("JSESSIONID"));
        }
    }

    private static Map<String, String> getUigsPara(Document doc, boolean hasLogin) {
        Elements elements = doc.select("script");
        Map<String, String> params = new HashMap<>();
        for (Element e : elements) {
            if (e.html().contains("var uigs_para =")) {
                String json = e.html()
                        .substring(e.html().indexOf("{"), e.html().indexOf("}") + 1)
                        .replace("passportUserId ? \"1\" : \"0\"", hasLogin ? "\"1\"" : "\"0\"");
                Map<String, String> para = new Gson().fromJson(json, new TypeToken<HashMap<String, String>>() {
                }.getType());
                params.putAll(para);

            } else if (e.html().contains("uigs_para.exp_id")) {
                String[] vars = e.html().split(";");
                String exp_id = vars[2];
                String value = exp_id.substring(exp_id.indexOf("\"") + 1, exp_id.lastIndexOf("\""));
                if (value.length() > 1) {
                    value = value.substring(0, value.length() - 1);
                }
                params.put("exp_id", value);
            }
        }
        return params;
    }

    private static void approveOuter(Connection.Response res, Map<String, String> params) throws IOException {
        String approve_url = "https://weixin.sogou.com/approve?uuid=" + params.get("uuid") + "&token=undefined&from=outer";

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "*/*");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        headers.put("Connection", "keep-alive");
        headers.put("Cookie", "ABTEST=" + res.cookies().get("ABTEST")
                + ";SNUID=" + res.cookies().get("SNUID")
                + ";IPLOC=" + res.cookies().get("IPLOC")
                + ";SUID=" + res.cookies().get("SUID")
                + ";SUV=" + res.cookies().get("SUV")
                + ";JSESSIONID=" + res.cookies().get("JSESSIONID"));
        headers.put("Host", "weixin.sogou.com");
        headers.put("Referer", res.url().toString());
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:74.0) Gecko/20100101 Firefox/74.0");
        headers.put("X-Requested-With", "XMLHttpRequest");

        Connection connection = Jsoup.connect(approve_url)
                .headers(headers)
                .timeout(TIME_OUT)
                .method(Connection.Method.GET);
        Connection.Response resp = connection.execute();
        System.out.println("outer = " + resp.statusCode());
    }

    private static void convertUrl(Article article, Connection.Response res) throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        headers.put("Connection", "keep-alive");
        headers.put("Cookie", "ABTEST=" + res.cookies().get("ABTEST") +
                ";SNUID=" + res.cookies().get("SNUID") +
                ";IPLOC=" + res.cookies().get("IPLOC") +
                ";SUID=" + res.cookies().get("SUID") +
                ";JSESSIONID=" + res.cookies().get("JSESSIONID") +
                ";SUV=" + res.cookies().get("SUV"));
        headers.put("Host", "weixin.sogou.com");
        headers.put("Referer", res.url().toString());
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:74.0) Gecko/20100101 Firefox/74.0");
        headers.put("Upgrade-Insecure-Requests", "1");

        Connection connection = Jsoup.connect(article.getUrlWithSuffix())
                .headers(headers)
                .timeout(TIME_OUT)
                .method(Connection.Method.GET);
        Connection.Response resp = connection.execute();
        Document doc = resp.parse();

        System.out.println(doc);

        Elements elements = doc.select("script");
        for (Element e : elements) {
            if (e.html().contains("var url =")) {
                String approve = e.html()
                        .substring(e.html().indexOf("(new Image()).src =") + "(new Image()).src =".length(),
                                e.html().indexOf("'&from=inner';") + "'&from=inner';".length())
                        .trim();
                String[] split = approve.split("\\+");
                String approveUuid = split[1].replace("\'", "").trim();
                String approveToken = split[3].replace("\'", "").trim();

                String temp = e.html().substring(e.html().indexOf("{"), e.html().indexOf("}") + 1);
                String[] tmp = temp.split(";");
                String tempUrl = "";
                for (String key : tmp) {
                    if (key.contains("url +=")) {
                        tempUrl += key.substring(key.indexOf("\'") + 1, key.length() - 1);
                    }
                }
                String realUrl = tempUrl.replace("@", "");
                article.setRealUrl(realUrl);

                approveInner(res, approveUuid, approveToken, article.getUrlWithSuffix());

                Thread.sleep(100); // 仿照搜狗微信的js延时100ms再打开文章

                openPage(article);

                break;
            }
        }
    }

    private static void openPage(Article article) throws IOException {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        headers.put("Connection", "keep-alive");
        headers.put("Host", "mp.weixin.qq.com");
        headers.put("Referer", article.getUrlWithSuffix());
        headers.put("Upgrade-Insecure-Requests", "1");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:74.0) Gecko/20100101 Firefox/74.0");

        Connection connection = Jsoup.connect(article.getRealUrl())
                .headers(headers)
                .timeout(TIME_OUT)
                .method(Connection.Method.GET);
        Connection.Response res = connection.execute();
        System.out.println("openPage：" + res.statusCode());
    }

    private static void approveInner(Connection.Response res, String approveUuid, String approveToken, String urlWithSuffix) throws IOException {
        String approve_url = "https://weixin.sogou.com/approve?uuid=" + approveUuid + "&token=" + approveToken + "&from=inner";

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "image/webp,*/*");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        headers.put("Connection", "keep-alive");
        headers.put("Cookie", "ABTEST=" + res.cookies().get("ABTEST")
                + ";SNUID=" + res.cookies().get("SNUID")
                + ";IPLOC=" + res.cookies().get("IPLOC")
                + ";SUID=" + res.cookies().get("SUID")
                + ";SUV=" + res.cookies().get("SUV")
                + ";JSESSIONID=" + res.cookies().get("JSESSIONID"));
        headers.put("Host", "weixin.sogou.com");
        headers.put("Referer", urlWithSuffix);
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:74.0) Gecko/20100101 Firefox/74.0");

        Connection connection = Jsoup.connect(approve_url)
                .headers(headers)
                .timeout(TIME_OUT)
                .method(Connection.Method.GET);
        Connection.Response resp = connection.execute();
        System.out.println("inner = " + resp.statusCode());
    }

    private static List<Article> getArticles(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements elements = doc.select("div[class=txt-box]");
        for (Element e : elements) {
            Article article = new Article();

            Element a = e.selectFirst("h3").selectFirst("a");
            article.setOriginalUrl("https://weixin.sogou.com" + a.attr("href"));
            article.setTitle(a.text());

            a = e.selectFirst("div[class=s-p]").selectFirst("a[class=account]");
            article.setOaName(a.text());

            articles.add(article);
        }
        return articles;
    }

    private static String getKH(String url) {
        int b = (int) (Math.floor(100 * Math.random()) + 1);
        int a = url.indexOf("url=");
        String temp = url.substring(a + 4 + 21 + b, a + 4 + 21 + b + 1);
        return url + "&k=" + b + "&h=" + temp;
    }

}
