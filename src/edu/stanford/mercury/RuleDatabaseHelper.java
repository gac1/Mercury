package edu.stanford.mercury;

import java.io.IOException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

public class RuleDatabaseHelper extends SQLiteOpenHelper {
	
	private final static String DATABASE_PATH = "/data/data/edu.stanford.mercury/databases/";
	private final static String DATABASE_NAME = "rules.db";
	private final static int DATABASE_VERSION = 1;
	
	// Table name
	public final static String TABLE = "rules";
	
	// Columns 
	public final static String APP_NAME = "app_name";
	public final static String ETHER_TYPE = "ether_type";
	public final static String PROTO = "protocol";
	public final static String TRANS_PORT_DST = "port_dst";
	public final static String TRANS_PORT_SRC = "port_src";
	public final static String DIRECTION = "direction";
	public final static String ACTION = "action";
		
	private SQLiteDatabase mDatabase;
	
	private static String[] PROJECTION_ROW = new String [] {
		"_id", APP_NAME, ETHER_TYPE, PROTO, TRANS_PORT_DST, TRANS_PORT_SRC, DIRECTION, ACTION
	};
	
	private static String[] PROJECTION_DECISION = new String [] {
		"_id", APP_NAME, ETHER_TYPE, PROTO, TRANS_PORT_DST, TRANS_PORT_SRC, DIRECTION, ACTION
	};
		
	public RuleDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);		
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
		if (oldVersion >= newVersion) {
			return;
		}
    }
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		mDatabase = db;
		String mSQL = "CREATE TABLE IF NOT EXISTS " + TABLE + " ( _id" 
			+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + APP_NAME + " TEXT, " 
			+ ETHER_TYPE + " INTEGER, " + PROTO + " INTEGER, " 
			+ TRANS_PORT_DST + " INTEGER, " + TRANS_PORT_SRC + " INTEGER, " + DIRECTION + " INTEGER, "
			+ ACTION + " TEXT);";
		mDatabase.execSQL(mSQL);
	}	
	
	public void createDatabase() throws IOException {
		boolean dbExists = checkDatabase();
		
		if (!dbExists) {			
			this.getWritableDatabase();
		}
	}
	
	private boolean checkDatabase() {
		SQLiteDatabase checkDB = null;
		
		try {
			String mPath = DATABASE_PATH + DATABASE_NAME;
			checkDB = SQLiteDatabase.openDatabase(mPath, null, SQLiteDatabase.OPEN_READONLY);			
		} catch (SQLiteException e) {
			// database doesn't exist
		}
		
		if (checkDB != null) {
			checkDB.close();
		}
		
		return checkDB != null ? true : false;
	}
	
	@Override
    public synchronized void close() {
		if(mDatabase != null) {
			mDatabase.close();
		}		
		super.close();
    }	

    public void openDatabase() throws SQLException {
             
    	String myPath = DATABASE_PATH + DATABASE_NAME;
    	mDatabase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE); 
    }
    
    public void insertRow(String application_name, Integer ether_type, Integer protocol, 
    		Integer trans_port_dst, Integer trans_port_src, Integer direction, String action) {    	
    	ContentValues mValues = new ContentValues();
    	mValues.put(APP_NAME, application_name);
    	mValues.put(ETHER_TYPE, ether_type);
    	mValues.put(PROTO, protocol);
    	mValues.put(TRANS_PORT_DST, trans_port_dst);
    	mValues.put(TRANS_PORT_SRC, trans_port_src);
    	mValues.put(DIRECTION, direction);
    	mValues.put(ACTION, action);
    	mDatabase.insert(TABLE, null, mValues);
    }
    
    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs, 
    		String groupBy, String having, String orderBy) {
    	return mDatabase.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
    }
    
    public Cursor getRows() {
    	return mDatabase.query(TABLE, PROJECTION_ROW, null, null, null, null, null);
    }
    
    public Cursor getDecision(String application_name) {
    	String selection = APP_NAME + " = '" + application_name + "'";
    	return mDatabase.query(TABLE, PROJECTION_DECISION, selection, null, null, null, null);
    }
}