package com.akurus.proxy.service;
import com.akurus.proxy.model.DataNotification;
import java.util.List;


public interface CacheService {
    void addSensorData(DataNotification dataNotification);
    void addSensorDataList(List<DataNotification> dataNotification);
}
