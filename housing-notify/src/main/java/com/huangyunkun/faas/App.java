package com.huangyunkun.faas;

import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.StreamRequestHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;


public class App implements StreamRequestHandler {
    private static final String REGION = "高新南区";

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        context.getLogger().info("开始处理");

        String targetUrl = "https://gfdj.cdfgj.gov.cn/lottery/accept/projectList";
        String token = System.getenv("DINGDING_TOKEN");
        String notifyUrl = String.format("https://oapi.dingtalk.com/robot/send?access_token=%s", token);

        String htmlSource = Request.Get(targetUrl).execute().returnContent().asString();

        Document document = Jsoup.parse(htmlSource);
        Elements infos = document.select("tbody#_projectInfo tr");

        List<HouseInfo> availableHouseInfo = infos.stream().map(element -> {
            Elements trs = element.select("td");
            HouseInfo houseInfo = new HouseInfo();

            houseInfo.setName(StringUtils.trimToEmpty(trs.get(3).text()));
            houseInfo.setRegion(StringUtils.trimToEmpty(trs.get(2).text()));
            houseInfo.setStatus(StringUtils.trimToEmpty(trs.get(11).text()));

            return houseInfo;
        }).filter(houseInfo -> {
            if (REGION.equalsIgnoreCase(houseInfo.getRegion()) && "正在报名".equalsIgnoreCase(houseInfo.getStatus())) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());

        context.getLogger().info("获取到有效房产数量:" + availableHouseInfo.size());

        if (!availableHouseInfo.isEmpty()) {
            String jsonBody =
                    "{\n" + "     \"msgtype\": \"text\",\n" + "     \"text\": {\n" + "         \"content\": \"监控到"
                            + REGION + "有新房源:" + availableHouseInfo.get(0).getName() + "\"\n" + "     }\n" + " }";
            Request.Post(notifyUrl).bodyString(jsonBody, ContentType.APPLICATION_JSON).execute();
        }
    }

    public class HouseInfo {
        private String name;
        private String region;
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }
    }
}
