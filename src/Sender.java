import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private long seed;


    private  Log logger = new Log("sender_Log.txt", true);

    //滑动窗口的起始位置
    public int startPoint = 0;

    //滑动窗口的结束边界
    public int endPoint = startPoint + this.MWS;

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

    private boolean isReceiving = false;

    private boolean isSending = false;

    private Random random;

    //将文件以字节读入byte[]
    public static byte[] readFile(String filePath) throws IOException {
        File file = new File(filePath);
        int length = (int) file.length();
        byte[] allData = new byte[length];
        FileInputStream fileInputStream = new FileInputStream(file);
        fileInputStream.read(allData);
        fileInputStream.close();
        return allData;
    }

    //将文件内容按照MSS，计算出每一个报文能够携带的文件内容长度，并且将它们塞进ArrayList<Segment> ContentList里
    public ArrayList<Segment> PacketContent(){
        ArrayList<Segment> result = new ArrayList<>();
        int contentInSegment = MSS - 192;
//        String tempContentString;
//        //如果整个要发送的内容只需要一个报文就可以装下
//        if (TextContent.length <= contentInSegment){
//            tempContentString = new String(TextContent);
//            Segment segment = new Segment();
//            segment.setSeq(1);
//            segment.setContent(tempContentString);
//            segment.setData_offset(segment.Auto_completion(Integer.toBinaryString(segment.toString().length()),8));
//            result.add(segment);
//            return result;
//        }
        //分成很多个报文装
            //一个指针，指向待包装的数据的位置
        int point = 0;
        int Complete_Segment_Num = TextContent.length / contentInSegment;
        int i;
        for (i = 0; i < Complete_Segment_Num ; i++) {
            Segment tempSegment = new Segment();
            tempSegment.setSeq(i);
            byte[] toBePackaged = Arrays.copyOfRange(TextContent,point,point+contentInSegment);
            tempSegment.setContent(new String(toBePackaged));
            //这里由于设计的失误导致记录报文的最大长度只有128；因此改为用十进制记录
            tempSegment.setData_offset(tempSegment.Auto_completion(String.valueOf(tempSegment.toString().length()),8));
            point += contentInSegment;
            result.add(tempSegment);
        }
        byte[] LastToBePackaged = Arrays.copyOfRange(TextContent,point,TextContent.length);
        Segment tempSegment = new Segment();
        tempSegment.setSeq(i);
        tempSegment.setContent(new String(LastToBePackaged));
        tempSegment.setData_offset(tempSegment.Auto_completion(String.valueOf(tempSegment.toString().length()),8));
        result.add(tempSegment);
        return result;


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
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }


    private void EstablishConnection(){
        try {
            logger.start();

            // 确定发送方的IP地址及端口号，地址为本地机器地址
            int port = receiver_port;
            InetAddress ip = receiver_host_ip;


            //发出第一次握手请求
            //初始化第一个请求报文
            Segment FirstSegment = new Segment();
            FirstSegment.setSYN("1");
            FirstSegment.setSeq(2);

//            FirstSegment.show_Details(FirstSegment);

            byte[] FirstBuffer = FirstSegment.toString().getBytes();
            // 构造数据报包，用来将长度为 length 的包发送到指定主机上的指定端口号
            DatagramPacket sendPacket = new DatagramPacket(FirstBuffer, FirstBuffer.length, ip, port);
            // 通过套接字发送数据,发送第一次握手
            sendSocket.send(sendPacket);

            //第三次握手，接收Receiver发出来的反馈

            // 确定接收反馈数据的缓冲存储器，即存储数据的字节数组;在这里为一个不含content的Segment的长度
            byte[] ThridShack = new byte[192];
            // 确定接收类型的数据报
            DatagramPacket getPacket = new DatagramPacket(ThridShack, ThridShack.length);
            sendSocket.receive(getPacket);
            String backMsg = new String(ThridShack, 0, getPacket.getLength());
            Segment ThirdShack = new Segment();
            ThirdShack.Parsing_Message(backMsg);

//            ThirdShack.show_Details(ThirdShack);

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

//            LastSegment.show_Details(LastSegment);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.isSending = true;
        this.isReceiving = false;
    }


    private ArrayList<Integer> toBeAcked = new ArrayList<>();

    private Thread SendSegment = new Thread(){
        @Override
        public void run() {
            while (isSending) {
                //TODO: 判断整个文件是否发送结束
//            if (sendPoint == ContentList.size()){
//                sendSocket.close();
//                return;
//            }
                Segment toBeTransported = ContentList.get(sendPoint);
                synchronized (currentThread()) {
                    sendPoint += 1;
                    //TODO 需要改用滑动窗口来进行判读
                    if (sendPoint == ContentList.size()) {
                        isSending = false;
                    }
                    sendSegmentWithPLD(toBeTransported);
                    toBeAcked.add(Integer.parseInt(toBeTransported.getSeq()));
                }
            }
        }
    };


    private Thread ReceiveAck = new Thread(){
        @Override
        public void run() {
            while (isReceiving && !interrupted()) {

                Segment AckSegment = receiveSegment();

                synchronized (currentThread()) {
                    toBeAcked.remove(Integer.parseInt(AckSegment.getAck(), 2));

                    toBeAcked.sort(new Comparator<Integer>() {
                        @Override
                        public int compare(Integer o1, Integer o2) {
                            return 01 > o2 ? 1 : -1;
                        }
                    });
                    startPoint = toBeAcked.get(0);
                    endPoint = (startPoint + MWS > ContentList.size()) ? (startPoint + MWS) : ContentList.size();

                    //TODO 测试滑动窗口是否已经失效
                }
            }
        }
    };



    private Thread ReSendSegment = new Thread(){
        @Override
        public void run() {

            while (isReceiving) {
                // TODO
                if (!isSending) {isReceiving = false; ReceiveAck.interrupt();}
            }

        }
    };


    private void send() {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        executorService.execute(SendSegment);
        executorService.execute(ReceiveAck);
        executorService.execute(ReSendSegment);
        executorService.shutdown();
        while (!executorService.isTerminated());
    }


    private void finishSend(){

    }


    public void sendSegmentWithPLD(Segment segment){
        long timestamp = System.nanoTime();
        segment.setTime(String.valueOf(timestamp));
        //TODO 需要将这里的判断逻辑修改为带有PLD逻辑的判断
        if (/*random.nextDouble() > pdrop*/
                true) {
            try {
                sendSocket.send(toDatagramPacket(segment,receiver_host_ip, receiver_port));
            } catch (IOException e) {
                e.printStackTrace();
            }
            logger.log(segment, Log.Type.SND, timestamp);
        } else {
            logger.log(segment, Log.Type.DROP, timestamp);
        }

    }



    public Segment receiveSegment(){
        //事先开辟一个足够大的数组
        byte[] receiveSegment = new byte[1024+192];
        DatagramPacket receivedSegmentPacket = new DatagramPacket(receiveSegment,receiveSegment.length);
        try {
            sendSocket.receive(receivedSegmentPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
        long timestamp = System.nanoTime();
        String StringSegment = new String(receiveSegment);
        //采取自动截断功能，去除后面的空数组
        Segment segment = new Segment(StringSegment);
        int segmentLength = Integer.parseInt(segment.getData_offset());
        Segment trueSegment = new Segment(StringSegment.substring(0,segmentLength));

        logger.log(trueSegment, Log.Type.RCV, timestamp);
        return trueSegment;
    }


    public DatagramPacket toDatagramPacket(Segment segment, InetAddress address, int port) throws IOException {
        byte[] array = segment.toString().getBytes();
        return new DatagramPacket(array, array.length, address, port);
    }



    //构造方法
    public Sender(/*String receiver_host_ip,*/ int receiver_port, String fileName, int mws, int mss, double timeout, double pdrop, long seed) throws UnknownHostException {
        //this.receiver_host_ip = receiver_host_ip;
        this.receiver_port = receiver_port;
        this.fileName = fileName;
        MWS = mws;
        MSS = mss;
        this.timeout = timeout;
        this.pdrop = pdrop;
        this.seed = seed;
    }


    public static void main(String[] args) throws IOException {
        Sender sender = null;
        try {
            sender = new Sender(12365,"testFile.txt",10,256,1.0,0.5,2);
            sender.random = new Random(sender.seed);
            if (sender.MSS>1024){
                System.out.println("MSS太大");
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        sender.TextContent = sender.readFile(sender.fileName);

        sender.ContentList = sender.PacketContent();
        sender.EstablishConnection();
        sender.send();

        sender.logger.close();

    }


}


