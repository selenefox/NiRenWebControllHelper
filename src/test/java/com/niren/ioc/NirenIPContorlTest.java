package com.niren.ioc;

import com.sevilinma.iot.netrelay.tcpserver.NiRenSimpleServer;
import com.sevilinma.iot.netrelay.tcpserver.net.model.DeviceBean;

import java.util.List;
import java.util.Scanner;

public class NirenIPContorlTest {
    public static void main(String[] args) {
        NiRenSimpleServer server = new NiRenSimpleServer("192.168.1.211",6000);
        server.start();
        Scanner scanner = new Scanner(System.in);
        String cmd = scanner.nextLine();
        while (!cmd.equals("exit")){
            if(cmd.equals("list")){
                List<DeviceBean> devices = server.listDevice();
                devices.forEach(deviceBean -> {
                    System.out.println(deviceBean.getIp()+":"+deviceBean.getPort() +"-->" + deviceBean.getTargetip()+":"+deviceBean.getTargetport());
                });
            }else if(cmd.indexOf("@:") == 0){
                String ip = cmd.split(":")[1];
                String c = cmd.split(":")[2];
                server.postCMD(ip,c);
            }else if(cmd.indexOf("O:") == 0){
                String ip = cmd.split(":")[1];
                String n = cmd.split(":")[2];
                String c = cmd.split(":")[3];
                server.setupDOChannel(ip, Integer.valueOf(n),c.equals("1"));
            }
            cmd = scanner.nextLine();
        }
        System.out.println("正在关闭服务端。");
        server.stop();
    }
}
