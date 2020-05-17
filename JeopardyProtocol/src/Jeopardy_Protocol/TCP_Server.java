package Jeopardy_Protocol;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
    static String[][] soru;
    int siradakiSoru = 0;//Burak Enes Demir
    int deneme = 1;//Burak Enes Demir
    String[][] yarismaciBilgileri = new String[3][2];//Berkay Özer

    public static int satirSayisi() {//cagri ustun proje sonu: text dosyasındaki satır okuma işlemini yapar
        int sayac = 0;
        try {
            FileInputStream fStream = new FileInputStream("test.txt");
            DataInputStream dStream = new DataInputStream((fStream));
            BufferedReader bReader = new BufferedReader(new InputStreamReader(dStream));

            int i = 0;
            while ((bReader.readLine()) != null) {
                sayac++;
            }
            dStream.close();
        } catch (Exception e) {
            System.err.println("Hata : " + e.getMessage());
        }
        return sayac;
    }

    public static void main(String[] args) throws IOException {// ara rapor 2: Burak Enes Demir server'a ait jtextpanehistory alanları temizlendi ve server arayüzü de aradan kaldırıldı.
        String str;//cagri ustun proje sonu: Tcp_Server server'a gelene kadar dosya okuma ve soruları alma işlemlerini tamamlar
        String[] veri;
        int satirSayisi2 = satirSayisi();
        System.out.println("Satır Sayısı: " + satirSayisi2);
        veri = new String[satirSayisi2];
        soru = new String[satirSayisi2 / 2][2];
        try {
            FileInputStream fStream = new FileInputStream("test.txt");
            DataInputStream dStream = new DataInputStream((fStream));
            BufferedReader bReader = new BufferedReader(new InputStreamReader(dStream));

            int i = 0;
            while ((str = bReader.readLine()) != null) {
                veri[i] = str;

                i++;
            }

            dStream.close();
        } catch (Exception e) {
            System.err.println("Hata : " + e.getMessage());
        }
        int a = 0;
        for (int i = 0; i < satirSayisi2; i++) {
            if (i % 2 == 0) {
                soru[a][0] = veri[i];
            }
            if (i % 2 == 1) {
                soru[a][1] = veri[i];
                a++;
            }
        }

        for (int i = 0; i < satirSayisi2 / 2; i++) {
            for (int k = 0; k < 2; k++) {
                System.out.println(soru[i][k]);
            }
        }
        TCP_Server server;
        server = new TCP_Server();
        server.start(44444);
    }

    protected void start(int port) throws IOException {
        // server soketi oluşturma (sadece port numarası)
        serverSocket = new ServerSocket(port);
        System.out.println(MessageUtil.SERVER_BASLATILDI);

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
        if (allClients.size() == yarismaciBilgileri.length) {//Burak Enes Demir
            try {
                if (soru.length > 0) {
                    sendBroadcast(soru[siradakiSoru][0]);
                } else {
                    sendBroadcast(MessageUtil.SON_SORU);
                }
            } catch (IOException e) {
                System.out.println("Olmadı!" + e.getMessage());
            }

        }
    }

    protected void findWinner(String[][] yarismaciBilgileri) throws IOException { //birincileri belirleme metodu
        List<Integer> list = new ArrayList<>(); //Ömer Faruk Küçüker
        Integer max;
        for (String[] yarismaciBilgisi : yarismaciBilgileri) {
            list.add(Integer.parseInt(yarismaciBilgisi[1]));
        }
        max = Collections.max(list);
        sendBroadcast("Birinci/Birinciler:");
        for (String[] yarismaciBilgisi : yarismaciBilgileri) {
            if (yarismaciBilgisi[1].equals(Integer.toString(max))) {
                sendBroadcast(yarismaciBilgisi[0] + "\n");
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
            if (yarismaciBilgileri[yarismaciBilgileri.length - 1][0] != null) {//sonradan başka bir oyuncu dahil olmasın diye allclients.size()==yarismaciBilgileri.length yerine bunu kullandık
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
                    yarismaciBilgileri[allClients.size()][0] = this.getName(); //Eklenen clientlerin adını ve puanlarını arrayde tutuyor
                    yarismaciBilgileri[allClients.size()][1] = "0";//Berkay Özer

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
                            out.writeObject(MessageUtil.CEVAP_KONTROL);

                        }
                        TimeUnit.SECONDS.sleep(4);
                        if (mesaj.equals(soru[siradakiSoru][1])) {//Burak Enes Demir
                            sendBroadcast(MessageUtil.DOGRU_CEVAP);
                            for (String[] tred : yarismaciBilgileri) {
                                //Berkay Özer
                                if (tred[0].equals(this.getName())) {
                                    //doğru bilenin puanı artırılır
                                    tred[1] = "" + (Integer.parseInt(tred[1]) + 1);
                                }
                                sendBroadcast(tred[0] + "'in puani:" + tred[1]); //tüm puanlar soru sonu ekrana yazdırılır
                                
                            }
                            siradakiSoru++;
                            deneme=1;
                            TimeUnit.SECONDS.sleep(4);
                            if (siradakiSoru == soru.length - 1) { //Berkay Özer Sona yaklaşıldığının veya yarışmaının bittiğinin bilgisini döndürür
                                sendBroadcast(MessageUtil.SON_SORUYA_ULASILDI);
                            } else if (siradakiSoru == soru.length || allClients.size() < 2) {//Sona yaklaşıldığının veya yarışmaının bittiğinin bilgisini döndürür
                                sendBroadcast(MessageUtil.YARISMA_SON);
                                findWinner(yarismaciBilgileri);//Ömer Faruk Küçüker
                                break;
                            }
                            TimeUnit.SECONDS.sleep(4);
                            sendBroadcast(soru[siradakiSoru][0]);
                        } else if (deneme == 1) {//Burak Enes Demir
                            sendBroadcast(MessageUtil.YANLIS_CEVAP);
                            deneme++;
                        } else {//Burak Enes Demir
                            sendBroadcast(MessageUtil.BILEN_YOK);
                            for (String[] tred : yarismaciBilgileri) {
                                //Berkay Özer
                                sendBroadcast(tred[0] + "'in puani:" + tred[1]); ////tüm puanlar soru sonu ekrana yazdırılır
                            }
                            siradakiSoru++;
                            if (siradakiSoru == soru.length - 1) {//Berkay Özer Sona yaklaşıldığının veya yarışmaının bittiğinin bilgisini döndürür
                                sendBroadcast(MessageUtil.SON_SORUYA_ULASILDI);
                            } else if (siradakiSoru == soru.length || allClients.size() < 2) {
                                sendBroadcast(MessageUtil.YARISMA_SON);
                                findWinner(yarismaciBilgileri);//Ömer Faruk Küçüker
                                break;
                            }
                            deneme = 1;
                            sendBroadcast(soru[siradakiSoru][0]);
                        }

                    }
                } catch (IOException | ClassNotFoundException ex) {
                    System.out.println("bir hata yaşandı"); // catch içerisine böyle bir durum koyduk çünkü tüm oyuncular oyunun orta yerinde çıkarsa oyunun başlatılabilmesi gerekmektedir
                    allClients.remove(clientOutput);// bir client kapandığında buradan exception atar, biz de bundan istifade aynı zamanda tredlerimizi ve allclient'imizi temizleyeceğiz. 
                    if (allClients.size() == 0) {//son kısım:tüm server'ı yeni oyun için sıfırlar Berkay Özer 
                        siradakiSoru = 0;// sonuç olarak da eğer tüm clientler aynı anda terkederlerse bile server kendi içerisinde sıradaki soruya geçilmesini bekleyeceğim diye bugda kalmayacak
                        for (String[] tred : yarismaciBilgileri) {
                            //son kısım:tredler içerisinde bulunan yarışmacı ve puanlarını yeni oyun için sildik Berkay Özer
                            tred[0] = null;
                            tred[1] = null;
                        }
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(TCP_Server.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    try {
                        // client'ların tutulduğu listeden çıkart
                        allClients.remove(clientOutput);
                        if (siradakiSoru == soru.length) {//ara rapor 2:tüm server'ı yeni oyun için sıfırlar Berkay Özer kısım 2

                            allClients.removeAll(allClients);
                            siradakiSoru = 0;
                            for (String[] tred : yarismaciBilgileri) {
                                //ara rapor 2:tredler içerisinde bulunan yarışmacı ve puanlarını yeni oyun için sildik Berkay Özer
                                tred[0] = null;
                                tred[1] = null;
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
