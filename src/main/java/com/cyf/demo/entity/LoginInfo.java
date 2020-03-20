package com.cyf.demo.entity;

import java.util.Map;

public class LoginInfo {
    /**
     * 登录请求的URL：
     * 1、点击登录调用接口后，会返回重定向之后的地址；
     * 2、刷新二维码，用的是之前重定向得到的地址。
     * 地址里有参数state，登录成功后回调接口使用
     */
    private String url;
    /**
     * 请求的UA，贯穿始终
     */
    private String ua;
    /**
     * 记录一次查询登录状态请求最开始的时间戳，与currentTime比较得出二维码是否需要刷新
     */
    private long firstTime;
    /**
     * 当前请求的时间戳，下一次请求+1处理
     */
    private long currentTime;
    /**
     * 登录成功后回调接口需要参数之一
     */
    private String state;
    /**
     * 二维码图片URL
     */
    private String qrCode;
    /**
     * 二维码图片ID，查询登录状态需要参数之一
     */
    private String uuid;
    /**
     * 调用登录或刷新二维码接口时得到，登录成功后回调使用
     */
    private Map<String, String> cookies;
    /**
     * 微信返回码：408（没扫码）、403（扫码后拒绝）、404（扫码后未选择）、405（扫码后同意）
     * 自定义返回码：500（二维码过期）
     */
    private int errorCode;
    /**
     * 登录成功后回调接口需要参数之一
     */
    private String code;
    /**
     * 登录成功后回调接口得到，查询微信信息使用
     */
    private Map<String, String> callBackCookies;
    /**
     * 微信昵称
     */
    private String username;
    /**
     * 微信id
     */
    private String puid;
    /**
     * 微信头像URL
     */
    private String headurl;
    /**
     * 微信状态
     */
    private int status;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPuid() {
        return puid;
    }

    public void setPuid(String puid) {
        this.puid = puid;
    }

    public String getHeadurl() {
        return headurl;
    }

    public void setHeadurl(String headurl) {
        this.headurl = headurl;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Map<String, String> getCallBackCookies() {
        return callBackCookies;
    }

    public void setCallBackCookies(Map<String, String> callBackCookies) {
        this.callBackCookies = callBackCookies;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUa() {
        return ua;
    }

    public void setUa(String ua) {
        this.ua = ua;
    }

    public long getFirstTime() {
        return firstTime;
    }

    public void setFirstTime(long firstTime) {
        this.firstTime = firstTime;
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
        if (firstTime == 0) {
            setFirstTime(currentTime);
        }
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return "LoginInfo{" +
                "url='" + url + '\'' +
                ", ua='" + ua + '\'' +
                ", firstTime=" + firstTime +
                ", currentTime=" + currentTime +
                ", state='" + state + '\'' +
                ", qrCode='" + qrCode + '\'' +
                ", uuid='" + uuid + '\'' +
                ", cookies=" + cookies +
                ", errorCode=" + errorCode +
                ", code='" + code + '\'' +
                ", callBackCookies=" + callBackCookies +
                ", username='" + username + '\'' +
                ", puid='" + puid + '\'' +
                ", headurl='" + headurl + '\'' +
                ", status=" + status +
                '}';
    }
}
