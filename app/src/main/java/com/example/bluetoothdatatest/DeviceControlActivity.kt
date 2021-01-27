package com.example.bluetoothdatatest

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.os.PersistableBundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DeviceControlActivity : AppCompatActivity(){
    private var deviceAddress: String = ""
    private var bluetoothService: BluetoothLeService? = null
    var connected : Boolean = false
    val gattUpdateReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            when (action){
                BluetoothLeService.ACTION_GATT_CONNECTED -> connected = true
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    connected = false
                    Toast.makeText(this@DeviceControlActivity, R.string.ble_device_disconnected, Toast.LENGTH_SHORT).show()
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    bluetoothService?.let {
                        //SelectCharacteristicData(it.supportedGattServices)
                    }
                }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gatt_services_characteristics)

        deviceAddress = intent.getStringExtra("address")
        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

    }
    private val serviceConnection = object: ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            bluetoothService = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bluetoothService = (service as BluetoothLeService.LocalBinder).service
            bluetoothService?.connect(deviceAddress)
        }
    }
}