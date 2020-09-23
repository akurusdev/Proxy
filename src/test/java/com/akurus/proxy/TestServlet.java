package com.akurus.proxy;

import com.akurus.proxy.model.DataNotification;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TestServlet extends HttpServlet {

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(4);

    private CountDownLatch countDownLatch;

    public TestServlet(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    private ObjectMapper mapper = new ObjectMapper();
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final AsyncContext ctxt = req.startAsync();

        List<DataNotification> dataNotification = mapper.readValue(req.getInputStream(), new TypeReference<List<DataNotification>>(){});
        ctxt.start(() -> {
            executorService.schedule(() -> {
                ctxt.getResponse().getWriter().write("Ok!");
                ctxt.complete();
                if(dataNotification !=null && !dataNotification.isEmpty()){
                    dataNotification.forEach(it->{
                        countDownLatch.countDown();
                    });
                }
                return null;

            }, 2000, TimeUnit.MILLISECONDS);
        });
    }


    @Override
    public void destroy() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.out.println("Executor service shutdown interrupted");
        }
    }
}
