package se.rtek.rid;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
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
import org.w3c.dom.Text;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SetupActivity extends AppCompatActivity {

    private OpenPgpServiceConnection mServiceConnection;
    private ListView lw;
    private ArrayAdapter<String> adapter;
    private HashSet<String> def;
    private SharedPreferences sp;
    private Context main;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        main = this;
        mServiceConnection = new OpenPgpServiceConnection(this, "org.sufficientlysecure.keychain");
        mServiceConnection.bindToService();

        sp = getSharedPreferences("prefs", 0);
        lw = (ListView) findViewById(R.id.listView);

        def = new HashSet<>();
        List<String> list = new ArrayList<String>();
        for (String row : sp.getStringSet("identities", def)) {
            list.add(row);
        }

        adapter = new ArrayAdapter<>(getBaseContext(), R.layout.listitem, list);

        lw.setAdapter(adapter);

        lw.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog.Builder(main)
                        .setTitle("Delete identity")
                        .setMessage("Do you wish to no longer monitor sign requests for this identity?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String key = adapter.getItem(position);
                                HashSet<String> newKeys = new HashSet<>();
                                newKeys.addAll(sp.getStringSet("identities", def));
                                newKeys.remove(key);
                                SharedPreferences.Editor editor = sp.edit();
                                editor.putStringSet("identities", newKeys);
                                adapter.remove(key);
                                editor.commit();
                                editor.apply();
                            }

                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab2);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getKey();
            }
        });

    }

    public void getKey()
    {
        Intent intent = new Intent();
        intent.setAction(OpenPgpApi.ACTION_GET_KEY_IDS);
        getKeys(intent);
    }

    private void getKeys(Intent data) {

        try {

            OpenPgpApi api = new OpenPgpApi(this, mServiceConnection.getService());
            Intent result = api.executeApi(data, null, null);

            switch (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
                case OpenPgpApi.RESULT_CODE_SUCCESS: {
                    long[] keyIds = result.getLongArrayExtra(OpenPgpApi.RESULT_KEY_IDS);
                    HashSet<String> newKeys = new HashSet<>();
                    SharedPreferences.Editor editor = sp.edit();
                    for (long keyId : keyIds) {
                        if (!sp.getStringSet("identities", def).contains(OpenPgpUtils.convertKeyIdToHex(keyId))) {
                            adapter.add(OpenPgpUtils.convertKeyIdToHex(keyId).toUpperCase().substring(2));
                            newKeys.add(OpenPgpUtils.convertKeyIdToHex(keyId).toUpperCase().substring(2));
                            editor.putLong(OpenPgpUtils.convertKeyIdToHex(keyId).toUpperCase().substring(2), keyId);

                            new AlertDialog.Builder(main)
                                    .setTitle("Key has been added")
                                    .setMessage("A key has been added to your certificate list. You may report your key id '" + OpenPgpUtils.convertKeyIdToHex(keyId).toUpperCase().substring(2) + "' to services you wish to be able to authenticate with.")
                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                        }

                                    })
                                    .show();
                        }
                    }
                    newKeys.addAll(sp.getStringSet("identities", def));
                    editor.putStringSet("identities", newKeys);
                    editor.commit();
                    editor.apply();
                    break;
                }
                case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED: {
                    PendingIntent pi = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
                    startIntentSenderForResult(pi.getIntentSender(), 43, null, 0, 0, 0);
                    break;
                }
                case OpenPgpApi.RESULT_CODE_ERROR: {
                    OpenPgpError error = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR);
                    new AlertDialog.Builder(main)
                            .setTitle("No certificates could be added")
                            .setMessage("Make sure that you have 'OpenKeychain' installed and a PGP certificate added. If not you can download the dependency by pressing install below and then create a new certificate in 'OpenKeychain'.")
                            .setPositiveButton("Install OpenKeychain", new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final String appPackageName = "org.sufficientlysecure.keychain";
                                    try {
                                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                                    } catch (android.content.ActivityNotFoundException anfe) {
                                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                                    }
                                }

                            })

                            .setNegativeButton("Close", null)
                            .show();
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
            case 43: {
                getKeys(data);
                break;
            }
        }
    }
}
