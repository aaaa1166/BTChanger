package com.example.btchanger;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Random;
public class MainActivity extends AppCompatActivity {
    TextView tvCurrentMac, tvLog;
    EditText etNewMac;
    Button btnApply, btnRandom;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvCurrentMac = findViewById(R.id.tvCurrentMac);
        tvLog = findViewById(R.id.tvLog);
        etNewMac = findViewById(R.id.etNewMac);
        btnApply = findViewById(R.id.btnApply);
        btnRandom = findViewById(R.id.btnRandom);
        showCurrentMac();
        btnRandom.setOnClickListener(v -> etNewMac.setText(randomMac()));
        btnApply.setOnClickListener(v -> {
            String newMac = etNewMac.getText().toString().trim().toUpperCase();
            if (!isValidMac(newMac)) { showLog("❌ כתובת MAC לא תקינה!", false); return; }
            applyMacAddress(newMac);
        });
    }
    private void showCurrentMac() {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) { String mac = adapter.getAddress(); tvCurrentMac.setText(mac != null ? mac : "לא זמין"); }
            else tvCurrentMac.setText("Bluetooth לא נתמך");
        } catch (Exception e) { tvCurrentMac.setText("לא ניתן לקרוא"); }
    }
    private boolean isValidMac(String mac) { return mac.matches("([0-9A-F]{2}:){5}[0-9A-F]{2}"); }
    private String randomMac() {
        Random rand = new Random(); StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) { if (i > 0) sb.append(":"); sb.append(String.format("%02X", rand.nextInt(256))); }
        return sb.toString();
    }
    private void applyMacAddress(String newMac) {
        showLog("⏳ מחיל שינוי...", true);
        new Thread(() -> {
            try {
                Process su = Runtime.getRuntime().exec("su");
                java.io.DataOutputStream os = new java.io.DataOutputStream(su.getOutputStream());
                os.writeBytes("svc bluetooth disable\n"); os.flush(); Thread.sleep(500);
                os.writeBytes("setprop persist.service.bdroid.bdaddr \"" + newMac + "\"\n"); os.flush(); Thread.sleep(500);
                os.writeBytes("setprop ro.boot.btmacaddr \"" + newMac + "\"\n"); os.flush(); Thread.sleep(500);
                os.writeBytes("svc bluetooth enable\n"); os.flush();
                os.writeBytes("exit\n"); os.flush(); su.waitFor();
                runOnUiThread(() -> { showLog("✅ הצלחה! כתובת: " + newMac, true); tvCurrentMac.setText(newMac); });
            } catch (Exception e) { runOnUiThread(() -> showLog("❌ שגיאה: " + e.getMessage(), false)); }
        }).start();
    }
    private void showLog(String msg, boolean ok) {
        tvLog.setVisibility(View.VISIBLE);
        tvLog.setTextColor(ok ? getColor(android.R.color.holo_green_light) : getColor(android.R.color.holo_red_light));
        tvLog.setText(msg);
    }
}
