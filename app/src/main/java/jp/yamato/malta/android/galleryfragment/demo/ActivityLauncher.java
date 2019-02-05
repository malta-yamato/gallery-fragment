package jp.yamato.malta.android.galleryfragment.demo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ActivityLauncher extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private ArrayList<ActivityInfo> mActivities = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ListView listView = new ListView(this);
        setContentView(listView);

        try {
            PackageInfo pi = getPackageManager()
                    .getPackageInfo("jp.yamato.malta.android.galleryfragment.demo",
                            PackageManager.GET_ACTIVITIES);
            mActivities = new ArrayList<>(Arrays.asList(pi.activities));
            for (Iterator<ActivityInfo> it = mActivities.iterator(); it.hasNext(); ) {
                if (getClass().getName().equals(it.next().name)) it.remove();
            }

            listView.setAdapter(
                    new MyAdapter(this, android.R.layout.simple_list_item_1, mActivities));
            listView.setOnItemClickListener(this);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private class MyAdapter extends ArrayAdapter<ActivityInfo> {
        private Context context;
        private int resourceId;

        MyAdapter(Context context, int resourceId, List<ActivityInfo> activities) {
            super(context, resourceId, activities);
            this.context = context;
            this.resourceId = resourceId;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null)
                convertView = LayoutInflater.from(context).inflate(resourceId, parent, false);
            String name = getItem(position).name;
            String[] spls = name.split("\\.");
            if (spls.length > 0) ((TextView) convertView).setText(spls[spls.length - 1]);
            return convertView;
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent();
        intent.setClassName(this, mActivities.get(position).name);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
