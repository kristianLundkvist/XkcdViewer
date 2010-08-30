package net.bytten.xkcdviewer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class ArchiveActivity extends ListActivity {
    
    static Pattern archiveItemPattern = Pattern.compile(
            // group(1): comic number;   group(2): date;   group(3): title
            "\\s*<a href=\"/(\\d+)/\" title=\"(\\d+-\\d+-\\d+)\">([^<]+)</a><br/>\\s*");
    
    public Handler handler = new Handler();
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        resetContent();
    }
    
    public void resetContent() {
        final Thread[] loadThread = {null};
        final List<ArchiveItem> items = new ArrayList<ArchiveItem>();

        final ProgressDialog pd = ProgressDialog.show(this,
                "XkcdViewer", "Loading archive...", true, true,
                new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                if (loadThread[0] != null) {
                    // tell loading to stop
                    loadThread[0].interrupt();
                }
            }
        });

        loadThread[0] = new Thread(new Runnable() {
            public void run() {
                URL url = null;
                try {
                    url = new URL("http://www.xkcd.com/archive/");
                    BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
                    
                    String line;
                    while ((line = br.readLine()) != null) {
                        Thread.sleep(0); // allow space for interruption
                        
                        Matcher m = archiveItemPattern.matcher(line);
                        if (m.matches()) {
                            ArchiveItem item = new ArchiveItem();
                            item.comicNumber = m.group(1);
                            item.date = m.group(2);
                            item.title = m.group(3);
                            items.add(item);
                        }
                    }
                    br.close();
                    
                    handler.post(new Runnable() {
                        public void run() {
                            setListAdapter(new ArchiveAdapter(items));
                        }
                    });
                    
                } catch (MalformedURLException e) {
                    failed("Malformed URL: "+e);
                } catch (IOException e) {
                    failed("IO error: "+e);
                } catch (InterruptedException e) {
                    // Do nothing. Loading was cancelled.
                } catch (Throwable e) {
                    failed(e.toString());
                } finally {
                    handler.post(new Runnable() {
                        public void run() {
                            pd.dismiss();
                        }
                    });
                }
            }
        });
        loadThread[0].start();
    }
    
    protected void failed(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        this.finish();
    }
    
    class ArchiveAdapter extends ArrayAdapter<ArchiveItem> {
        
        public ArchiveAdapter(List<ArchiveItem> items) {
            super(ArchiveActivity.this, 0, items);
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ArchiveItem item = getItem(position);
            if (convertView != null && convertView instanceof ArchiveItemView) {
                ((ArchiveItemView)convertView).setItem(item);
                return convertView;
            } else {
                return new ArchiveItemView(getContext(), item);
            }
        }
    }
    
    static class ArchiveItemView extends TextView implements View.OnClickListener {
        protected ArchiveItem item = null;
        public ArchiveItemView(Context cxt, ArchiveItem item) {
            super(cxt);
            setItem(item);
        }
        public ArchiveItem getItem() { return item; }
        public void setItem(ArchiveItem item) {
            this.item = item;
            setText(item.comicNumber + " - " + item.title);
            setOnClickListener(this);
        }
        public void onClick(View v) {
            Intent comic = new Intent();
            comic.addCategory(Intent.CATEGORY_BROWSABLE);
            comic.setData(Uri.parse("http://www.xkcd.com/"+item.comicNumber+"/"));
            comic.setClass(getContext(), XkcdViewerActivity.class);
            getContext().startActivity(comic);
        }
    }
    
    static class ArchiveItem {
        public boolean bookmarked = false;
        public String title, date, comicNumber;
    }
}
