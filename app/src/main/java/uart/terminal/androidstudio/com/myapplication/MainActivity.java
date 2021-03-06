package uart.terminal.androidstudio.com.myapplication;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;


//https://github.com/rcties/PrinterPlusCOMM
//https://github.com/mik3y/usb-serial-for-android
public class MainActivity extends AppCompatActivity  implements SerialInputOutputManager.Listener {
    private static final String ACTION_USB_PERMISSION = "com.android.recipes.USB_PERMISSION";

    TextView txtOut;
    DrawerLayout mDrawerLayout;
    NavigationView settings_drawer;
    EditText TextInput;
    Button sendData;

    private static final Pattern inputPattern = Pattern.compile("([T])([0-9]+)([A])([0-9]+)");
    private static final Pattern inputPatternHalf = Pattern.compile("([TA])([0-9]+)");

    UsbSerialPort port;

    MQTTHelper mqttHelper;
    private void startMQTT(){
        mqttHelper = new MQTTHelper(getApplicationContext());
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) { }

            @Override
            public void connectionLost(Throwable throwable) { }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws IOException {
                if (mqttMessage.toString().equals("TOGGLE1")){
                    port.write("one".getBytes(), 1000);
                } else if (mqttMessage.toString().equals("TOGGLE2")){
                    port.write("two".getBytes(), 1000);
                }
                txtOut.append("\n" + mqttMessage.toString());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) { }

        });

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);

        sendData = findViewById(R.id.send_data);
        TextInput = findViewById(R.id.TextInput);
        txtOut = findViewById(R.id.txtOut);
        txtOut.setMovementMethod(new ScrollingMovementMethod());

        mDrawerLayout = findViewById(R.id.drawer_layout);
        settings_drawer = findViewById(R.id.settings_drawer);
        settings_drawer.setClickable(false);

        startMQTT();

        sendData.setOnClickListener((View v) -> {
            String a = "T12A65";
            System.out.println(a);
            Matcher m = inputPattern.matcher(a);
            System.out.println(m.matches());
            System.out.println(m.group(1));
            System.out.println(m.group(2));
            System.out.println(m.group(3));
            System.out.println(m.group(4));

//            m = inputPatternHalf.matcher("AMB34");
//            m.matches();
//            if(m.group(1).equals("AMB")){
//            a.append("AMB34");}
//            System.out.println(a);
//            m = inputPattern.matcher(a);
//            System.out.println(m.matches());
        });

        TextInput.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)){
                txtOut.append("\n \t" + TextInput.getText());
                try {   /* push TextInput to UART */
                    port.write(TextInput.getText().toString().getBytes(), 1000);
                } catch (Exception e){
                    txtOut.append("\n Message send failed");
                }
                TextInput.getText().clear();
                return true;
            }
            return false;
        });


        ArrayAdapter<CharSequence> baudrates = ArrayAdapter.createFromResource(MainActivity.this, R.array.baudrate_array, android.R.layout.simple_spinner_item);
        Spinner spinner_baudrate = (Spinner) settings_drawer.getMenu().findItem(R.id.baudrate).getActionView();
        spinner_baudrate.setAdapter(baudrates);
        spinner_baudrate.setSelection(5);
        spinner_baudrate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (spinner_baudrate.getSelectedItemPosition() == 0) {
                    Values.baudrate = 2400;
                } else if (spinner_baudrate.getSelectedItemPosition() == 1) {
                    Values.baudrate = 9600;
                } else if (spinner_baudrate.getSelectedItemPosition() == 2) {
                    Values.baudrate = 19200;
                } else if (spinner_baudrate.getSelectedItemPosition() == 3) {
                    Values.baudrate = 38400;
                } else if (spinner_baudrate.getSelectedItemPosition() == 4) {
                    Values.baudrate = 57600;
                } else if (spinner_baudrate.getSelectedItemPosition() == 5) {
                    Values.baudrate = 115200;
                }
                try {
                    port.setParameters(Values.baudrate, Values.dataBits, Values.stopBits, Values.parity);
                    Toast.makeText(MainActivity.this, "Set baudrate to " + Values.baudrate, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    txtOut.append("\n error setting parameter.");
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ArrayAdapter<CharSequence> dataBits = ArrayAdapter.createFromResource(MainActivity.this, R.array.dataBits_array, android.R.layout.simple_spinner_item);
        Spinner spinner_dataBits = (Spinner) settings_drawer.getMenu().findItem(R.id.dataBits).getActionView();
        spinner_dataBits.setAdapter(dataBits);
        spinner_dataBits.setSelection(3);
        spinner_dataBits.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (spinner_dataBits.getSelectedItemPosition() == 0) {
                    Values.dataBits = UsbSerialPort.DATABITS_5;
                } else if (spinner_dataBits.getSelectedItemPosition() == 1) {
                    Values.dataBits = UsbSerialPort.DATABITS_6;
                } else if (spinner_dataBits.getSelectedItemPosition() == 2) {
                    Values.dataBits = UsbSerialPort.DATABITS_7;
                } else if (spinner_dataBits.getSelectedItemPosition() == 3) {
                    Values.dataBits = UsbSerialPort.DATABITS_8;
                }
                try {
                    port.setParameters(Values.baudrate, Values.dataBits, Values.stopBits, Values.parity);
                    Toast.makeText(MainActivity.this, "Set data bits to " + Values.dataBits, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    txtOut.append("\n error setting parameter.");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ArrayAdapter<CharSequence> parity = ArrayAdapter.createFromResource(MainActivity.this, R.array.parity_array, android.R.layout.simple_spinner_item);
        Spinner spinner_parity = (Spinner) settings_drawer.getMenu().findItem(R.id.parity).getActionView();
        spinner_parity.setAdapter(parity);
        spinner_parity.setSelection(0);
        spinner_parity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (spinner_parity.getSelectedItemPosition() == 1) {
                    Values.parity = UsbSerialPort.PARITY_ODD;
                } else if (spinner_parity.getSelectedItemPosition() == 0) {
                    Values.parity = UsbSerialPort.PARITY_NONE;
                } else if (spinner_parity.getSelectedItemPosition() == 2) {
                    Values.parity = UsbSerialPort.PARITY_EVEN;
                }
                try {
                    port.setParameters(Values.baudrate, Values.dataBits, Values.stopBits, Values.parity);
                    Toast.makeText(MainActivity.this, "Set parity " + Values.parity, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    txtOut.append("\n error setting parameter.");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ArrayAdapter<CharSequence> stopBits = ArrayAdapter.createFromResource(MainActivity.this, R.array.stopbits_array, android.R.layout.simple_spinner_item);
        Spinner spinner_stopBits = (Spinner) settings_drawer.getMenu().findItem(R.id.stopBits).getActionView();
        spinner_stopBits.setAdapter(stopBits);
        spinner_stopBits.setSelection(0);
        spinner_stopBits.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (spinner_stopBits.getSelectedItemPosition() == 0) {
                    Values.stopBits = UsbSerialPort.STOPBITS_1;
                } else if (spinner_stopBits.getSelectedItemPosition() == 1) {
                    Values.stopBits = UsbSerialPort.STOPBITS_2;
                } else if (spinner_stopBits.getSelectedItemPosition() == 2) {
                    Values.stopBits = UsbSerialPort.STOPBITS_1_5;
                }
                try {
                    port.setParameters(Values.baudrate, Values.dataBits, Values.stopBits, Values.parity);
                    Toast.makeText(MainActivity.this, "Set stop bits to " + Values.stopBits, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    txtOut.append("\n error setting parameter.");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        //////////////////////////******************//////////////////////////////////////////

        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);


        if (availableDrivers.isEmpty()) {
            Log.d("UART", "UART is not available");
            txtOut.setText("UART is not available");
        } else {
            Log.d("UART", "UART is available");
            txtOut.setText("UART is available");

            UsbSerialDriver driver = availableDrivers.get(0);
            UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
            if (connection == null) {
                manager.requestPermission(driver.getDevice(), PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0));
            } else {
                port = driver.getPorts().get(0);
                try {
                    port.open(connection);
                    port.setParameters(Values.baudrate, Values.dataBits, Values.stopBits, Values.parity);
                    SerialInputOutputManager usbIoManager = new SerialInputOutputManager(port, this);
                    Executors.newSingleThreadExecutor().submit(usbIoManager);
                } catch (Exception e) {
                    txtOut.setText("Error occurred");
                }

            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mDrawerLayout.openDrawer(settings_drawer);
        return true;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            port.close();
            txtOut.setText("Device disconnected");
        } catch (Exception e) {
        }
    }


    private StringBuilder uploadString = new StringBuilder();

    @Override
    public void onNewData(final byte[] data) {
        String receivedData = new String(data);
        runOnUiThread(() -> {
            txtOut.append("\n" + receivedData);
        });
        Matcher checkPatternHalf = inputPatternHalf.matcher(receivedData);
        Matcher checkPatternFull = inputPattern.matcher(uploadString);


        if (checkPatternHalf.matches() && !checkPatternFull.matches()){
                runOnUiThread(() -> {
                    txtOut.append("\n Detected half string");
                    txtOut.append("\n Group1: " + checkPatternHalf.group(1));
                    txtOut.append("\n Group2: " + checkPatternHalf.group(2));
                });
                if (
                        (uploadString.toString().equals("") && checkPatternHalf.group(1).equals("T"))
                        || (!uploadString.toString().equals("") && checkPatternHalf.group(1).equals("A"))
                ){
                    uploadString.append(receivedData);
                    runOnUiThread(() -> { txtOut.append("\n Upload string: " + uploadString); });
                }
            }
            //else if (!checkPatternHalf.matches()) runOnUiThread(() -> { txtOut.append("\n No half detection "); });
            if (checkPatternFull.matches()) {
                runOnUiThread(() -> {
                    txtOut.append("\n Detected full string: " + uploadString);
                    txtOut.append("\n Group0: " + checkPatternFull.group(1));
                    txtOut.append("\n Group1: " + checkPatternFull.group(2));
                    txtOut.append("\n Group2: " + checkPatternFull.group(3));
                    txtOut.append("\n Group3: " + checkPatternFull.group(4));
                });
                String requestURl = ("https://api.thingspeak.com/update?api_key=WA0O4CNVG5RY1SLH&field2=" + checkPatternFull.group(4)
                        + "&field1=" + checkPatternFull.group(2));
                runOnUiThread(() -> {txtOut.setText("\n" + requestURl); });
                new Thread(){
                    public void run() {
                        try {
                            Request request = new Request.Builder().url(requestURl).build();
                            new OkHttpClient().newCall(request).execute().close();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
                txtOut.setText("");
                uploadString = new StringBuilder();
            }
    }

    @Override
    public void onRunError(Exception e) {

    }
}
