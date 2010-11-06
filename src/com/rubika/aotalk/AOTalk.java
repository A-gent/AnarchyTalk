package com.rubika.aotalk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import ao.chat.AOChatBot;
import ao.misc.AONameFormat;
import ao.protocol.AOBot;
import ao.protocol.AOBotListener;
import ao.protocol.AOCharacter;
import ao.protocol.AODimensionAddress;
import ao.protocol.AOBot.State;
import ao.protocol.packets.AOPacket;
import ao.protocol.packets.bi.AOGroupMessagePacket;
import ao.protocol.packets.bi.AOPrivateMessagePacket;
import ao.protocol.packets.in.AOCharListPacket;
import ao.protocol.packets.in.AOChatNoticePacket;
import ao.protocol.packets.in.AOGroupAnnouncePacket;


public class AOTalk extends Activity {
	private String PASSWORD  = "";
	private String USERNAME  = "";
	private String D_STRING  = "ANARCHYTALK";
	private String PREFSNAME = "ANARCHYTALK";
	private boolean SAVEPREF = false;
	
	protected static final int MSG_CONNECTED     = 0x101;
	protected static final int MSG_FAILURE       = 0x102;
	protected static final int MSG_STARTED       = 0x103;
	protected static final int MSG_CHARLIST      = 0x104;
	protected static final int MSG_TELL          = 0x105;
	protected static final int MSG_GROUP         = 0x106;
	protected static final int MSG_NOTICE        = 0x107;
	protected static final int MSG_DISCONNECTED  = 0x108;
	protected static final int MSG_AUTHENTICATED = 0x109;
	protected static final int MSG_LOGGED_IN     = 0x110;

	private final String SRV_RK1 = "Atlantean";
	private final String SRV_RK2 = "Rimor";
	
	private String CHANNEL_MSG = "Private Message";
	private String CHATCHANNEL = "";
	private String MESSAGETO   = "";
	private String SERVER      = "Atlantean"; //Sets default value
	
	private Button channelbutton;
	private Context context;
	private AOCharacter aochar;
	private AOChatBot aobot;
	private TextView logtext;
	private ScrollView textscroll;
	private AOCharListPacket charpacket;
	private EditText msginput;
	
	private AOPrivateMessagePacket lastmessage  = null;	
	private AOGroupMessagePacket lastgroupmess  = null;
	private AOChatNoticePacket lastnotice       = null;
	
	private List<String> groupList;
	private List<String> groupIgnore;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        SharedPreferences settings = getSharedPreferences(PREFSNAME, 0);
        SAVEPREF = settings.getBoolean("savepref", SAVEPREF);
        USERNAME = settings.getString("username", USERNAME);
        PASSWORD = settings.getString("password", PASSWORD);
        CHATCHANNEL = settings.getString("chatchannel", CHATCHANNEL);
        MESSAGETO = settings.getString("messageto", MESSAGETO);
        SERVER = settings.getString("server", SERVER);
       
        //Disable automatic popup of keyboard at launch
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN); 
        
        groupList   = new ArrayList<String>();
        
        groupIgnore = new ArrayList<String>();
        groupIgnore.add("Tower Battle Outcome");
        groupIgnore.add("Tour Announcements");
        groupIgnore.add("IRRK News Wire");
        groupIgnore.add("Org Msg");

        context = this;
                
        logtext    = (TextView) findViewById(R.id.logtext);
        textscroll = (ScrollView) findViewById(R.id.scrolltext);

        channelbutton = (Button) findViewById(R.id.msgchannel);
        channelbutton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setChannel();
			}
		});
        channelbutton.setVisibility(View.GONE);
		
		if(AOTalk.this.CHATCHANNEL != "") {
			if(AOTalk.this.CHATCHANNEL == AOTalk.this.CHANNEL_MSG) {
    	    	AOTalk.this.channelbutton.setText("Tell: " + AONameFormat.format(AOTalk.this.MESSAGETO));	
	    	} else {
    	    	AOTalk.this.channelbutton.setText(CHATCHANNEL);	    	    		
	    	}
		} else {
	    	AOTalk.this.channelbutton.setText("Select channel");	  
		}
        
        msginput = (EditText) findViewById(R.id.msginput);
        msginput.setOnKeyListener(new OnKeyListener() {
			@Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
					if(aobot.getState() == ao.protocol.AOBot.State.LOGGED_IN) { 
						//cp.parse(String) adds "\n" and timestamp to the beginning of the string and remove html-tags from it.
						ChatParser cp = new ChatParser();
						
	                	try {
	                		Log.d(D_STRING, CHATCHANNEL);
	                		
	                		//Send tell
	                		if(CHATCHANNEL == CHANNEL_MSG) { 
	                			AOTalk.this.aobot.sendTell(MESSAGETO, msginput.getText().toString(), true);
								AOTalk.this.logtext.append(
									cp.parse("to [Novagen]: " + msginput.getText().toString())
								);
	                		} else { //Send to group
	                			AOTalk.this.aobot.sendGMsg(CHATCHANNEL, msginput.getText().toString());
	                		}
	                		
							Log.d(D_STRING, "Sent message : " + msginput.getText().toString());		
							
							AOTalk.this.msginput.setText("");
							AOTalk.this.scrollLog();
	                	} catch (IOException e) {
	                		AOTalk.this.logtext.append(cp.parse("\nFailed to send message : " + e.getMessage()));
						}
	                	return true;
					} else {
                	Log.d(D_STRING, "Not logged in, can't send message");
                	}
                }
				
                return false;
            }
        });
        msginput.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AOTalk.this.scrollLog();
			}
		});
        msginput.setVisibility(View.GONE);

    }
 
    private void scrollLog() {
		AOTalk.this.textscroll.post(new Runnable() {
	        @Override
	        public void run() {
	        	AOTalk.this.textscroll.fullScroll(ScrollView.FOCUS_DOWN);
	        }
	    });    	
    }
    
    private void setChannel() {
    	CharSequence[] tempChannels = null;
    	
    	if(AOTalk.this.groupList != null) {
    		tempChannels = new CharSequence[AOTalk.this.groupList.size()];
	    	for(int i = 0; i < AOTalk.this.groupList.size(); i++) {
	    		if(i == 0) {
	    			tempChannels[i] = AOTalk.this.CHANNEL_MSG;
	    		} else {
	    			tempChannels[i] = AOTalk.this.groupList.get(i - 1);
	    		}
	    	} 
    	}
     	
    	final CharSequence[] channellist = tempChannels;

    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Select channel");
    	
    	builder.setItems(channellist, new DialogInterface.OnClickListener() {
    	    public void onClick(DialogInterface dialog, int item) {
         	    AOTalk.this.CHATCHANNEL = channellist[item].toString();
         	    
         	    if(channellist[item].toString() == AOTalk.this.CHANNEL_MSG) {
     	        	LayoutInflater inflater = (LayoutInflater)AOTalk.this.getSystemService(LAYOUT_INFLATER_SERVICE);
    	            final View layout = inflater.inflate(R.layout.sendto,(ViewGroup) findViewById(R.layout.sendto));
    	            
    	        	Builder builder = new AlertDialog.Builder(context);
    	        	builder.setTitle(getResources().getString(R.string.sendtotitle));
    	        	builder.setView(layout);
    	        	
    	    		EditText TargetEditText = (EditText) layout.findViewById(R.id.targetname);
    	    		TargetEditText.setText(AOTalk.this.MESSAGETO);
    	        	
    	        	builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
    	    			public void onClick(DialogInterface dialog, int which) {
    	    				EditText TargetEditText = (EditText) layout.findViewById(R.id.targetname);
    	    				AOTalk.this.MESSAGETO = TargetEditText.getText().toString();
    	    				
			    	    	if(AOTalk.this.CHATCHANNEL == AOTalk.this.CHANNEL_MSG) {
				    	    	AOTalk.this.channelbutton.setText("Tell: " + AONameFormat.format(AOTalk.this.MESSAGETO));	
			    	    	} else {
				    	    	AOTalk.this.channelbutton.setText(CHATCHANNEL);	    	    		
			    	    	}    	    				
    	    				return;
    	    			}
    	    		});
    	        	
    	        	AlertDialog targetbox = builder.create();
    	        	targetbox.show();    	    		
    	    	} else {
        	    	if(AOTalk.this.CHATCHANNEL == AOTalk.this.CHANNEL_MSG) {
    	    	    	AOTalk.this.channelbutton.setText("Tell: " + AONameFormat.format(AOTalk.this.MESSAGETO));	
        	    	} else {
    	    	    	AOTalk.this.channelbutton.setText(CHATCHANNEL);	    	    		
        	    	}    	    		
    	    	}
    	    }
    	});
    	
    	AlertDialog channels = builder.create();
    	channels.show();
    }
    
    @SuppressWarnings("unused")
	private boolean isOnline() {
    	 ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    	 return cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }
    
    private void selectCharacter() {
    	if(charpacket != null) {
	    	CharSequence names[] = new CharSequence[charpacket.getNumCharacters()];
	    	
    		for(int i = 0; i < charpacket.getNumCharacters(); i++) {
    			names[i] = charpacket.getCharacter(i).getName();
	    	}

	    	final CharSequence[] charlist = names;
	
	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    	builder.setTitle("Select character");
	    	builder.setItems(charlist, new DialogInterface.OnClickListener() {
	    	    public void onClick(DialogInterface dialog, int item) {
	    	    	aochar = charpacket.findCharacter(AONameFormat.format(charlist[item].toString()));
	    	    	login();
	    	    }
	    	});
	    	
	    	AlertDialog characters = builder.create();
	    	characters.show();
    	} else {
    		Log.d(D_STRING, "Packet is NULL");
    	}
    }
    
    private void boot() {
        if(AOTalk.this.aobot != null) {
        	AOTalk.this.aobot = null;
        }
        AOTalk.this.aobot = new AOChatBot();
        AOTalk.this.aobot.addListener(new AOBotListener() {
			@Override
			public void authenticated(AOBot bot) {
				Message m = new Message();
				m.what = AOTalk.MSG_AUTHENTICATED; 
				AOTalk.this.ConnectionLogHandler.sendMessage(m);
			}

			@Override
			public void connected(AOBot bot) {
				Message m = new Message();
				m.what = AOTalk.MSG_CONNECTED; 
				AOTalk.this.ConnectionLogHandler.sendMessage(m); 
				
				authenticate();
			}

			@Override
			public void disconnected(AOBot bot) {
				Message m = new Message();
				m.what = AOTalk.MSG_DISCONNECTED; 
				AOTalk.this.ConnectionLogHandler.sendMessage(m);
			}

			@Override
			public void exception(AOBot bot, Exception e) {}

			@Override
			public void loggedIn(AOBot bot) {
				Message m = new Message();
				m.what = AOTalk.MSG_LOGGED_IN; 
				AOTalk.this.ConnectionLogHandler.sendMessage(m);
				
				startBot();
			}

			@Override
			public void packet(AOBot bot, AOPacket packet) {
				//Character list packet
				if(packet.getType() == AOCharListPacket.TYPE) {
					charpacket = (AOCharListPacket) packet;
					
					Message m = new Message();
					m.what = AOTalk.MSG_CHARLIST; 
					AOTalk.this.ConnectionLogHandler.sendMessage(m);
				}
				
				//Private message
				if(packet.getType() == AOPrivateMessagePacket.TYPE && packet.getDirection() == AOPacket.Direction.IN) {
					AOPrivateMessagePacket tell = (AOPrivateMessagePacket) packet;
					lastmessage = tell;
						
					Message m = new Message();
					m.what = AOTalk.MSG_TELL; 
					AOTalk.this.ConnectionLogHandler.sendMessage(m);
				}
				
				//Chat group message
				if(packet.getType() == AOGroupMessagePacket.TYPE && packet.getDirection() == AOPacket.Direction.IN) {
					AOGroupMessagePacket group = (AOGroupMessagePacket) packet;
					lastgroupmess = group;
						
					Message m = new Message();
					m.what = AOTalk.MSG_GROUP; 
					AOTalk.this.ConnectionLogHandler.sendMessage(m);
				}
				
				//Chat notice
				if(packet.getType() == AOChatNoticePacket.TYPE && packet.getDirection() == AOPacket.Direction.IN) {
					AOChatNoticePacket notice = (AOChatNoticePacket) packet;
					lastnotice = notice;
						
					Message m = new Message();
					m.what = AOTalk.MSG_NOTICE; 
					//AOTalk.this.ConnectionLogHandler.sendMessage(m);
				}
				
				//Group announcement
				if(packet.getType() == AOGroupAnnouncePacket.TYPE) {
					AOGroupAnnouncePacket group = (AOGroupAnnouncePacket) packet;
					Log.d(D_STRING, "Got group : " + group.getGroupName());
											
					if(!AOTalk.this.groupIgnore.contains(group.getGroupName())) {
						AOTalk.this.groupList.add(group.getGroupName());
					}
				}
			}
			
			@Override
			public void started(AOBot bot) {
				Message m = new Message();
				m.what = AOTalk.MSG_STARTED; 
				AOTalk.this.ConnectionLogHandler.sendMessage(m);
			}
		});
        
        selectServer();
    }
    
	private void selectServer() {
    	final CharSequence servers[] = {"Atlantean", "Rimor"};

    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Select server");
    	builder.setItems(servers, new DialogInterface.OnClickListener() {
    	    public void onClick(DialogInterface dialog, int item) {
    	    	SERVER = servers[item].toString();
    	    	connect();
    	    }
    	});
    	
    	AlertDialog serverlist = builder.create();
    	serverlist.show();
    }
    
	Handler ConnectionLogHandler = new Handler(){ 
		public void handleMessage(Message msg) { 
			@SuppressWarnings("unused")
			State CONNECTED = State.CONNECTED;
			@SuppressWarnings("unused")
			State AUTHENTICATED = State.AUTHENTICATED;
			@SuppressWarnings("unused")
			State DISCONNECTED = State.DISCONNECTED;
			@SuppressWarnings("unused")
			State LOGGED_IN = State.LOGGED_IN;
			
			ChatParser cp = new ChatParser();
				        	
			switch (msg.what) { 				
				case AOTalk.MSG_CONNECTED:
					AOTalk.this.logtext.append(cp.parse("CONNECTED"));
					break;
					
				case AOTalk.MSG_AUTHENTICATED:
					AOTalk.this.logtext.append(cp.parse("AUTHENTICATED"));
					AOTalk.this.logtext.append(cp.parse("ACCOUNT : " + USERNAME));
					break;
					
				case AOTalk.MSG_LOGGED_IN:
					AOTalk.this.logtext.append(cp.parse("LOGGED_IN"));
					AOTalk.this.logtext.append(cp.parse("CHARACTER : " + AOTalk.this.aochar.getName()));
					break;
					
				case AOTalk.MSG_DISCONNECTED:
					AOTalk.this.logtext.append(cp.parse("DISCONNECTED"));
					AOTalk.this.msginput.setVisibility(View.GONE);
					AOTalk.this.channelbutton.setVisibility(View.GONE);
					break;
					
				case AOTalk.MSG_FAILURE:
					AOTalk.this.logtext.append(cp.parse("FAILURE"));
					break;
					
				case AOTalk.MSG_STARTED:
					AOTalk.this.logtext.append(cp.parse("READY"));
					AOTalk.this.msginput.setVisibility(View.VISIBLE);
					AOTalk.this.channelbutton.setVisibility(View.VISIBLE);
					break;
					
				case AOTalk.MSG_CHARLIST:
					selectCharacter();
					break;
					
				case AOTalk.MSG_TELL:
					if(lastmessage != null) {
						AOTalk.this.logtext.append(
							cp.parse(
								AOTalk.this.lastmessage.display(
									AOTalk.this.aobot.getCharTable(), 
									AOTalk.this.aobot.getGroupTable()
								)
							)
						);
					}
					break;
					
				case AOTalk.MSG_GROUP:
					if(lastgroupmess != null) {
						AOTalk.this.logtext.append(
							cp.parse(
								AOTalk.this.lastgroupmess.display(
									AOTalk.this.aobot.getCharTable(), 
									AOTalk.this.aobot.getGroupTable()
								)
							)
						);
					}
					break;

				case AOTalk.MSG_NOTICE:
					if(lastnotice != null) {
						AOTalk.this.logtext.append(cp.parse(AOTalk.this.lastnotice.toString()));
					}
					break;
			} 
			
			scrollLog();			
			super.handleMessage(msg); 
		} 
	};
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.mainmenu, menu);
        return true;
    }
    
    private void connect() {
    	LayoutInflater inflater = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View layout = inflater.inflate(R.layout.login,(ViewGroup) findViewById(R.layout.login));
    	Builder builder = new AlertDialog.Builder(context);
    	builder.setTitle(getResources().getString(R.string.logintitle));
    	builder.setView(layout);
    	
		EditText UserEditText = (EditText) layout.findViewById(R.id.username);
		EditText PassEditText = (EditText) layout.findViewById(R.id.password);
		CheckBox SavePrefs    = (CheckBox) layout.findViewById(R.id.savepassword);
		
		UserEditText.setText(USERNAME);
    	PassEditText.setText(PASSWORD);
    	SavePrefs.setChecked(SAVEPREF);
    	
    	builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {  	                						
				AODimensionAddress tempConnect = null;
				
				if(AOTalk.this.SERVER == AOTalk.this.SRV_RK1) {
					tempConnect = AODimensionAddress.RK1;
				} else if(AOTalk.this.SERVER == AOTalk.this.SRV_RK2) {
					tempConnect = AODimensionAddress.RK2;
	        	} else {
	            	Log.d(D_STRING, "No server selected : " + AOTalk.this.SERVER);				            		
	        		selectServer();
	        	}
				
				if(tempConnect != null) {
					Log.d(D_STRING, "Server selected : " + tempConnect.getName());
					final AODimensionAddress connectTo = tempConnect;
				
					EditText UserEditText = (EditText) layout.findViewById(R.id.username);
					EditText PassEditText = (EditText) layout.findViewById(R.id.password);
					CheckBox SavePrefs    = (CheckBox) layout.findViewById(R.id.savepassword);
					
					AOTalk.this.USERNAME = UserEditText.getText().toString();
					AOTalk.this.PASSWORD = PassEditText.getText().toString();
					AOTalk.this.SAVEPREF = SavePrefs.isChecked();
					
					final ProgressDialog loader = new ProgressDialog(context);
			    	loader.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			    	loader.setTitle(getResources().getString(R.string.connecting));
					loader.setMessage(getResources().getString(R.string.please_wait));
					loader.show();
					
					new Thread() {
			            public void run() {
				    		if(aobot.getState() == ao.protocol.AOBot.State.DISCONNECTED) {
				                try {
				                	aobot.connect(connectTo);
					            	Log.d(D_STRING, "Connected");
					    		} catch (IOException e) {
					    			Message m = new Message();
									m.what = AOTalk.MSG_FAILURE; 
									AOTalk.this.ConnectionLogHandler.sendMessage(m); 
									
					    			Log.d(D_STRING, "Failed to connect : " + e.getMessage());
					    		}
				    		} else {
								Log.d(D_STRING, "Failed to connect : Already connected");
				    		}
				    		loader.dismiss();
			        	}
					}.start();
				} else {
					Log.d(D_STRING, "No server found");
				}
				
				return;
			} 
		});
		
    	builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				return;
			}
		}); 
    	
    	AlertDialog loginbox = builder.create();
    	loginbox.show();
	}
    
    private void authenticate() {
    	//Authenticating
		if(aobot.getState() == ao.protocol.AOBot.State.CONNECTED) {
			try {
				aobot.authenticate(USERNAME, PASSWORD);
				Log.d(D_STRING, "Authenticated");
			} catch (IOException e) {
				Log.d(D_STRING, "Failed to authenticate : " + e.getMessage());
			}
		} else {
			Log.d(D_STRING, "Failed to authenticate : Not connected");
		}
    }
    
    private void login() {  	
    	//Logging in character  	
    	if(aobot.getState() == ao.protocol.AOBot.State.AUTHENTICATED) {
			try {
				aobot.login(aochar);
				Log.d(D_STRING, "Logged in"); 								
			} catch (IOException e) {
				Log.d(D_STRING, "Failed to log in : " + e.getMessage());
			}
		} else {
			Log.d(D_STRING, "Failed to log in : Not connected or authenticated");
		}
	}
    
    private void startBot() {
		// Starting bot
    	if(aobot.getState() == ao.protocol.AOBot.State.LOGGED_IN) {
        	aobot.start();
			Log.d(D_STRING, "Started"); 	
		} else {
			Log.d(D_STRING, "Failed to start : Not logged in");					
		}
    }
    
    private void disconnect() {
	    //Disconnecting from server
		if(aobot.getState() != ao.protocol.AOBot.State.DISCONNECTED) {
			try {
				aobot.disconnect();
				Log.d(D_STRING, "Disconnected");
				
				System.gc();
			} catch (IOException e) {
				Log.d(D_STRING, "Failed to disconnect : " + e.getMessage());
			}
		} else {
			Log.d(D_STRING, "Failed to disconnect : Not connected");
		}    	
    }
    
    private void clearLog() {
    	AOTalk.this.logtext.setText("");
    }
    
    private void settings() {
    	LayoutInflater inflater = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View layout = inflater.inflate(R.layout.settings,(ViewGroup) findViewById(R.layout.settings));
    	Builder builder = new AlertDialog.Builder(context);
    	builder.setTitle(getResources().getString(R.string.settings));
    	builder.setView(layout);
    	
    	builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {  	                						
				return;
			} 
		});
		
    	/*
    	builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				return;
			}
		}); 
		*/
    	
    	AlertDialog settingsbox = builder.create();
    	settingsbox.show();    	
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
	        case R.id.connect:
	        	boot();
	            return true;
	        case R.id.disconnect:
	        	disconnect();
	        	return true;
	        case R.id.clear:
	        	clearLog();
	        	return true;
	        case R.id.settings:
	        	settings();
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    protected void onResume(){
    	super.onResume();

    	SharedPreferences settings = getSharedPreferences(PREFSNAME, 0);
    	SAVEPREF = settings.getBoolean("savepref", false);
        USERNAME = settings.getString("username", "");
        PASSWORD = settings.getString("password", "");
        CHATCHANNEL = settings.getString("chatchannel", CHATCHANNEL);
        MESSAGETO = settings.getString("messageto", "");
        SERVER = settings.getString("server", SERVER);
	}
    
    @Override
    protected void onPause(){
    	super.onPause();

		SharedPreferences settings = getSharedPreferences(PREFSNAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		
		editor.putBoolean("savepref", SAVEPREF);
		editor.putString("chatchannel", CHATCHANNEL);
		editor.putString("messageto", MESSAGETO);	
		editor.putString("server", SERVER);	
		
		if(SAVEPREF) {
			editor.putString("username", USERNAME);
			editor.putString("password", PASSWORD);
		} else {
			editor.putString("username", "");
			editor.putString("password", "");			
		}
		
		editor.commit();
	}
    
    @Override
    protected void onStop(){
    	super.onStop();
    	
    	if(aobot != null) {
    		if(aobot.getState() != ao.protocol.AOBot.State.DISCONNECTED) {
    			try {
					aobot.disconnect();
				} catch (IOException e) {
					Log.d(D_STRING, "Failed to disconnect : " + e.getMessage());
				}
    		}
    	}

		SharedPreferences settings = getSharedPreferences(PREFSNAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		
		editor.putBoolean("savepref", SAVEPREF);
		editor.putString("chatchannel", CHATCHANNEL);	
		editor.putString("messageto", MESSAGETO);	
		editor.putString("server", SERVER);	
		
		if(SAVEPREF) {
			editor.putString("username", USERNAME);
			editor.putString("password", PASSWORD);
		} else {
			editor.putString("username", "");
			editor.putString("password", "");			
		}
		
		editor.commit();
	}
}