/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package KDC;

import java.net.InetAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 *
 * @author MyPC
 */
public class ClientKey{
    public SecretKey key;
    public InetAddress address;
    public int port;
    
    public ClientKey(SecretKey key, Socket socket) {
        this.address = socket.getInetAddress();
        this.port = socket.getPort();
        this.key = key;
    }

    public ClientKey(Socket socket) {
        this.address = socket.getInetAddress();
        this.port = socket.getPort();
        
        // Tạo SecretKey cho Client
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(128);
            this.key = kg.generateKey();
        } catch (NoSuchAlgorithmException ex) {
            System.out.println(ex.getMessage());
        }
    }
    
    public boolean equals (ClientKey clk) {
        return (this.address == clk.address)&&(this.port == clk.port);
    }
    public boolean equals (InetAddress address, int port) {
        return (address.equals(this.address))&&(this.port == port);
    }
    public boolean equals (Socket socket) {
        if (address.equals(socket.getInetAddress()))
            if (this.port == socket.getPort())
                return true;
        return false;
    }
    
}
