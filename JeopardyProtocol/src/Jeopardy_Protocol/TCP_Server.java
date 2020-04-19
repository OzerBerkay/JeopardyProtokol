package Jeopardy_Protocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TCP_Server {

    private ServerSocket serverSocket;
    private javax.swing.JTextPane historyJTextPane;
    private Thread serverThread;
    private HashSet<ObjectOutputStream> allClients = new HashSet<>();
    String[][] soru = {{"Soru1: Türkiyenin Baskenti Neresidir? \n A)Ankara  \nB)Bursa  \nC)İzmir  \nD)Denizli ", "A"},
    {"Soru2: Kadıköy nerededir? \nA)Ankara  \nB)Bursa  \nC)İstanbul  \nD)Denizli ", "C"},
    {"Soru3: Osmangazi nerededir ? \nA)Ankara  \nB)Bursa  \nC)İzmir  \nD)Denizli ", "B"}};
    int siradakiSoru = 0;//Burak Enes Demir
    int deneme = 1;//Burak Enes Demir
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

                    for (ObjectOutputStream out : allClients) {//Burak Enes Demir
                        out.writeObject("Cevap Kontrol Ediliyor Lütfen Bekleyiniz...");
                    }
                    TimeUnit.SECONDS.sleep(4);
                    if (mesaj.equals(soru[siradakiSoru][1])) {//Burak Enes Demir
                        sendBroadcast("Dogru cevap!");
                        siradakiSoru++;
                        TimeUnit.SECONDS.sleep(4);
                        sendBroadcast(soru[siradakiSoru][0]);
                    } else if (deneme == 1) {//Burak Enes Demir
                        sendBroadcast("Yanlis cevap!");
                        deneme++;
                    } else {//Burak Enes Demir
                        sendBroadcast("Kimse Bilemedi!");
                        siradakiSoru++;
                        deneme = 1;
                        sendBroadcast(soru[siradakiSoru][0]);
                    }
                    // "son" mesajı iletişimi sonlandırır
                    if (mesaj.equals("son")) {
                        break;
                    }
                }

            } catch (IOException | ClassNotFoundException ex) {
                System.out.println("Hata - ListenThread : " + ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(TCP_Server.class.getName()).log(Level.SEVERE, null, ex);
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
