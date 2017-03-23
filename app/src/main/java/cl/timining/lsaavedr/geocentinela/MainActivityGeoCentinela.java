package cl.timining.lsaavedr.geocentinela;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.Integer.toHexString;
import static java.util.Arrays.copyOfRange;

public class MainActivityGeoCentinela extends Activity
{
    private static final String TAG = "ActivityGeoCentinela";

    private static int log_cmd = 0;
    private static int headstep = 0;
    private static byte[] head = new byte[]{
            (byte) 0xaa, (byte) 0xaa, (byte) 0xaa,
            (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0x00, (byte) 0x00, (byte) 0x00};

    private static final int LOG_IN = 1;
    private static final int DATA_IN = 3;
    private static final int DATA_END = 4;
    private static final int JSON_IN = 7;

    private Queue<byte[]> cmdQueue = new LinkedList<>();
    private int cmdQueueSize = 10;

    private UsbManager manager;
    private UsbSerialDriver device;
    private SerialInputOutputManager ioManager;
    private SerialInputOutputManager.Listener ioListener = new SerialInputOutputManager.Listener()
    {
        @Override
        public void onNewData(final byte[] data)
        {
            MainActivityGeoCentinela.this.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Log.v(TAG, "onNewData:"+ new String(data));
                    MainActivityGeoCentinela.updateDataIn(MainActivityGeoCentinela.this, data);
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

    public DBHelper dbHelper;

    static public final File dir = new File(Environment.getExternalStorageDirectory(),
            "GeoCentinela");

    private String filename;
    private int filesize = 0;
    private int dataCount = 0;
    private FileOutputStream outputStream;
    public ProgressBar fileprogress;

    static public int baudrate = 115200;
    static public int timeout = 1000;

    private int instrument_id = 1;

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

        setContentView(R.layout.main_activity_geocentinela);

        dbHelper = new DBHelper(this);
        if (!dir.exists()) dir.mkdirs();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private TextView idname;
    TextView ttemp;
    TextView tvbat;
    TextView ttime;

    @Override
    protected void onStart()
    {
        super.onStart();
        Log.v(TAG, "onStart");

        idname = (TextView) this.findViewById(R.id.idname);
/*
        idname.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
                alert.setTitle(R.string.idname);

                final NumberPicker np = new NumberPicker(v.getContext());
                final String[] values = new String[]{"1", "2", "4", "8", "16", "32", "64", "128"};
                np.setMinValue(0);
                np.setMaxValue(values.length-1);
                np.setDisplayedValues(values);

                try {
                    int gain_num = Integer.parseInt(idname.getText().toString().trim());
                    if (gain_num < 1 || gain_num > 128) gain_num = 0;
                    else gain_num = (int) (Math.log(gain_num)/Math.log(2));
                    np.setValue(gain_num);
                } catch (Exception e) {
                    np.setValue(0);
                }

                alert.setView(np);
                alert.create();

                alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        idname.setText(values[np.getValue()]);
                        dialog.dismiss();
                    }
                });
                alert.setNegativeButton(android.R.string.cancel, null);

                alert.show();
            }
        });
*/
        if (dbHelper != null && instrument_id > 0)
            idname.setText(dbHelper.getInstrumentName(instrument_id));

        ttemp = (TextView) this.findViewById(R.id.textTemp);
        tvbat = (TextView) this.findViewById(R.id.textVBat);
        ttime = (TextView) this.findViewById(R.id.textTime);

        Button temp = (Button) this.findViewById(R.id.temp);
        temp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCmd(new byte[]{'j', 'g', 't'});
            }
        });

        Button vbat = (Button) this.findViewById(R.id.vbat);
        vbat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCmd(new byte[]{'j', 'g', 'v'});
            }
        });

        Button time = (Button) this.findViewById(R.id.time);
        time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCmd(new byte[]{'j', 'g', 'e'});
            }
        });

        ListView listView = (ListView) findViewById(R.id.listFiles);
        if (listView.getAdapter() != null
                && listView.getAdapter().getClass() == FileCursorAdapter.class) {
            ((FileCursorAdapter)listView.getAdapter()).changeCursor(
                    dbHelper.getFilesCursor(instrument_id));

            if (listView.getAdapter() == null ||
                    listView.getAdapter().getClass() != FileCursorAdapter.class) {
                listView.setAdapter(new FileCursorAdapter(this,
                        dbHelper.getFilesCursor(instrument_id)));
            } else {

            }
        } else {
            listView.setAdapter(new FileCursorAdapter(this,
                    dbHelper.getFilesCursor(instrument_id)));
        }
        ((FileCursorAdapter)listView.getAdapter()).notifyDataSetChanged();

        Button ls = (Button) this.findViewById(R.id.ls);
        ls.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (instrument_id == 0) sendCmd(new byte[]{'j', 'g', 'i'});
                else sendCmd(new byte[]{'j', 'g', 'l'});
            }
        });

        log = (TextView) this.findViewById(R.id.log);
        log.setMovementMethod(new ScrollingMovementMethod());

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
    protected void onStop()
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

        if (dbHelper != null) dbHelper.close();

        super.onDestroy();
    }

    private void startIOSerial()
    {
        if (device == null)
            device = UsbSerialProber.acquire(manager);
        if (exec == null || exec.isShutdown() || exec.isTerminated())
            exec = Executors.newSingleThreadExecutor();

        if (device == null) {
            if (log != null) log.setText("No serial device.");
        } else {
            try {
                device.open();
                device.setBaudRate(MainActivityGeoCentinela.baudrate);

                ioManager = new SerialInputOutputManager(device, ioListener);
                exec.submit(ioManager);

                sendCmd(new byte[]{ 'j', 'g', 'i' });
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_activity_geocentinela, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case (R.id.action_settings):
                Intent settings = new Intent(this, SettingsActivity.class);
                startActivityForResult(settings, 0);
                return true;
            case (R.id.action_timesync):
                long timestamp_old = System.currentTimeMillis() / 1000;
                int tz_offset = TimeZone.getDefault().getRawOffset();

                long timestamp = System.currentTimeMillis() / 1000;
                while (timestamp_old == timestamp) {
                    timestamp = System.currentTimeMillis() / 1000;
                }

                sendCmd(("sr" + (timestamp + tz_offset)).getBytes(Charset.forName("UTF-8")));
            case (R.id.action_sdcheck):
                sendCmd("jgb".getBytes(Charset.forName("UTF-8")));
                return true;
        }

        return super.onOptionsItemSelected(item);
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

    static public void updateDataIn(Context context, byte[] data)
    {
        if (data.length <= 0) return;

        int ini = 0;

        if (headstep == head.length) {
            switch (data[0]) {
                case (byte) 't': {
                    log_cmd = LOG_IN;
                } break;
                case (byte) 'f': {
                    log_cmd = DATA_IN;
                } break;
                case (byte) 'g': {
                    log_cmd = DATA_END;
                    processSubData(context, new byte[] {});
                } break;
                case (byte) 'j': {
                    log_cmd = JSON_IN;

                    jsonStr = "";
                    bracketCnt = 0;
                } break;
            }

            headstep = 0;
            ini = 1;
        }

        int ini_prev = ini;
        while (ini < data.length) {
            if (headstep == 0) {
                ini = indexOf(data, ini_prev, head[0]);
                if (ini == -1) {
                    processSubData(context, copyOfRange(data, ini_prev, data.length));
                    return;
                }
            }

            while (ini < data.length && headstep < head.length && data[ini] == head[headstep]) {
                ini++;
                headstep++;

                headstep %= head.length;
            }

            if (headstep == 0) {
                if ((ini - ini_prev) > head.length) {
                    processSubData(context, copyOfRange(data, ini_prev, ini - head.length));
                }

                if (ini < data.length) {
                    switch (data[ini]) {
                        case (byte) 't': {
                            log_cmd = LOG_IN;
                        } break;
                        case (byte) 'f': {
                            log_cmd = DATA_IN;
                        } break;
                        case (byte) 'g': {
                            log_cmd = DATA_END;

                            processSubData(context, new byte[] {});
                        } break;
                        case (byte) 'j': {
                            log_cmd = JSON_IN;

                            jsonStr = "";
                            bracketCnt = 0;
                        } break;
                    }

                    ini++;
                    ini_prev = ini;
                } else {
                    headstep = head.length;
                }
            } else {
                if ((ini - ini_prev) > headstep) {
                    processSubData(context, copyOfRange(data, ini_prev, ini - headstep));
                }

                if (ini < data.length) {
                    processSubData(context, copyOfRange(head, 0, headstep));

                    ini_prev = ini;
                    headstep = 0;
                }
            }
        }
    }

    static String jsonStr = "";
    static int bracketCnt = 0;

    private static void processSubData(Context context, byte[] data)
    {
        MainActivityGeoCentinela mc = null;
        SettingsActivity sa = null;
        if (context.getClass() == MainActivityGeoCentinela.class) {
            mc = (MainActivityGeoCentinela) context;
        } else if (context.getClass() == SettingsActivity.class) {
            sa = (SettingsActivity) context;
        }

        switch (log_cmd) {
            case LOG_IN: { // log
                if (data.length == 0) break;

                String message = new String(data);

                if (mc != null) mc.appendLog(message);
                if (sa != null) sa.appendLog(message);
            } break;
            case DATA_IN: { // file incoming
                if (data.length == 0) break;

                if (mc != null && mc.outputStream != null) {
                    try {
                        mc.outputStream.write(data);
                        mc.dataCount += data.length;
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    if (mc.filesize > 0 && mc.fileprogress != null) {
                        mc.fileprogress.setVisibility(View.VISIBLE);
                        mc.fileprogress.setMax(mc.filesize);
                        mc.fileprogress.setProgress(mc.dataCount);
                    }

                    if (mc.dataCount >= mc.filesize) {
                        mc.appendLog("end:" + mc.dataCount + ":" + mc.filesize + "%\n");
                        Log.v(TAG, "end:" + mc.dataCount + ":" + mc.filesize);
                    }
                }
            } break;
            case DATA_END: // file end
                if (mc != null && mc.outputStream != null) {
                    try {
                        mc.outputStream.close();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    if (mc.dbHelper.updateStatus(mc.instrument_id, mc.filename, 1) > 0) {
                        ListView listView = (ListView) mc.findViewById(R.id.listFiles);
                        ((FileCursorAdapter) listView.getAdapter())
                                .changeCursor(mc.dbHelper.getFilesCursor(mc.instrument_id));
                        ((FileCursorAdapter) listView.getAdapter()).notifyDataSetChanged();
                    }

                    if (mc.filesize > 0 && mc.fileprogress != null)
                        mc.fileprogress.setVisibility(View.INVISIBLE);

                    mc.filename = null;
                    mc.filesize = 0;
                    mc.outputStream = null;
                    mc.dataCount = 0;
                    mc.fileprogress = null;
                } break;
            case JSON_IN: { // json
                if (data.length == 0) break;

                int data_ini = 0, data_len = 0;
                boolean jsonOpen = bracketCnt != 0;
                for (int i = 0; i < data.length; i++) {
                    if (data[i] == '{') {
                        if (!jsonOpen) {
                            data_ini = i;
                            jsonOpen = true;
                        }
                        bracketCnt++;
                    } else if (data[i] == '}') {
                        if (jsonOpen) {
                            bracketCnt--;
                            if (bracketCnt == 0) {
                                data_len++;
                                break;
                            }
                        }
                    }
                    if (jsonOpen) data_len++;
                }
                if (!jsonOpen) break;

                jsonStr += new String(data, data_ini, data_len, Charset.forName("UTF-8"));

                if (bracketCnt != 0 || jsonStr.length() == 0) break;

                JSONObject jsonObject;
                try {
                    jsonObject = new JSONObject(jsonStr);
                    jsonStr = "";
                } catch (JSONException e) {
                    e.printStackTrace();
                    break;
                }

                if (mc != null) {
                    if (jsonObject.has("instrument")) {
                        JSONObject jsonInstrument = jsonObject.optJSONObject("instrument");
                        if (jsonInstrument != null) {
                            int id = jsonInstrument.optInt("id", mc.instrument_id);

                            int hid = jsonInstrument.optInt("hid", 0);
                            int hmid = jsonInstrument.optInt("hmid", 0);
                            int lmid = jsonInstrument.optInt("lmid", 0);
                            int lid = jsonInstrument.optInt("lid", 0);

                            mc.idname.setText(toHexString(hid) +
                                    "." + toHexString(hmid) +
                                    "." + toHexString(lmid) +
                                    "." + toHexString(lid));
                            mc.idname.setTextColor(mc.getResources()
                                    .getColor(android.R.color.black)); // black color

                            if (hid == 0 || hmid == 0 || lmid == 0 || lid == 0) {
                                Log.v(TAG, "jsonInstrument:" + jsonInstrument.toString());
                                mc.sendCmd(new byte[]{'j', 'g', 'i'});
                                return;
                            }

                            Log.v(TAG, "id:"+id+":"+mc.instrument_id);

                            if (id == 0) {
                                mc.idname.setTextColor(mc.getResources()
                                        .getColor(android.R.color.holo_red_dark)); // red color

                                mc.instrument_id = id;
                                mc.dbHelper.rmFile(id);
                                mc.sendCmd(new byte[]{'j', 'g', 'l'});
                            } else if (id != mc.instrument_id) {
                                mc.instrument_id = id;
                                if (mc.dbHelper.addInstrument(id, hid, hmid, lmid, lid) > 0) {
                                    mc.dbHelper.rmFile(id);
                                }
                                mc.sendCmd(new byte[]{'j', 'g', 'l'});
                            }
                        }
                    } else if (jsonObject.has("message")) {
                        JSONObject jsonMessage = jsonObject.optJSONObject("message");
                        if (jsonMessage != null) {
                            String from = jsonMessage.optString("from", "");
                            String sms = jsonMessage.optString("sms", "");

                            switch (from.toLowerCase().trim()) {
                                case "temp": {
                                    mc.ttemp.setText(sms);
                                } break;
                                case "vbat": {
                                    mc.tvbat.setText(sms);
                                } break;
                                case "epoch": {
                                    Date date = new Date(Long.parseLong(sms) * 1000L);
                                    DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss",
                                            Locale.US);
                                    format.setTimeZone(TimeZone.getTimeZone("UTC"));
                                    mc.ttime.setText(format.format(date));
                                } break;
                                default: {
                                    mc.appendLog(from + ":" + sms + "\n");
                                }
                            }
                        }
                    } else if (jsonObject.has("fat_file")) {
                        JSONObject jsonFatFile = jsonObject.optJSONObject("fat_file");
                        if (jsonFatFile != null) {
                            int instrument_id = jsonFatFile.optInt("instrument_id", 0);
                            String name = jsonFatFile.optString("name", "");
                            int action = jsonFatFile.optInt("action", 0);
                            int size = jsonFatFile.optInt("size", 0);
                            int ts = jsonFatFile.optInt("ts", 0);

                            boolean reload_list = false;
                            switch (action) {
                                case 0: { // show
                                    if (!name.equalsIgnoreCase("")) {
                                        mc.dbHelper.addFile(instrument_id, name, size, ts);
                                        reload_list = true;
                                    } else {
                                        mc.appendLog("empty\n");
                                    }
                                } break;
                                case 1: { // remove
                                    File file = new File(mc.dir, name);
                                    if (file.exists()) file.delete();
                                    mc.dbHelper.rmFile(instrument_id, name);
                                    reload_list = true;
                                } break;
                                case 2: { // request
                                } break;
                                case 3: { // send
                                    try {
                                        File file = new File(dir, name);
                                        mc.outputStream = new FileOutputStream(file);
                                        mc.filename = name;
                                        mc.filesize = size;
                                        mc.dataCount = 0;

                                        if (data_ini + data_len < data.length) {
                                            mc.outputStream.write(copyOfRange(data,
                                                    data_ini + data_len, data.length));
                                            mc.dataCount += data.length - (data_ini + data_len);
                                        }

                                        log_cmd = DATA_IN;
                                    } catch (Exception e) {
                                        e.printStackTrace();

                                        mc.outputStream = null;
                                        mc.filename = null;
                                        mc.filesize = 0;
                                        mc.dataCount = 0;
                                        mc.fileprogress = null;
                                    }
                                } break;
                            }

                            if (reload_list) {
                                ListView listView = (ListView) mc.findViewById(R.id.listFiles);
                                ((FileCursorAdapter) listView.getAdapter())
                                        .changeCursor(mc.dbHelper.getFilesCursor(instrument_id));
                                ((FileCursorAdapter) listView.getAdapter()).notifyDataSetChanged();
                            }

                            Log.v(TAG, "jsonFatFile:" + jsonFatFile.toString());
                        }
                    }
                }

                if (sa != null) {
                    FragmentManager fm = sa.getSupportFragmentManager();

                    if (jsonObject.has("instrument")) {
                        InstrumentFragment instrumentFragment = (InstrumentFragment)
                                fm.findFragmentById(R.id.instrument_fragment);
                        instrumentFragment.setJson(jsonObject.optJSONObject("instrument"));
                    } else if (jsonObject.has("configure")) {
                        ConfigureFragment configureFragment = (ConfigureFragment)
                                fm.findFragmentById(R.id.configure_fragment);
                        configureFragment.setJson(jsonObject.optJSONObject("configure"));
                    } else if (jsonObject.has("sensor")) {
                        SensorFragment sensorFragment = (SensorFragment)
                                fm.findFragmentById(R.id.sensor_fragment);
                        sensorFragment.setJson(jsonObject.optJSONObject("sensor"));
                    }
                }
            } break;
        }
    }

    private static int indexOf(byte[] data, int ini, byte b)
    {
        for (int i = ini; i < data.length; i++) {
            if (data[i] == b) return i;
        }
        return -1;
    }

    private void appendLog(String message)
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
                .setName("MainActivityGeoCentinela Page")
                // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }
}