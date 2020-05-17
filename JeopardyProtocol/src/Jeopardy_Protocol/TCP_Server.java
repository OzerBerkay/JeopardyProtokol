package Jeopardy_Protocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
    String[][] tred = new String[3][2];//Berkay Özer

    public static void main(String[] args) throws IOException {// ara rapor 2: Burak Enes Demir server'a ait jtextpanehistory alanları temizlendi ve server arayüzü de aradan kaldırıldı.
        TCP_Server server;
        server = new TCP_Server();
        server.start(44444);
    }

    protected void start(int port) throws IOException {
        // server soketi oluşturma (sadece port numarası)
        serverSocket = new ServerSocket(port);
        System.out.println("Server başlatıldı ..");

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
        //  historyJTextPane.setText(historyJTextPane.getText() + "\n" + message);
        System.out.println(message);//artık mesaj serverin outputunda veriliyor

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

    public void checkClientSizeAndQuestion() {
        if (allClients.size() == tred.length) {//Burak Enes Demir
            try {
                if (soru.length > 0) {
                    sendBroadcast(soru[siradakiSoru][0]);
                } else {
                    sendBroadcast("Hiç sorumuz kalmamıştır!");
                }
            } catch (Exception e) {
                System.out.println("Olmadı!" + e.getMessage());
            }

        }
    }

    protected void findWinner(String[][] arr) throws IOException { //birincileri belirleme metodu
        List<Integer> list = new ArrayList<Integer>(); //Ömer Faruk Küçüker
        for (int i = 0; i < arr.length; i++) {
            list.add(Integer.parseInt(arr[i][1]));
        }
        int max = Collections.max(list);
        sendBroadcast("Birinci/Birinciler:");
        for (int i = 0; i < arr.length; i++) {
            if (arr[i][1].equals(Integer.toString(max))) {
                sendBroadcast(arr[i][0] + "\n");
            }
        }
    }

    void StreamveSoketKapat(Socket clientSocket, ObjectInputStream clientInput, ObjectOutputStream clientOutput) throws IOException {
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
            if (tred[tred.length - 1][0] != null) {//sonradan başka bir oyuncu dahil olmasın diye allclients.size()==tred.length yerine bunu kullandık
                System.out.println("bağlanamadı"); //eğer böyle kullanmasaydık başka bir oyuncu oyundan çıktığında başkası yerine girebilirdi
                try {
                    clientOutput = new ObjectOutputStream(clientSocket.getOutputStream()); //böylece odanın bu şekilde dolu olduğunu ve oyuna giremeyeceğini gösterdik
                    clientOutput.writeObject("Oda Şuan Dolu Daha Sonra Deneyiniz!!");//Berkay Özer ara rapor 2
                    StreamveSoketKapat(clientSocket, clientInput, clientOutput);
                } catch (IOException ex) {
                    Logger.getLogger(TCP_Server.class.getName()).log(Level.SEVERE, null, ex);
                }

            } else {// ara rapor 2 : Berkay Özer eğer sonradan dahil olmaya çalışan biri değilse zaten istenilen çoğunluğa ulaşılamadıysa buraya gelir ve kullanıcı oyuna girer
                //writeToHistory("Bağlanan client için thread oluşturuldu : " + this.getName());
                System.out.println("Bağlanan client için thread oluşturuldu : " + this.getName());// ara rapor 2: artık bağlanan thread ismi outputa yazılıyor
                try {
                    // input  : client'dan gelen mesajları okumak için
                    // output : server'a bağlı olan client'a mesaj göndermek için
                    clientInput = new ObjectInputStream(clientSocket.getInputStream());
                    clientOutput = new ObjectOutputStream(clientSocket.getOutputStream());

                    // Bütün client'lara yeni katılan client bilgisini gönderir
                    for (ObjectOutputStream out : allClients) {
                        out.writeObject(this.getName() + " server'a katıldı.");
                    }
                    tred[allClients.size()][0] = this.getName(); //Eklenen clientlerin adını ve puanlarını arrayde tutuyor
                    tred[allClients.size()][1] = "0";//Berkay Özer
                    System.out.println("buraya da geldiiiiş");
                    // broadcast için, yeni gelen client'ın output stream'ını listeye ekler
                    allClients.add(clientOutput);

                    // client ismini mesaj olarak gönder
                    clientOutput.writeObject("@id-" + this.getName());

                    //soru sayısını kontrol et
                    checkClientSizeAndQuestion();

                    Object mesaj;
                    // client mesaj gönderdiği sürece mesajı al
                    while ((mesaj = clientInput.readObject()) != null) {
                        // client'in gönderdiği mesajı server ekranına yaz
                        //writeToHistory(this.getName() + " : " + mesaj);
                        System.out.println(this.getName() + " : " + mesaj);//ara rapor 2:artık clientin gönderdiği mesaj outputa yazılıyor
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
                            for (int i = 0; i < tred.length; i++) {//Berkay Özer
                                if (tred[i][0].equals(this.getName())) {    //doğru bilenin puanı artırılır
                                    tred[i][1] = "" + (Integer.parseInt(tred[i][1]) + 1);
                                }
                                sendBroadcast(tred[i][0] + "'in puani:" + tred[i][1]); //tüm puanlar soru sonu ekrana yazdırılır
                            }
                            siradakiSoru++;
                            if (siradakiSoru == soru.length - 1) { //Berkay Özer Sona yaklaşıldığının veya yarışmaının bittiğinin bilgisini döndürür
                                sendBroadcast("Son soruya ulaştınız!!");
                            } else if (siradakiSoru == soru.length || allClients.size() < 2) {//Sona yaklaşıldığının veya yarışmaının bittiğinin bilgisini döndürür
                                sendBroadcast("Yarisma bitmistir!!");
                                findWinner(tred);//Ömer Faruk Küçüker
                                break;
                            }
                            TimeUnit.SECONDS.sleep(4);
                            sendBroadcast(soru[siradakiSoru][0]);
                        } else if (deneme == 1) {//Burak Enes Demir
                            sendBroadcast("Yanlis cevap!");
                            deneme++;
                        } else {//Burak Enes Demir
                            sendBroadcast("Kimse Bilemedi!");
                            for (int i = 0; i < tred.length; i++) {//Berkay Özer
                                sendBroadcast(tred[i][0] + "'in puani:" + tred[i][1]);  ////tüm puanlar soru sonu ekrana yazdırılır
                            }
                            siradakiSoru++;
                            if (siradakiSoru == soru.length - 1) {//Berkay Özer Sona yaklaşıldığının veya yarışmaının bittiğinin bilgisini döndürür
                                sendBroadcast("Son soruya ulaştınız!!");
                            } else if (siradakiSoru == soru.length || allClients.size() < 2) {
                                sendBroadcast("Yarisma bitmistir!!");
                                findWinner(tred);//Ömer Faruk Küçüker
                                break;
                            }
                            deneme = 1;
                            sendBroadcast(soru[siradakiSoru][0]);
                        }

                        // "son" mesajı iletişimi sonlandırır
                        if (mesaj.equals("son")) {
                            break;
                        }
                    }
                } catch (IOException | ClassNotFoundException ex) {
                    System.out.println("bir hata yaşandı"); // catch içerisine böyle bir durum koyduk çünkü tüm oyuncular oyunun orta yerinde çıkarsa oyunun başlatılabilmesi gerekmektedir
                    allClients.remove(clientOutput);// bir client kapandığında buradan exception atar, biz de bundan istifade aynı zamanda tredlerimizi ve allclient'imizi temizleyeceğiz. 
                    if (allClients.size() == 0) {//son kısım:tüm server'ı yeni oyun için sıfırlar Berkay Özer 
                        siradakiSoru = 0;// sonuç olarak da eğer tüm clientler aynı anda terkederlerse bile server kendi içerisinde sıradaki soruya geçilmesini bekleyeceğim diye bugda kalmayacak
                        for (int i = 0; i < tred.length; i++) { //son kısım:tredler içerisinde bulunan yarışmacı ve puanlarını yeni oyun için sildik Berkay Özer
                            tred[i][0] = null;
                            tred[i][1] = null;
                        }
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(TCP_Server.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    try {
                        // client'ların tutulduğu listeden çıkart
                        allClients.remove(clientOutput);
                        if (siradakiSoru == soru.length) {//ara rapor 2:tüm server'ı yeni oyun için sıfırlar Berkay Özer kısım 2
                            System.out.println("buraya geldi sonunda");
                            allClients.removeAll(allClients);
                            siradakiSoru = 0;
                            for (int i = 0; i < tred.length; i++) { //ara rapor 2:tredler içerisinde bulunan yarışmacı ve puanlarını yeni oyun için sildik Berkay Özer
                                tred[i][0] = null;
                                tred[i][1] = null;
                            }
                        }

                        // bütün client'lara ayrılma mesajı gönder
                        for (ObjectOutputStream out : allClients) {
                            out.writeObject(this.getName() + " server'dan ayrıldı.");
                        }
                        // bütün streamleri ve soketleri kapat
                        StreamveSoketKapat(clientSocket, clientInput, clientOutput);
                        //writeToHistory("Soket kapatıldı : " + clientSocket);
                        System.out.println("Soket kapatıldı : " + clientSocket);//ara rapor 2: artık kapatılan soket outputa yazılıyor Berkay Özer
                    } catch (IOException ex) {
                        System.out.println("Hata - Soket kapatılamadı : " + ex);
                    }
                }
            }
        }
    }

}
