package com.haniokasai.app.pmmp_srv;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MainActivity extends SherlockActivity
{
	final static int
		CONSOLE_CODE = 1,
		INSTALL_PHP_CODE = CONSOLE_CODE + 1,
		DOWNLOAD_SERVER_CODE = INSTALL_PHP_CODE + 1,
		DELETE_SERVER = DOWNLOAD_SERVER_CODE+1,
		BACKUP_SERVER =DELETE_SERVER +1,
		FORCE_CLOSE_CODE = BACKUP_SERVER+1,
		HP=BACKUP_SERVER+2,
		TWITTER=HP+1,
		BL_TWITTER=TWITTER+1,
		HELP=BL_TWITTER+1;

	public static Intent serverIntent=null;
	public static MainActivity instance = null;
	public static CheckBox check_ansi=null;
	public static Button button_start=null,button_stop=null;
	public static SeekBar seekbar_fontsize=null;
	public static MenuItem menu_force_close=null,menu_install_php=null,menu_download_server=null,menu_delete_server=null,menu_backup_server=null,menu_hp=null,menu_twitter=null,menu_bl_twitter=null,menu_help=null;
	public static SharedPreferences config=null;
	private FirebaseAnalytics mFirebaseAnalytics;

	public static boolean isStarted = false,ansiMode=false;

	public static final String[] jenkins_pocketmine=new String[]
	{
		"BlueLight (haniokasai)|http://jenkins.haniokasai.com/job/BlueLight-PMMP/",
		"Genisys (ZXDA)|https://jenkins.zxda.net/job/Genisys/",
		"ClearSky-PHP7 (ZXDA)|https://jenkins.zxda.net/job/ClearSky-PHP7/",
		"PocketMine-MP (ZXDA)|https://jenkins.zxda.net/job/PocketMine-MP/",
		"PocketMine-MP (pmmp)|https://jenkins.pmmp.gq/job/PocketMine-MP/"
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());


		////
		// Obtain the FirebaseAnalytics instance.
		mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

		Bundle fireLogBundle = new Bundle();
		fireLogBundle.putString("TEST", "FireSample app MainActivity.onCreate() is called.");
		mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, fireLogBundle);
		////
		//////ads///
		MobileAds.initialize(getApplicationContext(),"putyourid");
		AdView mAdView = (AdView) findViewById(R.id.adView);
		AdRequest adRequest = new AdRequest.Builder().build();
		mAdView.loadAd(adRequest);
		///////////

		instance=this;
		config=getSharedPreferences("config",0);
		ServerUtils.setContext(instance);
		
		ansiMode=config.getBoolean("ANSIMode",ansiMode);
		
		button_stop=(Button) findViewById(R.id.button_stop);
		button_start=(Button) findViewById(R.id.button_start);
		
		check_ansi=(CheckBox)findViewById(R.id.check_ansi);
		
		seekbar_fontsize=(SeekBar)findViewById(R.id.seekbar_fontsize);
		
		seekbar_fontsize.setProgress(config.getInt("ConsoleFontSize",16));
		seekbar_fontsize.setMax(30);
		
		ConsoleActivity.font_size=seekbar_fontsize.getProgress();
		
		check_ansi.setChecked(ansiMode);
		
		check_ansi.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				ansiMode=check_ansi.isChecked();
				config.edit().putBoolean("ANSIMode",ansiMode).apply();
			}
		});

		
		seekbar_fontsize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			@Override
			public void onProgressChanged(SeekBar p1,int p2,boolean p3)
			{
				
			}

			@Override
			public void onStartTrackingTouch(SeekBar p1)
			{
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar p1)
			{
				ConsoleActivity.font_size=p1.getProgress();
				config.edit().putInt("ConsoleFontSize",p1.getProgress()).apply();
			}
		});

		button_start.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				isStarted=true;
				refreshEnabled();
				serverIntent=new Intent(instance,ServerService.class);
				startService(serverIntent);
				ServerUtils.runServer();
			}
		});
		button_stop.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v)
			{
				if(ServerUtils.isRunning())
				{
					ServerUtils.writeCommand("stop");
				}
			}
		});
		refreshEnabled();
		installphp();
	}

	public static void installBusybox() throws Exception
	{
		copyAsset("busybox",ServerUtils.getAppDirectory()+"/busybox");
		new File(ServerUtils.getAppDirectory()+"/busybox").setExecutable(true,true);
	}

	public static void refreshEnabled()
	{
		check_ansi.setEnabled(!isStarted);
		if(menu_install_php!=null)
		{
			menu_install_php.setEnabled(!isStarted);
			menu_download_server.setEnabled(!isStarted);
		}
		if(!new File(ServerUtils.getAppDirectory()+"/php").exists())
		{
			button_start.setEnabled(false);
		}
		else
		{
			button_start.setEnabled(!isStarted);
		}
		button_stop.setEnabled(isStarted);


	}


	public static void copyAsset(String name,String target) throws Exception
	{
		File tmp=new File(target);
		tmp.delete();
		OutputStream os=new FileOutputStream(tmp);
		InputStream is=instance.getAssets().open(name);
		int cou=0;
		byte[] buffer=new byte[8192];
		while((cou=is.read(buffer))!=-1)
		{
			os.write(buffer,0,cou);
		}
		is.close();
		os.close();
	}

	public static void stopNotifyService()
	{
		if(instance != null && serverIntent != null)
		{
			instance.runOnUiThread(new Runnable()
			{
				public void run()
				{
					isStarted=false;
					refreshEnabled();
					instance.stopService(serverIntent);
				}
			});
		}
	}
	
	public static String getInternetString(String url)
	{
		try
		{
			BufferedReader reader=new BufferedReader(new InputStreamReader(openNetConnection(url).getInputStream()));
			StringBuilder sb=new StringBuilder();
			String line=null;
			while((line=reader.readLine())!=null)
			{
				sb.append(line).append('\r');
			}
			reader.close();
			return sb.toString();
		}
		catch(Exception e)
		{
			instance.toast(e.toString());
		}
		return null;
	}

	public static void downloadServer(String jenkins,File saveTo,final ProgressDialog dialog)
	{
		try
		{
			JSONObject json=new JSONObject(getInternetString(jenkins+"lastSuccessfulBuild/api/json"));
			JSONArray artifacts=json.getJSONArray("artifacts");
			if(artifacts.length()<=0)
			{
				throw new Exception(instance.getString(R.string.message_no_artifacts));
			}
			json=artifacts.getJSONObject(0);
			downloadFile(jenkins+"lastSuccessfulBuild/artifact/"+json.getString("relativePath"),saveTo,dialog);
		}
		catch(Exception e)
		{
			instance.toast(e.getMessage());
		}
	}
	
	public static void downloadFile(String url,File saveTo,final ProgressDialog dialog)
	{
		OutputStream output=null;
		InputStream input=null;
		try
		{
			if(saveTo.exists())
			{
				saveTo.delete();
			}
			URLConnection connection=openNetConnection(url);
			input=new BufferedInputStream(connection.getInputStream());
			output=new FileOutputStream(saveTo);
			int count=0;
			long read=0;
			if(dialog!=null)
			{
				final long max=connection.getContentLength();
				instance.runOnUiThread(new Runnable()
				{
					public void run()
					{
						dialog.setMax((int)max/1024);
					}
				});
			}
			byte[] buffer=new byte[4096];
			while((count=input.read(buffer))>=0)
			{
				output.write(buffer,0,count);
				read+=count;
				if(dialog!=null)
				{
					final int temp=(int)(read/1000);
					instance.runOnUiThread(new Runnable()
					{
						public void run()
						{
							dialog.setProgress(temp);
						}
					});
				}
			}
			output.close();
			input.close();
			instance.toast(R.string.message_done);
		}
		catch(Exception e)
		{
			instance.toast(e.getMessage());
		}
		finally
		{
			try
			{
				output.close();
				input.close();
			}
			catch(Exception e)
			{
				
			}
		}
	}
	
	public static URLConnection openNetConnection(String url) throws Exception
	{
		final SSLContext sc=SSLContext.getInstance("SSL");
		sc.init(null,new TrustManager[]
		{
			new X509TrustManager()
			{
				@Override
				public void checkClientTrusted(X509Certificate[] p1,String p2) throws CertificateException
				{
					
				}
				
				@Override
				public void checkServerTrusted(X509Certificate[] p1,String p2) throws CertificateException
				{
					
				}
				
				@Override
				public X509Certificate[] getAcceptedIssuers()
				{
					return null;
				}
			}
		},new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier()
		{
			public boolean verify(String hostname,SSLSession session)
			{
				return true;
			}
		});
		URL req=new URL(url);
		URLConnection connection=req.openConnection();
		connection.connect();
		return connection;
	}
	
	public void toast(int text)
	{
		toast(getString(text));
	}
	
	public void toast(final String text)
	{
		if(instance!=null)
		{
			instance.runOnUiThread(new Runnable()
			{
				public void run()
				{
					Toast.makeText(instance,text,Toast.LENGTH_SHORT).show();
				}
			});
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0,CONSOLE_CODE,0,getString(R.string.menu_console))
			.setIcon(R.drawable.hardware_dock)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		menu_install_php=menu.add(0,INSTALL_PHP_CODE,0,getString(R.string.menu_install_php));
		menu_download_server=menu.add(0,DOWNLOAD_SERVER_CODE,0,getString(R.string.menu_download));
		menu_delete_server=menu.add(0,DELETE_SERVER,0,getString(R.string.menu_delete_server));
		menu_backup_server=menu.add(0,BACKUP_SERVER,0,getString(R.string.menu_backup_server));
		menu_force_close= menu.add(0,FORCE_CLOSE_CODE,0,getString(R.string.menu_kill));
		menu_hp=menu.add(0,HP,0,getString(R.string.menu_hp));
		menu_twitter=menu.add(0,TWITTER,0,getString(R.string.menu_twitter));
		menu_bl_twitter=menu.add(0,BL_TWITTER,0,getString(R.string.menu_bl_twitter));
		menu_help=menu.add(0,HELP,0,getString(R.string.menu_help));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		Uri uri;
		Intent intent;
		final ProgressDialog processing_dialog=new ProgressDialog(instance);
		switch(item.getItemId())
		{
		case 0:
		case android.R.id.home:
			return false;
		case FORCE_CLOSE_CODE:
			ServerUtils.killServer();
			if(serverIntent != null)
			{
				stopService(serverIntent);
			}
			isStarted=false;
			refreshEnabled();
			break;
		case CONSOLE_CODE:
			startActivity(new Intent(instance,ConsoleActivity.class));
			break;
		case INSTALL_PHP_CODE:
			installphp();
			break;
		case DOWNLOAD_SERVER_CODE:
			AlertDialog.Builder download_dialog_builder=new AlertDialog.Builder(this);
			String[] jenkins=jenkins_pocketmine,values=new String[jenkins.length];
			for(int i=0;i<jenkins.length;++i)
			{
				String[] split=jenkins[i].split("\\|",2);
				values[i]=split[0];
			}
			download_dialog_builder.setTitle(getString(R.string.message_select_repository).replace("%s","PocketMine"));
			download_dialog_builder.setItems(values,new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface p1,final int p2)
				{
					p1.dismiss();
					processing_dialog.setCancelable(false);
					processing_dialog.setMessage(getString(R.string.message_downloading).replace("%s","PocketMine-MP.phar"));
					processing_dialog.setIndeterminate(false);
					processing_dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
					processing_dialog.show();
					new Thread(new Runnable()
					{
						public void run()
						{
							String[] wtf=jenkins_pocketmine;
							wtf=wtf[p2].split("\\|");
							downloadServer(wtf[1],new File(ServerUtils.getDataDirectory()+"/"+("PocketMine-MP.phar")),processing_dialog);
							runOnUiThread(new Runnable()
							{
								public void run()
								{
									processing_dialog.dismiss();
								}
							});
						}
					}).start();
				}
			});
			download_dialog_builder.show();
			break;
			case DELETE_SERVER:
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
				// アラートダイアログのメッセージを設定します
				alertDialogBuilder.setMessage(R.string.dialog_delete);
				// アラートダイアログの肯定ボタンがクリックされた時に呼び出されるコールバックリスナーを登録します
				alertDialogBuilder.setPositiveButton("OK",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								serverdel();
							}
						});
				alertDialogBuilder.setNegativeButton("NG",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
							}
						});
				alertDialogBuilder.setCancelable(true);
				alertDialogBuilder.create().show();


				break;
			case BACKUP_SERVER:
				processing_dialog.setCancelable(false);
				processing_dialog.setMessage(getString(R.string.message_backuping));
				processing_dialog.show();
				new Thread(new Runnable()
				{
					public void run()
					{
						try
						{
							ServerUtils.BackupDir();
							toast(R.string.message_backup_success);
						}
						catch(Exception e)
						{
							toast(getString(R.string.message_backup_fail)+"\n"+e.toString());
						}
						runOnUiThread(new Runnable()
						{
							public void run()
							{
								processing_dialog.dismiss();
								refreshEnabled();
							}
						});
					}
				}).start();
				break;
			case HP:
				uri = Uri.parse("http://bluelight.cf/");
				intent = new Intent(Intent.ACTION_VIEW,uri);
				startActivity(intent);
				break;
			case TWITTER:
				uri = Uri.parse(String.valueOf(R.string.twitter_hani));
				intent = new Intent(Intent.ACTION_VIEW,uri);
				startActivity(intent);
				break;
			case BL_TWITTER:
				uri = Uri.parse("https://twitter.com/BlueLightJapan");
				intent = new Intent(Intent.ACTION_VIEW,uri);
				startActivity(intent);
				break;
			case HELP:
				uri = Uri.parse("https://twitter.com/BlueLightJapan");
				intent = new Intent(Intent.ACTION_VIEW,uri);
				startActivity(intent);
				break;

		}
		return true;
	}

	public void installphp(){
		final ProgressDialog processing_dialog=new ProgressDialog(instance);
		processing_dialog.setCancelable(false);
		processing_dialog.setMessage(getString(R.string.message_installing));
		processing_dialog.show();
		new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					installBusybox();
					copyAsset("php",ServerUtils.getAppDirectory()+"/php");
					toast(R.string.message_install_success);
				}
				catch(Exception e)
				{
					toast(getString(R.string.message_install_fail)+"\n"+e.toString());
				}
				runOnUiThread(new Runnable()
				{
					public void run()
					{
						processing_dialog.dismiss();
						refreshEnabled();
					}
				});
			}
		}).start();

	}

	public void serverdel(){
		new Thread(new Runnable()
		{
			public void run()
			{
				ServerUtils.killServer();
				if(serverIntent != null)
				{
					stopService(serverIntent);
				}
				if(ServerUtils.RemoveSrvDirectory()){
					runOnUiThread(new Runnable() {
						public void run() {
							toast(R.string.message_delete_success);
						}
					});
				}else{
					runOnUiThread(new Runnable() {
						public void run() {
					toast(R.string.message_delete_failed);
						}
					});
				}


			}
		}).start();
	}
}

