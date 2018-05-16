package com.example.utkarsh.news;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles=new ArrayList<>();
    ArrayList<String> content=new ArrayList<>();
    ArrayAdapter arrayAdapter;
    SQLiteDatabase articlesDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView listView=(ListView)findViewById(R.id.listView);
        arrayAdapter=new ArrayAdapter(this,android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent=new Intent(getApplicationContext(),Main2Activity.class);
                intent.putExtra("content",content.get(position));
                startActivity(intent);
            }
        });
        articlesDB=this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles(id INTEGER PRIMARY KEY,articleId INTEGER,title VARCHAR,content VARCHAR)");
        updateListView();
        DownloadTask task=new DownloadTask();
        try {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }



    }
    public void updateListView(){
        Cursor c=articlesDB.rawQuery("SELECT * FROM articles",null);
        int contentIndex=c.getColumnIndex("content");
        int titleIndex=c.getColumnIndex("title");
        if(c.moveToFirst()){
            titles.clear();
            content.clear();
            do{
                titles.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));

            }while(c.moveToNext());
            arrayAdapter.notifyDataSetChanged();
        }
    }

    public class DownloadTask extends AsyncTask<String ,Void,String>{

        @Override
        protected String doInBackground(String... strings) {
            String result="";
            URL url;
            HttpURLConnection httpURLConnection=null;
            try {
                url=new URL(strings[0]);
                httpURLConnection=(HttpURLConnection)url.openConnection();
                InputStream inputStream= httpURLConnection.getInputStream();
                InputStreamReader inputStreamReader=new InputStreamReader(inputStream);
                int data=inputStreamReader.read();
                while (data!=-1){
                    char current=(char)data;
                    result+=current;
                    data=inputStreamReader.read();

                }

                JSONArray jsonArray=new JSONArray(result);
                int noOfItems=20;
                if(jsonArray.length() <20){
                    noOfItems=jsonArray.length();
                }
                articlesDB.execSQL("DELETE FROM articles");
                for(int i=0;i<noOfItems;i++){
                    String articleId=jsonArray.getString(i);
                url =new URL("https://hacker-news.firebaseio.com/v0/item/" +articleId+".json?print=pretty");
                httpURLConnection=(HttpURLConnection)url.openConnection();
                inputStream=httpURLConnection.getInputStream();
                inputStreamReader=new InputStreamReader(inputStream);
                data=inputStreamReader.read();
                String articleInfo="";
                while (data!=-1){

                char current=(char)data;
                articleInfo+=current;
                data=inputStreamReader.read();

                }
                    JSONObject jsonObject=new JSONObject(articleInfo);
                if(!jsonObject.isNull("title")&&!jsonObject.isNull("url")) {
                    String articleTitle = jsonObject.getString("title");
                    String articleUrl = jsonObject.getString("url");
                    url =new URL(articleUrl);
                    httpURLConnection=(HttpURLConnection)url.openConnection();
                    inputStream=httpURLConnection.getInputStream();
                    inputStreamReader=new InputStreamReader(inputStream);
                    data=inputStreamReader.read();
                    String articleContent="";
                    while (data!=-1){

                        char current=(char)data;
                        articleContent+=current;
                        data=inputStreamReader.read();

                    }
                    String sql="INSERT INTO articles(articleId,title,content)VALUES(? ,? ,? )";
                    SQLiteStatement sqLiteStatement=articlesDB.compileStatement(sql);
                    sqLiteStatement.bindString(1,articleId);
                    sqLiteStatement.bindString(2,articleTitle);
                    sqLiteStatement.bindString(3,articleContent);
                    sqLiteStatement.execute();

                }
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }
    }
}
