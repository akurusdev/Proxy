package com.akurus.proxy.controllers;

import com.akurus.proxy.model.DataNotification;
import com.akurus.proxy.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SensorsController {
    private static Logger log = LoggerFactory.getLogger(SensorsController.class);

    @Autowired
    private CacheService cacheService;

    @PostMapping(path= "/sensor/notifications", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Object>  handleNotifications(@RequestBody DataNotification dataNotification){
        cacheService.addSensorData(dataNotification);
        log.info("Notifications--> ");
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
