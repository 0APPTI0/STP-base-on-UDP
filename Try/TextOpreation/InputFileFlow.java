package TextOpreation;

import java.io.*;

public class InputFileFlow {
    /**
     *
     * <p>Title: getContent</p>
     * <p>Description:根据文件路径读取文件转出byte[] </p>
     * @return 字节数组
     * @throws IOException
     */
    public static byte[] getContent(String filePath) throws IOException {
        File file = new File(filePath);
        long fileSize = file.length();
        FileInputStream fi = new FileInputStream(file);
        byte[] buffer = new byte[(int) fileSize];
        int offset = 0;
        int numRead = 0;
        while (offset < buffer.length
                && (numRead = fi.read(buffer, offset, buffer.length - offset)) >= 0) {
            offset += numRead;
        }
        // 确保所有数据均被读取
        if (offset != buffer.length) {
            throw new IOException("Could not completely read file "
                    + file.getName());
        }
        fi.close();
        return buffer;
    }


    public static void main(String[] args) {
        File file = new File("testfile1");
        File file2 = new File("receiver");
        if (!file2.exists()) {
            try {
                file2.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            byte[] a = new byte[(int) file.length()];
            fis = new FileInputStream(file);
            fos = new FileOutputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            bis.read(a);
            System.out.println(a[0]+a[1]);
            fos = new FileOutputStream(file2);
            fos.write(a);

            fis.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
