package Jeopardy_Protocol;

import java.io.Serializable;

public class Mesaj implements Serializable {

    String text;

    @Override
    public String toString() {
        return "Mesaj nesnesi : " + text;
    }

}
