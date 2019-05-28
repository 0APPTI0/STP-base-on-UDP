package TextOpreation;

import java.io.FileInputStream;
import java.io.IOException;
/**
 * @author chenrz(simon)
 * @date 2006-6-29
 *       <p>
 *       JAVA字节流例子-读文件（www.1cn.biz）
 *       </p>
 */
public class ReadFile {
    public static void main(String[] args) {
        try {
            // 创建文件输入流对象
            FileInputStream IS = new FileInputStream("testFile.txt");
            // 设定读取的字节数
            int n = 1024;
            byte buffer[] = new byte[n];
            // 读取输入流
//            while ((IS.read(buffer, 0, n) != -1) && (n > 0)) {
//                System.out.print(new String(buffer));
//            }
            IS.read(buffer);
//            byte[] result;
//            for (int i = 0 ; i < n ; i ++){
//                if (buffer[i]!=  ) {
//
//                }
//            }
            System.out.println(new String(buffer));
            // 关闭输入流
            IS.close();
        } catch (IOException ioe) {
            System.out.println(ioe);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}