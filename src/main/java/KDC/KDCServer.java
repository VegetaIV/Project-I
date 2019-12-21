/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package KDC;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author MyPC
 */
public class KDCServer {

    public final static int SERVER_PORT = 7;
    public static final int NUM_OF_THREAD = 2;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        ExecutorService executor = Executors.newFixedThreadPool(NUM_OF_THREAD);
        try {
            // Tạo Server Socket 
            System.out.println("Binding to port " + SERVER_PORT + ", please wait  ...");
            serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server started: " + serverSocket);
            System.out.println("Waiting for a client ...");
            while (true) {
                try {
                    // Server đợi kết nối từ một Client
                    Socket socket = serverSocket.accept();
                    System.out.println("Client accepted: " + socket);
                    
                    // Tạo luống xử lý yêu cầu từ Client
                    WorkerThread handler = new WorkerThread(socket);
                    executor.execute(handler);
                } catch (IOException e) {
                    System.err.println(" Connection Error: " + e);
                }
            }
        } catch (IOException e1) {
            System.out.println(e1.getMessage());
        } finally {
            if (serverSocket != null) {
                serverSocket.close();
            }
        }
    }
}
