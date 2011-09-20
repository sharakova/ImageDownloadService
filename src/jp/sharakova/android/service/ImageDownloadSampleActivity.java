package jp.sharakova.android.service;

import jp.sharakova.android.imagedownload.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

public class ImageDownloadSampleActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        findViewById(R.id.download_btn).setOnClickListener(downloadBtnListener);
    }
    
    OnClickListener downloadBtnListener = new OnClickListener() {
		@Override
		public void onClick(View arg0) {
            Intent intent = new Intent(ImageDownloadService.IMAGE_DOWNLOAD_SERVICE);
            intent.setClass(getApplicationContext(), ImageDownloadService.class);
            intent.putExtra("image_url", "http://lh5.googleusercontent.com/-U_ZrYg86EMc/S9uxmpuT6hI/AAAAAAAACUs/-zOKvjJKH8E/s640/golf.png");
            intent.putExtra("image_title", "golf");
            startService(intent);
			Toast.makeText(getApplicationContext(), "download start", Toast.LENGTH_SHORT).show();
		}
	};
}