package edu.stanford.mercury.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.openflow.protocol.OFMatch;

import net.holyc.HolyCIntent;

import com.google.gson.Gson;

import edu.stanford.lal.LalPermTester;
import edu.stanford.lal.permission.PermissionInquiry;
import edu.stanford.mercury.R;
import edu.stanford.mercury.MercuryMessage;
import edu.stanford.mercury.RuleDatabaseHelper;
import edu.stanford.mercury.RuleService;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class MercuryList extends ListActivity implements OnItemClickListener, OnClickListener {
	
	private String TAG = "MercuryStart";
	private String mRuleServiceName;
	private boolean mRuleServiceIsBound = false;
	private Messenger mRuleService = null;
	private ListView mListView;
    private ListItemAdapter mListAdapter;
    private RuleDatabaseHelper mDatabaseHelper;
    private Cursor mFirewall;
    private Gson mGson = new Gson();
    private PermissionInquiry mInquiry;
	private String mApplicationName;
	private OFMatch mOFMatch;
    
    private ArrayList<ListItem> mListItems = new ArrayList<ListItem>();
    
    private MercuryMessage mMessage = new MercuryMessage();
	
	/** Handler for messages passed to show on the screen */
	private Handler mHandler = new IncomingHandler();
	
	/** Target we publish for clients to send messages to IncomingHandler. */
	public final Messenger mMessenger = new Messenger(mHandler);
	
	/**
	 * Broadcast receiver
	 */
	BroadcastReceiver bReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(MercuryMessage.Result.action)) {
				String r = intent.getStringExtra(MercuryMessage.Result.str_key);
				Log.d(TAG, r);
				MercuryMessage.RuleResult result = mGson.fromJson(r,
						MercuryMessage.RuleResult.class);

				for (int i = 0; i < result.results.size(); i++) {
					mListItems.add(new ListItem(result.results.get(i)));
				}
				mListAdapter.notifyDataSetChanged();
			}
		}

	};
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	 super.onCreate(savedInstanceState);
         mRuleServiceName = this.getPackageName()+".RuleService";
         doStartRuleService();
         doBindRuleService();
         
         IntentFilter mIntentFilter = new IntentFilter();
 		 mIntentFilter.addAction(MercuryMessage.Result.action);
 		 registerReceiver(bReceiver, mIntentFilter);
         
         setContentView(R.layout.firewall_list);
         
         mListView = getListView();
             	 
         mListAdapter = new ListItemAdapter(this);       
         this.setListAdapter(mListAdapter);  
         Bundle mExtras = getIntent().getExtras(); 
         if (mExtras != null) {
     		Log.i(TAG, "Saved Instance State Exists!");
         }
         
         if (mExtras != null) {
        	 Log.i(TAG, "Should be calling the notification!");
        	 String response_json = mExtras.getString(MercuryMessage.NetworkActionQuery.str_key);
			 mInquiry = mGson.fromJson(response_json, PermissionInquiry.class);
			 mOFMatch = mInquiry.getOFMatch();
			 mApplicationName = mInquiry.getAppName();
			 notificationDialog();
         }
         Log.i(TAG, "After Notification call!");
         
         if (mExtras == null) {
        	 //startService(new Intent(this, LalPermTester.class));
         }
    }    
    
    @Override
	protected void onStart() {
		super.onStart();		

	}
    
    @Override
    public void onStop() {
    	super.onStop();
    	if (mDatabaseHelper != null) {
    		mDatabaseHelper.close();
    	}
    	if (mFirewall != null) {
    		mFirewall.close();
    	}
    }
    
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	doUnbindRuleService();
    }
    
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    	// On list item click
    }
    
    public void onClick(View view) {
    	
    }
    
    @Override 
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.rule_list_context, menu);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    	Integer mPosition = info.position;
    	switch (item.getItemId()) {
    		case R.id.remove_entry:
    			//Remove Entry
    			return true;    		
    		default:
    			return super.onContextItemSelected(item);
    	}
    }
    
    private void notificationDialog() {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage("Add Allow/Deny Firewall Rule for:\n" +
    			mApplicationName)
    	       .setCancelable(false)
    	       .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	        	   	Intent i = new Intent(MercuryMessage.NetworkActionResult.action);
    	   				i.setPackage(getPackageName());
    	   				i.putExtra(MercuryMessage.NetworkActionResult.str_key,
    	   				"Allow");
    	   				i.putExtra(HolyCIntent.LalPermInquiry.str_key, mGson.toJson(mInquiry, 
    	   						PermissionInquiry.class));
    	   				sendBroadcast(i);
    	   				dialog.cancel();
    	   				finish();
    	           }
    	       })
    	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	        	    Intent i = new Intent(MercuryMessage.NetworkActionResult.action);
   	   					i.setPackage(getPackageName());
   	   					i.putExtra(MercuryMessage.NetworkActionResult.str_key,
   	   					"Deny");
   	   					i.putExtra(HolyCIntent.LalPermInquiry.str_key, mGson.toJson(mInquiry, 
   	   					PermissionInquiry.class));
   	   					sendBroadcast(i);
    	                dialog.cancel();
    	                finish();
    	           }
    	       });
    	AlertDialog alert = builder.create();
    	alert.show();
    }
    
    void doBindRuleService() {		
		Intent intent = new Intent(this, RuleService.class);
		if(isRuleServiceRunning() ){
			Log.d(TAG, "Rule Service is Running!!!");
			//if the service is not running
			doStartRuleService();
		}
	    bindService(intent, mRuleConnection, 0);
	    mRuleServiceIsBound = true;	    
	}
    
    private ComponentName doStartRuleService(){
    	ComponentName c = startService(new Intent(this, RuleService.class));
    	Log.d(TAG, "Starting Service!!!");
    	return c;
    }
    
    void doUnbindRuleService() {
	    if (mRuleServiceIsBound) {
	        // Unregister from the service
	        if (mRuleService != null) {
	            try {
	                Message msg = Message.obtain(null,
	                		MercuryMessage.UNREGISTER_CLIENT.type);
	                msg.replyTo = mMessenger;
	                mRuleService.send(msg);
	            } catch (RemoteException e) {}
	        }
	        // Detach our existing connection.
	        unbindService(mRuleConnection);
	        mRuleServiceIsBound = false;	        
	    }
	}
    
    private boolean isRuleServiceRunning(){
    	boolean isRunning = false;
    	    	
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<RunningServiceInfo> runningServices = activityManager.getRunningServices(50);

        for (int i = 0; i < runningServices.size(); i++) {
        	RunningServiceInfo runningServiceInfo = runningServices.get(i);
        	Log.d(TAG, "running service:" +runningServiceInfo.service.getClassName());
        	if(runningServiceInfo.service.getClassName().equals(mRuleServiceName)){
        		isRunning = true;
        		Log.d(TAG, "Rule Service is running");
        		break;
        	}
        }        
    	
    	return isRunning;
    }
    
    /**
	 * Handler of incoming messages from service.
	 */
	class IncomingHandler extends Handler {
	    @Override
	    public void handleMessage(Message msg) {
	        switch (msg.what) {
	            case MercuryMessage.CONNECTION_NOTICE.type:
					//mMsgBuffer.append(msg.getData().getString(MercuryMessage.CONNECTION_NOTICE.str_key)+"\n");
	                break;
	            default:
	                super.handleMessage(msg);
	        }	        	        
	    }
	}
	
	private ServiceConnection mRuleConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className,
	            IBinder service) {
	    	//get the Messenger of the service
	    	mRuleService = new Messenger(service);
	        Log.d(TAG, "Rule Service Attached");
	        try {
	        	//Send the mMessenger to the service and Register itself to the service
	            Message msg = Message.obtain(null, MercuryMessage.REGISTER_CLIENT.type);
	            msg.replyTo = mMessenger;
	            mRuleService.send(msg);
	        } catch (RemoteException e) {}
	        // Send broadcast query
			MercuryMessage.RuleQuery q = mMessage.new RuleQuery();
			q.columns = new String[] { "app_name", "ether_type",
					"protocol", "port_dst", "port_src", "direction", "action" };				
			q.groupBy = "app_name";		
			Intent i = new Intent(MercuryMessage.Query.action);
			i.setPackage(getPackageName());
			i.putExtra(MercuryMessage.Query.str_key,
					mGson.toJson(q, MercuryMessage.RuleQuery.class));
			sendBroadcast(i);
			Log.i(TAG, "Sent Broadcast Query!");
	        Toast.makeText(getApplicationContext(), R.string.rule_service_bound, Toast.LENGTH_SHORT).show();
	    }
	    public void onServiceDisconnected(ComponentName className) {
	    	mRuleService = null;
	        Toast.makeText(getApplicationContext(), R.string.rule_service_unbound, Toast.LENGTH_SHORT).show();
	    }
	};
	
	public class ListItemAdapter extends BaseAdapter {

		static final String TAG = "ListItemAdapter";
		
		private LayoutInflater mInflator;
     	
    	public ListItemAdapter(Context context) {    		
    		mInflator = LayoutInflater.from(context);
    	}
    	
    	public int getCount() {
    		return mListItems.size();
    	}
    	
    	public ListItem getItem(int position) {
    		return mListItems.get(position);
    	}
    	
    	public long getItemId(int position) {
    		return position;
    	}
    	
    	public View getView(int position, View convertView, ViewGroup parent) {
    		ViewHolder mHolder;
    		
    		if (convertView == null) {
    			convertView = mInflator.inflate(R.layout.firewall_list_item, parent, false);
    			mHolder = new ViewHolder();
    			mHolder.txtAppName = (TextView) convertView.findViewById(R.id.app_name);
    			mHolder.txtEtherType = (TextView) convertView.findViewById(R.id.ether_type);
    			mHolder.txtProtocol = (TextView) convertView.findViewById(R.id.protocol);
    			mHolder.txtPort = (TextView) convertView.findViewById(R.id.port);
    			mHolder.txtAction = (TextView) convertView.findViewById(R.id.action);
    			mHolder.imgIcon = (ImageView) convertView.findViewById(R.id.icon);
			
    			convertView.setTag(mHolder);    			    			
    		} else {
    			mHolder = (ViewHolder) convertView.getTag();
    		}
		
    		mHolder.txtAppName.setText(mListItems.get(position).app_name);
    		mHolder.txtEtherType.setText(mListItems.get(position).ether_type);
    		mHolder.txtProtocol.setText(mListItems.get(position).proto);
    		mHolder.txtPort.setText(mListItems.get(position).port);
    		mHolder.txtAction.setText(mListItems.get(position).action);
    		PackageManager pk = getApplicationContext().getPackageManager();
			try {
				mHolder.imgIcon.setImageDrawable(pk.getApplicationIcon(mListItems.get(position).app_name));
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				mHolder.imgIcon.setImageDrawable(pk.getDefaultActivityIcon());
				Log.d(TAG, "Error: " + e.getMessage());
			}
		 		
    		return convertView;
    	}
    	
       	class ViewHolder {
    		TextView txtAppName;
    		TextView txtEtherType;
    		TextView txtProtocol;
    		TextView txtPort;
    		TextView txtAction;
    		ImageView imgIcon;
    	}
    }
	
	class ListItem {
		
		static final String TAG = "RuleItem";
		
		public String app_name;
		public String ether_type;
		public String proto;
		public String port;
		public String action;

		public ListItem(Vector v) {
			app_name = (String) v.get(0);
			ether_type = (String) v.get(1);
			proto = (String) v.get(2);
			port = (String) v.get(3);
			action = (String) v.get(4);
		}
	}    
}