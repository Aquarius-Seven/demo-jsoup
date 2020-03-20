package com.cyf.demo.entity;

public class OfficialAccount {
    /**
     * 标题
     */
    private String title;
    /**
     * 微信号
     */
    private String account;
    /**
     * 功能介绍
     */
    private String introduction;
    /**
     * 微信认证
     */
    private String identify;
    /**
     * 最近文章标题
     */
    private String recentArticle;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public String getIdentify() {
        return identify;
    }

    public void setIdentify(String identify) {
        this.identify = identify;
    }

    public String getRecentArticle() {
        return recentArticle;
    }

    public void setRecentArticle(String recentArticle) {
        this.recentArticle = recentArticle;
    }

    @Override
    public String toString() {
        return "OfficialAccount{" +
                "title='" + title + '\'' +
                ", account='" + account + '\'' +
                ", introduction='" + introduction + '\'' +
                ", identify='" + identify + '\'' +
                ", recentArticle='" + recentArticle + '\'' +
                '}';
    }
}
