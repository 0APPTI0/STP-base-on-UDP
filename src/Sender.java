import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Sender {
    private InetAddress ip = InetAddress.getLocalHost();

    private int Sender_port = 1111;

    private int Receive_port;

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


    private  Log logger = new Log("sender_Log1.txt", true);

    //滑动窗口的起始位置
    public int startPoint = 0;

    //滑动窗口的结束边界
    public int endPoint = 0;

    //滑动窗口 下一个带发送报文位置的指针
    private int sendPoint = 0;

    //待读取的文本内容的存放位置
    byte[] TextContent;

    //包装好的Segment 的list
    ArrayList<Segment> ContentList;

    private boolean isReceiving = true;

    private boolean isSending = true;

    private boolean allDone = false;

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
            sendSocket = new DatagramSocket(Sender_port,ip);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }



    private CopyOnWriteArrayList<Integer> toBeAcked = new CopyOnWriteArrayList<>();

    private CopyOnWriteArrayList<Segment> toBeAcked_Segment_list = new CopyOnWriteArrayList<>();

    private HashMap<Integer,Segment> toBeAcked_Segment = new HashMap<>();

    private Thread SendSegment = new Thread(){
        @Override
        public void run() {
            while (isSending && ! interrupted()) {

                Segment toBeTransported = ContentList.get(sendPoint);
                synchronized (currentThread()) {


                    sendPoint += 1;
                    if (sendPoint == ContentList.size()){
                        isSending = false;
                    }
                    //TODO 如果达到了滑动窗口的最大值，那么应该休眠一段时间；等接收的线程收到ACK,让滑动窗口继续往前推进
                    if (sendPoint > endPoint) {
//                        try {
//                            currentThread().wait();
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        try {
//                            Thread.sleep(5000);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
                        continue;
                    }
                    else {
                        try {
                            sendSegmentWithPLD(toBeTransported);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
        }
    };


    private Thread ReceiveAck = new Thread(){
        @Override
        public void run() {
            while (isReceiving && ! interrupted()) {

                Segment AckSegment = receiveSegment();

                synchronized (currentThread()) {
                    toBeAcked.remove((Integer) Integer.parseInt(AckSegment.getAck(), 2));
//                    toBeAcked_Segment_list.remove(toBeAcked_Segment.get(Integer.parseInt(AckSegment.getAck(), 2)));
                    for (Segment segment:toBeAcked_Segment_list){
                        if (segment.getSeq().equals(AckSegment.getAck())){
                            toBeAcked_Segment_list.remove(segment);
                        }endPoint = (startPoint + MWS > ContentList.size()) ? ContentList.size():(startPoint + MWS)  ;
                    }

                    toBeAcked_Segment.remove(Integer.parseInt(AckSegment.getAck(), 2));
                    toBeAcked.sort(new Comparator<Integer>() {
                        @Override
                        public int compare(Integer o1, Integer o2) {
                            return o1 > o2 ? 1 : -1;
                        }
                    });
                    if (toBeAcked.size()!=0 && toBeAcked.get(0) > startPoint) {
                        startPoint = toBeAcked.get(0);
                        endPoint = (startPoint + MWS > ContentList.size()) ? ContentList.size():(startPoint + MWS)  ;
                        //TODO 滑动窗口向前滑动，归还线程使用权
//                        currentThread().notifyAll();
                    }

                    if (toBeAcked.size() == 0 && !isSending){
                        isReceiving = false;
                        ReceiveAck.interrupt();
                    }
                }
            }
        }
    };



    private Thread ReSendSegment = new Thread(){
        @Override
        public void run() {

            while (isReceiving && ! interrupted()) {
                if (toBeAcked.size() != 0){
                    synchronized (currentThread()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        //不断检查所有待收到ACK的报文是否已经收到ACK
                        for (Segment segment : toBeAcked_Segment_list){
                            System.out.println(Integer.parseInt(segment.getSeq(),2));
                            if ((System.nanoTime() - Long.parseLong(segment.getTime())) / 1000000 > timeout){
                                try {
                                    sendSegmentWithPLD(segment, Log.AdditionalType.RETRANS);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        if (toBeAcked_Segment_list.size() ==0 && !isSending){
                            allDone = true;
                        }

                    }
                }
                if (allDone) {
                    isReceiving = false;
                    isSending = false;
                    ReSendSegment.interrupt();
                    ReceiveAck.interrupt();
                    SendSegment.interrupt();
                }
            }

        }
    };


    private void EstablishConnection(){
        try {
            logger.start();



            //发出第一次握手请求
            //初始化第一个请求报文
            Segment FirstSegment = new Segment();
            FirstSegment.setSYN("1");
            FirstSegment.setSeq(2);

//            FirstSegment.show_Details(FirstSegment);

            byte[] FirstBuffer = FirstSegment.toString().getBytes();
            // 构造数据报包，用来将长度为 length 的包发送到指定主机上的指定端口号
            DatagramPacket sendPacket = new DatagramPacket(FirstBuffer, FirstBuffer.length, ip, Receive_port);
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
            DatagramPacket sendLastPacket = new DatagramPacket(LastSegmentBytes, LastSegmentBytes.length, ip, Receive_port);
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
        this.isReceiving = true;
    }

    private void send() {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        executorService.execute(SendSegment);
        executorService.execute(ReceiveAck);
        executorService.execute(ReSendSegment);
        executorService.shutdown();
        while (!executorService.isTerminated());

        System.out.println("===========================================");

    }

    private void finishSend(){
        this.isSending = false;
        this.isReceiving = false;
        Segment segment0 = new Segment();
        segment0.setFIN("1");
        segment0.setSeq(67);
        sendSegment(segment0);
        Segment finSegment1 = receiveSegment();
        Segment finSegment2 = receiveSegment();
        Segment segment1 = new Segment();
        segment1.setACK("1");
        segment1.setSeq(Integer.parseInt(segment0.getSeq(),2)+1);
        segment1.setAck(Integer.parseInt(finSegment2.getSeq(),2)+1);
        sendSegment(segment1);
    }



    public void sendSegment(Segment segment){
        long timestamp = System.nanoTime();
        segment.setTime(String.valueOf(timestamp));
        try {
            sendSocket.send(toDatagramPacket(segment,ip, Receive_port));
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.log(segment, Log.Type.SND, timestamp);
    }

    public void sendSegmentWithPLD(Segment segment) throws IOException {
        long timestamp = System.nanoTime();
        segment.setTime(String.valueOf(timestamp));
        if (!toBeAcked.contains(Integer.parseInt(segment.getSeq(),2))) {
            toBeAcked.add(Integer.parseInt(segment.getSeq(), 2));
            toBeAcked_Segment_list.add(segment);
            toBeAcked_Segment.put(Integer.parseInt(segment.getSeq()), segment);
        }
        if (random.nextDouble() > pdrop) {
            sendSocket.send(toDatagramPacket(segment,ip,Receive_port));
            logger.log(segment, Log.Type.SND, timestamp);
        } else {
            logger.log(segment, Log.Type.DROP, timestamp);
        }

    }

    private void sendSegmentWithPLD(Segment segment, Log.AdditionalType type) throws IOException {
        long timestamp = System.nanoTime();
//        segment.setTime(String.valueOf(timestamp));
//        toBeAcked.add(Integer.parseInt(segment.getSeq(),2));
//        toBeAcked_Segment_list.add(segment);
//        toBeAcked_Segment.put(Integer.parseInt(segment.getSeq()), segment);
        if (random.nextDouble() > pdrop) {
            sendSocket.send(toDatagramPacket(segment,ip,Receive_port));
            logger.log(segment, Log.Type.SND, type, timestamp);
        } else {
            logger.log(segment, Log.Type.DROP, type, timestamp);
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
        this.Receive_port = receiver_port;
        this.fileName = fileName;
        MWS = mws;
        MSS = mss;
        this.endPoint = this.startPoint + MWS;
        this.timeout = timeout;
        this.pdrop = pdrop;
        this.seed = seed;
    }


    public static void main(String[] args) throws IOException {
        Sender sender = null;
        try {
            sender = new Sender(2222,"testFile.txt",10,192+512,2000.0,0.5,2);
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
        sender.finishSend();
        sender.logger.close();

    }


}


