/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import KDC.InforClient;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Base64;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 *
 * @author MyPC
 */
public class Sending  extends Thread{
    public Socket socket;
    public DataOutputStream out;
    public InforClient des; // Định danh client đích
    private final SecretKey kS; // SessionKey

    public Sending(Socket socket, InforClient des, SecretKey kS) throws IOException {
        this.socket = socket;
        this.out = new DataOutputStream(socket.getOutputStream());
        this.kS = kS;
        this.des = des;
    }

    @Override
    public void run() {
        try {
            while (true) {
                System.out.print("Enter your message: ");
                InputStreamReader isr = new InputStreamReader(System.in); // Nhập
                BufferedReader br = new BufferedReader(isr); // một chuỗi
                String theString = br.readLine(); // từ bàn phím
                if (theString.equals("")) continue;
                byte[] cipher = encrypt(theString.getBytes(), kS, "AES/ECB/PKCS5PADDING");
                
                
                //Tạo outsending gửi đi
                int len = cipher.length;
                byte[] outsending = new byte[len + 13];
                outsending[0] = (byte) 0;
                InforClient ic = new InforClient(socket);
                byte[] id = ic.ID();
                System.arraycopy(id, 0, outsending, 1, 6);
                id = des.ID();
                System.arraycopy(id, 0, outsending, 7, 6);
                System.arraycopy(cipher, 0, outsending, 13, len);
                
                out.write(outsending);
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }
    
    // Mã hóa tin nhắn trước khi gửi
    public static byte[] encrypt(byte[] message, SecretKey key, String mode) {
        String strEncrypted = null;
        try {
            Cipher c = Cipher.getInstance(mode);
            c.init(Cipher.ENCRYPT_MODE, key);
            byte encryptOut[] = c.doFinal(message);
            strEncrypted = Base64.getEncoder().encodeToString(encryptOut);
        } catch (InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            System.out.println(e.getMessage());
        }
        return strEncrypted.getBytes();
    }
}