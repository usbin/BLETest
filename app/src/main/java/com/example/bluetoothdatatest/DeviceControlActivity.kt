package com.example.bluetoothdatatest

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.*
import android.content.ContentValues.TAG
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ExpandableListView
import android.widget.ExpandableListView.OnChildClickListener
import android.widget.SimpleExpandableListAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.dialog_loading.*

import kotlinx.android.synthetic.main.gatt_services_characteristics.*
import kotlinx.android.synthetic.main.item_bledevice.device_address
import java.util.*


class DeviceControlActivity : AppCompatActivity(){
    private val handler = Handler()
    private var deviceName : String = ""
    private var deviceAddress: String = ""
    private lateinit var connectionState: TextView
    private lateinit var dataField : TextView
    private lateinit var gattServiceList : ExpandableListView
    private var bluetoothService: BluetoothLeService? = null
    private var bleGatt : BluetoothGatt? = null
    var connected : Boolean = false
    private lateinit var loadingDialog: LoadingDialog
    private var context = this
    private val LIST_NAME = "NAME"
    private val LIST_UUID = "UUID"
    private val CONNECTION_PERIOD : Long = 10000
    val autoStopConnecting = Runnable{
        Log.d("thread", "connecting timeout. - ${CONNECTION_PERIOD}")
        connected = false
        updateConnectionState("Disconnected");
        invalidateOptionsMenu()
        clearUI();
        loadingDialog.dismiss()
    }

    private lateinit var gattCharacteristics: ArrayList<ArrayList<BluetoothGattCharacteristic>>
    private var writeCharacteristic: BluetoothGattCharacteristic?=null
    private var notifyCharacteristic: BluetoothGattCharacteristic?=null

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            bluetoothService = (service as BluetoothLeService.LocalBinder).service
            if (!bluetoothService?.initialize()!!) {
                Log.e(TAG, "Unable to initialize Bluetooth")

                loadingDialog.dismiss();
                finish()
            }

            // Automatically connects to the device upon successful start-up initialization.
            bluetoothService?.connect(deviceAddress)

        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bluetoothService = null
        }
    }

    //실제 응답이 돌아올 때 실행됨.
    private val gattUpdateReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("bluetoothConnection", "broadcast : ${intent?.action}")
            when (intent?.action){
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    connected = true
                    updateConnectionState("Connected")
                    invalidateOptionsMenu()
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    connected = false
                    updateConnectionState("Disconnected");
                    invalidateOptionsMenu()
                    clearUI();
                    loadingDialog.dismiss();
                    handler.removeCallbacks(autoStopConnecting)
                    Log.d("thread", "is Disconnected, remove autoStopConnecting")

                    Toast.makeText(this@DeviceControlActivity, R.string.ble_device_disconnected, Toast.LENGTH_SHORT).show()

                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    displayGattServices(bluetoothService?.supportedGattServices)
                    loadingDialog.dismiss();
                    handler.removeCallbacks(autoStopConnecting)
                    Log.d("thread", "is connected, remove autoStopConnecting")
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE -> {
                    displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA))
                }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gatt_services_characteristics)


        //뒤로가기 버튼 있는 툴바 선택
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        //device이름과 주소 표시.
        deviceName = intent.getStringExtra("name");
        deviceAddress = intent.getStringExtra("address");
        supportActionBar?.title = deviceName;
        device_address.text = deviceAddress;

        //상태와 데이터필드, 서비스 부분은 객체를 째로 받아옴.
        connectionState = connection_state;
        dataField = data_value;
        gattServiceList = gatt_services_list;
        gattServiceList.setOnChildClickListener(servicesListClickListener);

        loadingDialog = LoadingDialog(this);
        loadingDialog.show()
        Log.d("bluetoothConnect", "state : $connected")
        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)

        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

    }
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onResume(){
        super.onResume()
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        Log.d("bluetoothConnect", "$bluetoothService")
        Log.d("bluetoothConnect", "state : $connected")
        if(bluetoothService != null){
            val result = bluetoothService!!.connect(deviceAddress);
            Log.d("Connection", "Connect request result=$result");
            handler.postDelayed(autoStopConnecting, CONNECTION_PERIOD)
            Log.d("thread", "start autoStopConnecting")
            loadingDialog.show();
        }


    }
    private fun selectCharacteristicData(gattServices: List<BluetoothGattService>){
        for(gattService in gattServices){
            var gattCharacteristics: List<BluetoothGattCharacteristic> = gattService.characteristics
            for(gattCharacteristic in gattCharacteristics){
                when(gattCharacteristic.uuid){
                    BluetoothLeService.UUID_DATA_WRITE -> writeCharacteristic = gattCharacteristic
                    BluetoothLeService.UUID_DATA_NOTIFY -> notifyCharacteristic = gattCharacteristic
                }
            }
        }

    }
    private fun sendData(data: String){
        writeCharacteristic?.let{
            if(it.properties or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE > 0 ){
                bluetoothService?.writeCharacteristic(it, data);
            }
        }
        notifyCharacteristic?.let{
            if(it.properties or BluetoothGattCharacteristic.PROPERTY_NOTIFY >0){
                bluetoothService?.setCharacteristicNotification(it, true);
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.gatt_services, menu)
        Log.d("bluetoothConnect", "state : $connected")
        if(connected){
            menu!!.findItem(R.id.menu_connect)!!.isVisible = false;
            menu!!.findItem(R.id.menu_disconnect)!!.isVisible = true;
        }
        else {
            menu!!.findItem(R.id.menu_connect)!!.isVisible = true;
            menu!!.findItem(R.id.menu_disconnect)!!.isVisible = false;
        }
        return true;
    }
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.menu_connect ->{
                Log.d("selectMenu", "${item.itemId}, ${bluetoothService!!.connect(deviceAddress)}")
                loadingDialog.show();

                Log.d("thread", "start autoStopConnecting")
                handler.postDelayed(autoStopConnecting, CONNECTION_PERIOD)
                return true;
            }
            R.id.menu_disconnect ->{
                bluetoothService?.disconnect()
                return true;
            }
            //뒤로가기 버튼 리스너
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onPause(){
        super.onPause();
        unregisterReceiver(gattUpdateReceiver)
    }
    override fun onDestroy(){
        super.onDestroy()
        unbindService(serviceConnection)
        bluetoothService = null
    }

    private val servicesListClickListener =
        OnChildClickListener { parent, v, groupPosition, childPosition, id ->
            if(gattCharacteristics != null){
                val characteristic: BluetoothGattCharacteristic? =
                    gattCharacteristics!!.get(groupPosition).get(childPosition)
                val charaProp = characteristic?.properties
                if (charaProp != null) {
                    if (charaProp or BluetoothGattCharacteristic.PROPERTY_READ > 0) {
                        // If there is an active notification on a characteristic, clear
                        // it first so it doesn't update the data field on the user interface.
                        if (notifyCharacteristic != null) {
                            bluetoothService?.setCharacteristicNotification(
                                notifyCharacteristic!!, false
                            )
                            notifyCharacteristic = null
                        }
                       bluetoothService?.readCharacteristic(characteristic)
                    }
                }
                if (charaProp != null) {
                    if (charaProp or BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
                        notifyCharacteristic = characteristic
                        bluetoothService?.setCharacteristicNotification(
                            characteristic, true
                        )
                    }
                }
                return@OnChildClickListener true
            }
            false

        }

    private fun makeGattUpdateIntentFilter(): IntentFilter? {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        return intentFilter
    }
    private fun updateConnectionState(resourceText: String) {
        runOnUiThread { connectionState.text = resourceText }
    }

    private fun displayData(data: String?) {
        if (data != null) {
            dataField.text = data
        }
    }
    private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        var uuid: String? = null
        val unknownServiceString = resources.getString(R.string.unknown_service)
        val unknownCharaString =
            resources.getString(R.string.unknown_characteristic)
        val gattServiceData =
            ArrayList<HashMap<String, String?>>()
        val gattCharacteristicData =
            ArrayList<ArrayList<HashMap<String, String?>>>()

        gattCharacteristics =
            ArrayList<ArrayList<BluetoothGattCharacteristic>>()

        // Loops through available GATT Services.
        for (gattService in gattServices) {
            val currentServiceData =
                HashMap<String, String?>()
            uuid = gattService.uuid.toString()
            currentServiceData[LIST_NAME] = SampleGattAttributes.lookup(uuid, unknownServiceString)
            currentServiceData[LIST_UUID] = uuid
            gattServiceData.add(currentServiceData)
            val gattCharacteristicGroupData =
                ArrayList<HashMap<String, String?>>()
            val gattCharacteristicsLocal = gattService.characteristics
            val charas =
                ArrayList<BluetoothGattCharacteristic>()

            // Loops through available Characteristics.
            for (gattCharacteristic in gattCharacteristicsLocal) {
                charas.add(gattCharacteristic)
                val currentCharaData =
                    HashMap<String, String?>()
                uuid = gattCharacteristic.uuid.toString()
                currentCharaData[LIST_NAME] = SampleGattAttributes.lookup(uuid, unknownCharaString)
                currentCharaData[LIST_UUID] = uuid
                gattCharacteristicGroupData.add(currentCharaData)
            }
            gattCharacteristics.add(charas)
            gattCharacteristicData.add(gattCharacteristicGroupData)
        }
        val gattServiceAdapter = SimpleExpandableListAdapter(
            this,
            gattServiceData,
            android.R.layout.simple_expandable_list_item_2,
            arrayOf(LIST_NAME, LIST_UUID),
            intArrayOf(android.R.id.text1, android.R.id.text2),
            gattCharacteristicData,
            android.R.layout.simple_expandable_list_item_2,
            arrayOf(LIST_NAME, LIST_UUID),
            intArrayOf(android.R.id.text1, android.R.id.text2)
        )
        gattServiceList.setAdapter(gattServiceAdapter)
    }
    private fun clearUI() {
        gattServiceList.setAdapter(null as SimpleExpandableListAdapter?)
        dataField.setText(R.string.no_data)
    }
}