package com.ultracast.demo;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class DemoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        SampleListLoader loaderTask = new SampleListLoader();
        loaderTask.execute("urls.json");
    }

    private void buildIntent(String url) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.URL_EXTRA, url);
        startActivity(intent);
    }

    private void showData(final List<Sample> samples) {
        ListView listView = (ListView) findViewById(R.id.items);
        listView.setAdapter(new SamplesAdapter(getApplicationContext(), samples));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                buildIntent(samples.get(position).getUrl());
            }
        });
    }

    private static class Sample {

        private String title;
        private String url;

        Sample(String title, String url) {
            this.title = title;
            this.url = url;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    private final class SampleListLoader extends AsyncTask<String, Void, List<Sample>> {

        private String TAG = "SampleListLoader";

        @Override
        protected List<Sample> doInBackground(String... strings) {
            List<Sample> samples = new ArrayList<>();
            AssetManager manager;
            try {
                manager = getAssets();
                InputStream in = manager.open(strings[0]);
                readJsonStream(in, samples);
            } catch (IOException e) {
                Log.e(TAG, "Error loading sample list: ", e);
            }

            return samples;
        }

        @Override
        protected void onPostExecute(List<Sample> samples) {
            showData(samples);
        }

        private List<Sample> readJsonStream(InputStream in, List<Sample> samples) throws IOException {
            JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
            try {
                return readSamplesArray(reader, samples);
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    //ignored
                }
            }
        }

        private List<Sample> readSamplesArray(JsonReader reader, List<Sample> samples) throws IOException {
            reader.beginArray();
            while (reader.hasNext()) {
                Sample sample = readSample(reader);
                if (sample != null) {
                    samples.add(sample);
                }
            }
            reader.endArray();
            return samples;
        }

        private Sample readSample(JsonReader reader) throws IOException {
            String title = null;
            String url = null;

            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if ("title".equals(name)) {
                    title = reader.nextString();
                } else if ("url".equals(name)) {
                    url = reader.nextString();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            if (!isNullOrEmpty(title) & !isNullOrEmpty(url)) {
                return new Sample(title, url);
            } else {
                return null;
            }
        }

        private boolean isNullOrEmpty(String s) {
            return s == null || s.isEmpty();
        }
    }

    private static class SamplesAdapter extends BaseAdapter {

        private LayoutInflater inflater;
        private List<Sample> samples;

        SamplesAdapter(Context context, List<Sample> samples) {
            inflater = LayoutInflater.from(context);
            this.samples = samples;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public int getCount() {
            return samples != null ? samples.size() : 0;
        }

        @Override
        public Object getItem(int position) {
            return samples.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        private Sample getSample(int position) {
            return (Sample) getItem(position);
        }

        @Override
        public View getView(int position, View convertedView, ViewGroup parent) {
            View view = convertedView;
            if (view == null) {
                view = inflater.inflate(R.layout.simple_list_item, parent, false);
            }

            Sample sample = getSample(position);
            TextView title = (TextView) view.findViewById(R.id.title);
            title.setText(sample.getTitle());
            return view;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }
    }
}