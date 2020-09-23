package com.akurus.proxy.model;


public class DataNotification {
    public DataNotification(Long timeStamp, String ip, String data){
        this.timeStamp = timeStamp;
        this.ip = ip;
        this.data = data;
    }
    public DataNotification(){}

    private Long timeStamp;
    private String ip;
    private String data;


    public Long getTimeStamp() {
        return timeStamp;
    }

    public String getIp() {
        return ip;
    }

    public String getData() {
        return data;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setData(String data) {
        this.data = data;
    }
}
