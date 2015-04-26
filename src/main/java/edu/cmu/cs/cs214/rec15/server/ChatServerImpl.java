package edu.cmu.cs.cs214.rec15.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.cmu.cs.cs214.rec15.util.Log;

public class ChatServerImpl extends Thread implements ChatServer {
    private static final String TAG = "SERVER";
    private static final int POOL_SIZE = Runtime.getRuntime()
            .availableProcessors();
    private int port;
    private final ExecutorService mExecutor;
    private static ArrayList<Socket> clients = new ArrayList<Socket>();
    private static ArrayList<Message> messages = new ArrayList<Message>();


    public ChatServerImpl(int port) {
        this.port = port;
        this.mExecutor = Executors.newFixedThreadPool(POOL_SIZE);
    }


    public void run() {
        try {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(port);
            } catch (IOException e) {
                Log.e(TAG,
                        "Could not open server socket on port " + port + ".", e);
                return;
            }

            Log.i(TAG, "Listening for incoming commands on port " + port + ".");

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    Log.i(TAG, String.format("Got connection from %s:%s",
                            clientSocket.getRemoteSocketAddress(),
                            clientSocket.getPort()));
                    clients.add(clientSocket);
                    mExecutor.execute(new ClientHandler(clientSocket));
                } catch (IOException e) {
                    Log.e(TAG,
                            "Error while listening for incoming connections.",
                            e);
                    break;
                }
            }

            Log.i(TAG, "Shutting down...");

            try {
                serverSocket.close();
            } catch (IOException e) {
                // Ignore because we're about to exit anyway.
            }
        } finally {
            mExecutor.shutdown();
        }
    }


    @Override
    public void startServer() {
        this.start();
    }


    @Override
    public int getNumClients() {
        return clients.size();
    }


    @Override
    public ArrayList<Message> getMessages() {
        return (ArrayList<Message>) Collections.unmodifiableList(messages);
    }

    /**
     * Handler for every client connection to the server.
     * 
     * @author tsun
     *
     */
    private static class ClientHandler implements Runnable {
        private final Socket socket;


        public ClientHandler(Socket socket) {
            this.socket = socket;
        }


        @Override
        public void run() {
            try {
                ObjectInputStream in = new ObjectInputStream(
                        socket.getInputStream());
                while (true) {
                    Message msg = (Message) in.readObject();
                    onNewMessage(socket, msg);
                }
            } catch (IOException e) {
                Log.e(TAG, "Connection lost.", e);
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "Received invalid task from client.", e);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignore because we're about to exit anyway.
                }
            }
        }


        private void onNewMessage(Socket from, Message msg) {
            for (Socket s : clients) {
                if (!s.equals(from)) {
                    try {
                        ObjectOutputStream out = new ObjectOutputStream(
                                s.getOutputStream());
                        out.writeObject(msg);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

    }
}
