package com.akurus.proxy.service;
import com.akurus.proxy.model.DataNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class CacheServiceImpl implements CacheService {
    private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private ConcurrentLinkedQueue<DataNotification> concurrentLinkedQueue = new ConcurrentLinkedQueue();
    private volatile Instant lastUpdateDate = Instant.now();

    @Value(value = "${proxy.batch.size:10}")
    private Integer batchSize;

    @Value(value = "${proxy.duration.scheduler.time.sec:60}")
    private Long durationTimeSec;

    @Autowired
    AsyncRequestSenderService asyncRequestSenderService;

    @Override
    public void addSensorData(DataNotification dataNotification){
        try {
            readWriteLock.readLock().lock();
            concurrentLinkedQueue.add(dataNotification);
          } finally {
            readWriteLock.readLock().unlock();
        }
        if(batchSize <= concurrentLinkedQueue.size()){
            sendMessage();
        }
    }

    @Override
    public void addSensorDataList(List<DataNotification> dataNotification){
        dataNotification.forEach(it ->{
            addSensorData(it);
        });
    }

    private void sendMessage(){
        List<DataNotification> dataNotificationList = new ArrayList<>();
        try {
            readWriteLock.writeLock().lock();
            Long delta = Instant.now().getEpochSecond() - lastUpdateDate.getEpochSecond();
            int currentBatchSize = concurrentLinkedQueue.size();
            if(durationTimeSec <= delta && !concurrentLinkedQueue.isEmpty()){
                dataNotificationList.addAll(concurrentLinkedQueue);
                concurrentLinkedQueue.clear();
            }else if(batchSize <= currentBatchSize){
                for(int i = 0; i < batchSize; i++){
                    dataNotificationList.add(concurrentLinkedQueue.poll());
                }
            }
            asyncRequestSenderService.sendNotificationsData(dataNotificationList);
            lastUpdateDate = Instant.now();

        }finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Scheduled(cron = "${proxy.cron:*/1 * * * * *}")
    private void checkUpdateData(){
        Long delta = Instant.now().getEpochSecond() - lastUpdateDate.getEpochSecond();
        if(durationTimeSec <= delta && !concurrentLinkedQueue.isEmpty()){
            sendMessage();
        }
    }
}
