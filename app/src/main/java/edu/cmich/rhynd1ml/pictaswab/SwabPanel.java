package edu.cmich.rhynd1ml.pictaswab;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;

/**
 * Created by mrhyndress on 11/14/16.
 */

public class SwabPanel extends AppCompatActivity {

    private static HashMap<String, String> colorInfo;
    private static int panelWidth;
    private static int panelHeight;
    private static ColorRecognize rnController = new ColorRecognize();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.swab_panel);

        initUI();
    }

    /**
     * initialize the User Interface
     */
    private void initUI() {
        setPanelSize();
        getColorInfo();
        setColorPreview();
        setCancelBtn();
        setStringValue();
        setHEXValue();
        setRGBValue();
        setSelectedArea();
    }

    /**
     * set the HEX text
     */
    private void setHEXValue() {
        TextView hexText = (TextView) findViewById(R.id.values_hex);
        hexText.setText(colorInfo.get("HEX"));
    }

    /**
     * set the color text using the ColorRecognize controller
     */
    private void setStringValue() {
        TextView hexText = (TextView) findViewById(R.id.values_string);
        hexText.setText(rnController.getColorNameFromRgb(
                Integer.parseInt(colorInfo.get("R")),
                Integer.parseInt(colorInfo.get("G")),
                Integer.parseInt(colorInfo.get("B"))
        ));
    }

    /**
     * set the RGB text
     */
    private void setRGBValue() {
        TextView hexText = (TextView) findViewById(R.id.values_rgb);
        String rgb = "(" + colorInfo.get("R") + "," + colorInfo.get("G") + "," + colorInfo.get("B") + ")";
        hexText.setText(rgb);
    }

    /**
     * set the image view with the base64 decoded image
     */
    private void setSelectedArea() {
        ImageView selectedArea = (ImageView) findViewById(R.id.touchedSection);
        byte[] decodedString = Base64.decode(colorInfo.get("AREA"), Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0,decodedString.length);
        selectedArea.setImageBitmap(decodedByte);
    }

    /**
     * Set the swabPanel size
     */
    private void setPanelSize() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        panelWidth = (int) (dm.widthPixels*.8);
        panelHeight = (int) (dm.heightPixels*.6);
        getWindow().setLayout(panelWidth, panelHeight);
    }

    /**
     * get the color info from the extras bundle passed via intent
     */
    private void getColorInfo() {
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getSerializable("COLOR_INFO") instanceof HashMap) {
            colorInfo = (HashMap<String, String>) extras.getSerializable("COLOR_INFO");
        }
    }

    /**
     * set the cancel button listener event
     */
    private void setCancelBtn() {
        Button cancelBtn = (Button) findViewById(R.id.cancelBtn);

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /**
     * Set the top border of the panel to the DECIMAL color
     */
    private void setColorPreview() {
        SurfaceView colorPreview = (SurfaceView) findViewById(R.id.colorPreview);
        colorPreview.setMinimumWidth(panelWidth);
        colorPreview.setBackgroundColor(Integer.parseInt(colorInfo.get("DECIMAL")));
    }
}
