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
    private javax.swing.JButton abut;//Ömer Faruk Küçüker
    private javax.swing.JButton bbut;
    private javax.swing.JButton cbut;
    private javax.swing.JButton dbut;

    String LastSender = "th:";//Çağrı Üstün



    protected void start(String host, int port, javax.swing.JTextPane jTextPaneHistory,
            javax.swing.JLabel jLabelName, javax.swing.JButton Abutton,
            javax.swing.JButton Bbutton, javax.swing.JButton Cbutton,
            javax.swing.JButton Dbutton) throws IOException {//Ömer Faruk Küçüker
        // client soketi oluşturma (ip + port numarası)
        clientSocket = new Socket(host, port);

        // client arayüzündeki history alanı, bütün olaylar buraya yazılacak
        this.historyJTextPane = jTextPaneHistory;
        // client arayüzündeki isim yazısı, client ismi server tarafından belirlenecek
        this.nameJLabel = jLabelName;

        //Butonlarımızı arayüzdekiler ile eşleştirdik.
        this.abut = Abutton;
        this.bbut = Bbutton;
        this.cbut = Cbutton;
        this.dbut = Dbutton;
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

    protected void openButtons() { //Ömer Faruk Küçüker
        this.abut.setEnabled(true);
        this.bbut.setEnabled(true);
        this.cbut.setEnabled(true);
        this.dbut.setEnabled(true);
    }

    protected void closeButtons() {//Ömer Faruk Küçüker
        this.abut.setEnabled(false);
        this.bbut.setEnabled(false);
        this.cbut.setEnabled(false);
        this.dbut.setEnabled(false);
    }


    protected void writeToHistory(Object message) {//Çağrı Üstün
        // client arayüzündeki history alanına mesajı yaz

        String mes = message.toString();
        if (mes.contains("Soru")) {// Bir soru geldiğinde butonlar aktif olur
            historyJTextPane.setText("");
            openButtons();
        } else if (mes.contains("Yanlis cevap!")) { // cevap yanlışsa tüm clientların butonları aktif olur
            openButtons();
            if (nameJLabel.getText().equals(LastSender)) { //yanlış veren kullanıcının butonları aktif olmaz
                closeButtons();
            }
        } else if (mes.contains("Dogru cevap!") || mes.contains("Cevap Kontrol Ediliyor Lütfen Bekleyiniz...")) {// doğru cevap verildiğinde sıradaki soru için herkesin butonları kapatılır.
            closeButtons();
        } else if (mes.contains("Kimse Bilemedi!")) {// ikinci cevapta da yanlış cevap verilirse sonraki soru için herkesin butonları kapatılır.
            closeButtons();
        }
        historyJTextPane.setText(historyJTextPane.getText() + "\n" + message);
        if (mes.contains(":") && !mes.contains("Server")) { //en son mesaj yollayan kullanıcının kim olduğu belirlenir
            LastSender = mes.substring(0, mes.indexOf(':')).trim();
            System.out.println(LastSender);
        }
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
