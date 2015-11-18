package com.example.tongmin.myble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xhc on 2015/11/18.
 */
public class DeviceAdapter extends BaseAdapter {


    private List<BluetoothDevice> listDevice;
    private LayoutInflater inflater;
    private ViewHolder holder;

    public DeviceAdapter(List<BluetoothDevice> listDevice, Context context) {
        this.listDevice = listDevice;
        inflater = LayoutInflater.from(context);
    }
    public DeviceAdapter(Context context) {
        this.listDevice = new ArrayList<BluetoothDevice>();
        inflater = LayoutInflater.from(context);
    }

    public void setListDevice(List<BluetoothDevice> listDevice) {
        this.listDevice.clear();
        this.listDevice.addAll(listDevice);
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.devicenameitem, null, false);
            holder.tv = (TextView) convertView.findViewById(R.id.name);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        BluetoothDevice device = listDevice.get(position);
        holder.tv.setText(device.getName());
        return convertView;
    }

    @Override
    public int getCount() {
        return listDevice.size();
    }

    @Override
    public Object getItem(int position) {
        return listDevice.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    class ViewHolder {
        TextView tv;

    }
}
