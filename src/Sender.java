import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class Sender {
    private InetAddress receiver_host_ip = InetAddress.getLocalHost();

    private int receiver_port;

    private String fileName;

    //the maximum window size used by your STP protocol in bytes.
    private int MWS = 0;

    //Maximum Segment Size which is the maximum amount of data (in bytes) carried in each STP segment.
    private int MSS;

    private double timeout;

    //the probability that a STP data segment which is ready to be transmitted will be dropped. This value must be between 0 and 1. For example if pdrop = 0.5, it means that 50% of the transmitted packets are dropped by the PLD.
    private double pdrop;

    //The seed for your random number generator. The use of seed will be explained in Section 4.5.2 of the specification.
    private double seed;

    //滑动窗口的起始位置
    private int startPoint = 0;

    //滑动窗口的结束边界
    private int endPoint = startPoint + this.MWS;

    //滑动窗口 下一个带发送报文位置的指针
    private int sendPoint = 0;

    //待读取的文本内容的存放位置
    byte[] TextContent;

    //包装好的Segment 的list
    ArrayList<Segment> ContentList;


    /**系统状态
     * 0 表示断开连接
     * 1 表示建立连接
     */
    private int ConnectionState = 0;

    //将文件以字节读入byte[]
    public byte[] readFile(String fileName) {
        String pathname = fileName; // 绝对路径或相对路径都可以，写入文件时演示相对路径,读取以上路径的input.txt文件
        String result = "";
        try (FileReader reader = new FileReader(pathname);
             BufferedReader br = new BufferedReader(reader) // 建立一个对象，它把文件内容转成计算机能读懂的语言
        ) {
            String line;
            //网友推荐更加简洁的写法
            while ((line = br.readLine()) != null) {
                // 一次读入一行数据
                result = result + line + "\n";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(result);

        byte[] R = result.getBytes();

        return R;
    }

    //将文件内容按照MSS，计算出每一个报文能够携带的文件内容长度，并且将它们塞进ArrayList<Segment> ContentList里
    public ArrayList<Segment> PacketContent(){
        ArrayList<Segment> result = new ArrayList<>();
        int contentInSegment = MSS - 128;
        String tempContentString;
        if (TextContent.length <= contentInSegment){
            tempContentString = new String(TextContent);
            Segment segment = new Segment();
            segment.setSeq(1);
            segment.setContent(tempContentString);
            result.add(segment);
            return result;
        }
        else {
            //一个指针，指向待包装的数据的位置
            int point = 0;
            int Complete_Segment_Num = TextContent.length / contentInSegment;
            int i;
            for (i = 0; i < Complete_Segment_Num ; i++) {
                Segment tempSegment = new Segment();
                tempSegment.setSeq(i);
                byte[] toBePackaged = Arrays.copyOfRange(TextContent,point,point+contentInSegment);
                tempSegment.setContent(new String(toBePackaged));
                point += contentInSegment;
                result.add(tempSegment);
            }
            byte[] LastToBePackaged = Arrays.copyOfRange(TextContent,point,TextContent.length);
            Segment tempSegment = new Segment();
            tempSegment.setSeq(i);
            tempSegment.setContent(new String(LastToBePackaged));
            result.add(tempSegment);
            return result;
        }

    }

    /**
     * 1 三次握手建立连接
     * 2 实现滑动窗口
     *   2.1 发送数据报文的同时使用LogFactroy创建日志
     *   2.2 其实每一次数据传输都对应着三次握手
     * 3 四次握手释放连接
     */

    // 创建发送方的套接字，IP默认为本地，不指定端口号则系统随机设置一个可用的端口号
    private DatagramSocket sendSocket;
    {
        try {
            sendSocket = new DatagramSocket(/*12341*/);
            sendSocket.setSoTimeout(2000);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }


    public void EstablishConnection(){
        try {
            // 确定发送方的IP地址及端口号，地址为本地机器地址
            int port = receiver_port;
            InetAddress ip = receiver_host_ip;


            //发出第一次握手请求
            //初始化第一个请求报文
            Segment FirstSegment = new Segment();
            FirstSegment.setSYN("1");
            FirstSegment.setSeq(2);

            FirstSegment.show_Details(FirstSegment);

            byte[] FirstBuffer = FirstSegment.toString().getBytes();
            // 构造数据报包，用来将长度为 length 的包发送到指定主机上的指定端口号
            DatagramPacket sendPacket = new DatagramPacket(FirstBuffer, FirstBuffer.length, ip, port);
            // 通过套接字发送数据,发送第一次握手
            sendSocket.send(sendPacket);

            //第三次握手，接收Receiver发出来的反馈

            // 确定接收反馈数据的缓冲存储器，即存储数据的字节数组
            byte[] ThridShack = new byte[32*4];
            // 确定接收类型的数据报
            DatagramPacket getPacket = new DatagramPacket(ThridShack, ThridShack.length);
            sendSocket.receive(getPacket);
            String backMsg = new String(ThridShack, 0, getPacket.getLength());
            Segment ThirdShack = new Segment();
            ThirdShack.Parsing_Message(backMsg);

            ThirdShack.show_Details(ThirdShack);

            if (!ThirdShack.getSYN().equals("1")|| !ThirdShack.getACK().equals("1") || Integer.parseInt(ThirdShack.getAck(),2)!= (Integer.parseInt(FirstSegment.getSeq(),2)+1)){
                System.out.println("出错了");
                System.exit(1);
            }

            //初始化第三次握手发送的报文
            Segment LastSegment = new Segment();
            LastSegment.setSeq(Integer.parseInt(FirstSegment.getSeq(),2)+1);
            LastSegment.setSYN("0");
            LastSegment.setACK("1");
            String tempAck = Integer.toBinaryString(Integer.parseInt(ThirdShack.getSeq(),2)+1);
            tempAck = LastSegment.Auto_completion(tempAck,32);
            LastSegment.setAck(tempAck);
            //发送第三次握手的报文
            byte[] LastSegmentBytes = LastSegment.toString().getBytes();
            DatagramPacket sendLastPacket = new DatagramPacket(LastSegmentBytes, LastSegmentBytes.length, ip, port);
            sendSocket.send(sendLastPacket);

            LastSegment.show_Details(LastSegment);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.ConnectionState = 1;
    }

    Thread SendSegment = new Thread(){
        @Override
        public void run() {
            //TODO: 判断整个文件是否发送结束
//            if (sendPoint == ContentList.size()){
//                sendSocket.close();
//                return;
//            }
            Segment toBeTransported = ContentList.get(sendPoint);
            sendPoint += 1;
            byte[] buffer = toBeTransported.toString().getBytes();
            DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, receiver_host_ip, receiver_port);
            try {
                sendSocket.send(sendPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    };

    ArrayList<Integer> AckList = new ArrayList<>();

    Thread ReceiveAck = new Thread(){
        @Override
        public void run() {
            byte[] receivedAck = new byte[128];
            DatagramPacket AckPackage = new DatagramPacket(receivedAck,receivedAck.length);
            try {
                sendSocket.receive(AckPackage);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Segment AckSegment = new Segment(new String(receivedAck));
            AckList.add(Integer.parseInt(AckSegment.getAck(),2));

            //TODO 测试滑动窗口是否失败
            Collections.sort(AckList);

            for (int i : AckList){
                if (i == startPoint){
                    AckList.remove(i);
                    startPoint ++;
                    endPoint = startPoint + MWS;
                }
            }
        }
    };



    Thread ReSendSegment = new Thread(){
        @Override
        public void run() {

        }
    };








    //构造方法
    public Sender(/*String receiver_host_ip,*/ int receiver_port, String fileName, int mws, int mss, double timeout, double pdrop, double seed) throws UnknownHostException {
        //this.receiver_host_ip = receiver_host_ip;
        this.receiver_port = receiver_port;
        this.fileName = fileName;
        MWS = mws;
        MSS = mss;
        this.timeout = timeout;
        this.pdrop = pdrop;
        this.seed = seed;
    }


    public static void main(String[] args) {
        Sender sender = null;
        try {
            sender = new Sender(12365,"testFile.txt",10,256,1.0,1.0,1.0);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        sender.TextContent = sender.readFile(sender.fileName);
        sender.ContentList = sender.PacketContent();
        sender.EstablishConnection();

        System.out.println(sender.ContentList);

        sender.SendSegment.start();
        sender.ReceiveAck.start();
//        sender.ReSendSegment.start();
    }


}


