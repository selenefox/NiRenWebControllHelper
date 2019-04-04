package com.sevilinma.iot.netrelay.tcpserver.net;

import com.sevilinma.iot.netrelay.tcpserver.net.model.DeviceBean;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NRServerListener implements Runnable{
    private Thread mainthread;
    private boolean threadmark;
    private String bindaddress;
    private int port;

    private Map<String,DeviceBean> devicesMap;
    private Map<String,NRServerHandel> devicesThreadMap;

    public NRServerListener(String bindaddress, int port) {
        this.bindaddress = bindaddress;
        this.port = port;
        //this.devicesMap = Collections.synchronizedMap(new HashMap<>());
        this.devicesMap = new ConcurrentHashMap<>();
        this.devicesThreadMap = new ConcurrentHashMap<>();
    }

    public void start(){
        if(!threadmark) {
            mainthread = new Thread(this);
            mainthread.setDaemon(true);
            mainthread.start();
        }
    }

    public void stop(){
        if(threadmark) {
            threadmark = false;
            try {
                mainthread.join(5000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            devicesThreadMap.forEach((s, t) -> {
                t.close();
            });
        }
    }

    public boolean serverMark(){
        return threadmark;
    }

    public List<DeviceBean> listDevice(){
        List<DeviceBean> all = new ArrayList<>();
        devicesMap.forEach((s, deviceBean) -> {
            all.add(deviceBean);
        });
        return all;
    }

    public void postCMD(String ip,String cmd){
        NRServerHandel handel = devicesThreadMap.get(ip);
        if(handel != null){
            handel.postCMD(cmd);
        }
    }

    public void refreshDeviceStatus(String ip){
        NRServerHandel handel = devicesThreadMap.get(ip);
        if(handel != null){
            handel.refreshIOChn();
        }
    }

    @Override
    public void run() {
        try {
            InetAddress ipaddress = InetAddress.getByName(bindaddress);
            ServerSocket serverSocket = new ServerSocket(port,50,ipaddress);
            System.out.println("服务器端--开始监听");
            threadmark = true;
            while (threadmark) {
                Socket socket = serverSocket.accept();
                DeviceBean d_info = new DeviceBean();
                NRServerHandel handel = new NRServerHandel(socket,d_info);
                handel.start();
                devicesMap.put(socket.getInetAddress().getHostAddress(),d_info);
                devicesThreadMap.put(socket.getInetAddress().getHostAddress(),handel);
            }
        }catch (Exception e){
            e.printStackTrace();
            threadmark = false;
        }
    }
}
