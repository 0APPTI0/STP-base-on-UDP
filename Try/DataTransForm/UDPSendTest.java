package DataTransForm;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;

public class UDPSendTest{

    public static void main(String[] args) throws Exception{
        //1、创建DatagramSocket用于UDP数据传送。
        DatagramSocket socket = new DatagramSocket();

        //2、创建需要发送的数据包
        byte[] buf = "Hello World.".getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName("192.168.136.222"), 7879);

        //3、发送
        socket.send(packet);

        //4、关闭连接
        socket.close();
    }
}