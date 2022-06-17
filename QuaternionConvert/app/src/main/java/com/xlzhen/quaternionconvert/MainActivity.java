package com.xlzhen.quaternionconvert;


import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilterCoefficients;
import biz.source_code.dsp.filter.IirFilterDesignExstrom;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DataFrameView dataFrameView = findViewById(R.id.data_frame_view);
        DataFrame acc = DataFrame.parseData(this, "data_2/Data_LinearAcc.txt");
        for (int i = 0; i < acc.size(); i++) {
            acc.set(i, "time", acc.get(i, "time") / 1000d);
            acc.set(i, "accx", acc.get(i, "accx") * 9.81d / 8192d);
            acc.set(i, "accy", acc.get(i, "accy") * 9.81d / 8192d);
            acc.set(i, "accz", acc.get(i, "accz") * 9.81d / 8192d);
        }
        DataFrame quat = DataFrame.parseData(this, "data_2/Data_Quaternions.txt");
        double[] time = new double[quat.size()];
        for (int i = 0; i < quat.size(); i++) {
            quat.set(i, "time", quat.get(i, "time") / 1000d);
            time[i] = quat.get(i, "time");
        }

        DataFrame cquat = get_mult_quat_DFxV(quat);
        DataFrame cacc = get_rotation_DFxDF(cquat, acc, new String[]{"accx", "accy", "accz"});
        DataFrame[] uvw = get_uvw(0.05d, quat);
        //numPyView.setNumPyData(numPyDatas[1]);

        DataFrame vel = get_vel_drift(time, cacc);
        DataFrame pos = integral(time, vel, new String[]{"posx", "posy", "posz"});
        dataFrameView.setDataFrame(pos);
    }

    private DataFrame integral(double[] time, DataFrame data, String[] name) {
        DataFrame l = new DataFrame(name);
        double[] ip = new double[l.getColumnsSize()];
        l.append(ip.clone());
        for (int i = 0; i < data.size() - 1; i++) {
            double vlex = data.get(i, "velx") + data.get(i + 1, "velx");
            double vley = data.get(i, "vely") + data.get(i + 1, "vely");
            double vlez = data.get(i, "velz") + data.get(i + 1, "velz");

            ip[0] = (vlex * (time[i + 1] - time[i]) / 2) + ip[0];
            ip[1] = (vley * (time[i + 1] - time[i]) / 2) + ip[1];
            ip[2] = (vlez * (time[i + 1] - time[i]) / 2) + ip[2];

            l.append(ip.clone());
        }
        for (double[] doubles : l) {
            for (int i = 0; i < doubles.length; i++) {
                doubles[i] = doubles[i] * 1500.25;
            }
        }
        return l;
    }


    private DataFrame get_vel_drift(double[] time, DataFrame acc) {
        // Calculate stationary period
        int[] stationary = get_stationary(acc);
        // Calculate velocity integral 速度积分
        DataFrame vel = integral_vel(acc, time, stationary);
        // Calculate integral drift 积分漂移
        DataFrame drift = get_drift(vel, stationary, time);
        // Remove integral drift
        DataFrame vel_drift = new DataFrame(new String[]{"velx", "vely", "velz"});
        for (int i = 0; i < vel.size(); i++) {
            double[] vels = vel.get(i);
            double[] drifts = drift.get(i);
            double[] doubles = new double[vels.length];
            for (int j = 0; j < vels.length; j++) {
                doubles[j] = vels[j] - drifts[j];
            }
            vel_drift.append(doubles);
        }
        return vel_drift;
    }

    private DataFrame get_drift(DataFrame data, int[] stationary, double[] time) {
        List<Integer> drift_start = diff(stationary, -1);
        List<Integer> drift_end = diff(stationary, 1);
        DataFrame drift_data = new DataFrame(new String[]{"velx", "vely", "velz"});

        for (int i = 0; i < stationary.length; i++) {
            drift_data.append(new double[drift_data.getColumnsSize()]);
        }
        if (drift_start.get(0) > drift_end.get(0)) {
            drift_start.add(0, 0);
        }
        for (int i = 0; i < drift_end.size(); i++) {
            int ti = drift_start.get(i);
            int tf = drift_end.get(i);
            double[] vel_end = data.get(tf + 1);
            double[] tg = new double[3];
            tg[0] = vel_end[0] / (time[tf] - time[ti]);
            tg[1] = vel_end[1] / (time[tf] - time[ti]);
            tg[2] = vel_end[2] / (time[tf] - time[ti]);
            double[] t_drift = new double[tf + 2 - ti];
            for (int j = ti; j < tf + 2; j++) {
                t_drift[j - ti] = time[j] - time[ti];
                drift_data.set(j, new double[]{t_drift[j - ti] * tg[0], t_drift[j - ti] * tg[1], t_drift[j - ti] * tg[2]});
            }
        }
        return drift_data;
    }

    private List<Integer> diff(int[] stationary, int position) {
        List<Integer> integers = new ArrayList<>();
        for (int i = 0; i < stationary.length - 1; i++) {
            if (stationary[i + 1] - stationary[i] == position) {
                integers.add(i);
            }
        }
        return integers;
    }


    private DataFrame integral_vel(DataFrame data, double[] time, int[] stationary) {
        DataFrame l = new DataFrame(new String[]{"velx", "vely", "velz"});

        double[] ip = new double[l.getColumnsSize()];
        l.append(ip.clone());
        for (int i = 0; i < data.size() - 1; i++) {
            double accx = data.get(i, "accx") + data.get(i + 1, "accx");
            double accy = data.get(i, "accy") + data.get(i + 1, "accy");
            double accz = data.get(i, "accz") + data.get(i + 1, "accz");
            ip[0] = (accx * (time[i + 1] - time[i]) / 2) + ip[0];
            ip[1] = (accy * (time[i + 1] - time[i]) / 2) + ip[1];
            ip[2] = (accz * (time[i + 1] - time[i]) / 2) + ip[2];
            if (stationary[i] != 0) {
                ip = new double[l.getColumnsSize()];
            } else {
                Log.v("不会清除", Arrays.toString(ip));
            }
            l.append(ip.clone());
        }
        return l;
    }


    private int[] get_stationary(DataFrame acc) {
        DataFrame norm = get_norm(acc);

        DataFrame lp = pass_filter(norm, FilterPassType.lowpass, 0.2f);
        int[] stationary = new int[lp.get(0).length];
        for (int i = 0; i < lp.get(0).length; i++) {
            stationary[i] = lp.get( 0)[i] < 0.45d ? 1 : 0;
        }
        /*stationary = lp < 0.45
        stationary = (stationary * 1)['norm'].to_numpy()*/
        return stationary;
    }

    private DataFrame pass_filter(DataFrame data, FilterPassType type, float filtcutoff) {
        DataFrame res = new DataFrame(new String[]{"norm"});
        IirFilterCoefficients iirFilterCoefficients = IirFilterDesignExstrom.design(type, 1,
                (2 * filtcutoff) / (10), (2 * filtcutoff) / (10));
        //b, a = sgn.butter(1, (2 * filtcutoff) / (10), type);
        for (int i = 0; i < data.getColumnsSize(); i++) {
            double[] doubles = data.get(data.getColumns().get(i));

            res.append(Filtfilt.filtfilt(doubles, iirFilterCoefficients.a, iirFilterCoefficients.b));
        }
        return res;
    }

    private double[] filtfilt(double[] signal, double[] a, double[] b) {
        double[] in = new double[b.length];
        double[] out = new double[a.length - 1];

        double[] outData = new double[signal.length];

        for (int i = 0; i < signal.length; i++) {

            System.arraycopy(in, 0, in, 1, in.length - 1);
            in[0] = signal[i];

            //calculate y based on a and b coefficients
            //and in and out.
            float y = 0;
            for (int j = 0; j < b.length; j++) {
                y += b[j] * in[j];

            }

            for (int j = 0; j < a.length - 1; j++) {
                y -= a[j + 1] * out[j];
            }

            //shift the out array
            System.arraycopy(out, 0, out, 1, out.length - 1);
            out[0] = y;

            outData[i] = y;


        }
        return outData;
    }

    /*private int[] get_stationary(DataFrame acc) {
        DataFrame norm = get_norm(acc);
        DataFrame md = median_filter(norm, 755);
        norm = norm - md;
        // Calculate High-Pass filter
        hp = pass_filter(norm, 'high', 0.1);
        // Value absolute
        hp = hp.abs();
        // Calculate Low-Pass filter
        lp = pass_filter(norm, 'low', 0.2);
        // Calculate stationary period
        int[] stationary = lp < 0.45
        stationary = (stationary * 1)['norm'].to_numpy();
        return stationary;
    }*/


    private DataFrame get_norm(DataFrame data) {
        DataFrame norm = new DataFrame(new String[]{"norm"});


        double t = 0;
        for (int i = 0; i < data.size(); i++) {
            for (double d : data.get(i)) {
                t += Math.pow(d, 2);
            }
            t = Math.sqrt(t);
            norm.append(new double[]{t});
            t = 0;
        }
        return norm;
    }

    private DataFrame[] get_uvw(double size_vector, DataFrame quat) {
        quat = get_mult_quat_DFxV(quat);
        String[] columns = new String[]{"u", "v", "w"};
        DataFrame x = get_rotation_DFxV(quat, new double[]{size_vector, 0d, 0d}, columns);
        DataFrame y = get_rotation_DFxV(quat, new double[]{0d, size_vector, 0d}, columns);
        DataFrame z = get_rotation_DFxV(quat, new double[]{0d, 0d, size_vector}, columns);
        return new DataFrame[]{x, y, z};
    }

    private DataFrame get_rotation_DFxV(DataFrame quat, double[] v, String[] name) {
        DataFrame res = new DataFrame(name);
        v = append(0d * (4 - name.length), v);
        for (int i = 0; i < quat.size(); i++) {
            double[] q = quat.get(i);
            if (name.length == 4) {
                double[] doubles = qq_mult(v, q);
                res.append(doubles);
            } else {
                double[] doubles = qq_mult(q, v);
                res.append(new double[]{doubles[1], doubles[2], doubles[3]});
            }
        }
        return res;
    }

    private double[] append(double position, double[] v) {
        double[] floats = new double[v.length + 1];
        floats[0] = position;
        for (int i = 1; i < v.length + 1; i++) {
            floats[i] = v[i - 1];
        }
        return floats;
    }

    private DataFrame get_rotation_DFxDF(DataFrame quat, DataFrame data, String[] name) {
        DataFrame res = new DataFrame(name);

        for (int i = 0; i < quat.size(); i++) {
            double[] q1 = quat.get(i);
            double[] q2 = data.get(i);

            if (q2.length == 5) {
                double[] doubles = qq_mult(q1, q2);
                res.append(doubles);
            } else {
                double[] doubles = qq_mult(q1, new double[]{0d, q2[1], q2[2], q2[3]});
                res.add(new double[]{doubles[1], doubles[2], doubles[3]});
            }
        }
        return res;
    }

    private double[] qq_mult(double[] q1, double[] q2) {
        return q_mult(q_mult(q1, q2), q_conjugate(q1));
    }


    private DataFrame get_mult_quat_DFxV(DataFrame quat) {
        double[] q2 = quat.get(0);

        double n = Math.pow(q2[1], 2) + Math.pow(q2[2], 2) + Math.pow(q2[3], 2) + Math.pow(q2[4], 2);
        double[] q2s = q_conjugate(new double[]{q2[1], q2[2], q2[3], q2[4]});
        for (int i = 0; i < q2s.length; i++) {
            q2s[i] = q2s[i] / n;
        }
        DataFrame res = new DataFrame(new String[]{"qw", "qx", "qy", "qz"});

        for (int i = 0; i < quat.size(); i++) {
            double[] q = quat.get(i);
            res.append(q_mult(q2s, new double[]{q[1], q[2], q[3], q[4]}));
        }
        return res;
    }

    private double[] q_mult(double[] q1, double[] q2) {
        double w1 = q1[0];
        double x1 = q1[1];
        double y1 = q1[2];
        double z1 = q1[3];

        double w2 = q2[0];
        double x2 = q2[1];
        double y2 = q2[2];
        double z2 = q2[3];

        double[] res = new double[4];

        res[0] = w1 * w2 - x1 * x2 - y1 * y2 - z1 * z2;
        res[1] = w1 * x2 + x1 * w2 + y1 * z2 - z1 * y2;
        res[2] = w1 * y2 + y1 * w2 + z1 * x2 - x1 * z2;
        res[3] = w1 * z2 + z1 * w2 + x1 * y2 - y1 * x2;
        return res;
    }

    private double[] q_conjugate(double[] q2) {
        return new double[]{q2[0], -q2[1], -q2[2], -q2[3]};
    }
}