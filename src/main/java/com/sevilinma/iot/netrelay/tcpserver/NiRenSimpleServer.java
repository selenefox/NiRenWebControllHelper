package com.sevilinma.iot.netrelay.tcpserver;

import com.sevilinma.iot.netrelay.tcpserver.net.NRServerListener;
import com.sevilinma.iot.netrelay.tcpserver.net.model.DeviceBean;

import java.util.List;

public class NiRenSimpleServer {
    private NRServerListener listener;

    public NiRenSimpleServer(String bindaddress, int port) {
        this.listener = new NRServerListener(bindaddress, port);
    }

    public boolean start(){
        if(!listener.serverMark()){
            listener.start();
        }
        return false;
    }

    public void stop(){
        if(listener.serverMark()){
            listener.stop();
        }
    }

    public List<DeviceBean> listDevice(){
        return listener.listDevice();
    }

    public void postCMD(String ip,String cmd){
        listener.postCMD(ip, cmd);
    }

    public void setupDOChannel(String ip, int num, boolean isOpen){
        listener.setupDOChannel(ip, num, isOpen);
    }

}
