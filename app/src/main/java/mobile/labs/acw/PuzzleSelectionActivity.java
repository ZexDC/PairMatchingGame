package mobile.labs.acw;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

public class PuzzleSelectionActivity extends AppCompatActivity {

    public final String INDEX_URL = "http://www.hull.ac.uk/php/349628/08027/acw/index.json";
    public final String PUZZLES_URL = "http://www.hull.ac.uk/php/349628/08027/acw/puzzles/";
    public final String PICSETS_URL = "http://www.hull.ac.uk/php/349628/08027/acw/picturesets/";
    public final String IMAGES_URL = "http://www.hull.ac.uk/php/349628/08027/acw/images/";

    public PuzzleDBHelper mDbHelper = new PuzzleDBHelper(this);
    public Puzzle mSelectedPuzzle = null;
    public PuzzleListAdapter mAdapter;
    public ProgressDialog mProgressDialog;
    public Dialog mFilterDialog;

    private class PuzzleListAdapter extends BaseAdapter {
        Context mContext;
        ArrayList<Puzzle> mPuzzleList;
        ArrayList<Puzzle> mFilteredPuzzleList;

        PuzzleListAdapter(Context context, ArrayList<Puzzle> puzzleList) {
            mContext = context;
            mPuzzleList = puzzleList;
            mFilteredPuzzleList = puzzleList;
        }

        @Override
        public int getCount() {
            return mFilteredPuzzleList.size();
        }

        @Override
        public Object getItem(int position) {
            return mFilteredPuzzleList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void filter(int id, int size, boolean showCompleted, boolean showUncompleted) {
            // setup variables to speed up checks
            boolean isIdFilter = true;
            boolean isSizeFilter = true;
            if (id == -1) {
                isIdFilter = false;
            }
            if (size == -1) {
                isSizeFilter = false;
            }

            mFilteredPuzzleList = new ArrayList<>();
            for (Puzzle p : mPuzzleList) {
                if (isIdFilter) { // check only for unique id
                    if (p.ID() == id) {
                        if (isSizeFilter && p.Size() == size) {
                            if (showCompleted && p.Highscore() > 0) {
                                mFilteredPuzzleList.add(p);
                                break; // unique id found, loop can be stopped
                            } else if (showUncompleted && p.Highscore() == 0) {
                                mFilteredPuzzleList.add(p);
                                break; // unique id found, loop can be stopped
                            }
                        } else if (!isSizeFilter) { // no size filter
                            if (showCompleted && p.Highscore() > 0) {
                                mFilteredPuzzleList.add(p);
                                break; // unique id found, loop can be stopped
                            } else if (showUncompleted && p.Highscore() == 0) {
                                mFilteredPuzzleList.add(p);
                                break; // unique id found, loop can be stopped
                            }
                        }
                    }
                } else if (isSizeFilter) { // check size and completion
                    if (p.Size() == size) {
                        if (showCompleted && p.Highscore() > 0) {
                            mFilteredPuzzleList.add(p);
                        } else if (showUncompleted && p.Highscore() == 0) {
                            mFilteredPuzzleList.add(p);
                        }
                    }
                } else { // no size filter, check only completion
                    if (showCompleted && p.Highscore() > 0) {
                        mFilteredPuzzleList.add(p);
                    } else if (showUncompleted && p.Highscore() == 0) {
                        mFilteredPuzzleList.add(p);
                    }
                }
            }
            notifyDataSetChanged();
        }

        public void remove(int id)
        {
            for (Puzzle p : mPuzzleList) {
                if (p.ID() == id) {
                    mFilteredPuzzleList.remove(p);
                    break;
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View row;
            Puzzle element = mFilteredPuzzleList.get(position);
            if (convertView == null) {
                // if it's not recycled, initialize some attributes
                LayoutInflater inflater = (LayoutInflater) PuzzleSelectionActivity.this.
                        getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.puzzle_list_item, parent, false);
            } else {
                row = convertView;
            }
            // update text UI for this row
            TextView textView = (TextView) row.findViewById(R.id.idTextView);
            textView.setText(String.valueOf(element.ID()));
            textView = (TextView) row.findViewById(R.id.sizeTextView);
            textView.setText(String.valueOf(element.Size()));
            textView = (TextView) row.findViewById(R.id.highscoreTextView);
            textView.setText(String.valueOf(element.Highscore()));
            return row;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_puzzle_selection);

        // disable play/download button until an item from the listview is clicked
        Button button = (Button) findViewById(R.id.puzzleSelectionButton);
        button.setEnabled(false);

        // local puzzles
        if (getIntent().getStringExtra("SelectionType").equals("Local")) {
            button.setText(R.string.PlayBtn);
            ArrayList<Puzzle> localPuzzleArray = getPuzzlesFromDB();
            if (localPuzzleArray.size() > 0) {
                setupListView(localPuzzleArray);
            }
        }
        // remote puzzles
        else {
            button.setText(R.string.DownloadBtn);
            if(isNetworkAvailable()) {
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setMessage("Loading. Please wait");
                mProgressDialog.show();
                new downloadJSON().execute(INDEX_URL, PUZZLES_URL, PICSETS_URL);
            }
            else { // no network
                AlertDialog.Builder alertDialogBuilder =
                        new AlertDialog.Builder(this)
                                .setMessage(getResources().getString(R.string.NoNetworkAlertDialog));
                alertDialogBuilder.show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // avoid crash on re-orientation
        if(mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    public void onPuzzleSelectedClick(View pView) {
        // play the selected puzzle
        if (getIntent().getStringExtra("SelectionType").equals("Local")) {
            // save the id in sharedpreferences in case the user tries to continue and old game
            MainActivity.prefs.edit().putInt(SharedPrefs.PUZZLE_ID, mSelectedPuzzle.ID()).apply();
            // start the play activity
            Intent intent = new Intent(this, PuzzlePlayActivity.class);
            intent.putExtra("PuzzleObject", mSelectedPuzzle);
            intent.putExtra("isNewGame", true);
            startActivity(intent);
        }
        // download the selected puzzle if intent extra is "Remote"
        else {
            String text = getResources().getString(R.string.DownloadingSelectedPuzzleToast) + " " +
                    mSelectedPuzzle.ID() + "...";
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
            new downloadPuzzle().execute(IMAGES_URL);
        }
    }

    public void onFilterButtonClick(View pView){
        mFilterDialog = new Dialog(PuzzleSelectionActivity.this);
        mFilterDialog.setContentView(R.layout.filter_dialog);
        try {
            mFilterDialog.setTitle(R.string.filterDialogTitle);
            mFilterDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onApplyFilterButtonClick(View pView) {
        EditText editText = (EditText) mFilterDialog.findViewById(R.id.filterPuzzleIdEditText);
        int id = -1;
        if (editText.getText().toString().trim().length() > 0) {
            id = Integer.valueOf(editText.getText().toString());
        }

        editText = (EditText) mFilterDialog.findViewById(R.id.filterSizeEditText);
        int size = -1;
        if (editText.getText().toString().trim().length() > 0) {
            size = Integer.valueOf(editText.getText().toString());
        }

        CheckBox checkBox = (CheckBox) mFilterDialog.findViewById(R.id.filterCompleteCheckBox);
        boolean showCompleted = checkBox.isChecked();

        checkBox = (CheckBox) mFilterDialog.findViewById(R.id.filterUncompleteCheckBox);
        boolean showUncompleted = checkBox.isChecked();

        mAdapter.filter(id, size, showCompleted, showUncompleted);
        mFilterDialog.dismiss();
    }

    private ArrayList<Puzzle> getPuzzlesFromDB() {
        ArrayList<Puzzle> puzzleArray = new ArrayList<>();
        try {
            SQLiteDatabase db = mDbHelper.getReadableDatabase();
            String[] projection = {
                    PuzzleDBContract.PuzzleEntry.COLUMN_NAME_ID,
                    PuzzleDBContract.PuzzleEntry.COLUMN_NAME_PICTURE_SET_ID,
                    PuzzleDBContract.PuzzleEntry.COLUMN_NAME_ROWS,
                    PuzzleDBContract.PuzzleEntry.COLUMN_NAME_LAYOUT_ARRAY,
                    PuzzleDBContract.PuzzleEntry.COLUMN_NAME_HIGH_SCORE
            };
            Cursor c = db.query(
                    PuzzleDBContract.PuzzleEntry.TABLE_NAME,
                    projection,
                    null,
                    null,
                    null, null, null
            );
            c.moveToFirst();
            do {
                int id = c.getInt(c.getColumnIndexOrThrow(PuzzleDBContract.PuzzleEntry.COLUMN_NAME_ID));
                String picsetid = c.getString(c.getColumnIndexOrThrow(PuzzleDBContract.PuzzleEntry.COLUMN_NAME_PICTURE_SET_ID));
                int rows = c.getInt(c.getColumnIndexOrThrow(PuzzleDBContract.PuzzleEntry.COLUMN_NAME_ROWS));
                String layout = c.getString(c.getColumnIndexOrThrow(PuzzleDBContract.PuzzleEntry.COLUMN_NAME_LAYOUT_ARRAY));
                int highscore = c.getInt(c.getColumnIndexOrThrow(PuzzleDBContract.PuzzleEntry.COLUMN_NAME_HIGH_SCORE));

                // find picset array from picsetid of this puzzle
                String[] picProjection = {
                        PuzzleDBContract.PictureSetEntry.COLUMN_NAME_PICTURES_ARRAY,
                };
                String picSelection = PuzzleDBContract.PictureSetEntry._ID + " = ?";
                String[] picSelectionArgs = {picsetid};
                Cursor picC = db.query(
                        PuzzleDBContract.PictureSetEntry.TABLE_NAME,
                        picProjection,
                        picSelection,
                        picSelectionArgs,
                        null, null, null
                );
                picC.moveToFirst();
                String picset = picC.getString(picC.getColumnIndexOrThrow(PuzzleDBContract.PictureSetEntry.COLUMN_NAME_PICTURES_ARRAY));
                picC.close();

                // create the puzzle object using the data retrieved and add it to the array
                puzzleArray.add(new Puzzle(id, picset, rows, layout, highscore));
            } while (c.moveToNext());
            c.close();
            db.close();
        } catch (Exception e) {
            // cursor c out of bounds because of empty db
            e.printStackTrace();
        }
        return puzzleArray;
    }

    public static Puzzle getPuzzleByIdFromDB(Context context, int puzzleId) {
        Puzzle puzzle = null;
        try {
            PuzzleDBHelper dbHelper = new PuzzleDBHelper(context);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String[] projection = {
                    PuzzleDBContract.PuzzleEntry.COLUMN_NAME_PICTURE_SET_ID,
                    PuzzleDBContract.PuzzleEntry.COLUMN_NAME_ROWS,
                    PuzzleDBContract.PuzzleEntry.COLUMN_NAME_LAYOUT_ARRAY,
                    PuzzleDBContract.PuzzleEntry.COLUMN_NAME_HIGH_SCORE
            };
            String selection = PuzzleDBContract.PuzzleEntry.COLUMN_NAME_ID + " = ?";
            String[] selectionArgs = {String.valueOf(puzzleId)};
            Cursor c = db.query(
                    PuzzleDBContract.PuzzleEntry.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null, null, null
            );
            c.moveToFirst();
            String picsetid = c.getString(c.getColumnIndexOrThrow(PuzzleDBContract.PuzzleEntry.COLUMN_NAME_PICTURE_SET_ID));
            int rows = c.getInt(c.getColumnIndexOrThrow(PuzzleDBContract.PuzzleEntry.COLUMN_NAME_ROWS));
            String layout = c.getString(c.getColumnIndexOrThrow(PuzzleDBContract.PuzzleEntry.COLUMN_NAME_LAYOUT_ARRAY));
            int highscore = c.getInt(c.getColumnIndexOrThrow(PuzzleDBContract.PuzzleEntry.COLUMN_NAME_HIGH_SCORE));

            // find picset array from picsetid of this puzzle
            String[] picProjection = {
                    PuzzleDBContract.PictureSetEntry.COLUMN_NAME_PICTURES_ARRAY,
            };
            String picSelection = PuzzleDBContract.PictureSetEntry._ID + " = ?";
            String[] picSelectionArgs = {picsetid};
            Cursor picC = db.query(
                    PuzzleDBContract.PictureSetEntry.TABLE_NAME,
                    picProjection,
                    picSelection,
                    picSelectionArgs,
                    null, null, null
            );
            picC.moveToFirst();
            String picset = picC.getString(picC.getColumnIndexOrThrow(PuzzleDBContract.PictureSetEntry.COLUMN_NAME_PICTURES_ARRAY));
            picC.close();
            c.close();
            db.close();
            dbHelper.close();
            // create the puzzle object using the data retrieved
            puzzle = new Puzzle(puzzleId, picset, rows, layout, highscore);
        } catch (Exception e) {
            // cursor c out of bounds because of empty db
            e.printStackTrace();
        }
        return puzzle;
    }

    private void setupListView(ArrayList<Puzzle> pList)
    {
        mAdapter = new PuzzleListAdapter(this, pList);
        final ListView puzzleListView = (ListView) findViewById(R.id.puzzleSelectionListView);
        puzzleListView.setAdapter(mAdapter);
        puzzleListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSelectedPuzzle = (Puzzle) puzzleListView.getItemAtPosition(position);
                // enable play/download
                Button button = (Button) findViewById(R.id.puzzleSelectionButton);
                button.setEnabled(true);
            }
        });
    }

    // http://stackoverflow.com/a/4239019 29/03/2017
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private class downloadPuzzle extends AsyncTask<String, Void, Integer> {
        protected Integer doInBackground(String... args) {
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            Puzzle puzzle = mSelectedPuzzle;
            try {
                ContentValues values = new ContentValues();
                String picsArray = puzzle.Picset();
                // add picture set to the database (duplicates will not be added because of UNIQUE array)
                values.clear();
                values.put(PuzzleDBContract.PictureSetEntry.COLUMN_NAME_PICTURES_ARRAY, picsArray);
                long picsetId;
                try {
                    picsetId = db.insertOrThrow(PuzzleDBContract.PictureSetEntry.TABLE_NAME, null, values);
                } catch (SQLException e) {
                    // if the insert failed then the id is already in the db
                    // get the id of the duplicate picset from the copy in the database
                    String[] projection = {PuzzleDBContract.PictureSetEntry._ID};
                    String selection = PuzzleDBContract.PictureSetEntry.COLUMN_NAME_PICTURES_ARRAY + " = ?";
                    String[] selectionArgs = {picsArray};
                    // query db to find picset with same pics array
                    Cursor c = db.query(
                            PuzzleDBContract.PictureSetEntry.TABLE_NAME,
                            projection,
                            selection,
                            selectionArgs,
                            null, null, null
                    );
                    c.moveToFirst();
                    picsetId = c.getInt(c.getColumnIndexOrThrow(PuzzleDBContract.PictureSetEntry._ID));
                    c.close();
                }
                // add puzzle to the database (duplicates will not be added because of UNIQUE id)
                values.clear();
                values.put(PuzzleDBContract.PuzzleEntry.COLUMN_NAME_ID, puzzle.ID());
                values.put(PuzzleDBContract.PuzzleEntry.COLUMN_NAME_PICTURE_SET_ID, String.valueOf(picsetId));
                values.put(PuzzleDBContract.PuzzleEntry.COLUMN_NAME_ROWS, puzzle.Rows());
                values.put(PuzzleDBContract.PuzzleEntry.COLUMN_NAME_LAYOUT_ARRAY, puzzle.Layout());
                values.put(PuzzleDBContract.PuzzleEntry.COLUMN_NAME_HIGH_SCORE, 0);
                db.insert(PuzzleDBContract.PuzzleEntry.TABLE_NAME, null, values);

                // download the images in the picture set
                // find picset array from picsetid of this puzzle
                String[] picProjection = {
                        PuzzleDBContract.PictureSetEntry.COLUMN_NAME_PICTURES_ARRAY,
                };
                String picSelection = PuzzleDBContract.PictureSetEntry._ID + " = ?";
                String[] picSelectionArgs = {String.valueOf(picsetId)};
                Cursor picC = db.query(
                        PuzzleDBContract.PictureSetEntry.TABLE_NAME,
                        picProjection,
                        picSelection,
                        picSelectionArgs,
                        null, null, null
                );
                picC.moveToFirst();
                String picset = picC.getString(picC.getColumnIndexOrThrow(PuzzleDBContract.PictureSetEntry.COLUMN_NAME_PICTURES_ARRAY));
                picC.close();
                db.close();

                // remove beginning [" and trailing "] to split on ","
                picset = picset.replaceAll("\\[\"|\"\\]", "");
                String[] picsetImages = picset.split("\",\"");
                for (String s : picsetImages) {
                    // check if image is in the internal storage
                    File file = new File(s);
                    if (!file.exists()) {
                        //try {
                        // if file was not found try to download it and store it for next time
                        Bitmap bitmap = BitmapFactory.decodeStream((InputStream) new URL(args[0] + s).getContent());
                        FileOutputStream writer = null;
                        try {
                            writer = getApplicationContext().openFileOutput(s, MODE_PRIVATE);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, writer);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return -1;
                        } finally {
                            if (writer != null)
                                writer.close();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
            return puzzle.ID();
        }

        protected void onPostExecute(Integer result) { // result is puzzle id, -1 if unsuccessful download
            if (result != -1) {
                mAdapter.remove(result);
                Toast.makeText(getApplicationContext(),
                        getResources().getString(
                                R.string.DownloadPuzzleSuccessfulToast), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        getResources().getString(
                                R.string.DownloadPuzzleUnsuccessfulToast), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class downloadJSON extends AsyncTask<String, String, ArrayList<Puzzle>> {
        protected ArrayList<Puzzle> doInBackground(String... args) {
            ArrayList<Puzzle> puzzleArrayResult = new ArrayList<>();
            String result = "";
            try {
                InputStream stream = (InputStream) new URL(args[0]).getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                String line = "";
                while (line != null) {
                    result += line;
                    line = reader.readLine();
                }
                reader.close();
                stream.close();
                JSONObject json = new JSONObject(result);
                JSONArray puzzles = json.getJSONArray("PuzzleIndex");
                // get puzzles files
                for (int i = 0; i < puzzles.length(); i++) {
                    stream = (InputStream) new URL(args[1] + puzzles.get(i)).getContent();
                    reader = new BufferedReader(new InputStreamReader(stream));
                    String puzzleLine = "";
                    String puzzleResult = "";
                    while (puzzleLine != null) {
                        puzzleResult += puzzleLine;
                        puzzleLine = reader.readLine();
                    }
                    JSONObject puzzleJson = new JSONObject(puzzleResult).getJSONObject("Puzzle");
                    // get picture set file of puzzle i
                    InputStream picsetStream = (InputStream) new URL(args[2] +
                            puzzleJson.getString("PictureSet")).getContent();
                    BufferedReader picsetReader = new BufferedReader(new InputStreamReader(picsetStream));
                    String picsetLine = "";
                    String picsetResult = "";
                    while (picsetLine != null) {
                        picsetResult += picsetLine;
                        picsetLine = picsetReader.readLine();
                    }
                    JSONObject picsetJson = new JSONObject(picsetResult);
                    String picsArray = picsetJson.getString("PictureFiles");
                    int id = puzzleJson.getInt("Id");
                    int rows = puzzleJson.getInt("Rows");
                    String layout = puzzleJson.getString("Layout");
                    puzzleArrayResult.add(new Puzzle(id, picsArray, rows, layout, 0));
                }
                reader.close();
                stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return puzzleArrayResult;
        }

        protected void onPostExecute(ArrayList<Puzzle> puzzleArray) {
            if (mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            // populate the listview with all NEW puzzles retrieved from server
            ArrayList<Puzzle> dbPuzzles = getPuzzlesFromDB();
            puzzleArray.removeAll(dbPuzzles);
            setupListView(puzzleArray);
        }
    }
}
