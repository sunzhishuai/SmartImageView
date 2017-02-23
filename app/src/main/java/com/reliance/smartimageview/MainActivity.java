package com.reliance.smartimageview;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity implements View.OnClickListener{

    private SmartImageView mSmartView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSmartView = (SmartImageView) findViewById(R.id.siv);
        mSmartView.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        mSmartView.delete();
    }
}
