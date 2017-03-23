package cl.timining.lsaavedr.geocentinela;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.Locale;

import static android.app.TimePickerDialog.*;
import static cl.timining.lsaavedr.geocentinela.SettingsActivity.TWO_DIGIT_FORMATTER;
import static java.lang.String.valueOf;

/**
 * Created by lsaavedr on 03-03-17.
 */

public class ConfigureFragment extends Fragment
{
    private static final String TAG = "ConfigureFragment";

    private int id = -1;

    private TextView name, gain, hardware_average, ksps;

    private CheckBox chrono, daily;
    private TextView delay, lapse, begin, end;

    private CheckBox trigger;
    private TextView trigger_level, trigger_time, send_trigger_time;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.configure_view, container, false);
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
                        name.setText(input.getText());
                        sendJson();
                    }
                });
                alert.setNegativeButton(getString(android.R.string.cancel), null);

                alert.show();
            }
        });

        gain = (TextView) getView().findViewById(R.id.gain);
        gain.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                alert.setTitle(R.string.gain);

                final NumberPicker np = new NumberPicker(getContext());
                final String[] values = new String[]{"1", "2", "4", "8", "16", "32", "64", "128"};
                np.setMinValue(0);
                np.setMaxValue(values.length-1);
                np.setDisplayedValues(values);

                try {
                    int gain_num = Integer.parseInt(gain.getText().toString().trim());
                    if (gain_num < 1 || gain_num > 128) gain_num = 0;
                    else gain_num = (int) (Math.log(gain_num)/Math.log(2));
                    np.setValue(gain_num);
                } catch (Exception e) {
                    np.setValue(0);
                }

                alert.setView(np);
                alert.create();

                alert.setPositiveButton(android.R.string.ok, new OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        gain.setText(values[np.getValue()]);
                        dialog.dismiss();
                        sendJson();
                    }
                });
                alert.setNegativeButton(android.R.string.cancel, null);

                alert.show();
            }
        });

        hardware_average = (TextView) getView().findViewById(R.id.hardware_average);
        hardware_average.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                alert.setTitle(R.string.hardware_average);

                final NumberPicker np = new NumberPicker(getContext());
                final String[] values = new String[]{"0", "4", "8", "16", "32"};
                np.setMinValue(0);
                np.setMaxValue(values.length-1);
                np.setDisplayedValues(values);

                try {
                    int hardware_average_num =
                            Integer.parseInt(hardware_average.getText().toString().trim());
                    if (hardware_average_num < 0 || hardware_average_num > 32)
                        hardware_average_num = 0;
                    else
                        hardware_average_num = (int) (Math.log(hardware_average_num)/Math.log(2)-1);
                    np.setValue(hardware_average_num);
                } catch (Exception e) {
                    np.setValue(0);
                }

                alert.setView(np);
                alert.create();

                alert.setPositiveButton(android.R.string.ok, new OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        hardware_average.setText(values[np.getValue()]);
                        dialog.dismiss();
                        sendJson();
                    }
                });
                alert.setNegativeButton(android.R.string.cancel, null);

                alert.show();
            }
        });

        ksps = (TextView) getView().findViewById(R.id.ksps);
        ksps.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                alert.setTitle(R.string.ksps);

                final NumberPicker np = new NumberPicker(getContext());
                np.setMinValue(1);
                np.setMaxValue(8);

                try {
                    int ksps_num = Integer.parseInt(ksps.getText().toString().trim());
                    if (ksps_num < 1 || ksps_num > 8) ksps_num = 1;
                    np.setValue(ksps_num);
                } catch(Exception e) {
                    np.setValue(1);
                }

                alert.setView(np);
                alert.create();

                alert.setPositiveButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ksps.setText(valueOf(np.getValue()));
                        dialog.dismiss();
                        sendJson();
                    }
                });
                alert.setNegativeButton(android.R.string.cancel, null);

                alert.show();
            }
        });

        chrono = (CheckBox) getView().findViewById(R.id.chrono);
        final TableLayout table_chrono = (TableLayout) getView().findViewById(R.id.table_chrono);

        daily = (CheckBox) getView().findViewById(R.id.daily);
        final TableLayout table_daily = (TableLayout) getView().findViewById(R.id.table_daily);

        chrono.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                daily.setChecked(false);
                table_daily.setEnabled(false);
                table_daily.setVisibility(View.INVISIBLE);

                LayoutParams parms;
                parms = (LayoutParams) table_daily.getLayoutParams();
                parms.height = 0;
                table_daily.setLayoutParams(parms);

                parms = (LayoutParams) table_chrono.getLayoutParams();
                parms.height = LayoutParams.MATCH_PARENT;
                table_chrono.setLayoutParams(parms);

                table_chrono.setVisibility(View.VISIBLE);
                table_chrono.setEnabled(true);
                chrono.setChecked(true);
            }
        });

        delay = (TextView) getView().findViewById(R.id.delay);
        delay.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (!chrono.isChecked()) chrono.performClick();

                // get time
                int[] time = getTimeNumbers(delay.getText().toString());

                // widget
                View view = getActivity().getLayoutInflater().inflate(R.layout.time_picker, null);

                AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                alert.setView(view);
                alert.create();
                alert.setTitle(getString(R.string.delay));

                final NumberPicker hour = (NumberPicker) view.findViewById(R.id.hour);
                hour.setMinValue(0);
                hour.setMaxValue(23);
                hour.setValue(time[0]);
                hour.setFormatter(TWO_DIGIT_FORMATTER);

                final NumberPicker minute = (NumberPicker) view.findViewById(R.id.minute);
                minute.setMinValue(0);
                minute.setMaxValue(59);
                minute.setValue(time[1]);
                minute.setFormatter(TWO_DIGIT_FORMATTER);

                final NumberPicker second = (NumberPicker) view.findViewById(R.id.second);
                second.setMinValue(0);
                second.setMaxValue(59);
                second.setValue(time[2]);
                second.setFormatter(TWO_DIGIT_FORMATTER);

                alert.setPositiveButton(android.R.string.ok, new OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        int time = hour.getValue()*3600 + minute.getValue()*60 + second.getValue();
                        delay.setText(getStringTime(time, true));
                        sendJson();
                    }
                });
                alert.setNegativeButton(android.R.string.cancel, null);
                alert.show();
            }
        });

        lapse = (TextView) getView().findViewById(R.id.lapse);
        lapse.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (!chrono.isChecked()) chrono.performClick();

                // get time
                int[] time = getTimeNumbers(lapse.getText().toString());

                // widget
                View view = getActivity().getLayoutInflater().inflate(R.layout.time_picker, null);

                AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                alert.setView(view);
                alert.create();
                alert.setTitle(R.string.lapse);

                final NumberPicker hour = (NumberPicker) view.findViewById(R.id.hour);
                hour.setMinValue(0);
                hour.setMaxValue(23);
                hour.setValue(time[0]);
                hour.setFormatter(TWO_DIGIT_FORMATTER);

                final NumberPicker minute = (NumberPicker) view.findViewById(R.id.minute);
                minute.setMinValue(0);
                minute.setMaxValue(59);
                minute.setValue(time[1]);
                minute.setFormatter(TWO_DIGIT_FORMATTER);

                final NumberPicker second = (NumberPicker) view.findViewById(R.id.second);
                second.setMinValue(0);
                second.setMaxValue(59);
                second.setValue(time[2]);
                second.setFormatter(TWO_DIGIT_FORMATTER);

                alert.setPositiveButton(android.R.string.ok, new OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        int time = hour.getValue()*3600 + minute.getValue()*60 + second.getValue();
                        lapse.setText(getStringTime(time, true));
                        sendJson();
                    }
                });
                alert.setNegativeButton(android.R.string.cancel, null);

                alert.show();
            }
        });

        daily.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                chrono.setChecked(false);
                table_chrono.setEnabled(false);
                table_chrono.setVisibility(View.INVISIBLE);

                LayoutParams parms;
                parms = (LayoutParams) table_chrono.getLayoutParams();
                parms.height = 0;
                table_chrono.setLayoutParams(parms);

                parms = (LayoutParams) table_daily.getLayoutParams();
                parms.height = LayoutParams.MATCH_PARENT;
                table_daily.setLayoutParams(parms);

                table_daily.setVisibility(View.VISIBLE);
                table_daily.setEnabled(true);
                daily.setChecked(true);
            }
        });

        begin = (TextView) getView().findViewById(R.id.begin);
        begin.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (!daily.isChecked()) daily.performClick();

                // get time
                int[] time = getTimeNumbers(begin.getText().toString());

                TimePickerDialog tp = new TimePickerDialog(getContext(), new OnTimeSetListener()
                {
                    @Override
                    public void onTimeSet(TimePicker view, int hour, int minute)
                    {
                        int time = hour*3600 + minute*60;
                        begin.setText(getStringTime(time, false));
                        sendJson();
                    }
                }, time[0], time[1], true);
                tp.setTitle(R.string.begin);
                tp.show();
            }
        });

        end = (TextView) getView().findViewById(R.id.end);
        end.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (!daily.isChecked()) daily.performClick();

                // get time
                int[] time = getTimeNumbers(end.getText().toString());

                TimePickerDialog tp = new TimePickerDialog(getContext(), new OnTimeSetListener()
                {
                    @Override
                    public void onTimeSet(TimePicker view, int hour, int minute)
                    {
                        int time = hour*3600 + minute*60;
                        end.setText(getStringTime(time, false));
                        sendJson();
                    }
                }, time[0], time[1], true);
                tp.setTitle(R.string.end);
                tp.show();
            }
        });

        trigger = (CheckBox) getView().findViewById(R.id.trigger);
        final TableLayout table_trigger = (TableLayout) getView().findViewById(R.id.table_trigger);

        trigger.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                LayoutParams parms = (LayoutParams) table_trigger.getLayoutParams();
                if (trigger.isChecked()) {
                    table_trigger.setEnabled(true);
                    table_trigger.setVisibility(View.VISIBLE);

                    parms.height = LayoutParams.MATCH_PARENT;
                } else {
                    table_trigger.setEnabled(false);
                    table_trigger.setVisibility(View.INVISIBLE);

                    parms.height = 0;
                }
                table_trigger.setLayoutParams(parms);
            }
        });

        trigger_level = (TextView) getView().findViewById(R.id.trigger_level);
        trigger_level.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (!trigger.isChecked()) trigger.performClick();

                AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                alert.setTitle(R.string.trigger_level);

                final NumberPicker np = new NumberPicker(getContext());
                np.setMinValue(1);
                np.setMaxValue(100);

                try {
                    int trigger_level_num =
                            Integer.parseInt(trigger_level.getText().toString().trim());
                    if (trigger_level_num < 1 || trigger_level_num > 100) trigger_level_num = 1;
                    np.setValue(trigger_level_num);
                } catch(Exception e) {
                    np.setValue(1);
                }

                alert.setView(np);
                alert.create();

                alert.setPositiveButton(android.R.string.ok, new OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        trigger_level.setText(valueOf(np.getValue()));
                        dialog.dismiss();
                        sendJson();
                    }
                });
                alert.setNegativeButton(android.R.string.cancel, null);

                alert.show();
            }
        });

        trigger_time = (TextView) getView().findViewById(R.id.trigger_time);
        trigger_time.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (!trigger.isChecked()) trigger.performClick();

                // widget
                View view = getActivity().getLayoutInflater().inflate(R.layout.time_picker, null);

                AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                alert.setView(view);
                alert.create();
                alert.setTitle(R.string.trigger_time);

                int[] time = getTimeNumbers(trigger_time.getText().toString());

                final NumberPicker hour = (NumberPicker) view.findViewById(R.id.hour);
                hour.setMinValue(0);
                hour.setMaxValue(23);
                hour.setValue(time[0]);
                hour.setFormatter(TWO_DIGIT_FORMATTER);

                final NumberPicker minute = (NumberPicker) view.findViewById(R.id.minute);
                minute.setMinValue(0);
                minute.setMaxValue(59);
                minute.setValue(time[1]);
                minute.setFormatter(TWO_DIGIT_FORMATTER);

                final NumberPicker second = (NumberPicker) view.findViewById(R.id.second);
                second.setMinValue(0);
                second.setMaxValue(59);
                second.setValue(time[2]);
                second.setFormatter(TWO_DIGIT_FORMATTER);

                alert.setPositiveButton(android.R.string.ok, new OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        int time = hour.getValue()*3600 + minute.getValue()*60 + second.getValue();
                        trigger_time.setText(getStringTime(time, true));
                        sendJson();
                    }
                });
                alert.setNegativeButton(android.R.string.cancel, null);

                alert.show();
            }
        });

        send_trigger_time = (TextView) getView().findViewById(R.id.send_trigger_time);
        send_trigger_time.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (!trigger.isChecked()) trigger.performClick();

                // get time
                int[] time = getTimeNumbers(send_trigger_time.getText().toString());

                TimePickerDialog tp = new TimePickerDialog(getContext(), new OnTimeSetListener()
                {
                    @Override
                    public void onTimeSet(TimePicker view, int hour, int minute)
                    {
                        int time = hour*3600 + minute*60;
                        send_trigger_time.setText(getStringTime(time, false));
                        sendJson();
                    }
                }, time[0], time[1], true);
                tp.setTitle(R.string.send_trigger_time);
                tp.show();
            }
        });
    }

    public void sendJson()
    {
        if (id >= 0) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("name", name.getText().toString());

                double gain_num = Integer.parseInt(gain.getText().toString());
                if (gain_num < 2 || gain_num > 128) gain_num = 0;
                else gain_num = Math.log(gain_num) / Math.log(2);
                jsonObject.put("gain", (int) gain_num);

                double hardware_average_num =
                        Integer.parseInt(hardware_average.getText().toString());
                if (hardware_average_num < 4 || hardware_average_num > 32) hardware_average_num = 4;
                else hardware_average_num = Math.log(hardware_average_num) / Math.log(2) - 2;
                jsonObject.put("hardware_average", (int) hardware_average_num);

                double tick_time_num = Integer.parseInt(ksps.getText().toString());
                if (tick_time_num < 2 || tick_time_num > 8) tick_time_num = 1000;
                else tick_time_num = 1000.0 / tick_time_num;
                jsonObject.put("tick_time", (int) tick_time_num);

                if (chrono.isChecked()) {
                    jsonObject.put("time_type", 0);
                    jsonObject.put("time_begin", getTimeNumber(delay.getText().toString()));
                    jsonObject.put("time_end", getTimeNumber(lapse.getText().toString()));
                } else if (daily.isChecked()) {
                    jsonObject.put("time_type", 1);
                    jsonObject.put("time_begin", getTimeNumber(begin.getText().toString()));
                    jsonObject.put("time_end", getTimeNumber(end.getText().toString()));
                } else jsonObject.put("type", 2);

                if (trigger.isChecked()) {
                    double trigger_level_num =
                            Integer.parseInt(trigger_level.getText().toString());
                    if (trigger_level_num < 1 || trigger_level_num > 100) trigger_level_num = 0;
                    else trigger_level_num = (0x7FFF * (100 - trigger_level_num)) / 100;
                    jsonObject.put("trigger_level", (int) trigger_level_num);
                    jsonObject.put("trigger_time",
                            getTimeNumber(trigger_time.getText().toString()));
                    jsonObject.put("send_trigger_time",
                            getTimeNumber(send_trigger_time.getText().toString()));
                } else {
                    jsonObject.put("trigger_level", 0);
                    jsonObject.put("trigger_time", 0);
                    jsonObject.put("send_trigger_time", 0);
                }

                String cmd = "jsc" + jsonObject.toString().replaceAll("[\n\r]", "");
                ((SettingsActivity) getActivity()).sendCmd(cmd.getBytes(Charset.forName("UTF-8")));

                Log.v(TAG, "sendJson:" + jsonObject.toString());

                ((SettingsActivity) getActivity()).sendCmd(new byte[]{'j', 'g', 'c'});
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

        int gain_num = jsonObject.optInt("gain", 0);
        if (gain_num < 0 || gain_num > 7) gain_num = 1;
        else gain_num = (int) Math.pow(2, gain_num);
        gain.setText(valueOf(gain_num));

        int hardware_average_num = jsonObject.optInt("hardware_average", 4);
        if (hardware_average_num < 0 || hardware_average_num > 3) hardware_average_num = 0;
        else hardware_average_num = (int) Math.pow(2, hardware_average_num + 2);
        hardware_average.setText(valueOf(hardware_average_num));

        int ksps_num = jsonObject.optInt("tick_time", 1000);
        if (ksps_num < 125 || ksps_num > 1000) ksps_num = 1;
        else ksps_num = (int) (1000.0/ksps_num);
        ksps.setText(valueOf(ksps_num));

        int type = jsonObject.optInt("time_type", 0);
        int time_begin_num = jsonObject.optInt("time_begin", 0);
        int time_end_num = jsonObject.optInt("time_end", 1);
        switch(type) {
            case 0:
                if (!chrono.isChecked()) chrono.performClick();
                delay.setText(getStringTime(time_begin_num, true));
                lapse.setText(getStringTime(time_end_num, true));
                break;
            case 1:
                if (!daily.isChecked()) daily.performClick();
                begin.setText(getStringTime(time_begin_num, false));
                end.setText(getStringTime(time_end_num, false));
                break;
        }

        int trigger_level_num = jsonObject.optInt("trigger_level", 0);
        int trigger_time_num = jsonObject.optInt("trigger_time", 0);
        if (trigger_level_num <= 0 || trigger_level_num > 0x7FFF || trigger_time_num <= 0) {
            if (trigger.isChecked()) trigger.performClick();
            else {
                trigger.performClick();
                trigger.performClick();
            }

            trigger_level_num = 0;
            trigger_time_num = 0;
        } else {
            if (!trigger.isChecked()) trigger.performClick();

            trigger_level_num = (int) ((100.0 * (0x7FFF - trigger_level_num))/0x7FFF);
        }
        trigger_level.setText(valueOf(trigger_level_num));
        trigger_time.setText(getStringTime(trigger_time_num, true));

        int send_trigger_time_num = jsonObject.optInt("send_trigger_time", 0);
        if (send_trigger_time_num < 0) send_trigger_time_num = 0;
        send_trigger_time.setText(getStringTime(send_trigger_time_num, false));
    }

    private String getStringTime(int time, boolean with_seconds)
    {
        if (with_seconds) return String.format(Locale.US, "%1$02d:%2$02d:%3$02d",
                (time%86400)/3600, (time%3600)/60, time%60);
        else return String.format(Locale.US, "%1$02d:%2$02d", (time%86400)/3600, (time%3600)/60);
    }

    private int[] getTimeNumbers(String time)
    {
        String[] data = time.split(":");
        int[] num = new int[3];

        num[0] = 0;
        try {
            num[0] = Integer.parseInt(data[0]);
        } catch (Exception e) {
            num[0] = 0;
        }

        num[1] = 0;
        try {
            num[1] = Integer.parseInt(data[1]);
        } catch (Exception e) {
            num[1] = 0;
        }

        num[2] = 0;
        try {
            num[2] = Integer.parseInt(data[2]);
        } catch (Exception e) {
            num[2] = 0;
        }

        return num;
    }

    private long getTimeNumber(String time)
    {
        int num[] = getTimeNumbers(time);

        return (long)num[0] * 3600 + (long)num[1] * 60 + (long) num[2];
    }
}
