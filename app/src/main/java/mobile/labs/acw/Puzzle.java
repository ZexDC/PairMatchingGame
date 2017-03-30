package mobile.labs.acw;

import java.io.Serializable;

public class Puzzle implements Serializable {
    private int id, rows, highscore, size;
    private String picset, layout;

    public Puzzle(int pId, String pPicset, int pRows, String pLayout, int pHighscore) {
        id = pId;
        picset = pPicset;
        rows = pRows;
        layout = pLayout;
        highscore = pHighscore;
        // calculate size
        // remove beginning [ and trailing ] to split on ,
        pLayout = pLayout.replaceAll("\\[|\\]", "");
        String[] layoutArray = pLayout.split(",");
        int columns = layoutArray.length / rows;
        size = (rows * columns) / 2;
    }

    public int ID() {
        return id;
    }

    public String Picset() {
        return picset;
    }

    public int Rows() {
        return rows;
    }

    public String Layout() {
        return layout;
    }

    public int Highscore() {
        return highscore;
    }

    public int Size() {
        return size;
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Puzzle)) {
            return false;
        }
        Puzzle tempObj = (Puzzle) obj;
        return tempObj.ID() == id;
    }
}
