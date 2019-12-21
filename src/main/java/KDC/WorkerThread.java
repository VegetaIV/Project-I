/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package KDC;

import java.util.Base64;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 *
 * @author MyPC
 */
public class WorkerThread extends Thread {

    private final Socket socket;
    private final DataOutputStream out;
    private final DataInputStream in;
    private ClientKey clientKey;
    private byte[] incoming = new byte[4096];
    public static ArrayList<WorkerThread> clients = new ArrayList<WorkerThread>();

    public WorkerThread(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new DataOutputStream(socket.getOutputStream());
        this.in = new DataInputStream(socket.getInputStream());
        clients.add(this);
    }

    @Override
    public void run() {
        // Tạo SecretKey và gửi về cho Client
        clientKey = new ClientKey(socket);
        byte[] encode = clientKey.key.getEncoded();
        byte[] message = new byte[17];
        message[0] = (byte) 1;
        System.arraycopy(encode, 0, message, 1, 16);
        try {
            out.write(message);
        } catch (IOException ex) {
            Logger.getLogger(WorkerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Sending Secret Key to " + socket);
        
        // Xử lý thông điệp gửi tới từ Client theo Code
        try {
            while (true) {
                int len = in.read(incoming);
                if (incoming[0] == 1) {
                    // Xử lý yêu cầu SessionKey
                    byte[] outsending = getSessionKey(incoming);
                    out.write(outsending);
                    System.out.println("Sending SessionKey to A and B");
                } else if (incoming[0] == 2) {
                    // Xử lý chuyển tiếp
                    System.out.println("Received from client: " + socket.getInetAddress() 
                            + " on port " + socket.getPort());
                    forward(incoming, len, (byte) 3);
                } else if (incoming[0] == 0) {
                    // Xử lý chuyển tiếp
                    System.out.println("Forward from client " + socket.getInetAddress() 
                            + " on port " + socket.getPort());
                    forward(incoming, len, (byte) 0);
                } else {
                    System.out.println("Close " + socket + " Code = " + (byte) incoming[0]);
                    socket.close();
                }
            }
        } catch (IOException e) {
            System.err.println("Request Processing Error: " + e);
        }
        System.out.println("Complete processing: " + socket);
    }
    
    // Tìm SecretKey của Client khi biết IP và port
    public static SecretKey getKey(InetAddress address, int port) {
        SecretKey key = null;
        for (WorkerThread i : clients) {
            ClientKey clk = i.clientKey;
            if (clk.equals(address, port)) {
                key = clk.key;
            }
        }
        return key;
    }

    
    // Mã hóa dữ liệu
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

    // Giải mã dữ liệu
    public static byte[] decrypt(byte[] message, SecretKey key, String mode) {
        byte[] decryptOut = null;
        try {
            Cipher c = Cipher.getInstance(mode);
            c.init(Cipher.DECRYPT_MODE, key);
            decryptOut = c.doFinal(Base64.getDecoder().decode(message));
        } catch (InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            System.out.println(e.getMessage());
        }
        return decryptOut;
    }

    // Tạo và đóng gói SessionKey
    public static byte[] getSessionKey(byte[] incoming) throws UnknownHostException {
        //Xác định idA, kA
        byte[] ip = new byte[4];
        byte[] idA = new byte[6];
        System.arraycopy(incoming, 1, idA, 0, 6);
        System.arraycopy(idA, 0, ip, 0, 4);
        int port = (idA[4] < 0 ? (idA[4] + 256) : idA[4]) * 256
                + (idA[5] < 0 ? (idA[5] + 256) : idA[5]);
        InetAddress address = InetAddress.getByAddress(ip);
        SecretKey kA = getKey(address, port);

        //Xác định idB, kB
        byte[] idB = new byte[6];
        System.arraycopy(incoming, 7, idB, 0, 6);
        System.arraycopy(idB, 0, ip, 0, 4);
        port = (idB[4] < 0 ? (idB[4] + 256) : idB[4]) * 256
                + (idB[5] < 0 ? (idB[5] + 256) : idB[5]);
        address = InetAddress.getByAddress(ip);
        SecretKey kB = getKey(address, port);

        //Tạo SessionKey
        SecretKey kS = null;
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(128);
            kS = kg.generateKey();
        } catch (NoSuchAlgorithmException ex) {
            System.out.println(ex.getMessage());
        }

        // Đóng gói SessionKey với các khóa bí mật
        byte[] messageB = new byte[22];
        System.arraycopy(kS.getEncoded(), 0, messageB, 0, 16);
        System.arraycopy(idA, 0, messageB, 16, 6);
        byte[] outsendingB = encrypt(messageB, kB, "AES/ECB/PKCS5PADDING");

        byte[] messageA = new byte[72];
        System.arraycopy(kS.getEncoded(), 0, messageA, 0, 16);
        System.arraycopy(idA, 0, messageA, 16, 6);
        System.arraycopy(idB, 0, messageA, 22, 6);
        System.arraycopy(outsendingB, 0, messageA, 28, 44);
        byte[] outsendingA = encrypt(messageA, kA, "AES/ECB/PKCS5PADDING");

        byte[] outsending = new byte[109];
        outsending[0] = (byte) 2;
        System.arraycopy(outsendingA, 0, outsending, 1, 108);
        return outsending;
    }

    public static void forward(byte[] incoming, int len, byte code) throws UnknownHostException, IOException {
        // Xác định IP, port đích để chuyển tiếp
        byte[] ip = new byte[4];
        byte[] idB = new byte[6];
        System.arraycopy(incoming, 7, idB, 0, 6);
        System.arraycopy(idB, 0, ip, 0, 4);
        int port = (idB[4] < 0 ? (idB[4] + 256) : idB[4]) * 256
                + (idB[5] < 0 ? (idB[5] + 256) : idB[5]);
        InetAddress address = InetAddress.getByAddress(ip);

        // Tìm luồng xử lý ứng với IP, port đích và chuyển tiếp
        for (WorkerThread i : clients) {
            ClientKey ck = i.clientKey;
            if (ck.equals(address, port)) {
                DataOutputStream dos = i.out;
                byte[] outsending = new byte[len];
                outsending[0] = code;
                System.arraycopy(incoming, 1, outsending, 1, len - 1);
                dos.write(outsending);
                break;
            }
        }
        System.out.println("\t to client: " + address + " on port " + port);
    }
}
