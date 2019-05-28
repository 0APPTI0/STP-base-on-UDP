package UDP;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpClient {
    private static final int MAXNUM = 5; // 设置重发数据的最多次数
    private static final int TIMEOUT = 5000;  //设置接收数据的超时时间
    private static final int CLIENT_PORT = 2222;
    private static final int SERVER_PORT = 3222;
    private static final int REV_SIZE = 1024; //接收数据的存储空间大小

    public static void main(String[] args) throws IOException {
        String str_send = "Hello UDPserver"; //要发送的字串
        byte[] buf_rev = new byte[REV_SIZE];     //要接收的存储空间

        /*第一步 实例化DatagramSocket*/
        DatagramSocket mSoc = new DatagramSocket(CLIENT_PORT);
        mSoc.setSoTimeout(TIMEOUT);              //设置接收数据时阻塞的最长时间  

        /*第二步 实例化用于发送的DatagramPacket和用于接收的DatagramPacket*/
        InetAddress inetAddress = InetAddress.getLocalHost();
        DatagramPacket data_send = new DatagramPacket(str_send.getBytes(),
                str_send.length(), inetAddress, SERVER_PORT);

        DatagramPacket data_rev = new DatagramPacket(buf_rev, REV_SIZE);


        /*第三步 DatagramPacket send发送数据，receive接收数据*/
        int send_count = 0; // 重发数据的次数
        boolean revResponse = false; // 是否接收到数据的标志位
        while (!revResponse && send_count < MAXNUM) {
            try {
                mSoc.send(data_send); //发送数据
                mSoc.receive(data_rev);//接收数据
                if (!data_rev.getAddress().getHostAddress()
                        .equals(InetAddress.getLocalHost().getHostAddress())) {
                    throw new IOException(
                            "Received packet from an umknown source");
                }
                revResponse = true;
            } catch (InterruptedIOException e) {
                // 如果接收数据时阻塞超时，重发并减少一次重发的次数
                send_count += 1;
                System.out.println("Time out," + (MAXNUM - send_count)
                        + " more tries...");
            }
        }
        if (revResponse) {
            // 如果收到数据，则打印出来
            System.out.println("client received data from server：");
            String str_receive = new String(data_rev.getData(), 0,
                    data_rev.getLength())
                    + " from "
                    + data_rev.getAddress().getHostAddress()
                    + ":"
                    + data_rev.getPort();
            System.out.println(str_receive);
            // 由于dp_receive在接收了数据之后，其内部消息长度值会变为实际接收的消息的字节数，
            // 所以这里要将dp_receive的内部消息长度重新置为1024
            data_rev.setLength(REV_SIZE);
        } else {
            // 如果重发MAXNUM次数据后，仍未获得服务器发送回来的数据，则打印如下信息
            System.out.println("No response -- give up.");
        }

        /*第四步 关闭DatagramPacket*/
        mSoc.close();
    }

}
