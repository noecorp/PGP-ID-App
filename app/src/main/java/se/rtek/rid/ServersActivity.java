package se.rtek.rid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ServersActivity extends AppCompatActivity {

    private SharedPreferences sp;
    private ListView lw;
    private Context main;
    private ArrayAdapter<String> adapter;
    private HashSet<String> def;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        main = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_servers);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        sp = getSharedPreferences("prefs", 0);
        lw = (ListView) findViewById(R.id.listView2);
        def = new HashSet<>();
        List<String> list = new ArrayList<String>();
        for (String row : sp.getStringSet("servers", def)) {
            list.add(row);
        }

        adapter = new ArrayAdapter<>(getBaseContext(), R.layout.listitem, list);

        lw.setAdapter(adapter);

        lw.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog.Builder(main)
                        .setTitle("Delete server")
                        .setMessage("Do you wish to no longer monitor sign requests from this server?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String key = adapter.getItem(position);
                                HashSet<String> newKeys = new HashSet<>();
                                newKeys.addAll(sp.getStringSet("servers", def));
                                newKeys.remove(key);
                                SharedPreferences.Editor editor = sp.edit();
                                editor.putStringSet("servers", newKeys);
                                adapter.remove(key);
                                editor.commit();
                                editor.apply();
                            }

                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText input = new EditText(main);
                new AlertDialog.Builder(main)
                        .setView(input)
                        .setTitle("Add server")
                        .setMessage("Enter the URL to monitor for signing requests from.")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String server = input.getText().toString();
                                HashSet<String> newServers = new HashSet<>();
                                newServers.addAll(sp.getStringSet("servers", def));
                                newServers.add(server);
                                SharedPreferences.Editor editor = sp.edit();
                                editor.putStringSet("servers", newServers);
                                adapter.add(server);
                                editor.commit();
                                editor.apply();
                            }

                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });
    }

}
