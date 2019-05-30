public class Segment implements Comparable<Segment>{

    private String seq = "00000000000000000000000000000000";

    private String ack = "00000000000000000000000000000000";

    //数据偏移字段,存放报文的长度
    private String Data_offset = "00000000";

    //若为ACK报文，则该值为1，否则为0
    private String ACK = "0";

    //若为SYN报文，则该值为1，否则为0
    private String SYN = "0";

    //若为FIN报文，则该值为1，否则为0
    private String FIN = "0";

    private String reversed = "00000";

    private String Checksum = "0000000000000000";

    private String Option = "00000000";

    private String Filling = "000000000000000000000000";

    private String time = "0000000000000000000000000000000000000000000000000000000000000000";

    private String Content = "";


    //-------------------------------构造方法------------------------------------//
    public Segment(){
    }

    public Segment(String message){
        this.Parsing_Message(message);
    }


    //-------------------------------应用方法------------------------------------//


    /**一些应该存在的字段的拆分、处理的方法
     */
    public String toString(){
        return this.seq + this.ack + this.Data_offset + this.ACK + this.SYN + this.FIN + this.reversed + this.Checksum + this.Option + this.Filling + this.time + this.Content;
    }


    /**
     *
     * @param toBeCompleted
     * @param length
     * @return 补齐后的二进制数字（在前面添加了相应位数的0）
     */
    public String Auto_completion(String toBeCompleted ,int length){
        int t = length-toBeCompleted.length();
        for (int i = 0 ; i < t ; i ++){
            toBeCompleted = "0"+toBeCompleted;
        }
        return toBeCompleted;
    }


    /**
     * @param ToBeHandled
     * @return 将输入的字符串转换为参数存入Segment对象
     */
    public void Parsing_Message(String ToBeHandled){
        this.setSeq(ToBeHandled.substring(0,32));
        this.setAck(ToBeHandled.substring(32,64));
        this.setData_offset(ToBeHandled.substring(64,72));
        this.setACK(String.valueOf(ToBeHandled.charAt(72)));
        this.setSYN(String.valueOf(ToBeHandled.charAt(73)));
        this.setFIN(String.valueOf(ToBeHandled.charAt(74)));
        this.setReversed(ToBeHandled.substring(75,80));
        this.setChecksum(ToBeHandled.substring(80,96));
        this.setOption(ToBeHandled.substring(96,104));
        this.setFilling(ToBeHandled.substring(104,128));
        this.setTime(ToBeHandled.substring(128,192));
        if (ToBeHandled.length()>192) {
            this.setContent(ToBeHandled.substring(192));
        }
    }


    public void ack_Equals_Seq_Plus_One(){
        this.setAck(this.Auto_completion(Integer.toBinaryString(Integer.parseInt(this.getSeq(),2)+1),32));
    }


    public void show_Details(Segment segment){
        System.out.println(segment.getSeq()+" 值为"+Integer.parseInt(segment.getSeq(),2)+" 长度为"+segment.getSeq().length());
        System.out.println(segment.getAck()+" 值为"+Integer.parseInt(segment.getAck(),2)+" 长度为"+segment.getAck().length());
        System.out.println(segment.getData_offset()+" 值为"+Integer.parseInt(segment.getData_offset(),2)+" 长度为"+segment.getData_offset().length());
        System.out.println(segment.getACK()+" ACK");
        System.out.println(segment.getSYN()+" SYN");
        System.out.println(segment.getFIN()+" FIN");
        System.out.println(segment.getReversed()+" 长度为"+segment.getSeq().length());
        System.out.println(segment.getChecksum()+" 值为"+Integer.parseInt(segment.getSeq(),2)+" 长度为"+segment.getSeq().length());
        System.out.println(segment.getOption()+" 长度为"+segment.getOption().length());
        System.out.println(segment.getFilling()+" 长度为"+segment.getFilling().length());
        System.out.println(segment.getTime()+" 长度为"+segment.getTime().length());
        System.out.println(segment.getContent()+" 长度为"+segment.getContent().length());
        System.out.println("报文总长度为："+segment.toString().length());
        System.out.println("----------------------------");
    }

    /**
     * 将报文可以按照seq的大小来比较
     * @param o
     * @return
     */
    @Override
    public int compareTo(Segment o) {
        return Integer.parseInt(this.getSeq())>Integer.parseInt(o.getSeq()) ? 1:-1;
    }





    //-------------------------------getter & setter------------------------------------//

    public String getSeq() {
        return seq;
    }

    public void setSeq(int i){
        this.setSeq(Auto_completion(Integer.toBinaryString(i),32));
    }

    public void setSeq(String seq) {
        this.seq = seq;
    }

    public String getAck() {
        return ack;
    }

    public void setAck(String ack) {
        this.ack = ack;
    }

    public void setAck(int ack){
        this.setAck(Auto_completion(Integer.toBinaryString(ack),32));
    }

    public String getData_offset() {
        return Data_offset;
    }

    public void setData_offset(String data_offset) {
        Data_offset = data_offset;
    }

    public String getACK() {
        return ACK;
    }

    public void setACK(String ACK) {
        this.ACK = ACK;
    }

    public String getSYN() {
        return SYN;
    }

    public void setSYN(String SYN) {
        this.SYN = SYN;
    }

    public String getFIN() {
        return FIN;
    }

    public void setFIN(String FIN) {
        this.FIN = FIN;
    }

    public String getReversed() {
        return reversed;
    }

    public void setReversed(String reversed) {
        this.reversed = reversed;
    }

    public String getChecksum() {
        return Checksum;
    }

    public void setChecksum(String checksum) {
        Checksum = checksum;
    }

    public String getOption() {
        return Option;
    }

    public void setOption(String option) {
        Option = option;
    }

    public String getFilling() {
        return Filling;
    }

    public void setFilling(String filling) {
        Filling = filling;
    }

    public String getContent() {
        return Content;
    }

    public void setContent(String content) {
        Content = content;
    }


    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
