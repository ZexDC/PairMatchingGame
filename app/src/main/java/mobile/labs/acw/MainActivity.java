package mobile.labs.acw;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    public static SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create global shared preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Button button = (Button) findViewById(R.id.continueGameButton);
        if (MainActivity.prefs.getInt(SharedPrefs.PUZZLE_ID, -1) == -1) {
            // no previous game, disable the continue button
            button.setEnabled(false);
        } else {
            // activate continue button
            button.setEnabled(true);
        }
    }

    public void localPuzzlesOnClick(View pView) {
        Intent intent = new Intent(this, PuzzleSelectionActivity.class);
        intent.putExtra("SelectionType", "Local");
        startActivity(intent);
    }

    public void remotePuzzlesOnClick(View pView) {
        Intent intent = new Intent(this, PuzzleSelectionActivity.class);
        intent.putExtra("SelectionType", "Remote");
        startActivity(intent);
    }

    public void continueGameButtonOnClick(View pView) {
        Intent intent = new Intent(this, PuzzlePlayActivity.class);
        intent.putExtra("IsNewGame", false);
        int puzzleId = MainActivity.prefs.getInt(SharedPrefs.PUZZLE_ID, -1);
        intent.putExtra("PuzzleId", puzzleId);
        startActivity(intent);
    }
}
