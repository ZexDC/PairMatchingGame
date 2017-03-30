package mobile.labs.acw;

import android.provider.BaseColumns;

public class PuzzleDBContract {

    private PuzzleDBContract(){}

    public static abstract class PuzzleEntry implements BaseColumns {
        public static final String TABLE_NAME = "Puzzle";
        public static final String COLUMN_NAME_ID = "PuzzleID";
        public static final String COLUMN_NAME_PICTURE_SET_ID = "PictureSetID";
        public static final String COLUMN_NAME_ROWS = "Rows";
        public static final String COLUMN_NAME_LAYOUT_ARRAY = "LayoutArray";
        public static final String COLUMN_NAME_HIGH_SCORE = "HighScore";
    }

    public static abstract class PictureSetEntry implements BaseColumns {
        public static final String TABLE_NAME = "PictureSet";
        public static final String COLUMN_NAME_PICTURES_ARRAY = "PicturesArray";
    }

    public static final String TEXT_TYPE = " TEXT";
    public static final String COMMA_SEP = ",";

    public static final String SQL_CREATE_PUZZLE_TABLE = "CREATE TABLE " + PuzzleEntry.TABLE_NAME +
            " (" + PuzzleEntry.COLUMN_NAME_ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
            PuzzleEntry.COLUMN_NAME_PICTURE_SET_ID + " INTEGER" + COMMA_SEP +
            PuzzleEntry.COLUMN_NAME_ROWS + " INTEGER" + COMMA_SEP +
            PuzzleEntry.COLUMN_NAME_LAYOUT_ARRAY + COMMA_SEP + PuzzleEntry.COLUMN_NAME_HIGH_SCORE +
            " INTEGER" + COMMA_SEP + " FOREIGN KEY(" + PuzzleEntry.COLUMN_NAME_PICTURE_SET_ID +
            ") REFERENCES " + PictureSetEntry.TABLE_NAME +  " )";

    public static final String SQL_CREATE_PICTURESET_TABLE = "CREATE TABLE " + PictureSetEntry.TABLE_NAME +
            " (" + PictureSetEntry._ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
            PictureSetEntry.COLUMN_NAME_PICTURES_ARRAY  + " UNIQUE )";

    public static final String SQL_DELETE_PUZZLE_TABLE = "DROP TABLE IF EXISTS " + PuzzleEntry.TABLE_NAME;
    public static final String SQL_DELETE_PICTURESET_TABLE = "DROP TABLE IF EXISTS " + PictureSetEntry.TABLE_NAME;
}
