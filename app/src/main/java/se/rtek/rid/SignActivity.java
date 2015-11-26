package se.rtek.rid;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpServiceConnection;
import org.openintents.openpgp.util.OpenPgpUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Objects;

public class SignActivity extends AppCompatActivity {

    private OpenPgpServiceConnection mServiceConnection;
    private String service;
    private String url;
    private String guid;
    private String payload;
    private String type;
    private String id;
    Context main;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign);
        main = this;
        mServiceConnection = new OpenPgpServiceConnection(this, "org.sufficientlysecure.keychain");
        mServiceConnection.bindToService();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            service = extras.getString("SERVICE");
            url = extras.getString("URL");
            guid = extras.getString("GUID");
            payload = extras.getString("PAYLOAD");
            type = extras.getString("TYPE");
            id = extras.getString("ID");
        }

        TextView serviceText = (TextView)findViewById(R.id.serviceText);
        serviceText.setText(service);

        TextView messageText = (TextView)findViewById(R.id.messageText);

        EditText editText = (EditText)findViewById(R.id.editText);
        editText.setKeyListener(null);
        editText.setText(payload);
        editText.setGravity(Gravity.TOP);

        Button signB = (Button)findViewById(R.id.signB);

        if (type.equals("Authentication"))
        {
            editText.setVisibility(View.INVISIBLE);
            signB.setText("Authenticate");
            messageText.setText("You are authenticating at ");
        }else{
            editText.setVisibility(View.VISIBLE);
            signB.setText("Sign");
            messageText.setText("You are signing data for ");
        }

        signB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateSignature();
            }
        });
    }

    public void generateSignature()
    {
        SharedPreferences sp = this.getSharedPreferences("prefs", 0);

        Intent intent = new Intent();
        intent.setAction(OpenPgpApi.ACTION_SIGN);
        intent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);
        intent.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, sp.getLong(id, 0));
        intent.putExtra("SIGNDATA", payload);

        cleartextSign(intent);
    }

    private void cleartextSign(Intent data) {

        try {

            InputStream is = new ByteArrayInputStream(data.getExtras().getString("SIGNDATA").getBytes());
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            OpenPgpApi api = new OpenPgpApi(this, mServiceConnection.getService());
            Intent result = api.executeApi(data, is, os);


            switch (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
                case OpenPgpApi.RESULT_CODE_SUCCESS: {


                    RequestQueue queue = Volley.newRequestQueue(this);

                    final JSONObject signatureJSON = new JSONObject();
                    signatureJSON.put("signature", os.toString());
                    signatureJSON.put("url", url);
                    signatureJSON.put("service", service);
                    signatureJSON.put("id", id);
                    signatureJSON.put("guid", guid);

                    JsonObjectRequest jsObjRequest = new JsonObjectRequest
                            (Request.Method.POST, url, signatureJSON, new Response.Listener<JSONObject>() {

                                @Override
                                public void onResponse(JSONObject response) {
                                    try {
                                        String message = response.getString("Message");

                                        if (message.equals("OK")){
                                            Toast.makeText(main, "Server accepted signature", Toast.LENGTH_LONG).show();
                                            Intent intent = new Intent(SignActivity.this, StartActivity.class);
                                            SignActivity.this.startActivity(intent);
                                        }

                                        if (message.equals("Fail")){
                                            Toast.makeText(main, "Server did not accept signature", Toast.LENGTH_LONG).show();
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, new Response.ErrorListener() {

                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    // TODO Auto-generated method stub

                                }
                            });

                    // Access the RequestQueue through your singleton class.
                    queue.add(jsObjRequest);

                    break;
                }
                case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED: {
                    PendingIntent pi = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
                    startIntentSenderForResult(pi.getIntentSender(), 42, null, 0, 0, 0);
                    break;
                }
                case OpenPgpApi.RESULT_CODE_ERROR: {
                    OpenPgpError error = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR);
                    Toast.makeText(this, "Signing failed", Toast.LENGTH_LONG).show();
                    throw new RuntimeException(error.getMessage());
                }
            }

        }catch(Exception e){
            Toast.makeText(this, "An error occured", Toast.LENGTH_LONG).show();
            Log.e("err", e.toString());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 42: {
                cleartextSign(data);
                break;
            }
        }
    }
}
