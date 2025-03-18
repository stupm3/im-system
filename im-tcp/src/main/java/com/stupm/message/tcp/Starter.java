package com.stupm.message.tcp;

import com.stupm.message.codec.config.BootstrapConfig;
import com.stupm.message.tcp.receiver.MessageReceiver;
import com.stupm.message.tcp.redis.RedisManager;
import com.stupm.message.tcp.register.RegistryZK;
import com.stupm.message.tcp.register.ZKit;
import com.stupm.message.tcp.server.ImServer;
import com.stupm.message.tcp.server.ImWebSocketServer;
import com.stupm.message.tcp.utils.MqFactory;
import org.I0Itec.zkclient.ZkClient;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Starter {
    public static void main(String[] args) throws FileNotFoundException {
        if(args.length > 0) {
            start(args[0]);
        }
    }

    private static void start(String path){
        try{
            Yaml yaml = new Yaml();
            InputStream fileInputStream = new FileInputStream(path);
            BootstrapConfig bootstrap = yaml.loadAs(fileInputStream, BootstrapConfig.class);
            new ImServer(bootstrap.getIm()).start();
            new ImWebSocketServer(bootstrap.getIm()).start();
            RedisManager.init(bootstrap);
            MqFactory.init(bootstrap.getIm().getRabbitmq());
            MessageReceiver.init(bootstrap.getIm().getBrokerId().toString());
            registerZK(bootstrap);

        }catch (Exception e){
            e.printStackTrace();
            System.exit(500);
        }
    }

    public static void registerZK(BootstrapConfig bootstrap) throws UnknownHostException {
        String hostAddress = InetAddress.getLocalHost().getHostAddress();
        ZkClient zkClient = new ZkClient(bootstrap.getIm().getZkConfig().getZkAddr(), bootstrap.getIm().getZkConfig().getZkConnectTimeOut());
        ZKit zKit = new ZKit(zkClient);
        RegistryZK registerZK = new RegistryZK(zKit, hostAddress, bootstrap.getIm());
        new Thread(registerZK).start();
    }
}
