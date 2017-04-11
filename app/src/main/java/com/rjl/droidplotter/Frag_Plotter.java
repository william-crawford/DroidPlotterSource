package com.rjl.droidplotter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.*;


public class Frag_Plotter extends Fragment {

    private LineGraphSeries<DataPoint> dataSeries;
    private PlotInterface plotter;
    private double plotSize = 121;
    private GraphView graphPlot;
    private TextView txtXval, txtYval;

    interface PlotInterface {
        void onSetLoggingEnabled(boolean b);
    }

    private void InitPlotter(View v) {
        graphPlot = (GraphView) v.findViewById(R.id.plotview);
        Switch swDataStream = (Switch) v.findViewById(R.id.swLogEn);
        txtXval = (TextView) v.findViewById(R.id.txtXval);
        txtYval = (TextView) v.findViewById(R.id.txtYval);
        graphPlot.setPadding(15, 15, 15, 15);
        graphPlot.getGridLabelRenderer().setGridColor(Color.parseColor("#005904"));
        graphPlot.getGridLabelRenderer().setVerticalLabelsColor(Color.parseColor("#008c07"));
        graphPlot.getGridLabelRenderer().setHorizontalLabelsColor(Color.parseColor("#008c07"));
        dataSeries = new LineGraphSeries<>();
        dataSeries.setColor(Color.parseColor("#e2ea00"));
        SetGraphParam(121, -0.5, 4.1, -4.1);
        setPlotParamBound();
        plotSize = 122;
        for (int i = 0; i < 121; i++) {
            dataSeries.appendData(new DataPoint(i, 2.75 * Math.sin(i / 2.5)), false, (int) plotSize);
        }
        graphPlot.addSeries(dataSeries);
        swDataStream.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    plotter.onSetLoggingEnabled(true);
                    ResetGraph();
                } else {
                    plotter.onSetLoggingEnabled(false);

                }
            }
        });
        dataSeries.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPointInterface) {

                //dataPointInterface has square backet
                // by default and should be removed when parsing the data of X and Y axis
                String strDataPoint = String.valueOf(dataPointInterface).replaceAll("\\[","").replaceAll("\\]","");

                String strSplit[];
                Double yVal, xVal;

                if (strDataPoint.contains("/")) {
                    strSplit = strDataPoint.split("/");
                    yVal =  Double.parseDouble(strSplit[1]);
                    xVal =  Double.parseDouble(strSplit[0]);

                    @SuppressLint("DefaultLocale")   String fyVal = String.format("%3.3f", yVal);
                    @SuppressLint("DefaultLocale")   String fxVal = String.format("%3.3f", xVal);
                    txtXval.setText(fxVal);
                    txtYval.setText(fyVal);
                }
            }
        });
    }

    private void SetGraphParam(double max_X, double min_X, double max_Y, double min_Y) {
        graphPlot.getViewport().setMinX(min_X);
        graphPlot.getViewport().setMaxX(max_X);
        graphPlot.getViewport().setMinY(min_Y);
        graphPlot.getViewport().setMaxY(max_Y);
    }

    private void setPlotParamBound() {
        graphPlot.getViewport().setXAxisBoundsManual(true);
        graphPlot.getViewport().setYAxisBoundsManual(true);
        graphPlot.getViewport().setScalable(true);
        graphPlot.getViewport().setScrollable(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            plotter = (PlotInterface) context;
        } catch (ClassCastException ex) {
            throw new ClassCastException(context.toString());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View plotView = inflater.inflate(R.layout.plot_layout, container, false);
        InitPlotter(plotView);
        return plotView;
    }

    void plotValue(double xVal, double yVal)         //x = time y = value
    {
        @SuppressLint("DefaultLocale") String fyVal = String.format("%3.3f", yVal);
        @SuppressLint("DefaultLocale") String fxVal = String.format("%3.3f", xVal);
        dataSeries.appendData(new DataPoint(xVal, yVal), false, (int) plotSize);
        txtXval.setText(fxVal);
        txtYval.setText(fyVal);
    }

    void ResetGraph() {
        dataSeries.resetData(new DataPoint[]{});
    }
}
