package Jeopardy_Protocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;


public class TCP_Server {

    private ServerSocket serverSocket;
    private javax.swing.JTextPane historyJTextPane;
    private Thread serverThread;
    private HashSet<ObjectOutputStream> allClients = new HashSet<>();

    protected void start(int port, javax.swing.JTextPane jTextPaneHistory) throws IOException {
        // server soketi oluşturma (sadece port numarası)
        serverSocket = new ServerSocket(port);
        System.out.println("Server başlatıldı ..");

        // server arayüzündeki history alanı, bütün olaylar buraya yazılacak
        this.historyJTextPane = jTextPaneHistory;

        // arayüzü kitlememek için, server yeni client bağlantılarını ayrı Thread'de beklemeli
        serverThread = new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    // blocking call, yeni bir client bağlantısı bekler
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Yeni bir client bağlandı : " + clientSocket);

                    // bağlanan her client için bir thread oluşturup dinlemeyi başlat
                    new ListenThread(clientSocket).start();
                } catch (IOException ex) {
                    System.out.println("Hata - new Thread() : " + ex);
                    break;
                }
            }
        });
        serverThread.start();
    }

    protected void sendBroadcast(String message) throws IOException {
        // bütün bağlı client'lara mesaj gönder
        for (ObjectOutputStream output : allClients) {
            output.writeObject("Server : " + message);
        }
    }

    protected void writeToHistory(String message) {
        // server arayüzündeki history alanına mesajı yaz
        historyJTextPane.setText(historyJTextPane.getText() + "\n" + message);
    }

    protected void stop() throws IOException {
        // bütün streamleri ve soketleri kapat
        if (serverSocket != null) {
            serverSocket.close();
        }
        if (serverThread != null) {
            serverThread.interrupt();
        }
    }

    class ListenThread extends Thread {

        // dinleyeceğimiz client'ın soket nesnesi, input ve output stream'leri
        private final Socket clientSocket;
        private ObjectInputStream clientInput;
        private ObjectOutputStream clientOutput;

        private ListenThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            writeToHistory("Bağlanan client için thread oluşturuldu : " + this.getName());

            try {
                // input  : client'dan gelen mesajları okumak için
                // output : server'a bağlı olan client'a mesaj göndermek için
                clientInput = new ObjectInputStream(clientSocket.getInputStream());
                clientOutput = new ObjectOutputStream(clientSocket.getOutputStream());

                // Bütün client'lara yeni katılan client bilgisini gönderir
                for (ObjectOutputStream out : allClients) {
                    out.writeObject(this.getName() + " server'a katıldı.");
                }

                // broadcast için, yeni gelen client'ın output stream'ını listeye ekler
                allClients.add(clientOutput);

                // client ismini mesaj olarak gönder
                clientOutput.writeObject("@id-" + this.getName());

                Object mesaj;
                // client mesaj gönderdiği sürece mesajı al
                while ((mesaj = clientInput.readObject()) != null) {
                    // client'in gönderdiği mesajı server ekranına yaz
                    writeToHistory(this.getName() + " : " + mesaj);

                    // bütün client'lara gelen bu mesajı gönder
                    for (ObjectOutputStream out : allClients) {
                        out.writeObject(this.getName() + ": " + mesaj);
                    }

                    // "son" mesajı iletişimi sonlandırır
                    if (mesaj.equals("son")) {
                        break;
                    }
                }

            } catch (IOException | ClassNotFoundException ex) {
                System.out.println("Hata - ListenThread : " + ex);
            } finally {
                try {
                    // client'ların tutulduğu listeden çıkart
                    allClients.remove(clientOutput);

                    // bütün client'lara ayrılma mesajı gönder
                    for (ObjectOutputStream out : allClients) {
                        out.writeObject(this.getName() + " server'dan ayrıldı.");
                    }

                    // bütün streamleri ve soketleri kapat
                    if (clientInput != null) {
                        clientInput.close();
                    }
                    if (clientOutput != null) {
                        clientOutput.close();
                    }
                    if (clientSocket != null) {
                        clientSocket.close();
                    }
                    writeToHistory("Soket kapatıldı : " + clientSocket);
                } catch (IOException ex) {
                    System.out.println("Hata - Soket kapatılamadı : " + ex);
                }
            }
        }
    }

}
