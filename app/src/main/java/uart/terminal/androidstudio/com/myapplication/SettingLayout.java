package uart.terminal.androidstudio.com.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

public class SettingLayout extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_layout);
    }

    @Override
    public void onBackPressed() {
        setContentView(R.layout.activity_main);
        concac
        return;
    }
}
