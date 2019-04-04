package com.sevilinma.iot.netrelay.tcpserver.net.model;

public class DeviceBean {
    private String ip;
    private int port;
    private String targetip;
    private int targetport;

    private int[]  diChn;
    private int[]  doChn;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getTargetip() {
        return targetip;
    }

    public void setTargetip(String targetip) {
        this.targetip = targetip;
    }

    public int getTargetport() {
        return targetport;
    }

    public void setTargetport(int targetport) {
        this.targetport = targetport;
    }

    public int[] getDiChn() {
        return diChn;
    }

    public void setDiChn(int[] diChn) {
        this.diChn = diChn;
    }

    public int[] getDoChn() {
        return doChn;
    }

    public void setDoChn(int[] doChn) {
        this.doChn = doChn;
    }
}
