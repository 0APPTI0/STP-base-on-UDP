package DataTransForm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Receiver {
    private static final String LOG_FILE_NAME = "receiver.txt";

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        // 先删除旧的log文件
        File f = new File(LOG_FILE_NAME);
        if (f.exists()) {
            f.delete();
        }

        System.out.println(getDateString() + " Receiver main BEGIN >>>>>>\n");
        LogToFile("Receiver main BEGIN >>>>>>\n");

        try {
            // 确定接受方的IP和端口号，IP地址为本地机器地址
            InetAddress ip = InetAddress.getLocalHost();
            int port = 12365;
            for(int i = 0; i < 5; i++) {
                // 创建接收方的套接字,并指定端口号和IP地址
                DatagramSocket getSocket = new DatagramSocket(port, ip);

                // 确定数据报接受的数据的数组大小
                byte[] buf = new byte[1024];

                // 创建接受类型的数据报，数据将存储在buf中
                DatagramPacket getPacket = new DatagramPacket(buf, buf.length);

                // 通过套接字接收数据, 此方法是阻塞的，会一直等待消息
                System.out.println(getDateString() + " Receiver begin receive message ...");
                LogToFile("Receiver begin receive message ...");
                getSocket.receive(getPacket);
                System.out.println(getDateString() + " Receiver end receive message ...");
                LogToFile("Receiver end receive message ...");

                // 解析发送方传递的消息，并打印
                String getMes = new String(buf, 0, getPacket.getLength());
                System.out.println(getDateString() + " Receiver 对方发送的消息：" + getMes);
                LogToFile("Receiver 对方发送的消息：" + getMes);

                // 通过数据报得到发送方的IP和端口号，并打印
                InetAddress sendIP = getPacket.getAddress();
                int sendPort = getPacket.getPort();
                System.out.println(getDateString() + " Receiver 对方的IP地址是：" + sendIP.getHostAddress());
                LogToFile("Receiver 对方的IP地址是：" + sendIP.getHostAddress());
                System.out.println(getDateString() + " Receiver 对方的端口号是：" + sendPort);
                LogToFile("Receiver 对方的端口号是：" + sendPort + '\n');

                // 通过数据报得到发送方的套接字地址
                SocketAddress sendAddress = getPacket.getSocketAddress();

                // 确定要反馈发送方的消息内容，并转换为字节数组
                String feedback = "接收方说：我收到了消息【" + getMes + "】";
                byte[] backBuf = feedback.getBytes();

                // 创建发送类型的数据报
                DatagramPacket sendPacket = new DatagramPacket(backBuf, backBuf.length, sendAddress);

                // 通过套接字发送数据
                getSocket.send(sendPacket);
                // 关闭套接字
                getSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(getDateString() + " Receiver main END <<<<<<");
        LogToFile("Receiver main END <<<<<<");
    }

    public static String getDateString() {
        SimpleDateFormat df = new SimpleDateFormat("MM-dd HH:mm:ss.SSS"); // 设置日期格式
        return df.format(new Date()); // new Date()为获取当前系统时间
    }

    public static void LogToFile(String log) {
        File sdFile = new File(LOG_FILE_NAME);

        try {
            FileWriter fw = new FileWriter(sdFile, true);
            fw.write(getDateString() + " " + log + '\n');
            fw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}