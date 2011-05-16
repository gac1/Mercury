package edu.stanford.mercury;

import java.io.File;

import android.app.Application;
import android.content.Context;

public class Mercury extends Application {
	public static final String LOG_TAG = "Mercury";
	
	private static File sTempDirectory;
	
	public static void setTempDirectory(Context context) {
		sTempDirectory = context.getCacheDir();
	}
	
	public static File getTempDirectory() {
		if (sTempDirectory == null) {
			throw new RuntimeException(
					"TempDirectory net set.   " + 
					"If in a unit test, call Mercury.setTempDirectory(context) in setUp().");
		}
		return sTempDirectory;		
	}
	
	@Override 
	public void onCreate() {
		super.onCreate();
		setTempDirectory(this);
	}
}