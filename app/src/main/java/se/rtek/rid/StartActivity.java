//PGP ID app

package se.rtek.rid;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class StartActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    Context main;
    Timer timer;
    SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("PGP ID");
        setTitle("PGP ID");

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        main = this;
        sp = this.getSharedPreferences("prefs", 0);

        if (sp.getStringSet("identities", new HashSet<String>()).size() == 0){
            Intent intent = new Intent(StartActivity.this, SetupActivity.class);
            StartActivity.this.startActivity(intent);
            Toast.makeText(this, "You need to add certificates to use with RID.", Toast.LENGTH_LONG).show();
        }


        Set<String> servers = sp.getStringSet("servers", new HashSet<String>());
        if (servers.size() == 0){
            servers.add("https://rtek.se/api/rid/sign");
            SharedPreferences.Editor editor = sp.edit();
            editor.putStringSet("servers", servers);
            editor.commit();
            editor.apply();
        }

        for (final String server : servers)
        {
            timer = new Timer();
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    Refresh(server);
                }

            }, 0, 5000);
            Refresh(server);
        }

    }

    private void Refresh(String server)
    {
        RequestQueue queue = Volley.newRequestQueue(this);

        String url = server;

        HashSet<String> def = new HashSet<String>();

        final JSONArray identitites = new JSONArray();
        for (String in : sp.getStringSet("identities", def))
        {
            identitites.put(in);
        }

        final JSONObject identity = new JSONObject();
        try {
            identity.put("identites", identitites);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, url, identity, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {

                            String Service = response.getString("Service");
                            String Guid = response.getString("GUID");
                            String Payload = response.getString("Payload");
                            String Url = response.getString("Url");
                            String Type = response.getString("Type");
                            String Id = response.getString("Id");

                            Intent intent = new Intent(StartActivity.this, SignActivity.class);
                            intent.putExtra("SERVICE", Service);
                            intent.putExtra("GUID", Guid);
                            intent.putExtra("PAYLOAD", Payload);
                            intent.putExtra("URL", Url);
                            intent.putExtra("ID", Id);
                            intent.putExtra("TYPE", Type);
                            StartActivity.this.startActivity(intent);
                            // Toast.makeText(main, "Sign request for " + Id + " found by " + Service, Toast.LENGTH_LONG).show();

                        } catch (JSONException e) {

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
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.start, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(StartActivity.this, SetupActivity.class);
            StartActivity.this.startActivity(intent);
        }else if (id == R.id.action_about) {
            Intent intent = new Intent(StartActivity.this, AboutActivity.class);
            StartActivity.this.startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_manage) {
            Intent intent = new Intent(StartActivity.this, SetupActivity.class);
            StartActivity.this.startActivity(intent);
        }else if (id == R.id.nav_servers) {
            Intent intent = new Intent(StartActivity.this, ServersActivity.class);
            StartActivity.this.startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
