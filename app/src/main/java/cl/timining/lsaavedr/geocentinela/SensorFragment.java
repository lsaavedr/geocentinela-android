package cl.timining.lsaavedr.geocentinela;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;

/**
 * Created by lsaavedr on 03-03-17.
 */

public class SensorFragment extends Fragment
{
    private static final String TAG = "SensorFragment";

    private int id = -1;
    private TextView name, sensitivity, lat, lng, cta;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.sensor_view, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        name = (TextView) getView().findViewById(R.id.name);
        name.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                alert.setTitle(getString(R.string.name));

                final EditText input = new EditText(getContext());
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setText(name.getText());

                alert.setView(input);
                alert.create();

                alert.setPositiveButton(getString(android.R.string.ok), new OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        name.setText(input.getText().toString());
                        sendJson();
                    }
                });
                alert.setNegativeButton(getString(android.R.string.cancel), null);

                alert.show();
            }
        });

        sensitivity = (TextView) getView().findViewById(R.id.sensitivity);
        sensitivity.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                alert.setTitle(getString(R.string.sensitivity));

                final EditText input = new EditText(getContext());
                input.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL| InputType.TYPE_CLASS_NUMBER);
                input.setText(sensitivity.getText());

                alert.setView(input);
                alert.create();

                alert.setPositiveButton(getString(android.R.string.ok), new OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        sensitivity.setText(input.getText().toString());
                        sendJson();
                    }
                });
                alert.setNegativeButton(getString(android.R.string.cancel), null);

                alert.show();
            }
        });

        lat = (TextView) getView().findViewById(R.id.lat);
        lat.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                alert.setTitle(getString(R.string.lat));

                final EditText input = new EditText(getContext());
                input.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL| InputType.TYPE_CLASS_NUMBER);
                input.setText(lat.getText());

                alert.setView(input);
                alert.create();

                alert.setPositiveButton(getString(android.R.string.ok), new OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        lat.setText(input.getText().toString());
                        sendJson();
                    }
                });
                alert.setNegativeButton(getString(android.R.string.cancel), null);

                alert.show();
            }
        });

        lng = (TextView) getView().findViewById(R.id.lng);
        lng.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                alert.setTitle(getString(R.string.lng));

                final EditText input = new EditText(getContext());
                input.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL| InputType.TYPE_CLASS_NUMBER);
                input.setText(lng.getText());

                alert.setView(input);
                alert.create();

                alert.setPositiveButton(getString(android.R.string.ok), new OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        lng.setText(input.getText().toString());
                        sendJson();
                    }
                });
                alert.setNegativeButton(getString(android.R.string.cancel), null);

                alert.show();
            }
        });

        cta = (TextView) getView().findViewById(R.id.cta);
        cta.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                alert.setTitle(getString(R.string.cta));

                final EditText input = new EditText(getContext());
                input.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL| InputType.TYPE_CLASS_NUMBER);
                input.setText(cta.getText());

                alert.setView(input);
                alert.create();

                alert.setPositiveButton(getString(android.R.string.ok), new OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        cta.setText(input.getText().toString());
                        sendJson();
                    }
                });
                alert.setNegativeButton(getString(android.R.string.cancel), null);

                alert.show();
            }
        });
    }

    private void sendJson()
    {
        if (id >= 0) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("name", name.getText().toString());

                double sensitivity_num = Double.parseDouble(sensitivity.getText().toString());
                if (sensitivity_num < 0) jsonObject.put("sensitivity", 1.0);
                else jsonObject.put("sensitivity", sensitivity_num);

                double lat_num = Double.parseDouble(lat.getText().toString());
                jsonObject.put("lat", lat_num);

                double lng_num = Double.parseDouble(lng.getText().toString());
                jsonObject.put("lng", lng_num);

                double cta_num = Double.parseDouble(cta.getText().toString());
                jsonObject.put("cta", cta_num);

                String cmd = "jss" + jsonObject.toString().replaceAll("[\n\r]", "");
                ((SettingsActivity) getActivity()).sendCmd(cmd.getBytes(Charset.forName("UTF-8")));

                Log.v(TAG, "sendJson:" + jsonObject.toString());

                ((SettingsActivity) getActivity()).sendCmd(new byte[]{'j', 'g', 's'});
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void setJson(JSONObject jsonObject)
    {
        if (jsonObject == null) return;
        Log.v(TAG, jsonObject.toString());

        id = jsonObject.optInt("id", 0);
        name.setText(jsonObject.optString("name", ""));
        name.setTextColor(Color.parseColor("#ffffff")); // black color
        if (id == 0) name.setTextColor(Color.parseColor("#ff0000")); // red color

        sensitivity.setText(String.valueOf(jsonObject.optDouble("sensitivity", 1)));
        lat.setText(String.valueOf(jsonObject.optDouble("lat", 0)));
        lng.setText(String.valueOf(jsonObject.optDouble("lng", 0)));
        cta.setText(String.valueOf(jsonObject.optDouble("cta", 0)));
    }
}