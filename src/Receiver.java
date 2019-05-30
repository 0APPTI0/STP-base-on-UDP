import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private boolean isReceiving = true;

    private boolean isSending = false;


    //在收到信息之后，接收方可以根据发送方的相关信息得到发送方的Address。
    private SocketAddress sendAddress ;


    private DatagramSocket getSocket;
    {
        try {
            getSocket = new DatagramSocket(port, ip);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }


    private ArrayList<Segment> receivedSegment = new ArrayList<>();

    private ArrayList<String> toBeACKed = new ArrayList<>();





    //三次握手建立连接
    public void EstablishConn(){
        // 确定接受方的IP和端口号，IP地址为本地机器地址
        try {

            //接收第一次握手，并对发送端进行第二次握手
            byte[] buf = new byte[192];
            DatagramPacket getPacket = new DatagramPacket(buf, buf.length);
            getSocket.receive(getPacket);
            String getMes = new String(buf, 0, getPacket.getLength());
            Segment segment = new Segment();
            segment.Parsing_Message(getMes);
            this.sendAddress= getPacket.getSocketAddress();
            Segment SecondShack = segment;

//            SecondShack.show_Details(SecondShack);

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
            byte[] buffer = new byte[192];
            DatagramPacket getLastPacket = new DatagramPacket(buffer, buffer.length);
            getSocket.receive(getLastPacket);
            String getLastMes = new String(buffer);
            LastSegment = new Segment();
            LastSegment.Parsing_Message(getLastMes);
//            LastSegment.show_Details(LastSegment);

            if (!LastSegment.getSYN().equals("0")|| !LastSegment.getACK().equals("1") || Integer.parseInt(LastSegment.getSeq(),2) != Integer.parseInt(SecondShack.getAck(),2) || Integer.parseInt(LastSegment.getAck(),2)!= (Integer.parseInt(SecondShack.getSeq(),2)+1)
            ){
                System.out.println("出错了");
                System.exit(1);
            }



        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.ConnectionState = 1;

    }




    Thread ReceiveSegment = new Thread(){
        @Override
        public void run() {
            Segment segment = receiveSegment();
            while (isReceiving) {
                receivedSegment.add(segment);
                toBeACKed.add(segment.getSeq());
            }
        }
    };

    Thread SendACK = new Thread(){
        @Override
        public void run() {
            for(String AckString : toBeACKed){
                Segment AckSegment = new Segment();
                AckSegment.setAck(AckString);
                byte[] AckSegmentBytes = AckSegment.toString().getBytes();
                DatagramPacket AckSegmentPacket = new DatagramPacket(AckSegmentBytes,AckSegmentBytes.length,ip,port);
                try {
                    getSocket.send(AckSegmentPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };


    //开辟线程池
    public void receiveData() {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.execute(ReceiveSegment);
        executorService.execute(SendACK);
        executorService.shutdown();
        while (!executorService.isTerminated()) ;
    }



    public Segment receiveSegment(){
        //事先开辟一个足够大的数组
        byte[] receiveSegment = new byte[1024+192];
        DatagramPacket receivedSegmentPacket = new DatagramPacket(receiveSegment,receiveSegment.length);
        try {
            getSocket.receive(receivedSegmentPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String StringSegment = new String(receiveSegment);

        //采取自动截断功能，去除后面的空数组
        Segment segment = new Segment(StringSegment);
        int segmentLength = Integer.parseInt(segment.getData_offset());
        Segment trueSegment = new Segment(StringSegment.substring(0,segmentLength));
        return trueSegment;
    }



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
        receiver.receiveData();


        String receiveText = "";
        for (Segment segment:receiver.receivedSegment){
            receiveText += segment.getContent();
        }
        System.out.println(receiveText);

    }
}
