package DataTransForm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Sender {
    private static final String LOG_FILE_NAME = "sender.txt";

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        // 先删除旧的log文件
        File f = new File(LOG_FILE_NAME);
        if (f.exists()) {
            f.delete();
        }

        System.out.println(getDateString() + " Sender main BEGIN >>>>>>\n");
        LogToFile("Sender main BEGIN >>>>>>\n");

        try {
            // 创建发送方的套接字，IP默认为本地，不指定端口号则系统随机设置一个可用的端口号
            DatagramSocket sendSocket = new DatagramSocket(/*12341*/);

            for (int i = 0; i < 5; i++) {
                // 确定要发送的内容
                String msg = "你好，接收方：" + i;
                // 由于数据报的数据是以字节数组的形式存储的，所以转为转换数据
                byte[] buffer = msg.getBytes();

                // 确定发送方的IP地址及端口号，地址为本地机器地址
                int port = 12365;
                InetAddress ip = InetAddress.getLocalHost();

                // 创建发送类型的数据库
                // 构造数据报包，用来将长度为 length 的包发送到指定主机上的指定端口号
                DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, ip, port);

                // 通过套接字发送数据
                System.out.println(getDateString() + " Sender send packet ..");
                LogToFile("Sender send packet ..");
                sendSocket.send(sendPacket);

                // 确定接收反馈数据的缓冲存储器，即存储数据的字节数组
                byte[] getBuffer = new byte[1024];

                // 确定接收类型的数据报
                DatagramPacket getPacket = new DatagramPacket(getBuffer, getBuffer.length);

                // 通过套接字接收数据
                System.out.println(getDateString() + " Sender receive packet ...");
                LogToFile("Sender receive packet ...");
                sendSocket.receive(getPacket);

                // 解析反馈的消息，并输出
                String backMsg = new String(getBuffer, 0, getPacket.getLength());
                System.out.println(getDateString() + " Sender 接收方返回的消息：" + backMsg);
                LogToFile("Sender 接收方返回的消息：" + backMsg);

                System.out.println(getDateString() + " Sender waiting 5s ...\n");
                LogToFile("Sender waiting 5s ...\n");

                Thread.sleep(5000);
            }

            // 关闭套接字
            sendSocket.close();
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.out.println(getDateString() + " Sender main END <<<<<<");
        LogToFile("Sender main END <<<<<<");
    }

    public static String getDateString() {
        SimpleDateFormat df = new SimpleDateFormat("MM-dd HH:mm:ss.SSS"); // 设置日期格式
        return df.format(new Date()); // new Date()为获取当前系统时间
    }

    public static void LogToFile(String log) {
        File file = new File(LOG_FILE_NAME);

        try {
            FileWriter fw = new FileWriter(file, true);
            fw.write(getDateString() + " " + log + '\n');
            fw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}