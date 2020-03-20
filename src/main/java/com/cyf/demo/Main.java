package com.cyf.demo;

import com.cyf.demo.entity.Article;
import com.cyf.demo.entity.LoginInfo;
import com.cyf.demo.entity.OfficialAccount;
import com.cyf.demo.entity.PageData;
import com.cyf.demo.utils.AccountManager;
import com.cyf.demo.utils.SearchManager;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        try {
            testSearch(null, null);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void testLogin(Scanner input) throws IOException, InterruptedException {
        // 点击登录按钮，返回登录信息
        LoginInfo info = AccountManager.login();
        if (info != null) {
            String qrCode = info.getQrCode(); // 二维码地址
            System.out.println("二维码URL = " + qrCode);

            try {
                // 模拟前端展示二维码后调用另一接口查询登录状态
                Runtime.getRuntime().exec("cmd /c start " + qrCode);
            } catch (IOException e) {
                e.printStackTrace();
            }

            queryStatus(input, info);

        }
    }

    private static void queryStatus(Scanner input, LoginInfo info) throws IOException, InterruptedException {
        LoginInfo latestInfo = AccountManager.queryLoginStatus(info);
        if (latestInfo != null) {
            System.out.println("error_code = " + latestInfo.getErrorCode());
            switch (latestInfo.getErrorCode()) {
                case 408: // 继续调用
                    queryStatus(input, latestInfo);
                    break;
                case 404: // 继续调用
                    queryStatus(input, latestInfo);
                    break;
                case 403: // 继续调用
                    queryStatus(input, latestInfo);
                    break;
                case 405: // 登录成功，取得微信信息
                    String username = latestInfo.getUsername();
                    String headurl = latestInfo.getHeadurl();
                    System.out.println("微信昵称 = " + username + "\n微信头像URL = " + headurl);

                    testSearch(input, latestInfo);

                    break;
                case 500: // 二维码过期，取出新二维码URL，并继续调用
                    String qrCode = latestInfo.getQrCode();
                    System.out.println("新二维码URL = " + qrCode);
                    try {
                        // 模拟前端刷新二维码后调用另一接口查询登录状态
                        Runtime.getRuntime().exec("cmd /c start " + qrCode);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    queryStatus(input, latestInfo);
                    break;
            }
        }
    }

    private static void testSearch(Scanner input, LoginInfo info) throws IOException, InterruptedException {
        if (input == null) {
            input = new Scanner(System.in);
        }
        System.out.println("搜公众号(1)/搜文章(2)/登录(3)/退出登录(4)");
        int type = input.nextInt();

        input.nextLine(); // **** 接收"\n"

        if (type == 1) {
            System.out.println("输入公众号关键词");
            String keywords = input.nextLine();

            PageData<OfficialAccount> res = SearchManager.searchOA(info == null ?
                    new PageData<OfficialAccount>(keywords) :
                    new PageData<OfficialAccount>(keywords, info.getUa(), info.getCallBackCookies()));

            checkOA(input, res);
        } else if (type == 2) {
            System.out.println("输入文章关键词");
            String keywords = input.nextLine();

            PageData<Article> res = SearchManager.searchArticle(info == null ?
                    new PageData<Article>(keywords).setMaxPage(3) :
                    new PageData<Article>(keywords, info.getUa(), info.getCallBackCookies()).setMaxPage(3));

            checkRes(input, res);
        } else if (type == 3) {
            if (info == null) {
                testLogin(input);
                return;
            } else {
                System.out.println("已登录");
            }
        } else if (type == 4) {
            if (info != null) {
                if (testLogout(info) == 200) {
                    info = null;
                }
            } else {
                System.out.println("未登录");
            }
        } else {
            System.out.println("无效指令");
        }
        testSearch(input, info);
    }

    private static void checkOA(Scanner input, PageData<OfficialAccount> res) throws IOException {
        System.out.println("是否登录：" + res.isHasLogin());
        System.out.println(res.getCookies());
        List<OfficialAccount> oas = res.getRecords();
        oas.forEach(officialAccount -> System.out.println(officialAccount.toString()));
//        System.out.println(oas.stream()
//                .filter(officialAccount -> officialAccount.getTitle().equals(res.getKeywords()))
//                .findFirst()
//                .orElse(new OfficialAccount())
//                .getIdentify());

        System.out.println("输入公众号关键词");
        String keywords = input.nextLine();

        if (!keywords.trim().equals("exit")) {
            res.setKeywords(keywords);
            PageData<OfficialAccount> resp = SearchManager.searchOA(res);
            checkOA(input, resp);
        }
    }

    private static void checkRes(Scanner input, PageData<Article> res) throws IOException, InterruptedException {
        System.out.println("是否登录：" + res.isHasLogin());
        System.out.println(res.getCookies());
        System.out.println("当前页：" + res.getPageNum());
        System.out.println("当前页记录数：" + res.getPageSize());
        System.out.println("总记录数：" + res.getTotalRecords());
        System.out.println("总页数" + res.getTotalPage());
        if (res.getPageNum() < res.getTotalPage()) {
            System.out.println("输入页数");
            int page = input.nextInt();

            input.nextLine(); // **** 接收"\n"

            if (page > 0) {
                res.setPageNum(page);
                PageData<Article> resp = SearchManager.searchArticle(res);
                checkRes(input, resp);
            }
        }
    }

    private static int testLogout(LoginInfo info) {
        int statusCode = AccountManager.logout(info);
        System.out.println("退出登录 = " + statusCode);
        return statusCode;
    }

}
