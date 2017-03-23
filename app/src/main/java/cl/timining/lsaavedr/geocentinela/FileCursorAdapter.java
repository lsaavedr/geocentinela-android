package cl.timining.lsaavedr.geocentinela;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;

import org.json.JSONException;
import org.json.JSONObject;

public class FileCursorAdapter extends CursorAdapter
{
    private static final String TAG = "FileCursorAdapter";

    public FileCursorAdapter(Context context, Cursor cursor)
    {
        super(context, cursor, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent)
    {
        return LayoutInflater.from(context).inflate(R.layout.file_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor)
    {
        final int id = cursor.getInt(cursor.getColumnIndex("_id"));
        final int instrument_id = cursor.getInt(cursor.getColumnIndex("instrument_id"));
        final String name = cursor.getString(cursor.getColumnIndex("name"));
        final int size = cursor.getInt(cursor.getColumnIndex("size"));
        final int ts = cursor.getInt(cursor.getColumnIndex("ts"));
        int _status = cursor.getInt(cursor.getColumnIndex("status"));

        final File file = new File(MainActivityGeoCentinela.dir, name);
        if (_status == 0 && file.exists()) {
            _status = 1;
            if (((MainActivityGeoCentinela) view.getContext()).dbHelper!=null)
                ((MainActivityGeoCentinela) view.getContext())
                        .dbHelper.updateStatus(instrument_id, name, _status);
        }
        final int status = _status;

        view.setId(id);

        final TextView filename = (TextView) view.findViewById(R.id.filename);
        filename.setText(name);

        TextView filesize = (TextView) view.findViewById(R.id.filesize);
        filesize.setText(String.valueOf(size));

        Date date = new Date(ts * 1000L);
        DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));

        TextView filedate = (TextView)view.findViewById(R.id.filedate);
        filedate.setText(format.format(date));

        final ProgressBar fileprogress = (ProgressBar) view.findViewById(R.id.fileprogress);
        fileprogress.setMax(size);
        if (size == 0) fileprogress.setMax(100);

        Button getFile = (Button)view.findViewById(R.id.getfile);
        getFile.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                if (status == 0) {
                    JSONObject jsonObj = new JSONObject();
                    try {
                        jsonObj.put("instrument_id", instrument_id);
                        jsonObj.put("name", name);
                        jsonObj.put("action", 2); // request
                        jsonObj.put("size", size);
                        jsonObj.put("ts", ts);

                        String jsonStr = "jsf" + jsonObj.toString().replaceAll("[\n\r]", "");

                        ((MainActivityGeoCentinela) v.getContext()).fileprogress = fileprogress;

                        ((MainActivityGeoCentinela) v.getContext()).sendCmd(
                                jsonStr.getBytes(Charset.forName("UTF-8")));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        Button viewFile = (Button)view.findViewById(R.id.viewfile);
        viewFile.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), GraphActivity.class);
                intent.putExtra("filename", file.getAbsolutePath());
                v.getContext().startActivity(intent);
            }
        });

        Button rmFile = (Button)view.findViewById(R.id.rmfile);
        rmFile.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                JSONObject jsonObj = new JSONObject();
                try {
                    jsonObj.put("instrument_id", instrument_id);
                    jsonObj.put("name", name);
                    jsonObj.put("action", 1); // remove
                    jsonObj.put("size", size);
                    jsonObj.put("ts", ts);

                    String jsonStr = "jsf" + jsonObj.toString().replaceAll("[\n\r]", "");

                    byte[] cmd = jsonStr.getBytes(Charset.forName("UTF-8"));
                    ((MainActivityGeoCentinela) v.getContext()).sendCmd(cmd);

                    DBHelper dbHelper = ((MainActivityGeoCentinela) v.getContext()).dbHelper;

                    if (dbHelper != null) {
                        dbHelper.rmFile(instrument_id, name);

                        FileCursorAdapter.this.changeCursor(dbHelper.getFilesCursor(instrument_id));
                        FileCursorAdapter.this.notifyDataSetChanged();
                    }
                    if (file.exists()) file.delete();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        if (status==1 && file.exists()) {
            fileprogress.setProgress(size);
            if (size == 0) fileprogress.setProgress(100);

            getFile.setEnabled(false);
            viewFile.setEnabled(true);
        } else {
            fileprogress.setProgress(0);

            getFile.setEnabled(true);
            viewFile.setEnabled(false);
        }

        fileprogress.setVisibility(View.INVISIBLE);
    }
}