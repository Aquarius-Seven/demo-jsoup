package com.cyf.demo.utils;

import com.cyf.demo.entity.Article;
import com.cyf.demo.entity.OfficialAccount;
import com.cyf.demo.entity.PageData;
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

/**
 * 公众号搜索/文章搜索
 */
public class SearchManager {

    private static final int TIME_OUT = 10000;
    private static final int TIME_DELAY = 5000;

    /**
     * 搜索公众号入口
     *
     * @param req
     * @return
     * @throws IOException
     */
    public static PageData<OfficialAccount> searchOA(PageData<OfficialAccount> req) throws IOException {
        if (req.getKeywords() == null || req.getKeywords().isEmpty()) {
            return req;
        }

        String url = "https://weixin.sogou.com/weixin?type=1&query=" + URLEncoder.encode(req.getKeywords(), "UTF-8") + "&ie=utf8&s_from=input&_sug_=n&_sug_type_=";

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        headers.put("Connection", "keep-alive");
        headers.put("Host", "weixin.sogou.com");
        headers.put("Upgrade-Insecure-Requests", "1");

        Connection connection = Jsoup.connect(url)
                .headers(headers)
                .userAgent(req.getUserAgent()) // searchOA
                .timeout(TIME_OUT)
                .method(Connection.Method.GET);

        Connection.Response res = connection.execute();
        Document doc = res.parse();

        List<OfficialAccount> oas = new ArrayList<>();
        Elements elements = doc.select("ul[class=news-list2]").select("li");
        for (Element e : elements) {
            OfficialAccount oa = new OfficialAccount();

            String title = e.selectFirst("p[class=tit]").selectFirst("a").text();
            oa.setTitle(title);

            String account = e.selectFirst("p[class=info]").selectFirst("label[name=em_weixinhao]").text();
            oa.setAccount(account);

            Elements dls = e.select("dl");
            for (Element dl : dls) {
                Element dd = dl.selectFirst("dd");
                if (!dd.select("i").isEmpty()) {
                    oa.setIdentify(dl.selectFirst("dd").text());
                } else if (!dd.select("a").isEmpty()) {
                    oa.setRecentArticle(dl.selectFirst("dd").selectFirst("a").text());
                } else {
                    oa.setIntroduction(dl.selectFirst("dd").text());
                }
            }

            oas.add(oa);
        }
        req.setRecords(oas);

        return req;
    }

    /**
     * 搜索文章入口
     *
     * @param req
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static PageData<Article> searchArticle(PageData<Article> req) throws IOException, InterruptedException {
        if (req.getKeywords() == null || req.getKeywords().isEmpty()) {
            return req;
        }

        String url = "https://weixin.sogou.com/weixin?type=2&s_from=input&query="
                + URLEncoder.encode(req.getKeywords(), "UTF-8")
                + "&ie=utf8&_sug_=n&_sug_type_=&page=" + req.getPageNum();

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        headers.put("Connection", "keep-alive");
        headers.put("Host", "weixin.sogou.com");
        headers.put("Upgrade-Insecure-Requests", "1");

        Connection connection = Jsoup.connect(url)
                .headers(headers)
                .userAgent(req.getUserAgent()) // searchArticle
                .timeout(TIME_OUT)
                .method(Connection.Method.GET);

        if (req.getReferer() != null) {
            connection.referrer(req.getReferer());
        }

        if (req.getCookies() != null) {
            connection.cookies(req.getCookies());
        }

        Connection.Response res = connection.execute();
        Document doc = res.parse();

        System.out.println(doc);

        req.setRecords(getArticles(doc));

        req.setUigsPara(getUigsPara(doc, req.isHasLogin()));

        req.setToken(getToken(doc));

        req.setTotalRecords(getTotalRecords(doc));

        if (req.getRecords().isEmpty() || req.getUigsPara() == null) {
            return req;
        }

        getCookies(res, req);

        approveSearch(res, req);

        System.out.println("延时2000ms");
        Thread.sleep(2000);

        System.out.println("模拟点开文章标题，提取真实URL");
        getRealUrl(res, req);

        return req;
    }

    /**
     * 1、读取当页所有文章：初始链接、标题、公众号名称
     *
     * @param doc
     * @return
     */
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
//        articles.forEach(System.out::println);
        return articles;
    }

    /**
     * 2、读取当页的参数，后面流程需要用到getSUV、approveSearch、approveOuter
     *
     * @param doc
     * @param hasLogin
     * @return
     */
    private static Map<String, String> getUigsPara(Document doc, boolean hasLogin) {
        Elements elements = doc.select("script");
        Map<String, String> params = null;
        for (Element e : elements) {
            if (e.html().contains("var uigs_para =")) {
                String json = e.html()
                        .substring(e.html().indexOf("{"), e.html().indexOf("}") + 1)
                        .replace("passportUserId ? \"1\" : \"0\"", hasLogin ? "\"1\"" : "\"0\"");
                params = new Gson().fromJson(json, new TypeToken<HashMap<String, String>>() {
                }.getType());

                params.put("right", "right0_0");
            } else if (e.html().contains("uigs_para.exp_id")) {
                String[] vars = e.html().split(";");
                String exp_id = vars[2];
                if (params != null) {
                    String value = exp_id.substring(exp_id.indexOf("\"") + 1, exp_id.lastIndexOf("\""));
                    if (value.length() > 1) {
                        value = value.substring(0, value.length() - 1);
                    }
                    params.put("exp_id", value);
                }
            }
        }
//        if (params != null) {
//            System.out.println(params);
//        }
        return params;
    }

    /**
     * 3、读取当页的参数，后面流程需要用到approveSearch、getKH
     *
     * @param doc
     * @return
     */
    private static String getToken(Document doc) {
        Elements elements = doc.select("script");
        String token = null;
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

    /**
     * 4、读取当页显示的总记录数，用来分页（非登录最多可访问10页，登录后可翻页10页以上）
     *
     * @param doc
     * @return
     */
    private static int getTotalRecords(Document doc) {
        Element element = doc.selectFirst("div[class=mun]");
        if (null != element) {
            String text = element.text();
            String num = text.substring(text.indexOf("约") + 1, text.indexOf("条"));
            String no = num.replace(",", "");
            return Integer.parseInt(no);
        }
        return 0;
    }

    /**
     * 5、关键步骤：需要通过多个接口拼接cookie参数才可正常转换URL（非登录情况下）
     *
     * @param res
     * @param req
     * @throws IOException
     */
    private static void getCookies(Connection.Response res, PageData<Article> req) throws IOException {
        Map<String, String> resCookies = res.cookies();
        Map<String, String> cookies = req.getCookies(); // 塞参数
        if (cookies.containsKey("SUV")) {
            return;
        }
        if (resCookies.containsKey("ABTEST")) {
            cookies.put("ABTEST", resCookies.get("ABTEST"));
        }
        if (resCookies.containsKey("SNUID")) {
            cookies.put("SNUID", resCookies.get("SNUID"));
        }
        if (resCookies.containsKey("IPLOC")) {
            cookies.put("IPLOC", resCookies.get("IPLOC"));
        }
        if (resCookies.containsKey("SUID")) {
            cookies.put("SUID", resCookies.get("SUID"));
        }

        getSUID(req);

        getJSESSIONID(res, req);

        getSUV(req);

    }

    /**
     * 6、拼接SUID参数
     *
     * @param req
     * @throws IOException
     */
    private static void getSUID(PageData<Article> req) throws IOException {
        String url = "https://www.sogou.com/sug/css/m3.min.v.7.css";

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "text/css,*/*;q=0.1");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        headers.put("Connection", "keep-alive");
        headers.put("Cookie", "SNUID=" + req.getCookies().get("SNUID") + ";IPLOC=" + req.getCookies().get("IPLOC"));
        headers.put("Host", "www.sogou.com");
        headers.put("Referer", "https://weixin.sogou.com/");
        headers.put("User-Agent", req.getUserAgent());

        Connection connection = Jsoup.connect(url)
                .headers(headers)
                .timeout(TIME_OUT)
                .method(Connection.Method.GET);

        Connection.Response res = connection.execute();
        Map<String, String> resCookies = res.cookies();
        if (resCookies.containsKey("SUID")) {
            req.getCookies().put("SUID", resCookies.get("SUID"));
        }
    }

    /**
     * 7、拼接JSESSIONID参数
     *
     * @param res
     * @param req
     * @throws IOException
     */
    private static void getJSESSIONID(Connection.Response res, PageData<Article> req) throws IOException {
        String url = "https://weixin.sogou.com/websearch/wexinurlenc_sogou_profile.jsp";

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "*/*");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        headers.put("Connection", "keep-alive");
        headers.put("Cookie", "ABTEST=" + req.getCookies().get("ABTEST") + ";SNUID=" + req.getCookies().get("SNUID") + ";IPLOC=" + req.getCookies().get("IPLOC") + ";SUID=" + req.getCookies().get("SUID"));
        headers.put("Host", "weixin.sogou.com");
        headers.put("Referer", res.url().toString());
        headers.put("User-Agent", req.getUserAgent());

        Connection connection = Jsoup.connect(url)
                .headers(headers)
                .timeout(TIME_OUT)
                .method(Connection.Method.GET);

        Connection.Response resp = connection.execute();

        Map<String, String> resCookies = resp.cookies();
        if (resCookies.containsKey("JSESSIONID")) {
            req.getCookies().put("JSESSIONID", resCookies.get("JSESSIONID"));
        }
    }

    /**
     * 8、拼接SUV参数
     *
     * @param req
     * @throws IOException
     */
    private static void getSUV(PageData<Article> req) throws IOException {
        String url = "https://pb.sogou.com/pv.gif";

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "image/webp,*/*");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        headers.put("Connection", "keep-alive");
        headers.put("Cookie", "SNUID=" + req.getCookies().get("SNUID") + ";IPLOC=" + req.getCookies().get("IPLOC") + ";SUID=" + req.getCookies().get("SUID"));
        headers.put("Host", "pb.sogou.com");
        headers.put("Referer", "https://weixin.sogou.com/");
        headers.put("User-Agent", req.getUserAgent());

        Connection connection = Jsoup.connect(url)
                .headers(headers)
                .data(req.getUigsPara())
                .timeout(TIME_OUT)
                .method(Connection.Method.GET);

        Connection.Response resp = connection.execute();

        Map<String, String> resCookies = resp.cookies();
        if (resCookies.containsKey("SUV")) {
            req.getCookies().put("SUV", resCookies.get("SUV"));
        }
    }

    /**
     * 9、每一次搜索都调用了一次，证明搜索操作
     *
     * @param res
     * @param req
     * @throws IOException
     */
    private static void approveSearch(Connection.Response res, PageData<Article> req) throws IOException {
        String approve_url = "https://weixin.sogou.com/approve?"
                + "uuid=" + req.getUigsPara().get("uuid")
                + "&token=" + req.getToken()
                + "&from=search";

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "*/*");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        headers.put("Connection", "keep-alive");
        headers.put("Cookie", "ABTEST=" + req.getCookies().get("ABTEST")
                + ";SNUID=" + req.getCookies().get("SNUID")
                + ";IPLOC=" + req.getCookies().get("IPLOC")
                + ";SUID=" + req.getCookies().get("SUID")
                + ";SUV=" + req.getCookies().get("SUV")
                + ";JSESSIONID=" + req.getCookies().get("JSESSIONID"));
        headers.put("Host", "weixin.sogou.com");
        headers.put("Referer", res.url().toString());
        headers.put("User-Agent", req.getUserAgent());
        headers.put("X-Requested-With", "XMLHttpRequest");

        Connection connection = Jsoup.connect(approve_url)
                .headers(headers)
                .timeout(TIME_OUT)
                .method(Connection.Method.GET);

        Connection.Response resp = connection.execute();
        System.out.println("search = " + resp.statusCode());
    }

    /**
     * 10、关键步骤：循环转换当页的所有文章链接，每转换一条进行延时，避免被反爬
     *
     * @param res
     * @param req
     * @throws IOException
     * @throws InterruptedException
     */
    private static void getRealUrl(Connection.Response res, PageData<Article> req) throws IOException, InterruptedException {
        for (Article article : req.getRecords()) {
            approveOuter(res, req);

            String urlWithSuffix = getKH(req.getToken(), article.getOriginalUrl());
            article.setUrlWithSuffix(urlWithSuffix);

            Document doc = convertUrl(urlWithSuffix, res, req).parse();
//            System.out.println(doc);

            Elements elements = doc.select("script");
            boolean hasResult = false;
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

                    approveInner(approveUuid, approveToken, urlWithSuffix, req);

                    Thread.sleep(100); // 仿照搜狗微信的js延时100ms再打开文章

                    openPage(urlWithSuffix, realUrl, req);
                    hasResult = true;
                    break;
                }
            }
            if (hasResult) {
                System.out.println(article.toString());
                int currentPos = req.getRecords().indexOf(article);
                if (currentPos == req.getRecords().size() - 1) {
                    System.out.println("已转换当前页面： - " + req.getPageNum() + " - 最后一条URL");
                    if (req.getPageNum() < req.getTotalPage()) {
//                    page++;
//                    System.out.println("下一页：" + page);
//                    searchKeywords(keywords, page, ua, null, referer);
                    } else {
//                    System.out.println("拿到" + page + "页数据");
                    }
                } else {
                    System.out.println("休眠" + TIME_DELAY + "ms");
                    Thread.sleep(TIME_DELAY);
                    System.out.println("转换下一条URL");
                }
            } else {
                System.out.println("出现异常");
                break;
            }
        }
    }

    /**
     * 11、每一次点击标题跳转文章详情时调用，证明点击操作（外部）
     *
     * @param res
     * @param req
     * @throws IOException
     */
    private static void approveOuter(Connection.Response res, PageData<Article> req) throws IOException {
        String approve_url = "https://weixin.sogou.com/approve?uuid=" + req.getUigsPara().get("uuid") + "&token=undefined&from=outer";

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "*/*");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        headers.put("Connection", "keep-alive");
        headers.put("Cookie", "ABTEST=" + req.getCookies().get("ABTEST")
                + ";SNUID=" + req.getCookies().get("SNUID")
                + ";IPLOC=" + req.getCookies().get("IPLOC")
                + ";SUID=" + req.getCookies().get("SUID")
                + ";SUV=" + req.getCookies().get("SUV")
                + ";JSESSIONID=" + req.getCookies().get("JSESSIONID"));
        headers.put("Host", "weixin.sogou.com");
        headers.put("Referer", res.url().toString());
        headers.put("User-Agent", req.getUserAgent());
        headers.put("X-Requested-With", "XMLHttpRequest");

        Connection connection = Jsoup.connect(approve_url)
                .headers(headers)
                .timeout(TIME_OUT)
                .method(Connection.Method.GET);
        Connection.Response resp = connection.execute();
        System.out.println("outer = " + resp.statusCode());
    }

    /**
     * 12、点击之后，按一定规则往原始链接拼接k、h参数
     *
     * @param token
     * @param url
     * @return
     */
    private static String getKH(String token, String url) {
        int b = (int) (Math.floor(100 * Math.random()) + 1);
        int a = url.indexOf("url=");
        String temp = url.substring(a + 4 + 21 + b, a + 4 + 21 + b + 1);
        if (url.contains("&token=")) {
            return url + "&k=" + b + "&h=" + temp;
        } else {
            return url + "&token=" + token + "&k=" + b + "&h=" + temp;
        }
    }

    /**
     * 13、关键步骤：拼接之后，访问此链接得到响应数据（approveInner所需数据、待拼接真实URL）
     *
     * @param urlWithSuffix
     * @param res
     * @param req
     * @return
     * @throws IOException
     */
    private static Connection.Response convertUrl(String urlWithSuffix, Connection.Response res, PageData<Article> req) throws IOException {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        headers.put("Connection", "keep-alive");
        headers.put("Cookie", "ABTEST=" + req.getCookies().get("ABTEST")
                + ";SNUID=" + req.getCookies().get("SNUID")
                + ";IPLOC=" + req.getCookies().get("IPLOC")
                + ";SUID=" + req.getCookies().get("SUID")
                + ";SUV=" + req.getCookies().get("SUV")
                + ";JSESSIONID=" + req.getCookies().get("JSESSIONID"));
        headers.put("Host", "weixin.sogou.com");
        headers.put("Referer", res.url().toString());
        headers.put("User-Agent", req.getUserAgent());
        headers.put("Upgrade-Insecure-Requests", "1");

        Connection connection = Jsoup.connect(urlWithSuffix)
                .headers(headers)
                .timeout(TIME_OUT)
                .method(Connection.Method.GET);
        Connection.Response resp = connection.execute();
//        System.out.println(resp.parse().toString());
        return resp;
    }

    /**
     * 14、每一次点击标题跳转文章详情时调用，证明点击操作（内部）
     *
     * @param approveUuid
     * @param approveToken
     * @param urlWithSuffix
     * @param req
     * @throws IOException
     */
    private static void approveInner(String approveUuid, String approveToken, String urlWithSuffix, PageData<Article> req) throws IOException {
        String approve_url = "https://weixin.sogou.com/approve?uuid=" + approveUuid + "&token=" + approveToken + "&from=inner";

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "image/webp,*/*");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        headers.put("Connection", "keep-alive");
        headers.put("Cookie", "ABTEST=" + req.getCookies().get("ABTEST")
                + ";SNUID=" + req.getCookies().get("SNUID")
                + ";IPLOC=" + req.getCookies().get("IPLOC")
                + ";SUID=" + req.getCookies().get("SUID")
                + ";SUV=" + req.getCookies().get("SUV")
                + ";JSESSIONID=" + req.getCookies().get("JSESSIONID"));
        headers.put("Host", "weixin.sogou.com");
        headers.put("Referer", urlWithSuffix);
        headers.put("User-Agent", req.getUserAgent());

        Connection connection = Jsoup.connect(approve_url)
                .headers(headers)
                .timeout(TIME_OUT)
                .method(Connection.Method.GET);
        Connection.Response resp = connection.execute();
        System.out.println("inner = " + resp.statusCode());
    }

    /**
     * 15、打开文章链接，跳转到文章详情
     *
     * @param urlWithSuffix
     * @param realUrl
     * @param req
     * @throws IOException
     */
    private static void openPage(String urlWithSuffix, String realUrl, PageData<Article> req) throws IOException {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        headers.put("Connection", "keep-alive");
        headers.put("Host", "mp.weixin.qq.com");
        headers.put("Referer", urlWithSuffix);
        headers.put("Upgrade-Insecure-Requests", "1");
        headers.put("User-Agent", req.getUserAgent());

        Connection connection = Jsoup.connect(realUrl)
                .headers(headers)
                .timeout(TIME_OUT)
                .method(Connection.Method.GET);
        Connection.Response res = connection.execute();
        System.out.println("openPage：" + res.statusCode());
    }

}
