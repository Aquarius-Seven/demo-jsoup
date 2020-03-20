package com.cyf.demo.entity;

import com.cyf.demo.utils.CommonUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PageData<T> {
    /**
     * 当前页数
     */
    private int pageNum;
    /**
     * 当页记录数
     */
    private int pageSize;
    /**
     * 总页数
     */
    private int totalPage;
    /**
     * 总记录数
     */
    private int totalRecords;
    /**
     * 当前页记录的数据
     */
    private List<T> records;
    /**
     * 最大页数，登录后可以访问的页数很多，可以通过设置最大页数来限制
     */
    private int maxPage;

    /**
     * 是否登录
     */
    private boolean hasLogin;
    /**
     * 关键词，记录下，翻页使用
     */
    private String keywords;
    /**
     * UA，记录下，所有接口调用统一使用
     */
    private String userAgent;
    /**
     * cookies，记录下，所有接口调用统一使用
     */
    private Map<String, String> cookies;

    // 以下参数不同页数不一致
    /**
     * 来源，翻页需要注明来源，这里处理为每次翻页的来源都为上一页
     */
    private String referer;
    /**
     * 参数，记录下，方便一些接口调用
     */
    private Map<String, String> uigsPara;
    /**
     * 参数，记录下，方便一些接口调用
     */
    private String token;

    public PageData(String keywords) {
        this.keywords = keywords;
    }

    public PageData(String keywords, String userAgent, Map<String, String> cookies) {
        this.hasLogin = true;
        this.keywords = keywords;
        this.userAgent = userAgent;
        this.cookies = cookies;
    }

    public boolean isHasLogin() {
        return hasLogin;
    }

    public void setHasLogin(boolean hasLogin) {
        this.hasLogin = hasLogin;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getUserAgent() {
        if (userAgent == null) {
            userAgent = CommonUtil.getRandomOne(CommonUtil.getUAs());
        }
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Map<String, String> getCookies() {
        if (cookies == null) {
            cookies = new HashMap<>();
        }
        return cookies;
    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
        setPageSize(records.size());
    }

    public int getPageNum() {
        if (pageNum == 0) {
            pageNum = 1;
        }
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalPage() {
        return Math.min(totalPage, getMaxPage());
//        return totalPage;
    }

    public void setTotalPage(int totalPage) {
        this.totalPage = totalPage;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    private static final int PAGE_SIZE = 10;

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
        int totalPage = (totalRecords + PAGE_SIZE - 1) / PAGE_SIZE;
        if (!isHasLogin()) {
            totalPage = totalPage <= 10 ? totalPage : 10;
        }
        setTotalPage(totalPage);
    }

    public int getMaxPage() {
        return maxPage;
    }

    public PageData<T> setMaxPage(int maxPage) {
        this.maxPage = maxPage;
        return this;
    }

    public String getReferer() {
        if (getPageNum() > 1) {
            try {
                referer = "https://weixin.sogou.com/weixin?type=2&s_from=input&query="
                        + URLEncoder.encode(getKeywords(), "UTF-8")
                        + "&ie=utf8&_sug_=n&_sug_type_=&page=" + (getPageNum() - 1);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public void setUigsPara(Map<String, String> uigsPara) {
        this.uigsPara = uigsPara;
    }

    public Map<String, String> getUigsPara() {
        return uigsPara;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
