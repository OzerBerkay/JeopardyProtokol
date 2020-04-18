package Jeopardy_Protocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


public class TCP_Client {

    private Socket clientSocket;
    private ObjectInputStream clientInput;
    private ObjectOutputStream clientOutput;
    private javax.swing.JTextPane historyJTextPane;
    private javax.swing.JLabel nameJLabel;
    private Thread clientThread;

    protected void start(String host, int port, javax.swing.JTextPane jTextPaneHistory, javax.swing.JLabel jLabelName) throws IOException {
        // client soketi oluşturma (ip + port numarası)
        clientSocket = new Socket(host, port);

        // client arayüzündeki history alanı, bütün olaylar buraya yazılacak
        this.historyJTextPane = jTextPaneHistory;
        // client arayüzündeki isim yazısı, client ismi server tarafından belirlenecek
        this.nameJLabel = jLabelName;
        // input  : client'a gelen mesajları okumak için
        // output : client'dan bağlı olduğu server'a mesaj göndermek için
        clientOutput = new ObjectOutputStream(clientSocket.getOutputStream());
        clientInput = new ObjectInputStream(clientSocket.getInputStream());

        // server'ı sürekli dinlemek için Thread oluştur
        clientThread = new ListenThread();
        clientThread.start();
    }

    protected void sendMessage(String message) throws IOException {
        // gelen mesajı server'a gönder
        clientOutput.writeObject(message);
    }

    protected void sendObject(Object message) throws IOException {
        // gelen nesneyi server'a gönder
        clientOutput.writeObject(message);
    }

    protected void writeToHistory(Object message) {
        // client arayüzündeki history alanına mesajı yaz
        historyJTextPane.setText(historyJTextPane.getText() + "\n" + message);
    }

    protected void disconnect() throws IOException {
        // bütün streamleri ve soketleri kapat
        if (clientInput != null) {
            clientInput.close();
        }
        if (clientOutput != null) {
            clientOutput.close();
        }
        if (clientThread != null) {
            clientThread.interrupt();
        }
        if (clientSocket != null) {
            clientSocket.close();
        }
    }

    class ListenThread extends Thread {

        // server'dan gelen mesajları dinle
        @Override
        public void run() {
            try {
                writeToHistory("Server'a bağlandı ..");

                Object mesaj;
                // server mesaj gönderdiği sürece gelen mesajı al
                while ((mesaj = clientInput.readObject()) != null) {
                    // id mesajı kontrolü, id mesajı alınırsa name etiketini değiştirir 
                    if (mesaj instanceof String && ((String) mesaj).contains("@id-")) {
                        nameJLabel.setText(((String) mesaj).substring(4));
                        continue;
                    }

                    // serverdan gelen mesajı arayüze yaz
                    writeToHistory(mesaj);

                    // "son" mesajı iletişimi sonlandırır
                    if (mesaj.equals("son")) {
                        break;
                    }
                }
            } catch (IOException | ClassNotFoundException ex) {
                System.out.println("Error - ListenThread : " + ex);
            }
        }
    }

}
