package com.sevilinma.iot.netrelay.tcpserver.net;

import com.sevilinma.iot.netrelay.atcommand.ATCommands;
import com.sevilinma.iot.netrelay.tcpserver.net.model.DeviceBean;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class NRServerHandel {
    private Socket socket;
    private DeviceBean deviceinfo;
    private Scanner scanner;
    private OutputStream os;
    private boolean threadmark;
    private List<String> buffer;
    private List<String> cache;
    private Thread writeThread;
    private Thread readThread;

    private boolean processCMDPollMark = true;

    public NRServerHandel(Socket socket, DeviceBean deviceinfo) {
        this.socket = socket;
        this.deviceinfo = deviceinfo;
        threadmark = false;
        buffer = Collections.synchronizedList(new ArrayList<>());
        cache = Collections.synchronizedList(new ArrayList<>());
    }

    public void start(){
        if(!threadmark) {
            try {
                scanner = new Scanner(socket.getInputStream());
                os = socket.getOutputStream();
                threadmark = true;
                deviceinfo();

                writeThread = new Thread(() -> {
                    while (threadmark) {
                        if (buffer.size() > 0) {
                            String cmd = buffer.get(0);
                            buffer.remove(0);
                            writeCMD(cmd);
                        }
                        try {
                            TimeUnit.MILLISECONDS.sleep(500);
                        } catch (InterruptedException ie) {
                            threadmark = false;
                        }
                    }
                    System.out.println(deviceinfo.getIp()+":设备 writeThread close.");
                });
                writeThread.start();

                readThread = new Thread(() -> {
                    String out;
                    while (threadmark) {
                        if(scanner.hasNext()) {
                            out = scanner.nextLine();
                            if(!out.equals("OK") && !out.equals("ERROR"))
                                if(processCMDPollMark){
                                    processCMDResponsePool(out);
                                }else{
                                    cache.add(out);
                                }
                            System.out.println(out);
                        }
                    }
                    System.out.println(deviceinfo.getIp()+":设备 readThread close.");
                });
                readThread.start();
                try{
                    TimeUnit.SECONDS.sleep(1);
                }catch (Exception ignored){}
                refreshIOChn();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

    }

    public void close(){
        if(threadmark){
            threadmark = false;
            readThread.interrupt();
            writeThread.interrupt();
        }
    }

    /**
     * 将需要发送的指令压入buffer
     * @param cmd 指令
     */
    public void postCMD(String cmd){
        buffer.add(cmd);
    }

    private void deviceinfo(){
        if(threadmark) {
            writeCMD("AT+IP=?");
            String response = scanner.nextLine();
            deviceinfo.setIp(processATResponseString(response));
            writeCMD("AT+PORT=?");
            response = scanner.nextLine();
            response = processATResponseString(response);
            deviceinfo.setPort(response != null ? Integer.valueOf(response) : 0);
            writeCMD("AT+DIP=?");
            response = scanner.nextLine();
            deviceinfo.setTargetip(processATResponseString(response));
            writeCMD("AT+DPORT=?");
            response = scanner.nextLine();
            response = processATResponseString(response);
            deviceinfo.setTargetport(response != null ? Integer.valueOf(response) : 0);
        }
    }

    private String processATResponseString(String response){
        String r;
        if(response.contains("ERROR")){
            r = null;
        }else{
            int last = response.indexOf(ATCommands.LF);
            if(last > 0){
                r = response.substring(response.indexOf(":")+1, last);
            }else{
                r = response.substring(response.indexOf(":")+1);
            }
        }
        return r;
    }

    private void processCMDResponsePool(String response){
        if(response.contains(ATCommands.DI_CHN_HEAD)){
            int n = Integer.valueOf(response.substring(ATCommands.DI_CHN_HEAD.length(), response.indexOf(":")));
            int v = Integer.valueOf(response.substring(response.indexOf(":")+1));
            if(deviceinfo.getDiChn() != null && deviceinfo.getDiChn().length >= n){
                deviceinfo.getDiChn()[n-1] = v;
            }
        }else if(response.contains(ATCommands.DO_CHN_HEAD)){
            int n = Integer.valueOf(response.substring(ATCommands.DO_CHN_HEAD.length(), response.indexOf(":")));
            int v = Integer.valueOf(response.substring(response.indexOf(":")+1,response.indexOf(",")));
            if(deviceinfo.getDoChn() != null && deviceinfo.getDoChn().length>=n){
                deviceinfo.getDoChn()[n-1] = v;
            }
        }else if(response.contains(ATCommands.IP_HEAD)){
            response = processATResponseString(response);
            deviceinfo.setIp(processATResponseString(response));
        }else if(response.contains(ATCommands.DIP_HEAD)){
            response = processATResponseString(response);
            deviceinfo.setTargetip(processATResponseString(response));
        }else if(response.contains(ATCommands.PORT_HEAD)){
            response = processATResponseString(response);
            deviceinfo.setPort(response != null ? Integer.valueOf(response) : 0);
        }else if(response.contains(ATCommands.DPORT_HEAD)){
            response = processATResponseString(response);
            deviceinfo.setTargetport(response != null ? Integer.valueOf(response) : 0);
        }
    }
    private void writeCMD(String cmd){
        cmd += ATCommands.LF;
        try{
            os.write(cmd.getBytes());
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }
    /**
     * 刷新所有IO端口状态及端口数量信息
     */
    private void refreshIOChn(){
        boolean t = processCMDPollMark;
        processCMDPollMark = false;
        cache.clear();
        writeCMD("AT+STACH0=?");
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException ie) {
            threadmark = false;
        }
        if(cache.size() > 0){
            int[] doChn = new int[cache.size()];
            cache.forEach(s -> {
                if(s.indexOf(ATCommands.DO_CHN_HEAD) == 0){
                    int n = Integer.valueOf(s.substring(ATCommands.DO_CHN_HEAD.length(), s.indexOf(":")));
                    int v = Integer.valueOf(s.substring(s.indexOf(":")+1,s.indexOf(",")));
                    doChn[n-1] = v;
                }
            });
            cache.clear();
            deviceinfo.setDoChn(doChn);
        }
        writeCMD("AT+OCCH0=?");
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException ie) {
            threadmark = false;
        }
        if(cache.size() > 0){
            int[] diChn = new int[cache.size()];
            cache.forEach(s -> {
                if(s.indexOf(ATCommands.DI_CHN_HEAD) == 0){
                    int n = Integer.valueOf(s.substring(ATCommands.DI_CHN_HEAD.length(), s.indexOf(":")));
                    int v = Integer.valueOf(s.substring(s.indexOf(":")+1));
                    diChn[n-1] = v;
                }
            });
            cache.clear();
            deviceinfo.setDiChn(diChn);
        }
        processCMDPollMark = t;
    }

    /**
     * 设置DO通道的开关情况
     * @param channelNum 通道号 从1 开始 0 表示全部
     * @param isOpen true:1 false:0
     */
    public void setupDOChannel(int channelNum, boolean isOpen){
        postCMD("AT+STACH"+channelNum+"="+(isOpen?"1":"0"));
    }
    /**
     * 设置DO通道的开关情况
     * @param channelNum 通道号 从1 开始 0 表示全部
     * @param time 延时 isOpen状态后再取反
     * @param isOpen true:1 false:0
     */
    public void setupDOChannelEx(int channelNum, int time, boolean isOpen){
        postCMD("AT+STACH"+channelNum+"="+(isOpen?"1":"0")+","+time);
    }

    /**
     * 刷新设备DO通道状态
     * @param channelNum 通道号 从1 开始 0 表示全部
     */
    public void refreshDOChannelStatus(int channelNum){
        postCMD("AT+STACH"+channelNum+"=?");
    }
}
