package UDP;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpServer {

    private static final int SERVER_PORT = 3222;
    private static final int REV_SIZE = 1024; // 接收数据的存储空间大小

    public static void main(String[] args) throws IOException {
        byte[] buf_rev = new byte[REV_SIZE];
        String str_send = "Hello UDPclient";
        /* 第一步 实例化DatagramSocket */
        DatagramSocket mSoc = new DatagramSocket(SERVER_PORT);

        /* 第二步 实例化用于接收的DatagramPacket 并从DatagramSocket接收数据 */
        DatagramPacket data_rev = new DatagramPacket(buf_rev, REV_SIZE);
        boolean f = true;
        while (f) {
            mSoc.receive(data_rev);
            InetAddress inetAddress = data_rev.getAddress();
            int port = data_rev.getPort();
            System.out.println("server received data from client：");
            String str_rev = new String(data_rev.getData(), 0,
                    data_rev.getLength())
                    + " from " + inetAddress.getHostAddress() + ":" + port;
            System.out.println(str_rev);

            /* 第三步 实例化用于发送的DatagramPacket，并从DatagramSocket中发送出去 */
            DatagramPacket data_send = new DatagramPacket(str_send.getBytes(),
                    str_send.length(), inetAddress, port);
            mSoc.send(data_send);

            /*
             * 由于dp_receive在接收了数据之后，其内部消息长度值会变为实际接收的消息的字节数，
             * 所以这里要将dp_receive的内部消息长度重新置为1024
             */
            data_rev.setLength(REV_SIZE);
        }
        mSoc.close();

    }
}
