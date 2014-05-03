package miga.tibeacon;

import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.KrollFunction;
import java.util.HashMap;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.KrollDict;
import android.content.Context;
import android.app.Activity;
import java.util.Timer;
import java.util.TimerTask;
import android.content.pm.PackageManager;

import com.easibeacon.protocol.IBeacon;
import com.easibeacon.protocol.IBeaconListener;
import com.easibeacon.protocol.IBeaconProtocol;
import com.easibeacon.protocol.Utils;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import java.util.ArrayList;


@Kroll.module(name="Tibeacon", id="miga.tibeacon")
public class TibeaconModule extends KrollModule implements IBeaconListener{
	
	Context context;
	Activity activity;
	private IBeaconProtocol ibp;
	int seconds=60;
	public static final int REQUEST_BLUETOOTH_ENABLE = 1;	
	KrollFunction success;
	KrollFunction region;
	KrollFunction error;
	KrollFunction found;
	Timer timer = new Timer();
	boolean isRunning = false;
	
       @Override
        public void onDestroy(Activity activity) {
	  Log.d("BEACON","destroy");
	  super.onDestroy(activity);
        }
        
       @Override
        public void onResume(Activity activity) {
         super.onResume(activity);
	  Log.d("BEACON","resume");
        }
        
       
       @Override
        public void onStart(Activity activity) {
	  super.onStart(activity);
	  Log.d("BEACON","start");
        }
	
	public TibeaconModule () {
		super();
		TiApplication appContext = TiApplication.getInstance();
		activity = appContext.getCurrentActivity();
		context=activity.getApplicationContext();
		
	}
	
	
	@Override
	public void operationError(int status) {
		Log.d("BEACON", "Bluetooth error: " + status);	
	}
	
	@Kroll.method
	public void initBeacon(HashMap args){
	    KrollDict arg = new KrollDict(args);
	    success =(KrollFunction) arg.get("success");
	    region=(KrollFunction) arg.get("region");
	    error =(KrollFunction) arg.get("error");
	    found =(KrollFunction) arg.get("found");
	    seconds=arg.optInt("interval",60);
	    
	    
	}

	
	@Override
	public void searchState(final int state) {
		activity.runOnUiThread(new Runnable() {			
			@Override
			public void run() {
				if(state == IBeaconProtocol.SEARCH_STARTED){
					Log.d("BEACON","started scanning");
				}else if (state == IBeaconProtocol.SEARCH_END_SUCCESS){
					sendData();
					Log.d("BEACON","scan end success");
				}else if (state == IBeaconProtocol.SEARCH_END_EMPTY){
					Log.d("BEACON","search end empty");
					sendData();
				}
			}
		});
	}
	
	@Override
	public void beaconFound(IBeacon ibeacon) {
		Log.d("BEACON","iBeacon found: " + ibeacon.toString());
		
		HashMap<String, KrollDict> event = new HashMap<String, KrollDict>();
		KrollDict d = new KrollDict();;
		d.put("mac",ibeacon.getMacAddress());
		d.put("major",ibeacon.getMajor());
		d.put("minor",ibeacon.getMinor());
		d.put("rssi",ibeacon.getRssiValue());
		d.put("power",ibeacon.getPowerValue());
		d.put("proximity",ibeacon.getProximity());
		d.put("uuid",ibeacon.getUuidHexString());
		d.put("uuid-dashed",ibeacon.getUuidHexStringDashed());
		event.put("device", d);
	    
	      // Success-Callback
	      if (found!=null){
		found.call(getKrollObject(), event);
	      }
	}
	
	@Override
	public void exitRegion(IBeacon ibeacon) {
		activity.runOnUiThread(new Runnable() {		
			@Override
			public void run() {
			}
		});
	}
	
	@Override
	public void enterRegion(IBeacon ibeacon) {
		Log.d("BEACON","Enter region: " + ibeacon.toString());

		HashMap<String, KrollDict> event = new HashMap<String, KrollDict>();
		KrollDict d = new KrollDict();;
		d.put("mac",ibeacon.getMacAddress());
		d.put("major",ibeacon.getMajor());
		d.put("minor",ibeacon.getMinor());
		d.put("rssi",ibeacon.getRssiValue());
		d.put("power",ibeacon.getPowerValue());
		d.put("proximity",ibeacon.getProximity());
		d.put("uuid",ibeacon.getUuidHexString());
		d.put("uuid-dashed",ibeacon.getUuidHexStringDashed());
		event.put("device", d);
	    
	      // Success-Callback
	      if (region!=null){
		region.call(getKrollObject(), event);
	      }
		
	}
	
	@Kroll.method
	public void startScanning(){
	    ibp = IBeaconProtocol.getInstance(activity);	    
	    ibp.setListener(this);
	    TimerTask searchIbeaconTask = new TimerTask() {	
			@Override
			public void run() {
				activity.runOnUiThread(new Runnable() {					
					@Override
					public void run() {
						scanBeacons();
					}
				});
			}
		};	
		timer = new Timer();
		timer.scheduleAtFixedRate(searchIbeaconTask, 1000, seconds*1000);
	}
	
	@Kroll.method
	public void stopScanning(){
	  Log.d("BEACON","stop scanning");
	  isRunning =false;
	  timer.cancel();
	}
	
	
	@Kroll.method
	public boolean isEnabled(){
	/*
	  BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	  if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
	    return false;
	  }*/
	
	  ibp = IBeaconProtocol.getInstance(activity);
	  return IBeaconProtocol.initializeBluetoothAdapter(activity);
	}
	
	
	@Kroll.method
	public boolean isRunning(){
	  return isRunning;
	}
	
	private void scanBeacons(){
		// Check Bluetooth every time
		isRunning =true;
		Log.d("BEACON","Scanning...");
		ibp = IBeaconProtocol.getInstance(activity);
		
		// Filter based on default easiBeacon UUID, remove if not required
		//ibp.setScanUUID(UUID);

		if(!IBeaconProtocol.initializeBluetoothAdapter(activity)){
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			activity.startActivityForResult(enableBtIntent, REQUEST_BLUETOOTH_ENABLE );
			
		} else{
			ibp.setListener(this);
			if(ibp.isScanning())
				ibp.scanIBeacons(false);
			ibp.reset();
			ibp.scanIBeacons(true);		
		}		
	}
	
	public void sendData(){
	  
	  ArrayList<IBeacon> beacons = ibp.getIBeaconsByProximity();
	  HashMap<String, KrollDict[]> event = new HashMap<String, KrollDict[]>();
	  KrollDict[] dList = new KrollDict[beacons.size()]; 
	 
	  Log.d("BEACON","returning: "+beacons.size());
	  for (int i=0; i<beacons.size();++i){
	    KrollDict d = new KrollDict();
	    
	    IBeacon beacon = beacons.get(i);
	    
	    d.put("mac",beacon.getMacAddress());
	    d.put("major",beacon.getMajor());
	    d.put("minor",beacon.getMinor());
	    d.put("rssi",beacon.getRssiValue());
	    d.put("power",beacon.getPowerValue());
 	    d.put("proximity",beacon.getProximity());
 	    d.put("uuid",beacon.getUuidHexString());
 	    d.put("uuid-dashed",beacon.getUuidHexStringDashed());
	    dList[i]=d;
	  }
	  event.put("devices", dList);
	 
	  // Success-Callback
	  if (success!=null){
	    success.call(getKrollObject(), event);
	  }
	}
	
}


