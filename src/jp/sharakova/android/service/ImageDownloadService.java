package jp.sharakova.android.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import jp.sharakova.android.imagedownload.R;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore.Images.Media;
import android.util.Log;

public class ImageDownloadService extends Service {
	
    public final static String IMAGE_DOWNLOAD_SERVICE = "jp.sharakova.android.service.ImageDownloadService";
    private NotificationManager mNM;
    private final IBinder mBinder = new LocalBinder();
    
    public class LocalBinder extends Binder {
    	ImageDownloadService getService() {
            return ImageDownloadService.this;
        }
    }
    
    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    }
    
    @Override
    public void onStart(Intent intent, int startId)
    {
    	super.onStart(intent, startId);
    	
    	// とりあえず、アクションもチェックする
        if (intent != null && IMAGE_DOWNLOAD_SERVICE.equals(intent.getAction())) {
        	
        	// URLとタイトルを設定する
        	final String imageUrl = intent.getStringExtra("image_url");
        	final String imageTitle = intent.getStringExtra("image_title");
        	
        	// ダウンロード開始するメッセージをタスクトレイで表示
        	showNotification(imageTitle, imageUrl);
        	
            new Thread(null, new Runnable() {
                public void run() {
                	String message = null;
                	try {
                		// HTTPで画像をダウンロード
                		DefaultHttpClient httpClient = new DefaultHttpClient();
        	        	HttpResponse response = httpClient.execute(new HttpGet(imageUrl));
        	        	InputStream stream = response.getEntity().getContent();
        	        	
        	        	// ファイルへ保存
        	        	saveImage(getContentResolver(), getFileName(imageUrl), imageTitle, stream);
        	        	
        	        	// タスクトレイで表示するメッセージ
        	        	message = imageTitle + " download complete.";
        	        	
        	        	// コンテンツマネージャーなどのアプリでは、キャッシュをクリアしてやらないと
        	        	// すぐに、画像が表示されないので、メディアマウントをしてやる。
        	        	sendBroadcast(
        	        		new Intent(
        	        			Intent.ACTION_MEDIA_MOUNTED,
        	        			Uri.parse("file://" + Environment.getExternalStorageDirectory())
        	        		)
        	        	);
                	} catch (Exception e) {
                		message = "Error.";
                	} finally {
                		mNM.cancel(imageUrl.hashCode());
                		// メッセージをタスクトレイに表示
                		showNotification(message, imageUrl);
                	}
                }
            }, "ImageDownloadService").start();
        }
    }

    @Override
    public void onDestroy() {
        mNM.cancelAll();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    private void showNotification(String message, String filePath) {
        Notification notification = new Notification(R.drawable.icon, message, System.currentTimeMillis());
        
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, null, 0);
        notification.setLatestEventInfo(this, "download picture.", message, contentIntent);
        mNM.notify(filePath.hashCode(), notification);
    }
    
    private static String saveImage(ContentResolver contentResolver, String filename, String title, InputStream inputStream) {
		OutputStream outputStream = null;
		File file = null;
		try {
			makeDownloadDir();
			file = new File(getDownloadPath(), filename);
			outputStream = new FileOutputStream(file);
			int DEFAULT_BUFFER_SIZE = 1024 * 4;
			  byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
			  int n = 0;
			  while (-1 != (n = inputStream.read(buffer))) {
				  outputStream.write(buffer, 0, n);
			  }
			  inputStream.close();
			  outputStream.close();
		} catch (Exception e) {
			Log.w("save", e);
			if (file != null) {
				file.delete();
			}
		} finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (Throwable t) {
					Log.w("save", "finally");
				}
			}
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (Throwable t) {
					Log.w("save", "finally");
				}
			}
		}
		
		// ContentResolver にファイル情報の書き込み。
		// これやらないと、ギャラリーなどのアプリでダウンロードした画像が表示されないです。
		String filePath = getDownloadPath() + "/" + filename;
	    ContentValues values = new ContentValues(7);
	    values.put(Media.TITLE, title);  
	    values.put(Media.DISPLAY_NAME, filename);  
	    values.put(Media.DATE_TAKEN, System.currentTimeMillis());  
	    values.put(Media.MIME_TYPE, getMimeType(filename));
	    values.put(Media.DATA, filePath);
	    contentResolver.insert(Media.EXTERNAL_CONTENT_URI, values);
	    
		return filePath;
	}
    
	private static String getSdCardPath() {
		return Environment.getExternalStorageDirectory().toString();
	}

	private static String getDownloadPath() {
		// 別のフォルダを指定する場合は、下記のフォルダ名を変更してください。
		return getSdCardPath() + "/download";
	}
	
	private static void makeDownloadDir() {
		makeDir(new File(getDownloadPath()));
	}
	
	private static void makeDir(File dir) {
		if (!dir.exists()) {
			dir.mkdir();
		}		
	}

	private static String getFileName(String url) {
		int point = url.lastIndexOf("/");
	    if (point != -1) {
	    	return url.substring(point + 1, url.length());
	    } 
		int hash = url.hashCode();
		return String.valueOf(hash);
	}

	
	// mime-typeの判定が微妙です。すみません。
	private static String getMimeType(String filename) {
		int point = filename.lastIndexOf(".");
	    if (point != -1) {
	    	String extension = filename.substring(point + 1, filename.length());
	    	if(extension.equals("jpeg") || extension.equals("jpg")) {
	    		return "image/jpeg";
	    	}
	    	if(extension.equals("png")) {
	    		return "image/png";
	    	}
	    	if(extension.equals("gif")) {
	    		return "image/gif";
	    	}
	    } 
		return "image/jpeg";
	}


}

