/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package KDC;

import java.net.InetAddress;
import java.net.Socket;

/**
 *
 * @author MyPC
 */
public class InforClient {
    // Định danh của Client

    public InetAddress address;
    public int port;

    public InforClient(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public InforClient(Socket socket) {
        this.address = socket.getLocalAddress();
        this.port = socket.getLocalPort();
    }

    public byte[] ID() {
        // Định danh thành mảng byte
        byte[] ip = this.address.getAddress();
        byte[] id = new byte[6];
        id[0] = ip[0];
        id[1] = ip[1];
        id[2] = ip[2];
        id[3] = ip[3];
        id[4] = (byte) (this.port / 256);
        id[5] = (byte) (this.port % 256);
        
        return id;
    }

    public boolean equals(InforClient ic) {
        if (this.address.equals(ic.address)) {
            if (this.port == ic.port) {
                return true;
            }
        }
        return false;
    }
}
