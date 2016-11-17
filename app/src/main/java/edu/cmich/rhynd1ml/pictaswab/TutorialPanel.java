package edu.cmich.rhynd1ml.pictaswab;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;

/**
 * Created by mrhyndress on 11/14/16.
 */

public class TutorialPanel extends AppCompatActivity {

    private static int panelWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tutorial_panel);
        initUI();
    }

    /**
     * Initialize User Interface
     */
    private void initUI() {
        setPanelSize();
        setTutorialCompleteBtn();
    }

    /**
     * Set the tutorial completion button listener event
     */
    private void setTutorialCompleteBtn() {
        Button completeBtn = (Button) findViewById(R.id.tutorialComplete);
        completeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = getApplicationContext().getSharedPreferences(getApplicationContext().getString(R.string.app_name), MODE_PRIVATE).edit();
                editor.putBoolean("Tutorial_Complete", true);
                editor.commit();
                finish();
            }
        });
    }

    /**
     * Set the tutorial panel size
     */
    private void setPanelSize() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        panelWidth = (int) (dm.widthPixels*.6);
        getWindow().setLayout(panelWidth, panelWidth);
    }
}
