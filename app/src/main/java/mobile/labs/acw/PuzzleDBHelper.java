package mobile.labs.acw;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PuzzleDBHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Puzzles.db";

    public PuzzleDBHelper(Context pContext) {
        super(pContext, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase pDb){
        pDb.execSQL(PuzzleDBContract.SQL_CREATE_PUZZLE_TABLE);
        pDb.execSQL(PuzzleDBContract.SQL_CREATE_PICTURESET_TABLE);
    }

    public void onUpgrade(SQLiteDatabase pDb, int pOldVersion, int pNewVerison) {
        /*
        * This method is required because it is an abstract method in SQLiteOpenHelper
        * We should be using it to upgrade the database from an older version to a newer version
        * */
    }
}
