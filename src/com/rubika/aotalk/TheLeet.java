package com.rubika.aotalk;

import android.app.Application;
import android.content.Context;

public class TheLeet extends Application {
	private static Context context;

	public void onCreate() {
		super.onCreate();
		//super.setTheme(R.style.Theme_AOTalkTheme_Light);
		
		TheLeet.context = getApplicationContext();
	}

	public static Context getContext() {
		return TheLeet.context;
	}
}