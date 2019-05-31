import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Receiver {



    private String file_name;

    public Receiver(int receiver_port, String file_name) {
        this.Receiver_port = receiver_port;
        this.file_name = file_name;
    }


    //接收方的ip
    private InetAddress ip;

    {
        try {
            ip = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    //接收方的port
    private int Receiver_port = 2222;

    private int Sender_port = 1111;

    private boolean isReceiving = true;

    private boolean isSending = true;




    private DatagramSocket getSocket;
    {
        try {
            getSocket = new DatagramSocket(Receiver_port, ip);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private Log logger = new Log("receiver_Log1.txt", false);

    private CopyOnWriteArrayList<Segment> receivedSegment = new CopyOnWriteArrayList<>();

    private CopyOnWriteArrayList<String> toBeACKed = new CopyOnWriteArrayList<>();

    Thread SendACK = new Thread(){
        @Override
        public void run() {
            while (isSending) {
                if (toBeACKed.size() != 0) {
                    for (String AckString : toBeACKed) {
                        Segment AckSegment = new Segment();
                        AckSegment.setAck(AckString);
                        AckSegment.setACK("1");
                        toBeACKed.remove(AckString);
                        sendSegment(AckSegment);
                    }
                }
            }
        }
    };


    Thread ReceiveSegment = new Thread(){

        @Override
        public void run() {
            while (isReceiving) {
                Segment segment = receiveSegment();
                synchronized (currentThread()) {
                    if ("1".equals(segment.getFIN())){
                        isReceiving = false;
                        break;
                    }
                    receivedSegment.add(segment);
                    toBeACKed.add(segment.getSeq());

                }
            }
        }
    };






    //三次握手建立连接
    public void EstablishConn(){
        // 确定接受方的IP和端口号，IP地址为本地机器地址
        try {
            logger.start();

            //接收第一次握手，并对发送端进行第二次握手
            byte[] buf = new byte[192];
            DatagramPacket getPacket = new DatagramPacket(buf, buf.length);
            getSocket.receive(getPacket);
            String getMes = new String(buf, 0, getPacket.getLength());
            Segment segment = new Segment();
            segment.Parsing_Message(getMes);
            Segment SecondShack = segment;

//            SecondShack.show_Details(SecondShack);

            if (!SecondShack.getSYN().equals("1")){
                System.out.println("出错了！");
                System.exit(1);
            }

            SecondShack.setACK("1");
            //ack = x+1Receiver_port
            SecondShack.ack_Equals_Seq_Plus_One();

            SecondShack.setSeq(34);
            String feedback = SecondShack.toString();
            byte[] backBuf = feedback.getBytes();
            // 创建发送类型的数据报
            DatagramPacket sendPacket = new DatagramPacket(backBuf, backBuf.length, ip,Sender_port);
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

        //确认握手之后，正式建立连接
        this.isReceiving = true;
        this.isSending = true;

    }

    //开辟线程池
    public void receiveData() {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        ReceiveSegment.setPriority(1);
        SendACK.setPriority(2);
        executorService.execute(ReceiveSegment);
        executorService.execute(SendACK);
        executorService.shutdown();
        while (!executorService.isTerminated()) ;
    }

    //四次握手松开连接
    public void finReceive(){
        this.isReceiving = false;
        this.isSending = false;
        Segment segment0 = receiveSegment();
        Segment segment1 = new Segment();
        segment1.setACK("1");
        segment1.setSeq(233);
        segment1.setAck(Integer.parseInt(segment0.getSeq(),1)+1);
        sendSegment(segment1);
        Segment segment2 = new Segment();
        segment2.setACK("1");
        segment2.setFIN("1");
        segment2.setSeq(666);
        segment2.setAck(Integer.parseInt(segment0.getSeq(),1)+1);
        Segment segment3 = receiveSegment();
    }



    public void sendSegment(Segment Acksegment){
        long timestamp = System.nanoTime();
        Acksegment.setTime(String.valueOf(timestamp));
            try {
                getSocket.send(toDatagramPacket(Acksegment,ip, Sender_port));
            } catch (IOException e) {
                e.printStackTrace();
            }
            logger.log(Acksegment, Log.Type.SND, timestamp);



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

    public void writeInFile(String file_name){
        PrintWriter printWriter = null;
        try {
            printWriter = new PrintWriter(file_name);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Collections.sort(receivedSegment);
        for (Segment segment : receivedSegment){
            printWriter.print(segment.getContent());
        }
        printWriter.close();
    }


    public static void main(String[] args) {
        Receiver receiver = new Receiver(1,"1");
        receiver.EstablishConn();
        receiver.receiveData();
        receiver.finReceive();

        receiver.writeInFile("receivedText");

        String receiveText = "";
        for (Segment segment:receiver.receivedSegment){
            receiveText += segment.getContent();
        }
        System.out.println(receiveText);

    }
}
