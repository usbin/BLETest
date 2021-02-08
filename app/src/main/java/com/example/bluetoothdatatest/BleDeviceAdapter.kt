package com.example.bluetoothdatatest

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_bledevice.view.*

class BleDeviceAdapter(private val context : Context) : RecyclerView.Adapter<BleDeviceViewHolder>(){
    var bleDeviceList = ArrayList<BluetoothDevice>()
    var scanning = false;

    lateinit var mainContext : Context;
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BleDeviceViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_bledevice, parent, false)
        return BleDeviceViewHolder(view)
    }

    override fun getItemCount(): Int = bleDeviceList.size

    fun getItem(i : Int) : BluetoothDevice { return bleDeviceList[i]}

    override fun onBindViewHolder(holder: BleDeviceViewHolder, position: Int) {
        holder.bind(bleDeviceList[position]);
        holder.itemView.setOnClickListener {
            val device = bleDeviceList[position];
            val intent = Intent(this.mainContext, DeviceControlActivity::class.java);
            if(device.name == null){
                intent.putExtra("name", "Unknown")
            }
            else{
                intent.putExtra("name", device.name);
            }
            intent.putExtra("address", device.address);

            this.mainContext.startActivity(intent);
        }

    }
    fun addDevice(device : BluetoothDevice){
        if(!bleDeviceList.contains(device)){
            Log.d("ble", "add device that not contained. ${bleDeviceList.size}")
            bleDeviceList.add(device)
            notifyDataSetChanged()
        }

    }
    fun clear() { bleDeviceList.clear() }

}

class BleDeviceViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {

    fun bind(bleDeviceData : BluetoothDevice){
        //뷰 안의 아이템에 텍스트 지정 등 동작 작성.
        val tvName = itemView.findViewById(R.id.device_name) as TextView
        tvName.text = bleDeviceData.name
        if(bleDeviceData.name == null){
            tvName.text = "Unknown"
        }
        val tvUid = itemView.findViewById(R.id.device_address) as TextView
        tvUid.text = bleDeviceData.address
        Log.d("ble", "now bind ${bleDeviceData.address}\n")


    }
}