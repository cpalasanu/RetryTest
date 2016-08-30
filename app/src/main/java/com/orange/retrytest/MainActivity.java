package com.orange.retrytest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.Locale;

import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private static final String TAG ="RetryTest";

    private static final int TIMEOUT = 10 * 1000;
    private static final float BACK_OFF_MULTIPLIER = 1f;
    private static final int NUM_RETRIES = 100;

    TextView tvRetries;
    RequestQueue requestQueue;
    Request currentRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestQueue = Volley.newRequestQueue(this, new OkHttpStack(new OkHttpClient()));
        requestQueue = Volley.newRequestQueue(this);

        findViewById(R.id.do_it).setOnClickListener(this);
        ((ToggleButton) findViewById(R.id.toggle_ok_http)).setOnCheckedChangeListener(this);

        tvRetries = (TextView) findViewById(R.id.retries);
    }

    @Override
    public void onClick(View view) {
        if (currentRequest != null) currentRequest.cancel();
        tvRetries.setText(null);
        Log.d(TAG, "~~~~~~~~~~~~~~~start~~~~~~~~~~~~~~~");
        currentRequest = new StringRequest("http://10.255.255.1", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                tvRetries.append("SUCCESS!!!!!!!\nresponse length: " + response.length());

                Log.d(TAG, "SUCCESS!!!!");
                Log.d(TAG, "~~~~~~~~~~~~~~~end~~~~~~~~~~~~~~~");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                tvRetries.append("FAILED :'(\n" + error);
                Log.d(TAG, "FAILED!!!!");
                Log.d(TAG, "~~~~~~~~~~~~~~~end~~~~~~~~~~~~~~~");
            }
        });

        currentRequest.setRetryPolicy(new DefaultRetryPolicy(TIMEOUT, NUM_RETRIES, BACK_OFF_MULTIPLIER) {
            private int retryCount = -1;
            private long retryTime = System.currentTimeMillis();
            @Override
            public int getCurrentTimeout() {
                final int timeout = super.getCurrentTimeout();
                int retryCount = getCurrentRetryCount();
                if (this.retryCount != retryCount) {
                    this.retryCount = retryCount;
                    long retryDuration = System.currentTimeMillis() - retryTime;

                    final String retryDurationStr;
                    final String retryTimeoutStr =String.format(Locale.getDefault(), "%.1fs", (float) timeout / 1000);
                    if (retryDuration > 100) {
                         retryDurationStr = String.format(Locale.getDefault(), "%.1fs", (float) retryDuration / 1000);
                    } else {
                        retryDurationStr = null;
                    }
                    retryTime = System.currentTimeMillis();
                    Log.d(TAG, "Retry no. " + retryCount + " with timout: " + retryTimeoutStr + " last retry duration: " + retryDurationStr);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (retryDurationStr != null) {
                                tvRetries.append(" duration: " + retryDurationStr + "\n");
                            }
                            tvRetries.append("Retry no. " + getCurrentRetryCount() + " with timout: " + retryTimeoutStr);
                        }
                    });
                }

                return super.getCurrentTimeout();
            }
        });
        currentRequest.setShouldCache(false);

        requestQueue.add(currentRequest);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        requestQueue = b ? Volley.newRequestQueue(this, new OkHttpStack(new OkHttpClient())) : Volley.newRequestQueue(this);
        Log.d(TAG, "changed Request queue to use " + (b ? "OkHttp" : "Default") + "http stack");
    }
}
