package com.xlzhen.quaternionconvert;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class DataFrame extends ArrayList<double[]> {
    private List<String> columns;

    public DataFrame() {
        super(new ArrayList<>());
    }

    public DataFrame(String[] columns) {
        super(new ArrayList<>());
        this.columns = new ArrayList<>(Arrays.asList(columns));
    }

    public DataFrame(Collection<? extends double[]> collection, String[] columns) {
        super(collection);
        this.columns = new ArrayList<>(Arrays.asList(columns));
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public int getColumnsSize() {
        return columns.size();
    }

    public void setColumns(String[] columns) {
        this.columns = new ArrayList<>(Arrays.asList(columns));
    }

    public DataFrame append(double[] item) {
        add(item);
        return this;
    }

    public double get(int index, String column) {
        return get(index)[columns.indexOf(column)];
    }

    public void set(int index, String column,double value){
        get(index)[columns.indexOf(column)] = value;
    }

    public void set(int index, int columnIndex,double value){
        get(index)[columnIndex] = value;
    }

    public static DataFrame parseData(Context context, String assetPath) {
        DataFrame dataFrame = new DataFrame();
        try {
            InputStream inputStream = context.getAssets().open(assetPath);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            List<String> lines = new ArrayList<>();

            String line = bufferedReader.readLine();
            String[] keys = line.split(",");
            dataFrame.setColumns(keys);
            while (line != null) {
                line = bufferedReader.readLine();
                if (line != null)
                    lines.add(line);
            }
            inputStream.close();

            for (int i = 0; i < lines.size(); i++) {
                if (!lines.get(i).contains(","))
                    continue;
                String[] values = lines.get(i).split(",");
                dataFrame.add(values);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataFrame;
    }

    private void add(String[] values) {
        double[] doubles = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            doubles[i] = Double.parseDouble(values[i].trim());
        }
        add(doubles);
    }

    public List<String> getColumns() {
        return columns;
    }

    /**
     * 取某列下所有数据
     * @param column 列名
     * @return 返回该列所有数据
     */
    public double[] get(String column) {
        double[] dataList = new double[size()];
        for(int i = 0;i<this.size();i++){
            dataList[i] = get(i,column);
        }
        return dataList;
    }
}
