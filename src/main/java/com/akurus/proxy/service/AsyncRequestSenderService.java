package com.akurus.proxy.service;

import com.akurus.proxy.model.DataNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Service
public class AsyncRequestSenderService {

    @Value(value = "${proxy.http.remote.url}")
    private String url;

    @Autowired
    private RestTemplate restTemplate;


    @Autowired
    public void setCacheService(@Lazy CacheService cacheService) {
        this.cacheService = cacheService;
    }

    private CacheService cacheService;

    @Async("asyncExecutor")
    public void sendNotificationsData(List<DataNotification> dataNotification) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<DataNotification>> request = new HttpEntity<>(dataNotification, headers);
            ResponseEntity<String> result = restTemplate.postForEntity(
                    url,
                    request,
                    String.class
            );

            Future<ResponseEntity> resultFuture = new AsyncResult(result);
            while (!resultFuture.isDone()) {
                Thread.sleep(10);
            }

            if(resultFuture.get().getStatusCodeValue() != HttpStatus.OK.value()){
                cacheService.addSensorDataList(dataNotification);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }catch (Exception e) {
            cacheService.addSensorDataList(dataNotification);
        }
    }


}
