package ml.chandrakant.newsapp2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<List<NewsItem>>, SharedPreferences.OnSharedPreferenceChangeListener {

    public NewsAdapter newsAdapter;
    public ListView newsListView;
    private TextView statusTextView;
    private ProgressBar progressBar;

    private static final String GUARDIAN_BASE_URL_STRING = "https://content.guardianapis.com/search";
    private static final String apiKey = "da37201a-f618-45e5-9248-287c214ba0a7";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnected();

        progressBar = findViewById(R.id.loading_progress_bar);

        newsAdapter = new NewsAdapter(this, new ArrayList<NewsItem>());
        newsListView = findViewById(R.id.news_list);
        newsListView.setAdapter(newsAdapter);

        statusTextView = findViewById(R.id.status_text_view);
        newsListView.setEmptyView(statusTextView);

        newsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                NewsItem currentNews = newsAdapter.getItem(position);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentNews.getWebUrl()));
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, currentNews.getWebUrl(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Obtain a reference to the SharedPreferences file for this app
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // And register to be notified of preference changes
        // So we know when the user has adjusted the query settings
        prefs.registerOnSharedPreferenceChangeListener(this);

        if (isConnected) {
            getSupportLoaderManager().initLoader(0, null, this);
        } else {
            progressBar.setVisibility(View.GONE);
            statusTextView.setText(R.string.connection_unavailable);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.settings_news_type_key))){
            // Clear the ListView as a new query will be kicked off
            newsAdapter.clear();

            // Hide the empty state text view as the loading indicator will be displayed
            statusTextView.setVisibility(View.GONE);

            // Show the loading indicator while new data is being fetched
            View loadingIndicator = findViewById(R.id.loading_progress_bar);
            loadingIndicator.setVisibility(View.VISIBLE);

            // Restart the loader to re-query as the query settings have been updated
            getSupportLoaderManager().restartLoader(0, null, this);
        }
    }

    @Override
    public Loader<List<NewsItem>> onCreateLoader(int id, Bundle args) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // getString retrieves a String value from the preferences. The second parameter is the default value for this preference.
        String newsType = sharedPrefs.getString(
                getString(R.string.settings_news_type_key),
                getString(R.string.settings_news_type_default));

        // parse breaks apart the URI string that's passed into its parameter
        Uri baseUri = Uri.parse(GUARDIAN_BASE_URL_STRING);

        // buildUpon prepares the baseUri that we just parsed so we can add query parameters to it
        Uri.Builder uriBuilder = baseUri.buildUpon();

        // Append query parameter and its value.
        uriBuilder.appendQueryParameter("q", newsType);
        uriBuilder.appendQueryParameter("api-key", apiKey);
        uriBuilder.appendQueryParameter("show-tags", "contributor");

        return new NewsLoader(MainActivity.this, uriBuilder.toString());
    }

    @Override
    public void onLoadFinished(Loader<List<NewsItem>> loader, List<NewsItem> data) {
        statusTextView.setText(R.string.news_unavailable);
        newsAdapter.clear();

        progressBar.setVisibility(View.GONE);

        if (data != null && !data.isEmpty()) {
            newsAdapter.addAll(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<NewsItem>> loader) {
        newsAdapter.clear();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the Options Menu we specified in XML
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
