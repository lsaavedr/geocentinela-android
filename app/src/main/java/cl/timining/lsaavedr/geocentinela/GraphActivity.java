package cl.timining.lsaavedr.geocentinela;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

public class GraphActivity extends Activity
{
    private static final String TAG = "GraphActivity";

    private String filename;

    private static int BUFFER_SIZE = 6*1000;

    private int head_size = 0;
    private int tail_size = 0;
    private boolean noShowPage = false;
    private int count = 0;

    private RandomAccessFile dfile, qfile;
    private int seekIndex = 0;

    // head
    private byte format;
    private int uidh = 0;
    private int uidmh = 0;
    private int uidml = 0;
    private int uidl = 0;
    private byte nbits = 0;
    private short vref = 0;
    private int tzoffset = 0;
    private float sensitivity = 0;
    private float latitud = 0;
    private float longitud = 0;
    private float cota = 0;
    private byte gain = 0;
    private byte havrg = 0;
    private int tick_time = 0;
    private int begin_time = 0;
    private int end_time = 0;
    private short trigger_level = 0;
    private int trigger_time = 0;
    private int send_trigger_time = 0;
    private float vbat_ini = 0;
    private float temp_ini = 0;
    private int rtc_ini = 0;
    private int nconf = 0;

    // tail
    private float vbat_end = 0;
    private float temp_end = 0;

    // plot
    private LineChart mLineChart;
    private List<Entry> xvalues = new ArrayList<Entry>();
    private List<Entry> yvalues = new ArrayList<Entry>();
    private List<Entry> zvalues = new ArrayList<Entry>();

    private DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
    private IAxisValueFormatter formatter = new IAxisValueFormatter()
    {
        @Override
        public String getFormattedValue(float value, AxisBase axis)
        {
            long rtc = rtc_ini * 1000L + (long) ((value * tick_time)/1000.0);
            Date date = new Date(rtc);

            return dateFormat.format(date);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graphic_activity);

        Log.v(TAG, "onCreate");

        this.filename = getIntent().getStringExtra("filename").replace("TRG", "XYZ");
        if (!filename.contains("XYZ")) try {
            this.finalize();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        mLineChart = (LineChart) findViewById(R.id.chart);

        try {
            dfile = new RandomAccessFile(this.filename, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            dfile = null;
            qfile = null;
        }

        if (dfile != null) {
            try {
                qfile = new RandomAccessFile(this.filename.replace("XYZ", "TRG"), "r");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                qfile = null;
            }
        }

        try {
            format = dfile.readByte();
            switch (format) {
                case 0x06: {
                    head_size = 84;
                    tail_size = 24;

                    byte buffer_array[] = new byte[head_size-1];
                    dfile.read(buffer_array);
                    ByteBuffer buffer = ByteBuffer.wrap(buffer_array);
                    buffer.order(ByteOrder.LITTLE_ENDIAN);

                    uidh = buffer.getInt(1-1); // uint32
                    uidmh = buffer.getInt(5-1); // uint32
                    uidml = buffer.getInt(9-1); // uint32
                    uidl = buffer.getInt(13-1); // uint32

                    nbits = buffer.get(17-1); // uint8
                    vref = buffer.getShort(18-1); // uint16

                    tzoffset = buffer.getInt(20-1); // int32

                    sensitivity = buffer.getFloat(24-1);
                    latitud = buffer.getFloat(28-1);
                    longitud = buffer.getFloat(32-1);
                    cota = buffer.getFloat(36-2);

                    gain = buffer.get(40-1); // uint8
                    havrg = buffer.get(41-1); // uint8
                    tick_time = buffer.getInt(42-1); // uint32
                    begin_time = buffer.getInt(46-1); // uint32
                    end_time = buffer.getInt(50-1); // uint32

                    trigger_level = buffer.getShort(54-1); // uint16
                    trigger_time = buffer.getInt(56-1); // uint32
                    send_trigger_time = buffer.getInt(60-1); // uint32

                    vbat_ini = buffer.getFloat(64-1);
                    temp_ini = buffer.getFloat(68-1);

                    rtc_ini = buffer.getInt(72-1); // uint32
                    nconf = buffer.getInt(76-1); // uint32

                    logHead();
                } break;
            }
        } catch (IOException e) {
            e.printStackTrace();
            dfile = null;
            qfile = null;
            try {
                this.finalize();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }

        Button ff = (Button) this.findViewById(R.id.ff);
        ff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Log.v(TAG, "ec0:" + mLineChart.getLineData().getEntryCount() + ":" + count);
                // xvalues.clear();
                // yvalues.clear();
                // zvalues.clear();
                LineData data = mLineChart.getLineData();

                Log.v(TAG, "ec1:" + mLineChart.getLineData().getEntryCount() + ":" + count);
                int count_ini = count;
                try {
                    float factor = (float) (vref / (Math.pow(2, nbits + gain) * sensitivity));

                    dfile.seek(head_size);

                    byte buf[] = new byte[BUFFER_SIZE];

                    int mCount = 0;

                    long nread;
                    while ((nread = dfile.read(buf)) >= 0) {
                        for (int i = 0; i < nread; i += 2*3) {
                            if (mCount < nconf) {
                                int x = ((buf[i+1] & 0xff) << 8) | (buf[i+0] & 0xff);
                                int y = ((buf[i+3] & 0xff) << 8) | (buf[i+2] & 0xff);
                                int z = ((buf[i+5] & 0xff) << 8) | (buf[i+4] & 0xff);

                                //xvalues.add(new Entry(count, factor * (x-0x7fff)));
                                //yvalues.add(new Entry(count, factor * (y-0x7fff)));
                                //zvalues.add(new Entry(count, factor * (z-0x7fff)));
                                data.addEntry(new Entry(count, factor * (x-0x7fff)), 0);
                                data.addEntry(new Entry(count, factor * (y-0x7fff)), 1);
                                data.addEntry(new Entry(count, factor * (z-0x7fff)), 2);

                                if (noShowPage) {
                                    data.getDataSetByIndex(0).removeFirst();
                                    data.getDataSetByIndex(1).removeFirst();
                                    data.getDataSetByIndex(2).removeFirst();
                                }
                            } else break;
                            mCount++;
                            count++;

                            Log.v(TAG, "mcount:" + mCount);
                            //data.notifyDataChanged();
                            //mLineChart.notifyDataSetChanged();
                        }
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                mLineChart.getLineData().notifyDataChanged();
                mLineChart.notifyDataSetChanged();

                Log.v(TAG, "ec2:" + mLineChart.getLineData().getEntryCount() + ":" + count);

                if (!noShowPage) {
                    showPage();
                    noShowPage = true;
                } else mLineChart.invalidate(); // refresh

                mLineChart.setVisibleXRange(count_ini, count);
                Log.v(TAG, "x0:" + count_ini + " x1:" + count);
                // mLineChart.moveViewTo(mLineChart.getLineData().getEntryCount(), 0f, YAxis.AxisDependency.LEFT);
                Log.v(TAG, "ec3:" + mLineChart.getLineData().getEntryCount() + ":" + count);
                mLineChart.invalidate(); // refresh

                mLineChart.moveViewToX(count_ini);
            }
        });

        Button rew = (Button) this.findViewById(R.id.rew);
        rew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                xvalues.clear();
                yvalues.clear();
                zvalues.clear();

                count = 0;

                mLineChart.invalidate(); // refresh
            }
        });

        if (TimeZone.getAvailableIDs(tzoffset*1000).length > 0) {
            TimeZone tz = TimeZone.getTimeZone(TimeZone.getAvailableIDs(tzoffset * 1000)[0]);
            dateFormat.setTimeZone(tz);
        } else dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        showPage();
    }

    private void logHead()
    {
        Log.v(TAG, "HEAD:\n  " +
                "format :" + Integer.toHexString(format) + "\n  " +
                "uidh :" + Integer.toHexString(uidh) + "\n  " +
                "uidmh :" + Integer.toHexString(uidmh) + "\n  " +
                "uidml :" + Integer.toHexString(uidml) + "\n  " +
                "uidl :" + Integer.toHexString(uidl) + "\n  " +
                "nbits :" + Integer.toHexString(nbits) + "\n  " +
                "vref :" + Integer.toString(vref) + "\n  " +
                "tzoffset :" + Integer.toString(tzoffset) + "\n  " +
                "sensitivity :" + sensitivity + "\n  " +
                "latitud :" + latitud + "\n  " +
                "longitud :" + longitud + "\n  " +
                "cota :" + cota + "\n  " +
                "gain :" + Integer.toHexString(gain) + "\n  " +
                "havrg :" + Integer.toHexString(havrg) + "\n  " +
                "tick_time :" + Integer.toString(tick_time) + "\n  " +
                "begin_time :" + Integer.toString(begin_time) + "\n  " +
                "end_time :" + Integer.toString(end_time) + "\n  " +
                "trigger_level :" + Integer.toHexString(trigger_level) + "\n  " +
                "trigger_time :" + Integer.toString(trigger_time) + "\n  " +
                "send_trigger_time :" + Integer.toString(send_trigger_time) + "\n  " +
                "vbat_ini :" + vbat_ini + "\n  " +
                "temp_ini :" + temp_ini + "\n  " +
                "rtc_ini :" + Integer.toString(rtc_ini) + "\n  " +
                "nconf :" + Integer.toString(nconf) + "\n  "
        );
    }

    private void showPage()
    {
        LineDataSet xset = new LineDataSet(xvalues, "Geophone X");
        xset.setAxisDependency(YAxis.AxisDependency.LEFT);
        xset.setColor(this.getResources().getColor(android.R.color.holo_red_dark));
        xset.setDrawCircles(false);
        xset.setDrawCircleHole(false);
        xset.setDrawFilled(false);
        xset.setDrawValues(false);
        xset.setDrawHighlightIndicators(false);
        xset.setDrawHorizontalHighlightIndicator(false);
        xset.setDrawVerticalHighlightIndicator(false);

        LineDataSet yset = new LineDataSet(yvalues, "Geophone Y");
        yset.setAxisDependency(YAxis.AxisDependency.LEFT);
        yset.setColor(this.getResources().getColor(android.R.color.holo_green_dark));
        yset.setDrawCircles(false);
        yset.setDrawCircleHole(false);
        yset.setDrawFilled(false);
        yset.setDrawValues(false);
        yset.setDrawHighlightIndicators(false);
        yset.setDrawHorizontalHighlightIndicator(false);
        yset.setDrawVerticalHighlightIndicator(false);

        LineDataSet zset = new LineDataSet(zvalues, "Geophone Z");
        zset.setAxisDependency(YAxis.AxisDependency.LEFT);
        zset.setColor(this.getResources().getColor(android.R.color.holo_blue_dark));
        zset.setDrawCircles(false);
        zset.setDrawCircleHole(false);
        zset.setDrawFilled(false);
        zset.setDrawValues(false);
        zset.setDrawHighlightIndicators(false);
        zset.setDrawHorizontalHighlightIndicator(false);
        zset.setDrawVerticalHighlightIndicator(false);

        List<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        dataSets.add(xset);
        dataSets.add(yset);
        dataSets.add(zset);

        LineData data = new LineData(dataSets);

        XAxis xAxis = mLineChart.getXAxis();
        xAxis.setGranularity(1f); // minimum axis-step (interval) is 1
        xAxis.setValueFormatter(formatter);

        mLineChart.setData(data);
        mLineChart.invalidate(); // refresh
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        Log.v(TAG, "onStart");
    }

    @Override
    protected void onDestroy()
    {
        Log.v(TAG, "onDestroy");

        if (dfile != null) try {
            dfile.close();
            dfile = null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (qfile != null) try {
            qfile.close();
            qfile = null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        super.onDestroy();
    }
}
