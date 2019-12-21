/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 *
 * @author MyPC
 */
public class Receiving extends Thread{
    public Socket socket;
    public DataInputStream in;
    private final SecretKey kS; // SessionKey
    
    public Receiving(Socket socket, SecretKey kS) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.kS = kS;
    }

    @Override
    public void run() {
        byte[] incoming = new byte[4096];
        int len;
        try {
            while (true) {
                len = in.read(incoming);
                
                //Giải mã tin nhắn nhận được
                byte[] cipher = new byte[len - 13];
                System.arraycopy(incoming, 13, cipher, 0, len - 13);
                String message = new String(decrypt(cipher, kS, "AES/ECB/PKCS5PADDING"));
                System.out.println("\nReceived: " + message);
                System.out.print("Enter your message: ");
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }
    
    // Giải mã tin nhắn nhận được
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
}