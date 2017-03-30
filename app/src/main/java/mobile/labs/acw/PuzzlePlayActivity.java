package mobile.labs.acw;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class PuzzlePlayActivity extends AppCompatActivity {

    public int mState;
    public boolean mIsConfigChanged;
    public String CONFIG_CHANGED;
    public int mTotPairs;
    public int mCorrectPairs = 0;
    public int mAttempts = 0;
    public Tile mSelectedTile;
    public Tile mTurnedTile;
    public TextView mTime;
    public int w, h;
    public PuzzleAdapter mAdapter;
    public Chronometer mStopWatch;
    public long mCountUp;
    public int mHighScore;
    public int mPuzzleId;
    public Dialog mDialog;

    final int STATE_START_BUTTON = 0;
    final int STATE_IN_PROGRESS = 1;
    final int STATE_GAMEOVER = 2;
    final String TILE_STATE_ENABLED = "enabled";
    final String TILE_STATE_DISABLED = "disabled";

    final ArrayList<Tile> mCardList = new ArrayList<>();

    private class PuzzleAdapter extends BaseAdapter {
        Context mContext;
        ArrayList<Tile> mCardList;
        int mTileSize;

        PuzzleAdapter(Context context, ArrayList<Tile> cardList, int tileSize) {
            mContext = context;
            mCardList = cardList;
            mTileSize = tileSize;
        }

        @Override
        public int getCount() {
            return mCardList.size();
        }

        @Override
        public Object getItem(int position) {
            return mCardList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            // Return true for clickable, false for not
            return mCardList.get(position).isEnabled();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            // create a new ImageView for each item referenced by the Adapter
            final ImageView imageView;
            Tile card = mCardList.get(position);
            if (convertView == null) {
                // if it's not recycled, initialize some attributes
                imageView = new ImageView(mContext);
                imageView.setLayoutParams(new GridView.LayoutParams(mTileSize, mTileSize));
                imageView.setPadding(10, 10, 10, 10);
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            } else {
                imageView = (ImageView) convertView;
            }
            imageView.setImageBitmap(card.Image());
            return imageView;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_puzzle_play);
        mIsConfigChanged = false;

        Puzzle puzzle;
        if (getIntent().getBooleanExtra("IsNewGame", true)) {
            // Check whether we're recreating a previously destroyed instance
            if (savedInstanceState != null) {
                // Restore value of members (to detect re-orientation) from saved state
                mIsConfigChanged = savedInstanceState.getBoolean(CONFIG_CHANGED);
            }
            if (!mIsConfigChanged) {
                // save state if it's actually a new game and not just a device re-orientation
                MainActivity.prefs.edit().putInt(SharedPrefs.STATE, STATE_START_BUTTON).apply();
                // reset other prefs
                MainActivity.prefs.edit().putInt(SharedPrefs.CORRECT_PAIRS, 0).apply();
                MainActivity.prefs.edit().putInt(SharedPrefs.ATTEMPTS, 0).apply();
                MainActivity.prefs.edit().putString(SharedPrefs.TIME, "00:00").apply();
                MainActivity.prefs.edit().putLong(SharedPrefs.START_TIME, 0).apply();
            }
            // get the selected puzzle object from the intent
            puzzle = (Puzzle) getIntent().getSerializableExtra("PuzzleObject");
        } else { // arrived here from Continue button, no need to reset state
            // get the puzzle object from the puzzleID in the intent
            int id = getIntent().getIntExtra("PuzzleId", -1);
            puzzle = PuzzleSelectionActivity.getPuzzleByIdFromDB(this, id);
        }

        int rows = puzzle.Rows();
        String picset = puzzle.Picset();
        String layout = puzzle.Layout();
        // remove beginning [" and trailing "] to split on ","
        picset = picset.replaceAll("\\[\"|\"\\]", "");
        String[] picsetImages = picset.split("\",\"");
        // remove beginning [ and trailing ] to split on ,
        layout = layout.replaceAll("\\[|\\]", "");
        String[] layoutArray = layout.split(",");
        int columns = layoutArray.length / rows;
        mTotPairs = (rows * columns) / 2;
        mHighScore = puzzle.Highscore();
        mPuzzleId = puzzle.ID();

        int layoutElement = 0;
        try {
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < columns; j++) {
                    // get the image from the internal storage
                    int imageIndex = Integer.valueOf(layoutArray[layoutElement]) - 1; // -1 because of 1 based layout
                    String imageFile = picsetImages[imageIndex];
                    Bitmap image = BitmapFactory.decodeFile(new File(getFilesDir(), imageFile).getAbsoluteFile().toString());
                    mCardList.add(new Tile(getApplicationContext(), i, j, image, imageFile));
                    layoutElement++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // set variables from prefs and initialise UI text
        TextView textView = (TextView) findViewById(R.id.pairsTextView);
        mCorrectPairs = MainActivity.prefs.getInt(SharedPrefs.CORRECT_PAIRS, 0);
        String text = getResources().getString(R.string.PairsTextView) + " " +
                String.valueOf(mCorrectPairs) + "/" + String.valueOf(mTotPairs);
        textView.setText(text);
        textView = (TextView) findViewById(R.id.attemptsTextView);
        mAttempts = MainActivity.prefs.getInt(SharedPrefs.ATTEMPTS, 0);
        text = getResources().getString(R.string.AttemptsTextView) + " " +
                String.valueOf(mAttempts);
        textView.setText(text);
        mTime = (TextView) findViewById(R.id.timeTextView);
        mTime.setText(MainActivity.prefs.getString(SharedPrefs.TIME, "00:00"));
        mCountUp = MainActivity.prefs.getLong(SharedPrefs.START_TIME, 0);

        final GridView gridView = (GridView) findViewById(R.id.puzzleGridView);

        final Display display = getWindowManager().getDefaultDisplay();
        Point p = new Point();
        display.getSize(p);
        int tileSize;
        int spacing = 30;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (rows >= columns) {
                tileSize = p.x / rows - spacing;
            } else {
                tileSize = p.x / columns - spacing;
            }
        } else {
            if (rows >= columns) {
                int incrementedOffset = rows;
                if (rows == 4) // dividing the height by 4 would hide part of last row and make the grid scrollable
                    incrementedOffset += 1;
                tileSize = p.y / incrementedOffset - spacing - 10;
            } else {
                tileSize = p.y / columns - spacing - 10;
            }
        }

        gridView.setNumColumns(columns);
        mAdapter = new PuzzleAdapter(this, mCardList, tileSize);
        gridView.setAdapter(mAdapter);

        mState = MainActivity.prefs.getInt(SharedPrefs.STATE, STATE_START_BUTTON);
        switch (mState) {
            case STATE_START_BUTTON:  // start game from beginning as usual
                break;
            case STATE_IN_PROGRESS: // resume game from saved state
                restoreInProgressState();
                break;
            case STATE_GAMEOVER: // show the ended game grid with the game over progressDialog
                restoreInProgressState();
                showGameOverDialog();
                break;
            default: // start game from beginning as usual
                break;
        }

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSelectedTile = (Tile) gridView.getItemAtPosition(position);
                mSelectedTile.gridPosition = position;
                if (mTurnedTile == null) {
                    // set this card as the current turned card
                    mTurnedTile = mSelectedTile;
                    mTurnedTile.setSelected(true);
                } else {
                    if (mSelectedTile.gridPosition != mTurnedTile.gridPosition) { // check that it's not the same card
                        // show cards by disabling them
                        mSelectedTile.setEnabled(false);
                        mTurnedTile.setEnabled(false);
                        if (mSelectedTile.ImageName().equals(mTurnedTile.ImageName())) { // valid pair
                            // update game data
                            mCorrectPairs++;
                        } else { // wrong pair
                            mAdapter.notifyDataSetChanged();
                            gridView.setEnabled(false);

                            // temporarily lock the orientation to avoid tiles bug while handler's task is running
                            // structure from http://stackoverflow.com/a/10453034 28/03/2017
                            final int rotation = ((WindowManager) PuzzlePlayActivity.this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getOrientation();
                            switch (rotation) {
                                case Surface.ROTATION_0: // portrait
                                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                                    break;
                                case Surface.ROTATION_90: // landscape
                                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                                    break;
                                case Surface.ROTATION_180: // reverse portrait
                                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                                    break;
                                case Surface.ROTATION_270: // reverse landscape
                                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                                    break;
                                default:
                                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                                    break;
                            }
                            // use a handler to delay custom runnable which covers the cards after 1 sec
                            final Handler handler = new Handler();
                            CoverCardsRunnable runnable = new CoverCardsRunnable(mSelectedTile, mTurnedTile, gridView);
                            handler.postDelayed(runnable, 1000);
                        }
                        // count as attempt only if different cards have been clicked
                        mAttempts++;
                        // update UI text
                        TextView textView = (TextView) findViewById(R.id.pairsTextView);
                        String text = getResources().getString(R.string.PairsTextView) + " " +
                                String.valueOf(mCorrectPairs) + "/" + String.valueOf(mTotPairs);
                        textView.setText(text);
                        textView = (TextView) findViewById(R.id.attemptsTextView);
                        text = getResources().getString(R.string.AttemptsTextView) + " " +
                                String.valueOf(mAttempts);
                        textView.setText(text);
                        // time is updated by ChronoTickListener

                        // check for game over
                        if (mCorrectPairs == mTotPairs) {
                            showGameOverDialog();
                        }
                    }
                    else { // deselect turned card
                        mTurnedTile.setSelected(false);
                    }
                    mTurnedTile = null;
                }
                // update image views
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        // avoid crash on re-orientation
        if(mDialog != null) {
            mDialog.dismiss();
        }

        // pause time in case the activity gets resumed later
        if (mState == STATE_IN_PROGRESS) {
            stopCountUpTimer();
        }

        SharedPreferences.Editor editor = MainActivity.prefs.edit();
        editor.putInt(SharedPrefs.STATE, mState);
        editor.putString(SharedPrefs.TIME, mTime.getText().toString());
        editor.putInt(SharedPrefs.CORRECT_PAIRS, mCorrectPairs);
        editor.putInt(SharedPrefs.ATTEMPTS, mAttempts);
        editor.putString(SharedPrefs.TIME, mTime.getText().toString());
        ArrayList<String> tilesState = new ArrayList<>();
        for (Tile t : mCardList) {
            if (t.isEnabled()) {
                tilesState.add(TILE_STATE_ENABLED);
            } else {
                tilesState.add(TILE_STATE_DISABLED);
            }
        }
        editor.putString(SharedPrefs.TILES_STATE, TextUtils.join(",", tilesState));
        if (mTurnedTile != null) {
            String turnedTileCoords = mTurnedTile.X() + "," + mTurnedTile.Y();
            editor.putString(SharedPrefs.TURNED_TILE_COORDS, turnedTileCoords);
        }
        else {
            editor.remove(SharedPrefs.TURNED_TILE_COORDS);
        }
        editor.putLong(SharedPrefs.START_TIME, mCountUp);
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // start timer again if in middle of a game
        if (mState == STATE_IN_PROGRESS) {
            startCountUpTimer();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(CONFIG_CHANGED, true);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // structure from http://stackoverflow.com/a/9123324 26/03/2017
    private class CoverCardsRunnable implements Runnable {
        private Tile t1, t2;
        private GridView grid;

        private CoverCardsRunnable(Tile pT1, Tile pT2, GridView pGrid) {
            t1 = pT1;
            t2 = pT2;
            grid = pGrid;
        }

        @Override
        public void run() {
            // cover cards
            t1.setEnabled(true);
            t2.setEnabled(true);
            mAdapter.notifyDataSetChanged();
            // re-enable touch on grid
            grid.setEnabled(true);
            // set orientation to default now that the task has finished
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    private void restoreInProgressState() {
        // show the puzzle and hide button
        final GridView gridView = (GridView) findViewById(R.id.puzzleGridView);
        gridView.setVisibility(View.VISIBLE);
        final Button button = (Button) findViewById(R.id.startGameButton);
        button.setVisibility(View.INVISIBLE);
        // recover time and start it again
        startCountUpTimer();
        // recover the tiles state from sharedpreferences
        String tilesStateRaw = MainActivity.prefs.getString(SharedPrefs.TILES_STATE, null);
        String[] tilesState = tilesStateRaw.split(",");
        boolean hasToCheckForTurnedTile = true;
        String sharedPrefTurnedTile = MainActivity.prefs.getString(SharedPrefs.TURNED_TILE_COORDS, "-1");
        if (sharedPrefTurnedTile.equals("-1")) {
            hasToCheckForTurnedTile = false;
        }
        for (int i = 0; i < mCardList.size(); i++) {
            String state = tilesState[i];
            Tile t = mCardList.get(i);
            if (state.equals(TILE_STATE_ENABLED)) {
                t.setEnabled(true);
                if (hasToCheckForTurnedTile) {
                    // check if it's a currently turned card - i.e. game waiting for second card turn
                    String[] s = sharedPrefTurnedTile.split(",");
                    int x = Integer.valueOf(s[0]);
                    int y = Integer.valueOf(s[1]);
                    if (t.X() == x && t.Y() == y) {
                        mTurnedTile = t;
                        mTurnedTile.setSelected(true);
                    }
                }
            } else {
                t.setEnabled(false);
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    private void showGameOverDialog() {
        // save state
        mState = STATE_GAMEOVER;
        stopCountUpTimer();
        int score = (int) getScore();
        String dialogText;
        if(score > mHighScore) {
            // update highscore in the database
            PuzzleDBHelper dbHelper = new PuzzleDBHelper(PuzzlePlayActivity.this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            try {
                ContentValues values = new ContentValues();
                values.put(PuzzleDBContract.PuzzleEntry.COLUMN_NAME_HIGH_SCORE, score);
                String selection = PuzzleDBContract.PuzzleEntry.COLUMN_NAME_ID + " = ?";
                String[] selectionArgs = {String.valueOf(mPuzzleId)};
                db.update(PuzzleDBContract.PuzzleEntry.TABLE_NAME, values, selection, selectionArgs);
                db.close();
                dbHelper.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // set new progressDialog text
            dialogText = getResources().getString(R.string.GameoverHighScoreTextView);
        }
        else {
            dialogText = getResources().getString(R.string.GameoverTextView);
        }
        mDialog = new Dialog(PuzzlePlayActivity.this);
        mDialog.setContentView(R.layout.gameover_dialog);
        try {
            mDialog.setCancelable(false);
            mDialog.setTitle(R.string.gameoverDialogTitle);
            mDialog.show();
            TextView gameoverTextView = (TextView) mDialog.findViewById(R.id.gameoverTextView);
            gameoverTextView.setText(dialogText + " " + String.valueOf(score));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onStartGameButtonClick(View pView) {

        // show the puzzle and hide button
        final GridView gridView = (GridView) findViewById(R.id.puzzleGridView);
        gridView.setVisibility(View.VISIBLE);
        final Button button = (Button) findViewById(R.id.startGameButton);
        button.setVisibility(View.INVISIBLE);

        // show all the cards by disabling them
        for (Tile t : mCardList) {
            t.setEnabled(false);
        }
        mAdapter.notifyDataSetChanged();

        long timeOut = mTotPairs / 2 * 1000; // give half the total pairs seconds time to memorise the grid

        // start countdown timer to show the cards
        // structure from https://developer.android.com/reference/android/os/CountDownTimer.html 26/03/2017
        new CountDownTimer(timeOut, 1000) {

            public void onTick(long millisUntilFinished) {
                mTime.setText(getResources().getString(R.string.CountDownTimeTextView) + " " +
                        millisUntilFinished / 1000 + "s");
            }

            public void onFinish() {
                // cover all the cards by enabling them
                for (Tile t : mCardList) {
                    t.setEnabled(true);
                }
                mAdapter.notifyDataSetChanged();

                // start countup timer of the actual game
                startCountUpTimer();
                // save state
                mState = STATE_IN_PROGRESS;
            }

        }.start();
    }

    private void startCountUpTimer() {
        // structure from http://stackoverflow.com/a/2537264 26/03/2017
        long startTimePrefs = MainActivity.prefs.getLong(SharedPrefs.START_TIME, -1) * 1000;
        final long startTime;
        if (startTimePrefs == -1) { // no saved elapsed time
            // set start time as current device total power-on time
            startTime = SystemClock.elapsedRealtime();
        } else { // retrieve elapsed time from sharedprefs
            startTime = SystemClock.elapsedRealtime() - startTimePrefs;
        }
        mStopWatch = (Chronometer) findViewById(R.id.chrono);
        mStopWatch.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer arg0) {
                mCountUp = (SystemClock.elapsedRealtime() - startTime) / 1000;
                String asText = String.format(Locale.UK, "%02d", (mCountUp / 60)) + ":" +
                        String.format(Locale.UK, "%02d", (mCountUp % 60));
                mTime.setText(asText);
            }
        });
        mStopWatch.start();
    }

    private void stopCountUpTimer() {
        if (mStopWatch != null)
            mStopWatch.stop();
    }

    public void onGameoverButtonClick(View pView) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private float getScore() {
        // score calculated with correct pairs, attempts and time
        String[] time = mTime.getText().toString().split(":");
        int min = Integer.valueOf(time[0]);
        int sec = Integer.valueOf(time[1]);
        float timePenalty = (1.0f / ((min + 59.0f) * min + sec));

        return ((mCorrectPairs * 1000) - (mAttempts / mCorrectPairs * 100)) * timePenalty;
    }
}