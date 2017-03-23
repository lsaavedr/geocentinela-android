package cl.timining.lsaavedr.geocentinela;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

public class SettingsActivity extends FragmentActivity
{
    private static final String TAG = "SettingsActivity";

    private Queue<byte[]> cmdQueue = new LinkedList<>();
    private int cmdQueueSize = 10;

    private UsbManager manager;
    protected UsbSerialDriver device;
    private SerialInputOutputManager ioManager;
    private SerialInputOutputManager.Listener ioListener = new SerialInputOutputManager.Listener()
    {
        @Override
        public void onNewData(final byte[] data)
        {
            SettingsActivity.this.runOnUiThread(new Runnable()
            {
                @Override
                public void run() {
                    Log.v(TAG, "onNewData:"+ new String(data));
                    MainActivityGeoCentinela.updateDataIn(SettingsActivity.this, data);
                }
            });
        }

        @Override
        public void onRunError(Exception arg0)
        {
            // TODO Auto-generated method stub
        }
    };
    private ExecutorService exec;

    private TextView log;

    /*
     * 2Formetter
     */
    public static final NumberPicker.Formatter TWO_DIGIT_FORMATTER =
            new NumberPicker.Formatter()
            {
                final StringBuilder mBuilder = new StringBuilder();
                final Formatter mFmt = new Formatter(mBuilder);
                final Object[] mArgs = new Object[1];

                public String toString(int value)
                {
                    mArgs[0] = value;
                    mBuilder.delete(0, mBuilder.length());
                    mFmt.format("%02d", mArgs);
                    return mFmt.toString();
                }

                @Override
                public String format(int value)
                {
                    // TODO Auto-generated method stub
                    return this.toString(value);
                }
            };
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate");

        setContentView(R.layout.main_settings_geocentinela);

        log = (TextView) this.findViewById(R.id.log);
        log.setMovementMethod(new ScrollingMovementMethod());

        Button refresh = (Button) this.findViewById(R.id.refresh);
        refresh.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                sendCmd(new byte[]{'j', 'g', 'i'});
                sendCmd(new byte[]{'j', 'g', 'c'});
                sendCmd(new byte[]{'j', 'g', 's'});
            }
        });

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        Log.v(TAG, "onStart");

        if (manager == null) manager = (UsbManager) getSystemService(Context.USB_SERVICE);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.v(TAG, "onResume");

        stopIOSerial();
        startIOSerial();
    }

    @Override
    protected void onPause()
    {
        Log.v(TAG, "onPause");
        stopIOSerial();

        super.onPause();
    }

    @Override
    public void onStop()
    {
        Log.v(TAG, "onStop");

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();

        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        Log.v(TAG, "onDestroy");

        super.onDestroy();
    }

    private void startIOSerial()
    {
        device = UsbSerialProber.acquire(manager);
        exec = Executors.newSingleThreadExecutor();

        if (device == null) {
            if (log != null) log.setText("No serial device.");
        } else {
            try {
                device.open();
                device.setBaudRate(MainActivityGeoCentinela.baudrate);

                ioManager = new SerialInputOutputManager(device, ioListener);
                exec.submit(ioManager);

                sendCmd(new byte[]{'j', 'g', 'i'});
                sendCmd(new byte[]{'j', 'g', 'c'});
                sendCmd(new byte[]{'j', 'g', 's'});
            } catch (IOException e) {
                e.printStackTrace();
                if (log != null) log.setText("Error opening device: " + e.getMessage());

                try {
                    device.close();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                device = null;
            }
            if (log != null) log.setText("Serial device: " + device + ".\n");
        }
    }

    private void stopIOSerial()
    {
        if (ioManager != null) {
            ioManager.stop();
            ioManager = null;
        }

        if (exec !=null) {
            if (!exec.isShutdown()) {
                exec.shutdown(); // Disable new tasks from being submitted
                try {
                    // Wait a while for existing tasks to terminate
                    if (!exec.awaitTermination(5, TimeUnit.SECONDS)) {
                        exec.shutdownNow(); // Cancel currently executing tasks
                        // Wait a while for tasks to respond to being cancelled
                        if (!exec.awaitTermination(5, TimeUnit.SECONDS))
                            if (log != null) log.setText("Pool did not terminate");
                    }
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();

                    // (Re-)Cancel if current thread also interrupted
                    exec.shutdownNow();
                    // Preserve interrupt status
                    Thread.currentThread().interrupt();
                }
            }
            exec = null;
        }

        if (device != null) {
            try {
                device.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            device = null;
        }
    }

    protected void sendCmd(byte[] cmd)
    {
        Log.v(TAG, "sendCmd:"+new String(cmd));

        if (device == null) {
            Log.v(TAG, "device == null");
            if (cmd.length > 0) {
                if (cmdQueue.size() <= cmdQueueSize) {
                    cmdQueue.add(cmd);
                } else {
                    cmdQueue.poll();
                    cmdQueue.add(cmd);
                }
            }
            if (log != null) log.setText("No serial device.");
            return;
        }
        Log.v(TAG, "device != null");

        try {
            while (cmdQueue.size() > 0) {
                byte qCmd[] = cmdQueue.poll();
                device.write(qCmd, MainActivityGeoCentinela.timeout);
                Log.v(TAG, "qCmd:"+ new String(qCmd));
            }
            if (cmd.length > 0) {
                device.write(cmd, MainActivityGeoCentinela.timeout);
                Log.v(TAG, "cmd:" + new String(cmd));
            }
        } catch (IOException e) {
            Log.v(TAG, "write fail");
            // TODO Auto-generated catch block
            e.printStackTrace();
            if (log != null) log.setText("serial exception");
        }
    }

    public void appendLog(String message)
    {
        if (log != null) {
            log.append(message);

            if (log.getLayout() != null) {
                int scrollAmount = log.getLayout().getLineTop(log.getLineCount());
                scrollAmount -= log.getHeight();

                if (scrollAmount < 0) scrollAmount = 0;
                log.scrollTo(0, scrollAmount);
            }
        }
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction()
    {
        Thing object = new Thing.Builder()
                .setName("Settings Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }
}
