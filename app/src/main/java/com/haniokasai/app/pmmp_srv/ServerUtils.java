package com.haniokasai.app.pmmp_srv;

import android.content.Context;

import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ServerUtils
{
	public static Context mContext;

	private static Process serverProcess;
	private static OutputStream stdin;
	private static InputStream stdout;

	public static void setContext(Context mContext)
	{
		ServerUtils.mContext=mContext;
	}

	public static String getAppDirectory()
	{
		return mContext.getApplicationInfo().dataDir;
	}

	public static String getDataDirectory()
	{
		String dir=android.os.Environment.getExternalStorageDirectory().getPath() + ("/PocketMine");
		new File(dir).mkdirs();
		return dir;
	}

	public static void killServer()
	{
		try
		{
			Runtime.getRuntime().exec(getAppDirectory() + "/busybox killall -9 " +("php")).waitFor();
		}
		catch(Exception e)
		{
			
		}
	}

	public static Boolean isRunning()
	{
		try
		{
			serverProcess.exitValue();
		}
		catch(Exception e)
		{
			return true;
		}
		return false;
	}

	final public static void runServer()
	{
		File f = new File(getDataDirectory(),"/tmp");
		if(!f.exists())
		{
			f.mkdir();
		}
		else if(!f.isDirectory())
		{
			f.delete();
			f.mkdir();
		}
		setPermission();
		String file;

			if(new File(getDataDirectory() + "/PocketMine-MP.phar").exists())
			{
				file="/PocketMine-MP.phar";
			}
			else
			{
				file = "/src/pocketmine/PocketMine.php";
			}

		File ini=new File(getDataDirectory() + "/php.ini");
		if(!ini.exists())
		{
			try
			{
				ini.createNewFile();
				FileOutputStream os=new FileOutputStream(ini);
				os.write("phar.readonly=0\nphar.require_hash=1\ndate.timezone=Asia/Shanghai\nshort_open_tag=0\nasp_tags=0\nopcache.enable=1\nopcache.enable_cli=1\nopcache.save_comments=1\nopcache.fast_shutdown=0\nopcache.max_accelerated_files=4096\nopcache.interned_strings_buffer=8\nopcache.memory_consumption=128\nopcache.optimization_level=0xffffffff".getBytes("UTF8"));
				os.close();
			}
			catch(Exception e)
			{

			}
		}
		String[] args=null;

			args=new String[]
			{
				getAppDirectory() + "/php",
				"-c",
				getDataDirectory() + "/php.ini",
				getDataDirectory() + file,
				MainActivity.ansiMode?"--enable-ansi":"--disable-ansi"
			};

		ProcessBuilder builder = new ProcessBuilder(args);
		builder.redirectErrorStream(true);
		builder.directory(new File(getDataDirectory()));
		Map<String,String> env=builder.environment();
		env.put("TMPDIR",getDataDirectory() + "/tmp");
		try
		{
			serverProcess=builder.start();
			stdout=serverProcess.getInputStream();
			stdin=serverProcess.getOutputStream();
			Thread tMonitor = new Thread()
			{
				public void run()
				{
					InputStreamReader reader = new InputStreamReader(stdout,Charset.forName("UTF-8"));
					BufferedReader br = new BufferedReader(reader);
					while(isRunning())
					{
						try
						{
							char[] buffer = new char[8192];
							int size = 0;
							while((size = br.read(buffer,0,buffer.length)) != -1)
							{
								StringBuilder s = new StringBuilder();
								for(int i = 0; i < size; i++) 
								{
									char c = buffer[i];
									if(c == '\r')
									{
										continue;
									}
									if(c == '\n' || c == '\u0007')
									{
										String line = s.toString();
										if(c == '\u0007' || line.startsWith("\u001B]0;"))
										{
											//Do nothing.
										}
										else
										{
											ConsoleActivity.log(line);
										}
										s=new StringBuilder();
									}
									else
									{
										s.append(buffer[i]);
									}
								}
							}
						}
						catch(Exception e)
						{
							e.printStackTrace();
						}
						finally
						{
							try
							{
								br.close();
							}
							catch(Exception e)
							{
								e.printStackTrace();
							}
						}
					}
					ConsoleActivity.log("[PE Server] Server was stopped.");
					MainActivity.stopNotifyService();
				}
			};
			tMonitor.start();
		}
		catch(Exception e)
		{
			ConsoleActivity.log("[PE Server] Unable to start "+("PHP")+".");
			ConsoleActivity.log(e.toString());
			MainActivity.stopNotifyService();
			killServer();
		}
		return;
	}

	final static public void setPermission()
	{
		try
		{
				new File(getAppDirectory()+"/php").setExecutable(true,true);
		}
		catch(Exception e)
		{

		}
	}

	public static void writeCommand(String Cmd)
	{
		try
		{
			stdin.write((Cmd + "\r\n").getBytes());
			stdin.flush();
		}
		catch(Exception e)
		{

		}
	}


	///add function
	public static String getBackupDirectory()
	{
		String dir=android.os.Environment.getExternalStorageDirectory().getPath() + ("/PocketMine-Backups");
		new File(dir).mkdirs();
		return dir;
	}

	public static Boolean RemoveSrvDirectory()
	{

		File dir=new File(android.os.Environment.getExternalStorageDirectory().getPath() + ("/PocketMine"));
		try {
			FileUtils.forceDelete(dir);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}



	public static boolean BackupDir(){
		File file=new File(getDataDirectory());
		Calendar c = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		File baseFile = new File(getBackupDirectory()+"/"+sdf.format(c.getTime())+".zip");
		ZipOutputStream outZip = null;
		try {
			// ZIPファイル出力オブジェクト作成
			outZip = new ZipOutputStream(new FileOutputStream(baseFile));
			archive(outZip, baseFile, file);
		} catch ( Exception e ) {
			// ZIP圧縮失敗
			return false;
		} finally {
			// ZIPエントリクローズ
			if ( outZip != null ) {
				try { outZip.closeEntry(); } catch (Exception e) {}
				try { outZip.flush(); } catch (Exception e) {}
				try { outZip.close(); } catch (Exception e) {}
			}
		}
		return true;
	}

	/**
	 * ディレクトリ圧縮のための再帰処理
	 *
	 * @param outZip ZipOutputStream
	 * @param baseFile File 保存先ファイル
	 * @param targetFile File 圧縮したいファイル
	 */
	private static void archive(ZipOutputStream outZip, File baseFile, File targetFile) {
		if ( targetFile.isDirectory() ) {
			File[] files = targetFile.listFiles();
			for (File f : files) {
				if ( f.isDirectory() ) {
					archive(outZip, baseFile, f);
				} else {
					if ( !f.getAbsoluteFile().equals(baseFile)  ) {
						// 圧縮処理
						archive(outZip, baseFile, f, f.getAbsolutePath().replace(baseFile.getParent(), "").substring(1));
					}
				}
			}
		}
	}

	/**
	 * 圧縮処理
	 *
	 * @param outZip ZipOutputStream
	 * @param baseFile File 保存先ファイル
	 * @param targetFile File 圧縮したいファイル
	 * @parma entryName 保存ファイル名
	 */
	private static boolean archive(ZipOutputStream outZip, File baseFile, File targetFile, String entryName) {
		// 圧縮レベル設定
		outZip.setLevel(5);

		try {

			// ZIPエントリ作成
			outZip.putNextEntry(new ZipEntry(entryName));

			// 圧縮ファイル読み込みストリーム取得
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(targetFile));

			// 圧縮ファイルをZIPファイルに出力
			int readSize = 0;
			byte buffer[] = new byte[1024]; // 読み込みバッファ
			while ((readSize = in.read(buffer, 0, buffer.length)) != -1) {
				outZip.write(buffer, 0, readSize);
			}
			// クローズ処理
			in.close();
			// ZIPエントリクローズ
			outZip.closeEntry();
		} catch ( Exception e ) {
			// ZIP圧縮失敗
			return false;
		}
		return true;
	}
}

