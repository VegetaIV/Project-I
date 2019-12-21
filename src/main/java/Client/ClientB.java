/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import KDC.InforClient;
import java.util.Base64;
import java.net.*;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
/**
 *
 * @author MyPC
 */
public class ClientB {
    
    public static final String SERVER_IP = "127.0.0.1";
    public static final int SERVER_PORT = 7;
    public static Socket socket;
    public static DataOutputStream out;
    public static DataInputStream in;
    public static SecretKey key; // Khóa bí mật (SecretKEy)
    public static SecretKey kS; // SessionKey
    public static InforClient icB; //Định danh Client đích

    public static void main(String[] args) {
        System.out.println("Waiting for a Secret Key from KDC...");
        try {
            // Tạo socket kết nối tới Server
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());

            byte[] incoming = new byte[109];
            
            // Nhận và xử lý Key
            while (true) {
                in.read(incoming);
                if (incoming[0] == 1) {
                    byte[] b = new byte[16];
                    System.arraycopy(incoming, 1, b, 0, 16);

                    // Tái tạo SecretKey
                    key = new SecretKeySpec(b, "AES");
                    System.out.println("Received Key from Server!");
    /*                
                    //Gửi thông điệp yêu cầu SessionKey
                    InforClient icA = new InforClient(socket);
                    byte[] id = icA.ID();
                    byte[] outsending = new byte[13];
                    outsending[0] = (byte) 1; // code = 1
                    System.arraycopy(id, 0, outsending, 1, 6);
                    
                    // Setup định danh Client đích
                    InetAddress dest = InetAddress.getByName("127.0.0.1");
                    int portB = 9332;
                    icB = new InforClient(dest, portB);
                    id = icB.ID();
                    System.arraycopy(id, 0, outsending, 7, 6);
                    out.write(outsending);
                    System.out.println("Sending Request SessionKey to chat with " + dest + " " + portB + "...");
    */
                } else if (incoming[0] == 2) {
                    // Nhận SessionKey từ Server và gửi SessionKey đến B
                    kS = getSessionKeyToB(incoming);
                    System.out.println("Received Session Key from Server. Sending Session Key to B...");
                } else if (incoming[0] == 3) {
                    // Nhận SessionKey từ A và gửi thông điệp kết thúc quá trình thiết lập Key
                    kS = getSessionKeyToA(incoming);
                    System.out.println("Received Session Key from A.");
                    System.out.println("\t Start Chatting with client " + icB.address + " on port " + icB.port);
                    break;
                } else if (incoming[0] == 0) {
                    // Nhận thông điệp kết thúc quá trình thiết lập Key
                    System.out.println("\t Start Chatting with client " + icB.address + " on port " + icB.port);
                    break;
                } else {
                    System.out.println("Socket Close! Code = " + (byte) incoming[0]);
                    socket.close();
                }
            }
            
            // Luồng gửi tin nhắn
            Thread sending = new Sending(socket, icB, kS);
            sending.start();
            try {
                sending.join(50);
            } catch (InterruptedException e) {
                System.out.println("Thread interrupted.");
            }

            // Luồng nhận tin nhắn
            Thread receiving = new Receiving(socket, kS);
            receiving.start();
            try {
                receiving.join();
            } catch (InterruptedException e) {
                System.out.println("Thread interrupted.");
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    // Mã hóa dữ liệu
    public static byte[] encrypt(byte[] message, SecretKey key, String mode) {
        String strEncrypted = null;
        try {
            Cipher c = Cipher.getInstance(mode);
            c.init(Cipher.ENCRYPT_MODE, key);
            byte encryptOut[] = c.doFinal(message);
            strEncrypted = Base64.getEncoder().encodeToString(encryptOut);
        } catch (InvalidKeyException | NoSuchAlgorithmException | BadPaddingException 
                | IllegalBlockSizeException | NoSuchPaddingException e) {
            System.out.println(e.getMessage());
        }
        return strEncrypted.getBytes();
    }

    // Giải mã dữ liệu
    public static byte[] decrypt(byte[] message, SecretKey key, String mode) {
        byte[] decryptOut = null;
        try {
            Cipher c = Cipher.getInstance(mode);
            c.init(Cipher.DECRYPT_MODE, key);
            decryptOut = c.doFinal(Base64.getDecoder().decode(message));
        } catch (InvalidKeyException | NoSuchAlgorithmException | BadPaddingException 
                | IllegalBlockSizeException | NoSuchPaddingException e) {
            System.out.println(e.getMessage());
        }
        return decryptOut;
    }

    // Thu được SessionKey và gửi thông điệp tới B
    public static SecretKey getSessionKeyToB(byte[] incoming) throws IOException {
        byte[] incomingA = new byte[108];
        System.arraycopy(incoming, 1, incomingA, 0, 108);
        byte[] messageA = decrypt(incomingA, key, "AES/ECB/PKCS5PADDING");

        //Lay SessionKey
        byte[] encode = new byte[16];
        System.arraycopy(messageA, 0, encode, 0, 16);
        kS = new SecretKeySpec(encode, "AES");

        //Xac dinh idA, idB
        
        //Tao message gui SessionKey cho Client B
        byte[] outsendingB = new byte[57];
        outsendingB[0] = (byte) 2; // code = 2
        System.arraycopy(messageA, 16, outsendingB, 1, 56);
        out.write(outsendingB);
        return kS;
    }
    
    // Thu được SessionKey và gửi thông điệp kết thúc thiết lập Key
    public static SecretKey getSessionKeyToA (byte[] incoming) throws IOException {
        //Xác đinh IDA, IDB
        byte[] ip = new byte[4];
        byte[] idA = new byte[6];
        System.arraycopy(incoming, 1, idA, 0, 6);
        System.arraycopy(idA, 0, ip, 0, 4);
        int port = (idA[4] < 0 ? (idA[4] + 256) : idA[4]) * 256
                + (idA[5] < 0 ? (idA[5] + 256) : idA[5]);
        InetAddress address = InetAddress.getByAddress(ip);
        icB = new InforClient(address, port);
        
        //Xác định SessionKey
        byte[] incomingB = new byte[44];
        System.arraycopy(incoming, 13, incomingB, 0, 44);
        byte[] messageB = decrypt(incomingB, key, "AES/ECB/PKCS5PADDING");
        
        byte[] encode = new byte[16];
        System.arraycopy(messageB, 0, encode, 0, 16);
        kS = new SecretKeySpec(encode, "AES");
        
        byte[] outsending = new byte[13];
        outsending[0] = 0;
        System.arraycopy(incoming, 7, outsending, 1, 6);
        System.arraycopy(incoming, 1, outsending, 7, 6);
        out.write(outsending);
        return kS;
    }
}
