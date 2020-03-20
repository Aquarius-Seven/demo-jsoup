package com.cyf.demo.utils;

import com.cyf.demo.entity.LoginInfo;
import net.sf.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 搜狗微信账号管理，登录或退出
 */
public class AccountManager {

    private static final int TIME_OUT = 10000;
    private static final int MAX_TIMES = 18;

    /**
     * 登录入口
     *
     * @return 登录信息，查询登录状态使用
     */
    public static LoginInfo login() {
        try {
            LoginInfo info = new LoginInfo();
            info.setUa(CommonUtil.getRandomOne(CommonUtil.getUAs()));

            String url = "https://account.sogou.com/connect/login?provider=weixin&client_id=2017&ru=https%3A%2F%2Fweixin.sogou.com&third_appid=wx6634d697e8cc0a29&href=https%3A%2F%2Fdlweb.sogoucdn.com%2Fweixin%2Fcss%2Fweixin_join.min.css%3Fv%3D20170315";

            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            headers.put("Accept-Encoding", "gzip, deflate, br");
            headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
            headers.put("Connection", "keep-alive");
            headers.put("Host", "account.sogou.com");
            headers.put("Upgrade-Insecure-Requests", "1");
            headers.put("User-Agent", info.getUa());

            Connection connection = Jsoup.connect(url)
                    .headers(headers)
                    .timeout(TIME_OUT)
                    .method(Connection.Method.GET);
            Connection.Response res = connection.execute();

            return genLoginInfo(res, info); // 点击登录按钮，提取登录信息
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 提取登录信息
     *
     * @param res  请求响应
     * @param info 原始登录信息
     * @return 补全或更新登录信息
     * @throws IOException
     */
    private static LoginInfo genLoginInfo(Connection.Response res, LoginInfo info) throws IOException {
        info.setUrl(res.url().toString());
        String[] keyValue = res.url().getQuery().split("&");
        String state = "";
        for (String s : keyValue) {
            if (s.contains("state")) {
                String[] stateArr = s.split("=");
                state = stateArr[1];
                break;
            }
        }
        info.setState(state);

        info.setCookies(res.cookies());

        Document doc = res.parse();
        String src = doc.selectFirst("img[class=qrcode lightBorder]").attr("src");
        info.setUuid(src.substring(src.lastIndexOf("/") + 1));
        info.setQrCode("https://open.weixin.qq.com" + src);
        return info;
    }

    /**
     * 查询登录状态
     *
     * @param info 登录信息
     * @return 最新登录状态的登录信息
     */
    public static LoginInfo queryLoginStatus(LoginInfo info) {
        try {
            if (info.getCurrentTime() == 0) { // 第一次登录或者二维码过期时
                info.setFirstTime(0);
                info.setErrorCode(0);
                info.setCurrentTime(System.currentTimeMillis());
            }

            long timestamp = info.getCurrentTime();
            long firstTime = info.getFirstTime();
            if (timestamp - firstTime > MAX_TIMES) { // 更新二维码
                return refreshQrCode(info);
            }

            String uuid = info.getUuid();
            int last = info.getErrorCode() == 408 ? 0 : info.getErrorCode(); // 408时last = 0

            String url = "https://lp.open.weixin.qq.com/connect/l/qrconnect?uuid=" + uuid + (last == 0 ? "" : "&last=" + last) + "&_=" + timestamp;

            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Accept", "*/*");
            headers.put("Accept-Encoding", "gzip, deflate, br");
            headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
            headers.put("Connection", "keep-alive");
            headers.put("Host", "lp.open.weixin.qq.com");
            headers.put("Referer", "https://open.weixin.qq.com/");
            headers.put("User-Agent", info.getUa());

            Connection connection = Jsoup.connect(url)
                    .headers(headers)
                    .timeout(TIME_OUT * 5)
                    .method(Connection.Method.GET);

            Connection.Response res = connection.execute();

            Document doc = res.parse();
            String wxRes = doc.body().text();

            System.out.println(res.url().toString());

            if (wxRes.contains("wx_errcode=408")) { // 未扫码，继续轮询 --> 扫码404,扫码后拒绝403,扫码后同意405
                info.setErrorCode(408);
                info.setCurrentTime(timestamp + 1);
                return info;
            } else if (wxRes.contains("wx_errcode=404")) {// 已扫码，继续轮询 --> 不操作408,拒绝403,同意405
                info.setErrorCode(404);
                info.setCurrentTime(timestamp + 1);
                return info;
            } else if (wxRes.contains("wx_errcode=403")) {// 拒绝，继续轮询 --> 不操作408，重新扫码404
                info.setErrorCode(403);
                info.setCurrentTime(timestamp + 1);
                return info;
            } else if (wxRes.contains("wx_errcode=405")) {// 同意，获取wx_code
                info.setErrorCode(405);
                info.setCode(wxRes.substring(wxRes.indexOf("wx_code=") + "wx_code=".length(), wxRes.lastIndexOf(";")).replace("'", ""));
                return callBack(info);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 二维码过期，刷新二维码
     *
     * @param info 登录信息
     * @return 新的登录信息（主要是二维码地址更换）
     */
    private static LoginInfo refreshQrCode(LoginInfo info) {
        try {
            info.setErrorCode(500);
            info.setCurrentTime(0);

            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            headers.put("Accept-Encoding", "gzip, deflate, br");
            headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
            headers.put("Connection", "keep-alive");
            headers.put("Host", "open.weixin.qq.com");
            headers.put("Upgrade-Insecure-Requests", "1");
            headers.put("User-Agent", info.getUa());

            Connection connection = Jsoup.connect(info.getUrl())
                    .headers(headers)
                    .timeout(TIME_OUT)
                    .method(Connection.Method.GET);
            Connection.Response res = connection.execute();

            return genLoginInfo(res, info); // 二维码过期，刷新
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 登录成功后回调
     *
     * @param info 登录信息
     * @return 补充微信账号信息后的登录信息
     * @throws IOException
     */
    private static LoginInfo callBack(LoginInfo info) throws IOException {
        String code = info.getCode();
        String state = info.getState();
        Map<String, String> cookies = info.getCookies();

        String url = "https://account.sogou.com/connect/callback/weixin?code=" + code + "&state=" + state;

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        headers.put("Connection", "keep-alive");
        headers.put("Cookie", "IPLOC=" + cookies.get("IPLOC") + ";JSESSIONID=" + cookies.get("JSESSIONID"));
        headers.put("Host", "account.sogou.com");
        headers.put("Referer", "https://open.weixin.qq.com/");
        headers.put("Upgrade-Insecure-Requests", "1");
        headers.put("User-Agent", info.getUa());

        Connection connection = Jsoup.connect(url)
                .headers(headers)
                .timeout(TIME_OUT)
                .method(Connection.Method.GET);

        Connection.Response res = connection.execute();
        info.setCallBackCookies(res.cookies());
        return queryLoginInfo(info);
    }

    /**
     * 查询补充微信账号信息
     *
     * @param info 登录信息
     * @return 补充微信账号信息后的登录信息
     */
    private static LoginInfo queryLoginInfo(LoginInfo info) {
        try {
            String url = "https://www.sogou.com/websearch/login/api/logininfo_ajax.jsp?callback=loginCallback";

            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Accept", "*/*");
            headers.put("Accept-Encoding", "gzip, deflate, br");
            headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
            headers.put("Connection", "keep-alive");
            headers.put("Host", "www.sogou.com");
            headers.put("Referer", "https://weixin.sogou.com/");
            headers.put("User-Agent", info.getUa());

            Connection connection = Jsoup.connect(url)
                    .headers(headers)
                    .cookies(info.getCallBackCookies())
                    .timeout(TIME_OUT)
                    .method(Connection.Method.GET);

            Connection.Response res = connection.execute();

            Document doc = res.parse();
            String loginCallback = doc.body().text();
            if (loginCallback.contains("loginCallback")) {
                String json = loginCallback.substring(loginCallback.indexOf("loginCallback(") + "loginCallback(".length(), loginCallback.lastIndexOf(")"))
                        .replace("\'", "\"")
                        .replace("username:", "\"username\":")
                        .replace("puid:", "\"puid\":")
                        .replace("headurl:", "\"headurl\":")
                        .replace("status:", "\"status\":");
                JSONObject obj = JSONObject.fromObject(json);
                info.setUsername(obj.optString("username"));
                info.setPuid(obj.optString("puid"));
                info.setHeadurl(obj.optString("headurl"));
                info.setStatus(obj.optInt("status"));
            }
            return info;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 退出登录
     *
     * @param info 登录信息
     * @return 返回码（200成功）
     */
    public static int logout(LoginInfo info) {
        try {
            String url = "https://account.sogou.com/web/logout_js?client_id=2017";
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            headers.put("Accept-Encoding", "gzip, deflate, br");
            headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
            headers.put("Connection", "keep-alive");
            headers.put("Host", "account.sogou.com");
            headers.put("Referer", "https://weixin.sogou.com/");
            headers.put("Upgrade-Insecure-Requests", "1");
            headers.put("User-Agent", info.getUa());

            Connection connection = Jsoup.connect(url)
                    .headers(headers)
                    .cookies(info.getCallBackCookies())
                    .timeout(TIME_OUT)
                    .method(Connection.Method.GET);
            Connection.Response res = connection.execute();

            System.out.println(res.url().toString());

            return res.statusCode();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

}
