package com.example.bluetoothdatatest

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private fun PackageManager.missingSystemFeature(name: String): Boolean = !hasSystemFeature(name)

    private val bluetoothAdapter: BluetoothAdapter by lazy() {
        //블루투스 어댑터 설정. api 18 이상 가능(삼성 SM-J727S는 api 27)
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    private val rvAdapter : BleDeviceAdapter by lazy(){
        BleDeviceAdapter(this)
    }
    //인텐트 요청 시 사용하는 고유번호
    private val REQUEST_ENABLE_BT = 1
    private val SCAN_PERIOD : Long = 10000
    private var scanning : Boolean = false
    private val handler = Handler() //핸들러 생성
    val requestActivity : ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){  activityResult ->

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        checkPermission()
        //supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        //BLE 지원 여부 체크 후 지원하지 않으면 어플 종료.
        packageManager.takeIf { it.missingSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)}?.also{
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
            finish()
        }




    }
    override fun onResume(){
        super.onResume()
        //블루투스 허가 요청 intent 띄움.
        bluetoothAdapter.takeIf{ it.isDisabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            //첫번째 인자:시작할 intent //두번째 인자:intent를 요청한 위치 식별용 id
           startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)

        }

        //리사이클러뷰 초기화 코드
        rvAdapter.notifyDataSetChanged()
        rvAdapter.scanning = this.scanning;
        rvAdapter.mainContext = this;
        rvDeviceList.adapter = rvAdapter

        //리사이클러뷰 구분선 추가
        rvDeviceList.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
        scanBleDevice(true)
    }



    private fun checkPermission(){
        var permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        var arrayPermission = ArrayList<String>()
        if (permissionCheck != PackageManager.PERMISSION_GRANTED){
            arrayPermission.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if(arrayPermission.size > 0){
            ActivityCompat.requestPermissions(this, arrayPermission.toTypedArray(), REQUEST_ENABLE_BT)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            REQUEST_ENABLE_BT -> {
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }

    private val scanCallback = object : ScanCallback(){
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d("ble", "${errorCode}")
            Log.d("ble","asdfasdfasdf")
            Toast.makeText(this@MainActivity, R.string.ble_scan_failed, Toast.LENGTH_SHORT).show()
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let{
                rvAdapter.addDevice(it.device)
                Log.d("ble", "${it.device.name} : ${it.device.address}")
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.let{
                for (result in it){
                    rvAdapter.addDevice(result.device)
                }
            }
        }


    }
    private fun scanBleDevice(enable : Boolean){
        if(enable){
            handler.postDelayed({
                scanning = false
                rvAdapter.scanning = this.scanning;
                bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
                invalidateOptionsMenu()
            }, SCAN_PERIOD)
            scanning = true
            rvAdapter.scanning = this.scanning;
            rvAdapter.clear()
            bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
            invalidateOptionsMenu()

        }
        else {
            scanning = false
            rvAdapter.scanning = this.scanning;
            bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
            invalidateOptionsMenu()
        }
    }



    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        if (!scanning) {
            menu?.findItem(R.id.menu_stop)?.isVisible = false
            menu?.findItem(R.id.menu_scan)?.isVisible = true
            menu?.findItem(R.id.menu_refresh)?.actionView = null
        } else {
            menu?.findItem(R.id.menu_stop)?.isVisible = true
            menu?.findItem(R.id.menu_scan)?.isVisible = false
            menu?.findItem(R.id.menu_refresh)?.setActionView(
                R.layout.toolbar_progress_icon
            )
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_scan -> {
                rvAdapter.clear()
                scanBleDevice(true)

            }
            R.id.menu_stop -> scanBleDevice(false)
        }
        return true
    }


}