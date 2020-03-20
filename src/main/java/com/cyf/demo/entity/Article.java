package com.cyf.demo.entity;

public class Article {
    /**
     * 初始链接
     */
    private String originalUrl;
    /**
     * 标题
     */
    private String title;
    /**
     * 公众号名称
     */
    private String oaName;
    /**
     * 拼接k ,h
     */
    private String urlWithSuffix;
    /**
     * 转换后的链接
     */
    private String realUrl;

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOaName() {
        return oaName;
    }

    public void setOaName(String oaName) {
        this.oaName = oaName;
    }

    public void setUrlWithSuffix(String urlWithSuffix) {
        this.urlWithSuffix = urlWithSuffix;
    }

    public String getUrlWithSuffix() {
        return urlWithSuffix;
    }

    public void setRealUrl(String realUrl) {
        this.realUrl = realUrl.replace("http://","https://");
    }

    public String getRealUrl() {
        return realUrl;
    }

    @Override
    public String toString() {
        return "Article{" +
                "originalUrl='" + originalUrl + '\'' +
                ", title='" + title + '\'' +
                ", oaName='" + oaName + '\'' +
                ", urlWithSuffix='" + urlWithSuffix + '\'' +
                ", realUrl='" + realUrl + '\'' +
                '}';
    }
}
