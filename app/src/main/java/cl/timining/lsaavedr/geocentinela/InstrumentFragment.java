package cl.timining.lsaavedr.geocentinela;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.NumberPicker;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.TimeZone;

/**
 * Created by lsaavedr on 03-03-17.
 */

public class InstrumentFragment extends Fragment
{
    private static final String TAG = "InstrumentFragment";
    static int REQUEST_PICK_CONTACT = 1000;

    private int id = -1;
    private int time_zone_offset = 0;

    private TextView idname;
    private TextView phone_warning;
    private TextView time_zone;
    private CheckBox gprs, gps;
    private Button call;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.instrument_view, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        idname = (TextView) getView().findViewById(R.id.idname);

        phone_warning = (TextView) getView().findViewById(R.id.phone_warning);
        phone_warning.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (ContextCompat.checkSelfPermission(getActivity(),
                        Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(Intent.ACTION_PICK,
                            Phone.CONTENT_URI);
                    startActivityForResult(intent, REQUEST_PICK_CONTACT);
                }
            }
        });


        time_zone = (TextView) getView().findViewById(R.id.time_zone);
        time_zone.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                alert.setTitle(R.string.time_zone);

                final NumberPicker np = new NumberPicker(getContext());
                final String[] values = TimeZone.getAvailableIDs();
                np.setMinValue(0);
                np.setMaxValue(values.length-1);
                np.setDisplayedValues(values);

                TimeZone tz = TimeZone.getTimeZone(time_zone.getText().toString());

                int index = Arrays.asList(values).indexOf(tz.getID());
                np.setValue(index);

                alert.setView(np);
                alert.create();

                alert.setPositiveButton(android.R.string.ok, new OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        TimeZone tz = TimeZone.getTimeZone(values[np.getValue()]);
                        time_zone.setText(tz.getID());
                        time_zone_offset = tz.getRawOffset()/1000;

                        sendJson();
                    }
                });
                alert.setNegativeButton(android.R.string.cancel, null);

                alert.show();
            }
        });

        gprs = (CheckBox) getView().findViewById(R.id.gprs);
        gprs.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                sendJson();
            }
        });

        gps = (CheckBox) getView().findViewById(R.id.gps);
        gps.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                sendJson();
            }
        });

        call = (Button) getView().findViewById(R.id.call);
        call.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_PICK_CONTACT) {
            Uri uri = data.getData();
            Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    String phoneNo = cursor.getString(cursor.getColumnIndex(Phone.NUMBER));
                    phone_warning.setText(phoneNo.replaceAll("[ -]", ""));
                    sendJson();
                }
                cursor.close();
            }
        }
    }

    public void sendJson()
    {
        JSONObject jsonObject = new JSONObject();
        try {
            if (id >= 0) {
                jsonObject.put("phone_warning", phone_warning.getText().toString());
                jsonObject.put("time_zone", time_zone.getText().toString());
                if (time_zone_offset != 0) jsonObject.put("time_zone_offset", time_zone_offset);
                jsonObject.put("gprs", gprs.isChecked());
                jsonObject.put("gps", gps.isChecked());

                String cmd = "jsi" + jsonObject.toString().replaceAll("[\n\r]", "");
                ((SettingsActivity) getActivity()).sendCmd(cmd.getBytes(Charset.forName("UTF-8")));

                Log.v(TAG, "sendJson:"+jsonObject.toString());

                ((SettingsActivity) getActivity()).sendCmd(new byte[]{'j', 'g', 'i'});
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setJson(JSONObject jsonObject)
    {
        if (jsonObject == null) return;
        Log.v(TAG, jsonObject.toString());

        id = jsonObject.optInt("id", 0);
        time_zone_offset = jsonObject.optInt("time_zone_offset", 0);

        idname.setText(
                Integer.toHexString(jsonObject.optInt("hid", 0)) + "." +
                        Integer.toHexString(jsonObject.optInt("hmid", 0)) + "." +
                        Integer.toHexString(jsonObject.optInt("lmid", 0)) + "." +
                        Integer.toHexString(jsonObject.optInt("lid", 0)));
        idname.setTextColor(Color.parseColor("#ffffff")); // black color
        if (id == 0) idname.setTextColor(Color.parseColor("#ff0000")); // red color

        phone_warning.setText(jsonObject.optString("phone_warning", "+56993118748"));

        String tz_str = jsonObject.optString("time_zone", "GMT0");
        TimeZone tz = TimeZone.getTimeZone(tz_str);
        if (tz.getRawOffset() == 0 && time_zone_offset != 0) {
            String tzs[] = TimeZone.getAvailableIDs(time_zone_offset*1000);

            for (String tz_i : tzs) {
                if (tz_i.contains(tz_str)) {
                    tz = TimeZone.getTimeZone(tz_i);
                    break;
                }
            }

            if (tz.getRawOffset() == 0) tz = TimeZone.getTimeZone(tzs[0]);
        }
        time_zone.setText(tz.getID());
        time_zone_offset = tz.getRawOffset()/1000;

        gprs.setChecked(jsonObject.optBoolean("gprs", false));
        gps.setChecked(jsonObject.optBoolean("gps", false));
    }
}