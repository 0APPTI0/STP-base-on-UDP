import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Receiver {

    private String receiver_port;

    private String file_name;

    public Receiver(String receiver_port, String file_name) {
        this.receiver_port = receiver_port;
        this.file_name = file_name;
    }


    static InetAddress ip;

    static {
        try {
            ip = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    static int port = 12365;


    private int ConnectionState = 0;


    //在收到信息之后，接收方可以根据发送方的相关信息得到发送方的Address。
    SocketAddress sendAddress ;


    DatagramSocket getSocket;

    {
        try {
            getSocket = new DatagramSocket(port, ip);
            getSocket.setSoTimeout(2000);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void EstablishConn(){
        // 确定接受方的IP和端口号，IP地址为本地机器地址
        try {

            //接收第一次握手，并对发送端进行第二次握手
            byte[] buf = new byte[32*4];
            DatagramPacket getPacket = new DatagramPacket(buf, buf.length);
            getSocket.receive(getPacket);
            String getMes = new String(buf, 0, getPacket.getLength());
            Segment segment = new Segment();
            segment.Parsing_Message(getMes);
            this.sendAddress= getPacket.getSocketAddress();
            Segment SecondShack = segment;

            SecondShack.show_Details(SecondShack);

            if (!SecondShack.getSYN().equals("1")){
                System.out.println("出错了！");
                System.exit(1);
            }

            SecondShack.setACK("1");
            //ack = x+1
            SecondShack.ack_Equals_Seq_Plus_One();

            SecondShack.setSeq(34);
            String feedback = SecondShack.toString();
            byte[] backBuf = feedback.getBytes();
            // 创建发送类型的数据报
            DatagramPacket sendPacket = new DatagramPacket(backBuf, backBuf.length, sendAddress);
            // 通过套接字发送数据
            getSocket.send(sendPacket);






            Segment LastSegment = null;
            byte[] buffer = new byte[32*4];
            DatagramPacket getLastPacket = new DatagramPacket(buffer, buffer.length);
            getSocket.receive(getLastPacket);
            String getLastMes = new String(buffer);
            LastSegment = new Segment();
            LastSegment.Parsing_Message(getLastMes);
            LastSegment.show_Details(LastSegment);

            if (!LastSegment.getSYN().equals("0")|| !LastSegment.getACK().equals("1") || Integer.parseInt(LastSegment.getSeq(),2) != Integer.parseInt(SecondShack.getAck(),2) || Integer.parseInt(LastSegment.getAck(),2)!= (Integer.parseInt(SecondShack.getSeq(),2)+1)
            ){
                System.out.println("出错了");
                System.exit(1);
            }

            //getSocket.close();


        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.ConnectionState = 1;

    }

    ArrayList<Segment> receivedSegment = new ArrayList<>();

    ArrayList<String> toBeACKed = new ArrayList<>();

    Thread ReceiveSegment = new Thread(){
        @Override
        public void run() {
            byte[] receiveSegment = new byte[256];
            DatagramPacket receivedSegmentPacket = new DatagramPacket(receiveSegment,receiveSegment.length);
            try {
                //TODO: 处理receive方法会线程阻塞的问题
                getSocket.receive(receivedSegmentPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String StringSegment = new String(receiveSegment);
            Segment segment = new Segment(StringSegment);
            receivedSegment.add(segment);
            toBeACKed.add(segment.getSeq());
        }
    };

    Thread SendACK = new Thread(){
        @Override
        public void run() {
            for(String AckString : toBeACKed){
                Segment AckSegment = new Segment();
                AckSegment.setAck(AckString);
                byte[] AckSegmentBytes = AckSegment.toString().getBytes();
                //TODO 这里可能有问题；IP和PORT可能会出问题
                DatagramPacket AckSegmentPacket = new DatagramPacket(AckSegmentBytes,AckSegmentBytes.length,ip,port);
                try {
                    getSocket.send(AckSegmentPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };


    public String SegmentHandle(){
        Collections.sort(receivedSegment);
        //取出每一个报文中携带的Text文本
        String result = "";
        for (Segment segment:receivedSegment){
            result += segment.getContent();
        }
        return result;
    }



    public static void main(String[] args) {
        Receiver receiver = new Receiver("1","1");
        receiver.EstablishConn();
        receiver.ReceiveSegment.start();
        receiver.SendACK.start();
        String receiveText = "";
        for (Segment segment:receiver.receivedSegment){
            receiveText += segment.getContent();
        }
        System.out.println(receiveText);
    }
}
