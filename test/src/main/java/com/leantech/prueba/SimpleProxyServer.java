package com.leantech.prueba;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class SimpleProxyServer {

    private static final String HOST = "127.0.0.1";
    private static final int SERVER_PORT = 9999;
    private static final int CLIENT_PORT = 9999;

    public static void main(String[] args) {
        try {

            System.out.println("Starting proxy for " + HOST + ":" + SERVER_PORT + " on port " + CLIENT_PORT);
            runServer(HOST, SERVER_PORT, CLIENT_PORT);   // never returns
        }
        catch (Exception e) {
            System.err.println(e);
        }
    }

    public static void runServer(String host, int remote_port, int local_port)
            throws IOException {
        ServerSocket ss = new ServerSocket(local_port);
        final byte[] request = new byte[1024];
        byte[] reply = new byte[4096];
        while(true) {
            Socket client = null, server = null;
            try {
                client = ss.accept();
                final InputStream from_client = client.getInputStream();
                final OutputStream to_client= client.getOutputStream();
                try { server = new Socket(host, remote_port); }
                catch (IOException e) {
                    PrintWriter out = new PrintWriter(new OutputStreamWriter(to_client));
                    out.println("Proxy server cannot connect to " + host + ":" +
                            remote_port + ":\n" + e);
                    out.flush();
                    client.close();
                    continue;
                }
                final InputStream from_server = server.getInputStream();
                final OutputStream to_server = server.getOutputStream();
                threadToServer(request, from_client, to_server);
                threadToClient(request, reply, to_client, from_server);
            }
            catch (IOException e) { System.err.println(e); }
            finally {
                try {
                    if (server != null) server.close();
                    if (client != null) client.close();
                }
                catch(IOException e) {}
            }
        }
    }

    private static void threadToClient(byte[] request, byte[] reply, OutputStream to_client, InputStream from_server) throws IOException {
        int bytes_read;
        try {
            while((bytes_read = from_server.read(reply)) != -1) {
                try {
                    Thread.sleep(1);
                    System.out.println(bytes_read+"to_client--->"+ new String(request, "UTF-8")+"<---");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                to_client.write(reply, 0, bytes_read);
                to_client.flush();
            }
        }
        catch(IOException e) {}
        to_client.close();
    }

    private static void threadToServer(byte[] request, InputStream from_client, OutputStream to_server) {
        new Thread() {
            public void run() {
                int bytes_read;
                try {
                    while((bytes_read = from_client.read(request)) != -1) {
                        to_server.write(request, 0, bytes_read);
                        System.out.println(bytes_read+"to_server--->"+ new String(request, "UTF-8")+"<---");
                        to_server.flush();
                    }
                }
                catch (IOException e) {}
                try {
                    to_server.close();} catch (IOException e) {}
            }
        }.start();
    }
}
