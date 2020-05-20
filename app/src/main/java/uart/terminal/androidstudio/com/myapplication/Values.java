package uart.terminal.androidstudio.com.myapplication;

import com.hoho.android.usbserial.driver.UsbSerialPort;

public class Values {
    public static int baudrate = 9600;
    public static int dataBits = UsbSerialPort.DATABITS_8;
    public static int parity = UsbSerialPort.PARITY_NONE;
    public static int stopBits = UsbSerialPort.STOPBITS_1;
}
