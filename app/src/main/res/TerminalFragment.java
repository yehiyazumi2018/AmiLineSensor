package de.kai_morich.AmiLineSensor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.hoho.android.usbserial.driver.SerialTimeoutException;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.EnumSet;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener, AdapterView.OnItemSelectedListener {


    private enum Connected {False, Pending, True}

    private final BroadcastReceiver broadcastReceiver;
    private int deviceId, portNum, baudRate;
    private UsbSerialPort usbSerialPort;
    private SerialService service;
    private TextView receiveText, TVUSBStatus;
    private TextView sendText;
    private ControlLines controlLines;
    private TextUtil.HexWatcher hexWatcher;
    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean hexEnabled = false;
    private boolean controlLinesEnabled = false;
    private boolean pendingNewline = false;
    private String newline = TextUtil.newline_crlf;

    public Thread Plotthread;
    public boolean graphvaluesread = false;

    private boolean keepScreenOn = true;

    Handler handler = new Handler();
    Runnable runnable;
    Handler handler1 = new Handler();
    Runnable runnable1;

    int delay = 0;

    private Spinner Axismode, channel;
    private String[] Axismodes = {"X-Axis", "Y-Axis", "Z-Axis", "3-Axis"};
    private String[] Channels = {"All channels", "Channel1", "Channel2", "Channel3", "Channel4", "Channel5",
            "Channel6", "Channel7", "Channel8", "Channel9", "Channel10", "Channel11",
            "Channel12", "Channel13", "Channel14", "Channel15", "Channel16"};
    private LinearLayout Lin1, Lin2, Lin3;
    private String all48data[];

    private LineChart mChart1, mChart2, mChart3;

    private int ch1Xaxis, ch1Yaxis, ch1Zaxis, ch2Xaxis, ch2Yaxis, ch2Zaxis, ch3Xaxis, ch3Yaxis, ch3Zaxis,
            ch4Xaxis, ch4Yaxis, ch4Zaxis, ch5Xaxis, ch5Yaxis, ch5Zaxis, ch6Xaxis, ch6Yaxis, ch6Zaxis,
            ch7Xaxis, ch7Yaxis, ch7Zaxis, ch8Xaxis, ch8Yaxis, ch8Zaxis, ch9Xaxis, ch9Yaxis, ch9Zaxis,
            ch10Xaxis, ch10Yaxis, ch10Zaxis, ch11Xaxis, ch11Yaxis, ch11Zaxis, ch12Xaxis, ch12Yaxis, ch12Zaxis,
            ch13Xaxis, ch13Yaxis, ch13Zaxis, ch14Xaxis, ch14Yaxis, ch14Zaxis, ch15Xaxis, ch15Yaxis, ch15Zaxis,
            ch16Xaxis, ch16Yaxis, ch16Zaxis;

    private double series_Xaxis_Ch1, series_Xaxis_Ch2, series_Xaxis_Ch3, series_Xaxis_Ch4, series_Xaxis_Ch5,
            series_Xaxis_Ch6, series_Xaxis_Ch7, series_Xaxis_Ch8, series_Xaxis_Ch9, series_Xaxis_Ch10,
            series_Xaxis_Ch11, series_Xaxis_Ch12, series_Xaxis_Ch13, series_Xaxis_Ch14, series_Xaxis_Ch15, series_Xaxis_Ch16;

    private double series_Yaxis_Ch1, series_Yaxis_Ch2, series_Yaxis_Ch3, series_Yaxis_Ch4, series_Yaxis_Ch5,
            series_Yaxis_Ch6, series_Yaxis_Ch7, series_Yaxis_Ch8, series_Yaxis_Ch9, series_Yaxis_Ch10,
            series_Yaxis_Ch11, series_Yaxis_Ch12, series_Yaxis_Ch13, series_Yaxis_Ch14, series_Yaxis_Ch15, series_Yaxis_Ch16;

    private double series_Zaxis_Ch1, series_Zaxis_Ch2, series_Zaxis_Ch3, series_Zaxis_Ch4, series_Zaxis_Ch5, series_Zaxis_Ch6,
            series_Zaxis_Ch7, series_Zaxis_Ch8, series_Zaxis_Ch9, series_Zaxis_Ch10, series_Zaxis_Ch11, series_Zaxis_Ch12,
            series_Zaxis_Ch13, series_Zaxis_Ch14, series_Zaxis_Ch15, series_Zaxis_Ch16;

    private double xdata = 0.0;


    private boolean Allchannels = false, ch1 = false;


    private boolean plotData = true, allaxis = false, plotxaxis = false, plotyaxis = false, plotzaxis = false;


    public TerminalFragment() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Constants.INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
                    Boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                    connect(granted);
                }
            }
        };
    }


    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceId = getArguments().getInt("device");
        portNum = getArguments().getInt("port");
        baudRate = getArguments().getInt("baud");
    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if (service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation")
    // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try {
            getActivity().unbindService(this);
        } catch (Exception ignored) {
        }
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(Constants.INTENT_ACTION_GRANT_USB));
        if (initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
        if (controlLinesEnabled && controlLines != null && connected == Connected.True)
            controlLines.start();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(broadcastReceiver);
        if (controlLines != null) {
            controlLines.stop();
        }
        if (Plotthread != null) {
            Plotthread.interrupt();
        }
        super.onPause();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if (initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // On selecting a spinner item
        String item = parent.getItemAtPosition(position).toString();


        //GraphView
        if (item.equals("3-Axis")) {

            mChart1.setVisibility(View.VISIBLE);
            mChart2.setVisibility(View.VISIBLE);
            mChart3.setVisibility(View.VISIBLE);

            Lin1.setVisibility(View.VISIBLE);
            Lin2.setVisibility(View.VISIBLE);
            Lin3.setVisibility(View.VISIBLE);
            allaxis = true;

            mChart1.clearValues();
            mChart2.clearValues();

            mChart1.clearAllViewportJobs();
            mChart2.clearAllViewportJobs();

        } else if (item.equals("X-Axis")) {
            mChart1.setVisibility(View.VISIBLE);
            mChart2.setVisibility(View.GONE);
            mChart3.setVisibility(View.GONE);

            Lin1.setVisibility(View.VISIBLE);
            Lin2.setVisibility(View.GONE);
            Lin3.setVisibility(View.GONE);
            plotxaxis = true;
            plotyaxis = false;
            plotzaxis = false;
            allaxis = false;

            mChart1.clearValues();
            mChart2.clearValues();

            mChart1.clearAllViewportJobs();
            mChart2.clearAllViewportJobs();

        } else if (item.equals("Y-Axis")) {
            mChart1.setVisibility(View.GONE);
            mChart2.setVisibility(View.VISIBLE);
            mChart3.setVisibility(View.GONE);

            Lin1.setVisibility(View.GONE);
            Lin2.setVisibility(View.VISIBLE);

            Lin3.setVisibility(View.GONE);
            plotxaxis = false;
            plotyaxis = true;
            plotzaxis = false;
            allaxis = false;

            mChart1.clearValues();
            mChart2.clearValues();

            mChart1.clearAllViewportJobs();
            mChart2.clearAllViewportJobs();


        } else if (item.equals("Z-Axis")) {

            mChart1.setVisibility(View.GONE);
            mChart2.setVisibility(View.GONE);
            mChart3.setVisibility(View.VISIBLE);

            Lin1.setVisibility(View.GONE);
            Lin2.setVisibility(View.GONE);
            Lin3.setVisibility(View.VISIBLE);
            plotxaxis = false;
            plotyaxis = false;
            plotzaxis = true;
            allaxis = false;
        }


        //Plot Graph

        if (item.contains("All channels")) {
            Allchannels = true;
            ch1 = false;
            Toast.makeText(getActivity(), "All Channels", Toast.LENGTH_LONG).show();
        } else if (item.contains("Channel1")) {
            Allchannels = false;
            ch1 = true;
            Toast.makeText(getActivity(), "Channel1", Toast.LENGTH_LONG).show();
        }


    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }


    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);

        //Stop Screen-off
        if (keepScreenOn)
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        TVUSBStatus = view.findViewById(R.id.mTVUSBStatus);
        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());

        sendText = view.findViewById(R.id.send_text);
        hexWatcher = new TextUtil.HexWatcher(sendText);
        hexWatcher.enable(hexEnabled);
        sendText.addTextChangedListener(hexWatcher);
        sendText.setHint(hexEnabled ? "HEX mode" : "");

        View sendBtn = view.findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(v -> send(sendText.getText().toString()));


        Axismode = (Spinner) view.findViewById(R.id.spn_Axismode);
        Axismode.setOnItemSelectedListener(this);
        ArrayAdapter<String> dataAdapter_axismode = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, Axismodes);
        dataAdapter_axismode.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Axismode.setAdapter(dataAdapter_axismode);


        channel = (Spinner) view.findViewById(R.id.spn_channels);
        channel.setOnItemSelectedListener(this);

        ArrayAdapter<String> dataAdapter_channels = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, Channels);
        dataAdapter_channels.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        channel.setAdapter(dataAdapter_channels);


        Lin1 = (LinearLayout) view.findViewById(R.id.l1);
        Lin2 = (LinearLayout) view.findViewById(R.id.l2);
        Lin3 = (LinearLayout) view.findViewById(R.id.l3);

        //start graph button
        View startgraphbutton = view.findViewById(R.id.Btn_startgraph);
        startgraphbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toast.makeText(getActivity(), "Start graph", Toast.LENGTH_LONG).show();
                graphvaluesread = true;
                Axismode.setEnabled(false);
                channel.setEnabled(false);

                mChart1.clearValues();
                mChart2.clearValues();

                mChart1.clearAllViewportJobs();
                mChart2.clearAllViewportJobs();
                mChart1.invalidate();
                mChart2.invalidate();


            }
        });

        //Stop graph button
        View stopgraphbutton = view.findViewById(R.id.Btn_Stopgraph);
        stopgraphbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(getActivity(), "Stop graph", Toast.LENGTH_LONG).show();
                graphvaluesread = false;
                Axismode.setEnabled(true);
                channel.setEnabled(true);
                /*if (Plotthread != null) {

                    Plotthread.stop();
                    handler.removeCallbacks(Plotthread);
                }*/
                // Plotthread.interrupt();
            }
        });


        //graph View  for x,y,z axis

        //x-axis
        mChart1 = (LineChart) view.findViewById(R.id.chart1);
        // enable description text
        mChart1.getDescription().setEnabled(true);
        // enable touch gestures
        mChart1.setTouchEnabled(false);
        // enable scaling and dragging
        mChart1.setDragEnabled(true);
        mChart1.setScaleEnabled(true);
        mChart1.setDrawGridBackground(true);
        mChart1.getAxisRight().setDrawGridLines(true);
        // if disabled, scaling can be done on x- and y-axis separately
        mChart1.setPinchZoom(true);
        // set an alternative background color
        mChart1.setBackgroundColor(Color.rgb(192, 192, 192));

        LineData data = new LineData();
        data.setValueTextColor(Color.BLACK);
        // add empty data
        mChart1.setData(data);
        mChart1.setGridBackgroundColor(Color.rgb(192, 192, 192));
        // get the legend (only possible after setting data)
        Legend l = mChart1.getLegend();
        // modify the legend ...
        l.setForm(Legend.LegendForm.LINE);
        // l.setTextColor(Color.RED);
        XAxis xl = mChart1.getXAxis();
        xl.setTextColor(Color.rgb(192, 192, 192));
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);
        xl.setGridColor(Color.BLACK);
        YAxis leftAxis = mChart1.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMaximum(1000);
        leftAxis.setAxisMinimum(-1000);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChart1.getAxisRight();
        rightAxis.setEnabled(false);
        rightAxis.setDrawGridLines(true);

        mChart1.getAxisLeft().setDrawGridLines(false);
        mChart1.getXAxis().setDrawGridLines(false);
        mChart1.setDrawBorders(true);


        //Y-axis
        mChart2 = (LineChart) view.findViewById(R.id.chart2);
        // enable description text
        mChart2.getDescription().setEnabled(true);
        // enable touch gestures
        mChart2.setTouchEnabled(false);
        // enable scaling and dragging
        mChart2.setDragEnabled(true);
        mChart2.setScaleEnabled(true);
        mChart2.setDrawGridBackground(true);
        mChart2.getAxisRight().setDrawGridLines(true);
        // if disabled, scaling can be done on x- and y-axis separately
        mChart2.setPinchZoom(true);
        // set an alternative background color
        mChart2.setBackgroundColor(Color.rgb(192, 192, 192));

        LineData data1 = new LineData();
        data1.setValueTextColor(Color.BLACK);
        // add empty data
        mChart2.setData(data);
        mChart2.setGridBackgroundColor(Color.rgb(192, 192, 192));
        // get the legend (only possible after setting data)
        Legend l1 = mChart2.getLegend();
        // modify the legend ...
        l1.setForm(Legend.LegendForm.LINE);
        // l.setTextColor(Color.RED);
        XAxis xl1 = mChart2.getXAxis();
        xl1.setTextColor(Color.rgb(192, 192, 192));
        xl1.setDrawGridLines(false);
        xl1.setAvoidFirstLastClipping(true);
        xl1.setEnabled(true);
        xl1.setGridColor(Color.BLACK);
        YAxis leftAxis1 = mChart2.getAxisLeft();
        leftAxis1.setTextColor(Color.BLACK);
        leftAxis1.setDrawGridLines(false);
        leftAxis1.setAxisMaximum(1000);
        leftAxis1.setAxisMinimum(-1000);
        leftAxis1.setDrawGridLines(false);

        YAxis rightAxis1 = mChart2.getAxisRight();
        rightAxis1.setEnabled(false);

        mChart2.getAxisLeft().setDrawGridLines(false);
        mChart2.getXAxis().setDrawGridLines(false);
        mChart2.setDrawBorders(true);

        //z-axis
        mChart3 = (LineChart) view.findViewById(R.id.chart3);
        // enable description text
        mChart3.getDescription().setEnabled(true);
        // enable touch gestures
        mChart3.setTouchEnabled(false);
        // enable scaling and dragging
        mChart3.setDragEnabled(true);
        mChart3.setScaleEnabled(true);
        mChart3.setDrawGridBackground(true);
        mChart3.getAxisRight().setDrawGridLines(true);
        // if disabled, scaling can be done on x- and y-axis separately
        mChart3.setPinchZoom(true);


        // set an alternative background color
        mChart3.setBackgroundColor(Color.rgb(192, 192, 192));

        LineData data2 = new LineData();
        data2.setValueTextColor(Color.BLACK);
        // add empty data
        mChart3.setData(data);
        mChart3.setGridBackgroundColor(Color.rgb(192, 192, 192));
        // get the legend (only possible after setting data)
        Legend l2 = mChart3.getLegend();
        // modify the legend ...
        l2.setForm(Legend.LegendForm.LINE);
        // l.setTextColor(Color.RED);
        XAxis xl2 = mChart3.getXAxis();
        xl2.setTextColor(Color.rgb(192, 192, 192));
        xl2.setDrawGridLines(false);
        xl2.setAvoidFirstLastClipping(true);
        xl2.setEnabled(true);
        xl2.setGridColor(Color.BLACK);
        YAxis leftAxis2 = mChart3.getAxisLeft();
        leftAxis2.setTextColor(Color.BLACK);
        leftAxis2.setDrawGridLines(false);
        leftAxis2.setAxisMaximum(1000);
        leftAxis2.setAxisMinimum(-1000);
        leftAxis2.setDrawGridLines(false);

        YAxis rightAxis2 = mChart3.getAxisRight();
        rightAxis2.setEnabled(false);

        mChart3.getAxisLeft().setDrawGridLines(false);
        mChart3.getXAxis().setDrawGridLines(false);
        mChart3.setDrawBorders(true);

        feedMultiple();

        handler.postDelayed(runnable = new Runnable() {
            public void run() {
                handler.postDelayed(runnable, delay);
                if (graphvaluesread == true) {
                    Log.d("Terminal", "graph start");
                    receiveText.setText("");
                    send("mea");

                   /* if (plotData) {
                        addEntry();
                        addEntry1();
                        //addEntry1();
                        plotData = false;
                    }*/
                    xdata++;
                } else {
                    Log.d("Terminal", "graph stop");
                }
            }
        }, 10);

        handler1.postDelayed(runnable1 = new Runnable() {
            public void run() {
                handler1.postDelayed(runnable1, delay);
                if (graphvaluesread == true) {


                    if (plotData) {
                        if (allaxis == true) {
                            addEntry();
                            addEntry1();
                        } else if (plotxaxis == true) {
                            addEntry();
                        }
                        if (plotyaxis == true) {
                            addEntry1();
                        }

                        if (plotzaxis == true) {
                            addEntry2();
                        }


                        if (allaxis == true || plotxaxis == true && ch1 == true) {

                            addEntrychanne1();
                        }

                        //addEntry1();
                        //addEntry1();
                        plotData = false;
                    }


                } else {
                    Log.d("Terminal", "graph stop");
                }
            }
        }, 10);


        controlLines = new ControlLines(view);
        return view;
    }


    private void addEntry() {


        if (allaxis == true || plotxaxis == true && Allchannels == true) {
            LineData data = mChart1.getData();
            if (data != null) {
                //x-axis
                ILineDataSet set1 = data.getDataSetByIndex(0);
                ILineDataSet set2 = data.getDataSetByIndex(1);
                ILineDataSet set3 = data.getDataSetByIndex(2);
                ILineDataSet set4 = data.getDataSetByIndex(3);
                ILineDataSet set5 = data.getDataSetByIndex(4);
                ILineDataSet set6 = data.getDataSetByIndex(5);
                ILineDataSet set7 = data.getDataSetByIndex(6);
                ILineDataSet set8 = data.getDataSetByIndex(7);
                ILineDataSet set9 = data.getDataSetByIndex(8);
                ILineDataSet set10 = data.getDataSetByIndex(9);
                ILineDataSet set11 = data.getDataSetByIndex(10);
                ILineDataSet set12 = data.getDataSetByIndex(11);
                ILineDataSet set13 = data.getDataSetByIndex(12);
                ILineDataSet set14 = data.getDataSetByIndex(13);
                ILineDataSet set15 = data.getDataSetByIndex(14);
                ILineDataSet set16 = data.getDataSetByIndex(15);
                if (set1 == null) {
                    //x-axis
                    set1 = createSet1();
                    set2 = createSet2();
                    set3 = createSet3();
                    set4 = createSet4();
                    set5 = createSet5();
                    set6 = createSet6();
                    set7 = createSet7();
                    set8 = createSet8();
                    set9 = createSet9();
                    set10 = createSet10();
                    set11 = createSet11();
                    set12 = createSet12();
                    set13 = createSet13();
                    set14 = createSet14();
                    set15 = createSet15();
                    set16 = createSet16();
                    //x-axis
                    data.addDataSet(set1);
                    data.addDataSet(set2);
                    data.addDataSet(set3);
                    data.addDataSet(set4);
                    data.addDataSet(set5);
                    data.addDataSet(set6);
                    data.addDataSet(set7);
                    data.addDataSet(set8);
                    data.addDataSet(set9);
                    data.addDataSet(set10);
                    data.addDataSet(set11);
                    data.addDataSet(set12);
                    data.addDataSet(set13);
                    data.addDataSet(set14);
                    data.addDataSet(set15);
                    data.addDataSet(set16);
                }
                //x-axis
                data.addEntry(new Entry(set1.getEntryCount(), ch1Xaxis), 0);
                data.addEntry(new Entry(set2.getEntryCount(), ch2Xaxis), 1);
                data.addEntry(new Entry(set3.getEntryCount(), ch3Xaxis), 2);
                data.addEntry(new Entry(set4.getEntryCount(), ch4Xaxis), 3);
                data.addEntry(new Entry(set5.getEntryCount(), ch5Xaxis), 4);
                data.addEntry(new Entry(set6.getEntryCount(), ch6Xaxis), 5);
                data.addEntry(new Entry(set7.getEntryCount(), ch7Xaxis), 6);
                data.addEntry(new Entry(set8.getEntryCount(), ch8Xaxis), 7);
                data.addEntry(new Entry(set9.getEntryCount(), ch9Xaxis), 8);
                data.addEntry(new Entry(set10.getEntryCount(), ch10Xaxis), 9);
                data.addEntry(new Entry(set11.getEntryCount(), ch11Xaxis), 10);
                data.addEntry(new Entry(set12.getEntryCount(), ch12Xaxis), 11);
                data.addEntry(new Entry(set13.getEntryCount(), ch13Xaxis), 12);
                data.addEntry(new Entry(set14.getEntryCount(), ch14Xaxis), 13);
                data.addEntry(new Entry(set15.getEntryCount(), ch15Xaxis), 14);
                data.addEntry(new Entry(set16.getEntryCount(), ch16Xaxis), 15);
                data.notifyDataChanged();
                // let the chart know it's data has changed
                mChart1.notifyDataSetChanged();
                // limit the number of visible entries
                mChart1.setVisibleXRangeMaximum(100);
                // mChart.setVisibleYRange(30, AxisDependency.LEFT);
                // move to the latest entry
                mChart1.moveViewToX(data.getEntryCount());

            }
        }
    }

    private void addEntrychanne1() {
        mChart1.clearAllViewportJobs();
        LineData datach1 = mChart1.getData();
        if (datach1 != null) {
            //x-axis
            ILineDataSet set1 = datach1.getDataSetByIndex(0);
            if (set1 == null) {
                //x-axis
                set1 = createSet1();
            }

        /*    mChart1.notifyDataSetChanged();
            mChart1.invalidate();
            mChart1.clearValues();*/

            datach1.setDrawValues(true);
            //x-axis
            datach1.addEntry(new Entry(set1.getEntryCount(), ch1Xaxis), 0);
            datach1.notifyDataChanged();
            // let the chart know it's data has changed
            mChart1.notifyDataSetChanged();
            // limit the number of visible entries
            mChart1.setVisibleXRangeMaximum(100);
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);
            // move to the latest entry
            mChart1.moveViewToX(datach1.getEntryCount());
        }
    }


    private void addEntry1() {


        LineData data1 = mChart2.getData();


        if (data1 != null) {
            //Y-axis
            ILineDataSet set17 = data1.getDataSetByIndex(0);
            ILineDataSet set18 = data1.getDataSetByIndex(1);
            ILineDataSet set19 = data1.getDataSetByIndex(2);
            ILineDataSet set20 = data1.getDataSetByIndex(3);
            ILineDataSet set21 = data1.getDataSetByIndex(4);
            ILineDataSet set22 = data1.getDataSetByIndex(5);
            ILineDataSet set23 = data1.getDataSetByIndex(6);
            ILineDataSet set24 = data1.getDataSetByIndex(7);
            ILineDataSet set25 = data1.getDataSetByIndex(8);
            ILineDataSet set26 = data1.getDataSetByIndex(9);
            ILineDataSet set27 = data1.getDataSetByIndex(10);
            ILineDataSet set28 = data1.getDataSetByIndex(11);
            ILineDataSet set29 = data1.getDataSetByIndex(12);
            ILineDataSet set30 = data1.getDataSetByIndex(13);
            ILineDataSet set31 = data1.getDataSetByIndex(14);
            ILineDataSet set32 = data1.getDataSetByIndex(15);


            if (set17 == null) {
                //Y-axis
                set17 = createSet17();
                set18 = createSet18();
                set19 = createSet19();
                set20 = createSet20();
                set21 = createSet21();
                set22 = createSet22();
                set23 = createSet23();
                set24 = createSet24();
                set25 = createSet25();
                set26 = createSet26();
                set27 = createSet27();
                set28 = createSet28();
                set29 = createSet29();
                set30 = createSet30();
                set31 = createSet31();
                set32 = createSet32();


                //Y-axis
                data1.addDataSet(set17);
                data1.addDataSet(set18);
                data1.addDataSet(set19);
                data1.addDataSet(set20);
                data1.addDataSet(set21);
                data1.addDataSet(set22);
                data1.addDataSet(set23);
                data1.addDataSet(set24);
                data1.addDataSet(set25);
                data1.addDataSet(set26);
                data1.addDataSet(set27);
                data1.addDataSet(set28);
                data1.addDataSet(set29);
                data1.addDataSet(set30);
                data1.addDataSet(set31);
                data1.addDataSet(set32);
            }

//            data.addEntry(new Entry(set.getEntryCount(), (float) (Math.random() * 80) + 10f), 0);


            //Y-axis
            data1.addEntry(new Entry(set17.getEntryCount(), ch1Yaxis), 0);
            data1.addEntry(new Entry(set18.getEntryCount(), ch2Yaxis), 1);
            data1.addEntry(new Entry(set19.getEntryCount(), ch3Yaxis), 2);
            data1.addEntry(new Entry(set20.getEntryCount(), ch4Yaxis), 3);
            data1.addEntry(new Entry(set21.getEntryCount(), ch5Yaxis), 4);
            data1.addEntry(new Entry(set22.getEntryCount(), ch6Yaxis), 5);
            data1.addEntry(new Entry(set23.getEntryCount(), ch7Yaxis), 6);
            data1.addEntry(new Entry(set24.getEntryCount(), ch8Yaxis), 7);
            data1.addEntry(new Entry(set25.getEntryCount(), ch9Yaxis), 8);
            data1.addEntry(new Entry(set26.getEntryCount(), ch10Yaxis), 9);
            data1.addEntry(new Entry(set27.getEntryCount(), ch11Yaxis), 10);
            data1.addEntry(new Entry(set28.getEntryCount(), ch12Yaxis), 11);
            data1.addEntry(new Entry(set29.getEntryCount(), ch13Yaxis), 12);
            data1.addEntry(new Entry(set30.getEntryCount(), ch14Yaxis), 13);
            data1.addEntry(new Entry(set31.getEntryCount(), ch15Yaxis), 14);
            data1.addEntry(new Entry(set32.getEntryCount(), ch16Yaxis), 15);


            data1.notifyDataChanged();
            // let the chart know it's data has changed

            mChart2.notifyDataSetChanged();
            // limit the number of visible entries

            mChart2.setVisibleXRangeMaximum(100);
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);
            // move to the latest entry

            mChart2.moveViewToX(data1.getEntryCount());


        }
    }


    private void addEntry2() {


        LineData data2 = mChart3.getData();


        if (data2 != null) {
            //Y-axis
            ILineDataSet set17 = data2.getDataSetByIndex(0);
            ILineDataSet set18 = data2.getDataSetByIndex(1);
            ILineDataSet set19 = data2.getDataSetByIndex(2);
            ILineDataSet set20 = data2.getDataSetByIndex(3);
            ILineDataSet set21 = data2.getDataSetByIndex(4);
            ILineDataSet set22 = data2.getDataSetByIndex(5);
            ILineDataSet set23 = data2.getDataSetByIndex(6);
            ILineDataSet set24 = data2.getDataSetByIndex(7);
            ILineDataSet set25 = data2.getDataSetByIndex(8);
            ILineDataSet set26 = data2.getDataSetByIndex(9);
            ILineDataSet set27 = data2.getDataSetByIndex(10);
            ILineDataSet set28 = data2.getDataSetByIndex(11);
            ILineDataSet set29 = data2.getDataSetByIndex(12);
            ILineDataSet set30 = data2.getDataSetByIndex(13);
            ILineDataSet set31 = data2.getDataSetByIndex(14);
            ILineDataSet set32 = data2.getDataSetByIndex(15);


            if (set17 == null) {
                //Y-axis
                set17 = createSet17();
                set18 = createSet18();
                set19 = createSet19();
                set20 = createSet20();
                set21 = createSet21();
                set22 = createSet22();
                set23 = createSet23();
                set24 = createSet24();
                set25 = createSet25();
                set26 = createSet26();
                set27 = createSet27();
                set28 = createSet28();
                set29 = createSet29();
                set30 = createSet30();
                set31 = createSet31();
                set32 = createSet32();


                //Y-axis
                data2.addDataSet(set17);
                data2.addDataSet(set18);
                data2.addDataSet(set19);
                data2.addDataSet(set20);
                data2.addDataSet(set21);
                data2.addDataSet(set22);
                data2.addDataSet(set23);
                data2.addDataSet(set24);
                data2.addDataSet(set25);
                data2.addDataSet(set26);
                data2.addDataSet(set27);
                data2.addDataSet(set28);
                data2.addDataSet(set29);
                data2.addDataSet(set30);
                data2.addDataSet(set31);
                data2.addDataSet(set32);
            }

//            data.addEntry(new Entry(set.getEntryCount(), (float) (Math.random() * 80) + 10f), 0);


            //Y-axis
            data2.addEntry(new Entry(set17.getEntryCount(), ch1Zaxis), 0);
            data2.addEntry(new Entry(set18.getEntryCount(), ch2Zaxis), 1);
            data2.addEntry(new Entry(set19.getEntryCount(), ch3Zaxis), 2);
            data2.addEntry(new Entry(set20.getEntryCount(), ch4Zaxis), 3);
            data2.addEntry(new Entry(set21.getEntryCount(), ch5Zaxis), 4);
            data2.addEntry(new Entry(set22.getEntryCount(), ch6Zaxis), 5);
            data2.addEntry(new Entry(set23.getEntryCount(), ch7Zaxis), 6);
            data2.addEntry(new Entry(set24.getEntryCount(), ch8Zaxis), 7);
            data2.addEntry(new Entry(set25.getEntryCount(), ch9Zaxis), 8);
            data2.addEntry(new Entry(set26.getEntryCount(), ch10Zaxis), 9);
            data2.addEntry(new Entry(set27.getEntryCount(), ch11Zaxis), 10);
            data2.addEntry(new Entry(set28.getEntryCount(), ch12Zaxis), 11);
            data2.addEntry(new Entry(set29.getEntryCount(), ch13Zaxis), 12);
            data2.addEntry(new Entry(set30.getEntryCount(), ch14Zaxis), 13);
            data2.addEntry(new Entry(set31.getEntryCount(), ch15Zaxis), 14);
            data2.addEntry(new Entry(set32.getEntryCount(), ch16Zaxis), 15);


            data2.notifyDataChanged();
            // let the chart know it's data has changed

            mChart3.notifyDataSetChanged();
            // limit the number of visible entries

            mChart3.setVisibleXRangeMaximum(100);
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);
            // move to the latest entry

            mChart3.moveViewToX(data2.getEntryCount());


        }
    }


    private LineDataSet createSet1() {
        //x axis channel1
        LineDataSet set = new LineDataSet(null, "Ch1");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.RED);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;
    }

    private LineDataSet createSet2() {

        //x axis channel1
        LineDataSet set = new LineDataSet(null, "Ch2");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.GREEN);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;


    }

    private LineDataSet createSet3() {

        //x axis channel1
        LineDataSet set = new LineDataSet(null, "ch3");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.BLUE);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;


    }

    private LineDataSet createSet4() {

        //x axis channel1
        LineDataSet set = new LineDataSet(null, "ch4");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.BLACK);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;


    }

    private LineDataSet createSet5() {

        //x axis channel1
        LineDataSet set = new LineDataSet(null, "ch5");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.YELLOW);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;


    }

    private LineDataSet createSet6() {

        //x axis channel1
        LineDataSet set = new LineDataSet(null, "ch6");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.rgb(75, 0, 130));
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;


    }

    private LineDataSet createSet7() {

        //x axis channel1
        LineDataSet set = new LineDataSet(null, "ch7");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.rgb(144, 238, 144));
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;


    }

    private LineDataSet createSet8() {

        //x axis channel1
        LineDataSet set = new LineDataSet(null, "ch8");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.rgb(255, 140, 0));
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;


    }

    private LineDataSet createSet9() {

        //x axis channel1
        LineDataSet set = new LineDataSet(null, "ch9");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.rgb(255, 165, 0));
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;


    }

    private LineDataSet createSet10() {

        //x axis channel1
        LineDataSet set = new LineDataSet(null, "ch10");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.rgb(0, 255, 255));
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;


    }

    private LineDataSet createSet11() {

        //x axis channel1
        LineDataSet set = new LineDataSet(null, "ch11");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.rgb(128, 128, 0));
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;


    }

    private LineDataSet createSet12() {

        //x axis channel1
        LineDataSet set = new LineDataSet(null, "ch12");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.rgb(255, 127, 80));
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;


    }

    private LineDataSet createSet13() {

        //x axis channel1
        LineDataSet set = new LineDataSet(null, "ch13");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.rgb(147, 112, 219));
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;


    }

    private LineDataSet createSet14() {

        //x axis channel1
        LineDataSet set = new LineDataSet(null, "ch14");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.rgb(128, 0, 0));
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;


    }

    private LineDataSet createSet15() {

        //x axis channel1
        LineDataSet set = new LineDataSet(null, "ch15");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.rgb(105, 105, 105));
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;


    }

    private LineDataSet createSet16() {

        //x axis channel1
        LineDataSet set = new LineDataSet(null, "ch16");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.rgb(135, 206, 250));
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;


    }

    private LineDataSet createSet17() {
        //x axis channel1
        LineDataSet set1 = new LineDataSet(null, "ch1");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setLineWidth(3f);
        set1.setColor(Color.RED);
        set1.setHighlightEnabled(false);
        set1.setDrawValues(false);
        set1.setDrawCircles(false);
        set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set1.setCubicIntensity(0.2f);
        return set1;
    }

    private LineDataSet createSet18() {

        //x axis channel1
        LineDataSet set1 = new LineDataSet(null, "ch2");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setLineWidth(3f);
        set1.setColor(Color.GREEN);
        set1.setHighlightEnabled(false);
        set1.setDrawValues(false);
        set1.setDrawCircles(false);
        set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set1.setCubicIntensity(0.2f);
        return set1;


    }

    private LineDataSet createSet19() {

        //x axis channel1
        LineDataSet set1 = new LineDataSet(null, "ch3");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setLineWidth(3f);
        set1.setColor(Color.BLUE);
        set1.setHighlightEnabled(false);
        set1.setDrawValues(false);
        set1.setDrawCircles(false);
        set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set1.setCubicIntensity(0.2f);
        return set1;


    }

    private LineDataSet createSet20() {

        //x axis channel1
        LineDataSet set1 = new LineDataSet(null, "ch4");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setLineWidth(3f);
        set1.setColor(Color.BLACK);
        set1.setHighlightEnabled(false);
        set1.setDrawValues(false);
        set1.setDrawCircles(false);
        set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set1.setCubicIntensity(0.2f);
        return set1;


    }

    private LineDataSet createSet21() {

        //x axis channel1
        LineDataSet set1 = new LineDataSet(null, "ch5");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setLineWidth(3f);
        set1.setColor(Color.YELLOW);
        set1.setHighlightEnabled(false);
        set1.setDrawValues(false);
        set1.setDrawCircles(false);
        set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set1.setCubicIntensity(0.2f);
        return set1;


    }

    private LineDataSet createSet22() {

        //x axis channel1
        LineDataSet set1 = new LineDataSet(null, "ch6");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setLineWidth(3f);
        set1.setColor(Color.rgb(75, 0, 130));
        set1.setHighlightEnabled(false);
        set1.setDrawValues(false);
        set1.setDrawCircles(false);
        set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set1.setCubicIntensity(0.2f);
        return set1;


    }

    private LineDataSet createSet23() {

        //x axis channel1
        LineDataSet set1 = new LineDataSet(null, "ch7");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setLineWidth(3f);
        set1.setColor(Color.rgb(144, 238, 144));
        set1.setHighlightEnabled(false);
        set1.setDrawValues(false);
        set1.setDrawCircles(false);
        set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set1.setCubicIntensity(0.2f);
        return set1;


    }

    private LineDataSet createSet24() {

        //x axis channel1
        LineDataSet set1 = new LineDataSet(null, "ch8");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setLineWidth(3f);
        set1.setColor(Color.rgb(255, 140, 0));
        set1.setHighlightEnabled(false);
        set1.setDrawValues(false);
        set1.setDrawCircles(false);
        set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set1.setCubicIntensity(0.2f);
        return set1;


    }

    private LineDataSet createSet25() {

        //x axis channel1
        LineDataSet set1 = new LineDataSet(null, "ch9");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setLineWidth(3f);
        set1.setColor(Color.rgb(255, 165, 0));
        set1.setHighlightEnabled(false);
        set1.setDrawValues(false);
        set1.setDrawCircles(false);
        set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set1.setCubicIntensity(0.2f);
        return set1;


    }

    private LineDataSet createSet26() {

        //x axis channel1
        LineDataSet set1 = new LineDataSet(null, "ch10");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setLineWidth(3f);
        set1.setColor(Color.rgb(0, 255, 255));
        set1.setHighlightEnabled(false);
        set1.setDrawValues(false);
        set1.setDrawCircles(false);
        set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set1.setCubicIntensity(0.2f);
        return set1;


    }

    private LineDataSet createSet27() {

        //x axis channel1
        LineDataSet set1 = new LineDataSet(null, "ch11");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setLineWidth(3f);
        set1.setColor(Color.rgb(128, 128, 0));
        set1.setHighlightEnabled(false);
        set1.setDrawValues(false);
        set1.setDrawCircles(false);
        set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set1.setCubicIntensity(0.2f);
        return set1;


    }

    private LineDataSet createSet28() {

        //x axis channel1
        LineDataSet set1 = new LineDataSet(null, "ch12");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setLineWidth(3f);
        set1.setColor(Color.rgb(255, 127, 80));
        set1.setHighlightEnabled(false);
        set1.setDrawValues(false);
        set1.setDrawCircles(false);
        set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set1.setCubicIntensity(0.2f);
        return set1;


    }

    private LineDataSet createSet29() {

        //x axis channel1
        LineDataSet set1 = new LineDataSet(null, "ch13");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setLineWidth(3f);
        set1.setColor(Color.rgb(147, 112, 219));
        set1.setHighlightEnabled(false);
        set1.setDrawValues(false);
        set1.setDrawCircles(false);
        set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set1.setCubicIntensity(0.2f);
        return set1;


    }

    private LineDataSet createSet30() {

        //x axis channel1
        LineDataSet set1 = new LineDataSet(null, "ch14");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setLineWidth(3f);
        set1.setColor(Color.rgb(128, 0, 0));
        set1.setHighlightEnabled(false);
        set1.setDrawValues(false);
        set1.setDrawCircles(false);
        set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set1.setCubicIntensity(0.2f);
        return set1;


    }

    private LineDataSet createSet31() {

        //x axis channel1
        LineDataSet set1 = new LineDataSet(null, "ch15");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setLineWidth(3f);
        set1.setColor(Color.rgb(105, 105, 105));
        set1.setHighlightEnabled(false);
        set1.setDrawValues(false);
        set1.setDrawCircles(false);
        set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set1.setCubicIntensity(0.2f);
        return set1;

    }

    private LineDataSet createSet32() {

        //x axis channel1
        LineDataSet set1 = new LineDataSet(null, "ch16");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setLineWidth(3f);
        set1.setColor(Color.rgb(135, 206, 250));
        set1.setHighlightEnabled(false);
        set1.setDrawValues(false);
        set1.setDrawCircles(false);
        set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set1.setCubicIntensity(0.2f);
        return set1;

    }


    private void feedMultiple() {

        if (Plotthread != null) {
            Plotthread.interrupt();
        }

        Plotthread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    plotData = true;
                    try {
                        Thread.sleep(0);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        });

        Plotthread.start();
    }

    /*private void feedMultiple1() {

        if (Receivethread != null) {
            Receivethread.interrupt();
        }

        Receivethread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    if (plotData) {
                        addEntry();
                        addEntry1();
                        //addEntry1();
                        plotData = false;
                    }
                }
            }
        });

        Plotthread.start();
    }*/


    /*
     * Serial + UI
     */
    private void connect() {
        connect(null);
    }

    @SuppressLint("ResourceAsColor")
    private void connect(Boolean permissionGranted) {
        UsbDevice device = null;
        UsbManager usbManager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);
        for (UsbDevice v : usbManager.getDeviceList().values())
            if (v.getDeviceId() == deviceId)
                device = v;
        if (device == null) {
            status("connection failed: device not found");
            TVUSBStatus.setText("Status:device not found");
            TVUSBStatus.setTextColor(getResources().getColor(R.color.colorStatusText));
            return;
        }
        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if (driver == null) {
            driver = CustomProber.getCustomProber().probeDevice(device);
            TVUSBStatus.setText("Status:" + driver.toString());
            TVUSBStatus.setTextColor(getResources().getColor(R.color.colorStatusText));
        }
        if (driver == null) {
            status("connection failed: no driver for device");
            TVUSBStatus.setText("Status:no driver for device");
            TVUSBStatus.setTextColor(getResources().getColor(R.color.colorStatusText));
            return;
        }
        if (driver.getPorts().size() < portNum) {
            status("connection failed: not enough ports at device");
            TVUSBStatus.setText("Status:not enough ports at device");
            TVUSBStatus.setTextColor(getResources().getColor(R.color.colorStatusText));
            return;
        }
        usbSerialPort = driver.getPorts().get(portNum);
        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());
        if (usbConnection == null && permissionGranted == null && !usbManager.hasPermission(driver.getDevice())) {
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(getActivity(), 0, new Intent(Constants.INTENT_ACTION_GRANT_USB), 0);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
            return;
        }
        if (usbConnection == null) {
            if (!usbManager.hasPermission(driver.getDevice())) {
                status("connection failed: permission denied");
                TVUSBStatus.setText("Status:permission denied");
                TVUSBStatus.setTextColor(getResources().getColor(R.color.colorStatusRed));
            } else {
                status("connection failed: open failed");
                TVUSBStatus.setText("Status:open failed");
                TVUSBStatus.setTextColor(getResources().getColor(R.color.colorStatusRed));

            }
            return;
        }

        connected = Connected.Pending;
        try {
            usbSerialPort.open(usbConnection);
            usbSerialPort.setParameters(baudRate, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), usbConnection, usbSerialPort);
            service.connect(socket);
            // usb connect is not asynchronous. connect-success and connect-error are returned immediately from socket.connect
            // for consistency to bluetooth/bluetooth-LE app use same SerialListener and SerialService classes
            onSerialConnect();
        } catch (Exception e) {
            onSerialConnectError(e);
            //Toast.makeText(getActivity(), "Exception1" + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void disconnect() {
        connected = Connected.False;
        controlLines.stop();
        service.disconnect();
        usbSerialPort = null;
    }

    @SuppressLint("ResourceAsColor")
    private void send(String str) {
        if (connected != Connected.True) {
            // Toast.makeText(getActivity(), "USB not connected", Toast.LENGTH_SHORT).show();
            TVUSBStatus.setText("Status:USB not connected");
            TVUSBStatus.setTextColor(getResources().getColor(R.color.colorStatusRed));

            Allchannels = false;
            graphvaluesread = false;
            return;
        }
        try {
            String msg;
            byte[] data;
            if (hexEnabled) {
                StringBuilder sb = new StringBuilder();
                TextUtil.toHexString(sb, TextUtil.fromHexString(str));

                TextUtil.toHexString(sb, newline.getBytes());
                msg = sb.toString();
                data = TextUtil.fromHexString(msg);
                //Toast.makeText(getActivity(), "if data :" + data, Toast.LENGTH_SHORT).show();
            } else {
                msg = str;
                data = (str + newline).getBytes();
            }
            SpannableStringBuilder spn = new SpannableStringBuilder(msg + '\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            service.write(data);
        } catch (SerialTimeoutException e) {
            status("write timeout: " + e.getMessage());

            //Toast.makeText(getActivity(), "SerialTimeoutException :" + e.toString(), Toast.LENGTH_SHORT).show();


        } catch (Exception e) {
            onSerialIoError(e);

            //Toast.makeText(getActivity(), "onSerialIoError :" + e.toString(), Toast.LENGTH_SHORT).show();

        }
    }

    private void receive(byte[] data) {
        try {


            if (hexEnabled) {
                //  receiveText.append(TextUtil.toHexString(data) + '\n');
                Toast.makeText(getActivity(), TextUtil.toHexString(data), Toast.LENGTH_LONG).show();
            } else {

                String msg = new String(data);
                if (newline.equals(TextUtil.newline_crlf) && msg.length() > 0) {
                    // don't show CR as ^M if directly before LF
                    msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);
                    // special handling if CR and LF come in separate fragments
                    if (pendingNewline && msg.charAt(0) == '\n') {
                        Editable edt = receiveText.getEditableText();
                        if (edt != null && edt.length() > 1)
                            edt.replace(edt.length() - 2, edt.length(), "");
                    }
                    pendingNewline = msg.charAt(msg.length() - 1) == '\r';
                }

                receiveText.append(msg);


                String readdata = receiveText.getText().toString();
                String finalReaddata = readdata.replace("mea|", "");
                String finalReaddata1 = finalReaddata.replace("mea", "");


                all48data = finalReaddata1.split(",");

                if (all48data.length != 0) {
                    //Toast.makeText(getActivity(), "all48data :" + all48data.length, Toast.LENGTH_LONG).show();

                  /*  //X-axis channels data
                    ch1Xaxis = Integer.parseInt(all48data[0]);
                    ch2Xaxis = Integer.parseInt(all48data[3]);
                    ch3Xaxis = Integer.parseInt(all48data[6]);
                    ch4Xaxis = Integer.parseInt(all48data[9]);
                    ch5Xaxis = Integer.parseInt(all48data[12]);
                    ch6Xaxis = Integer.parseInt(all48data[15]);
                    ch7Xaxis = Integer.parseInt(all48data[18]);
                    ch8Xaxis = Integer.parseInt(all48data[21]);

                    ch9Xaxis = Integer.parseInt(all48data[24]);
                    ch10Xaxis = Integer.parseInt(all48data[27]);
                    ch11Xaxis = Integer.parseInt(all48data[30]);
                    ch12Xaxis = Integer.parseInt(all48data[33]);
                    ch13Xaxis = Integer.parseInt(all48data[36]);
                    ch14Xaxis = Integer.parseInt(all48data[39]);
                    ch15Xaxis = Integer.parseInt(all48data[42]);
                    ch16Xaxis = Integer.parseInt(all48data[45]);*/


                    //channel1 data
                    ch1Xaxis = Integer.parseInt(all48data[0]);
                    ch1Yaxis = Integer.parseInt(all48data[1]);
                    ch1Zaxis = Integer.parseInt(all48data[2]);

                    //channel2 data
                    ch2Xaxis = Integer.parseInt(all48data[3]);
                    ch2Yaxis = Integer.parseInt(all48data[4]);
                    ch2Zaxis = Integer.parseInt(all48data[5]);

                    //channel3 data
                    ch3Xaxis = Integer.parseInt(all48data[6]);
                    ch3Yaxis = Integer.parseInt(all48data[7]);
                    ch3Zaxis = Integer.parseInt(all48data[8]);

                    //channel4 data
                    ch4Xaxis = Integer.parseInt(all48data[9]);
                    ch4Yaxis = Integer.parseInt(all48data[10]);
                    ch4Zaxis = Integer.parseInt(all48data[11]);

                    //channel5 data
                    ch5Xaxis = Integer.parseInt(all48data[12]);
                    ch5Yaxis = Integer.parseInt(all48data[13]);
                    ch5Zaxis = Integer.parseInt(all48data[14]);

                    //channel6 data
                    ch6Xaxis = Integer.parseInt(all48data[15]);
                    ch6Yaxis = Integer.parseInt(all48data[16]);
                    ch6Zaxis = Integer.parseInt(all48data[17]);

                    //channel7 data
                    ch7Xaxis = Integer.parseInt(all48data[18]);
                    ch7Yaxis = Integer.parseInt(all48data[19]);
                    ch7Zaxis = Integer.parseInt(all48data[20]);

                    //channel8 data
                    ch8Xaxis = Integer.parseInt(all48data[21]);
                    ch8Yaxis = Integer.parseInt(all48data[22]);
                    ch8Zaxis = Integer.parseInt(all48data[23]);


                    //channel9 data
                    ch9Xaxis = Integer.parseInt(all48data[24]);
                    ch9Yaxis = Integer.parseInt(all48data[25]);
                    ch9Zaxis = Integer.parseInt(all48data[26]);

                    //channel0 data
                    ch10Xaxis = Integer.parseInt(all48data[27]);
                    ch10Yaxis = Integer.parseInt(all48data[28]);
                    ch10Zaxis = Integer.parseInt(all48data[29]);

                    //channel11 data
                    ch11Xaxis = Integer.parseInt(all48data[20]);
                    ch11Yaxis = Integer.parseInt(all48data[31]);
                    ch11Zaxis = Integer.parseInt(all48data[32]);

                    //channel2 data
                    ch12Xaxis = Integer.parseInt(all48data[33]);
                    ch12Yaxis = Integer.parseInt(all48data[34]);
                    ch12Zaxis = Integer.parseInt(all48data[35]);

                    //channel13 data
                    ch13Xaxis = Integer.parseInt(all48data[36]);
                    ch13Yaxis = Integer.parseInt(all48data[37]);
                    ch13Zaxis = Integer.parseInt(all48data[38]);

                    //channel14 data
                    ch14Xaxis = Integer.parseInt(all48data[39]);
                    ch14Yaxis = Integer.parseInt(all48data[40]);
                    ch14Zaxis = Integer.parseInt(all48data[41]);

                    //channel15 data
                    ch15Xaxis = Integer.parseInt(all48data[42]);
                    ch15Yaxis = Integer.parseInt(all48data[43]);
                    ch15Zaxis = Integer.parseInt(all48data[44]);

                    //channel16 data
                    ch16Xaxis = Integer.parseInt(all48data[45]);
                    ch16Yaxis = Integer.parseInt(all48data[46]);
                    ch16Zaxis = Integer.parseInt(all48data[47]);


                }


            }
        } catch (Exception e) {
            //  Toast.makeText(getActivity(), "ReceiveExceptio:" + e.toString(), Toast.LENGTH_LONG).show();
        }

    }

    void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        //.append(spn);
    }

    /*
     * SerialListener
     */
    @SuppressLint("ResourceAsColor")
    @Override
    public void onSerialConnect() {
        status("connected");
        TVUSBStatus.setText("Status:Connected");
        TVUSBStatus.setTextColor(getResources().getColor(R.color.colorRecieveText));
        connected = Connected.True;
        if (controlLinesEnabled)
            controlLines.start();
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        TVUSBStatus.setText("Status:Disconnected");
        TVUSBStatus.setTextColor(getResources().getColor(R.color.colorStatusRed));
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        receive(data);
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        TVUSBStatus.setText("Status:Disconnected");
        TVUSBStatus.setTextColor(getResources().getColor(R.color.colorStatusRed));
        disconnect();
    }

    class ControlLines {
        private static final int refreshInterval = 200; // msec

        private final Handler mainLooper;
        private final Runnable runnable;
        private final LinearLayout frame;
        private final ToggleButton rtsBtn, ctsBtn, dtrBtn, dsrBtn, cdBtn, riBtn;

        ControlLines(View view) {
            mainLooper = new Handler(Looper.getMainLooper());
            runnable = this::run; // w/o explicit Runnable, a new lambda would be created on each postDelayed, which would not be found again by removeCallbacks

            frame = view.findViewById(R.id.controlLines);
            rtsBtn = view.findViewById(R.id.controlLineRts);
            ctsBtn = view.findViewById(R.id.controlLineCts);
            dtrBtn = view.findViewById(R.id.controlLineDtr);
            dsrBtn = view.findViewById(R.id.controlLineDsr);
            cdBtn = view.findViewById(R.id.controlLineCd);
            riBtn = view.findViewById(R.id.controlLineRi);
            rtsBtn.setOnClickListener(this::toggle);
            dtrBtn.setOnClickListener(this::toggle);
        }

        @SuppressLint("ResourceAsColor")
        private void toggle(View v) {
            ToggleButton btn = (ToggleButton) v;
            if (connected != Connected.True) {
                btn.setChecked(!btn.isChecked());
                return;
            }
            String ctrl = "";
            try {
                if (btn.equals(rtsBtn)) {
                    ctrl = "RTS";
                    usbSerialPort.setRTS(btn.isChecked());
                }
                if (btn.equals(dtrBtn)) {
                    ctrl = "DTR";
                    usbSerialPort.setDTR(btn.isChecked());
                }
            } catch (IOException e) {
                status("set" + ctrl + " failed: " + e.getMessage());
                TVUSBStatus.setText("Status:failed");
                TVUSBStatus.setTextColor(getResources().getColor(R.color.colorStatusRed));
                // Toast.makeText(getActivity(), "Exception2" + e.toString(), Toast.LENGTH_LONG).show();
            }
        }

        private void run() {
            if (connected != Connected.True)
                return;
            try {
                EnumSet<UsbSerialPort.ControlLine> controlLines = usbSerialPort.getControlLines();
                rtsBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.RTS));
                ctsBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.CTS));
                dtrBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.DTR));
                dsrBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.DSR));
                cdBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.CD));
                riBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.RI));
                mainLooper.postDelayed(runnable, refreshInterval);
            } catch (IOException e) {
                status("getControlLines() failed: " + e.getMessage() + " -> stopped control line refresh");
                //      Toast.makeText(getActivity(), "Exception3" + e.toString(), Toast.LENGTH_LONG).show();

            }
        }

        void start() {
            frame.setVisibility(View.VISIBLE);
            if (connected != Connected.True)
                return;
            try {
                EnumSet<UsbSerialPort.ControlLine> controlLines = usbSerialPort.getSupportedControlLines();
                if (!controlLines.contains(UsbSerialPort.ControlLine.RTS))
                    rtsBtn.setVisibility(View.INVISIBLE);
                if (!controlLines.contains(UsbSerialPort.ControlLine.CTS))
                    ctsBtn.setVisibility(View.INVISIBLE);
                if (!controlLines.contains(UsbSerialPort.ControlLine.DTR))
                    dtrBtn.setVisibility(View.INVISIBLE);
                if (!controlLines.contains(UsbSerialPort.ControlLine.DSR))
                    dsrBtn.setVisibility(View.INVISIBLE);
                if (!controlLines.contains(UsbSerialPort.ControlLine.CD))
                    cdBtn.setVisibility(View.INVISIBLE);
                if (!controlLines.contains(UsbSerialPort.ControlLine.RI))
                    riBtn.setVisibility(View.INVISIBLE);
                run();
            } catch (IOException e) {
                // Toast.makeText(getActivity(), "getSupportedControlLines() failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        void stop() {
            frame.setVisibility(View.GONE);
            mainLooper.removeCallbacks(runnable);
            rtsBtn.setChecked(false);
            ctsBtn.setChecked(false);
            dtrBtn.setChecked(false);
            dsrBtn.setChecked(false);
            cdBtn.setChecked(false);
            riBtn.setChecked(false);
        }
    }


    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                String cameback = "CameBack";
                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.putExtra("Comingback", cameback);
                startActivity(intent);
                return true;
        }
        return false;
    }

}
