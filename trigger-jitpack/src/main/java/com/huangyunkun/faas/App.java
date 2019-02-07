package com.huangyunkun.faas;

import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.StreamRequestHandler;
import com.google.common.collect.Lists;
import org.apache.http.client.fluent.Request;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class App implements StreamRequestHandler {
    public static void main(String[] args) {
        System.out.println("Hello World!");
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        List<String> projectList = Lists.newArrayList("apache/incubator-dubbo",
                "apache/incubator-dubbo-ops",
                "Var3D/var3dframe"
        );


        ExecutorService executorService = Executors.newFixedThreadPool(3);
        for (String projectName : projectList) {
            executorService.submit(() -> {
                try {
                    context.getLogger().info("正在处理:" + projectName);
                    Request.Get("https://jitpack.io/com/github/" + projectName + "/-SNAPSHOT").connectTimeout(2000)
                            .execute();
                } catch (IOException e) {
                    //ignore
                }
            });
        }

        try {
            executorService.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //ignore
        }
    }
}
