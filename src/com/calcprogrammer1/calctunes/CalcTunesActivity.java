/*---------------------------------------------------------------------------------------------*\
| CalcTunes - A desktop-like media player for Android!                                          |
|                                                                                               |
| CalcTunes features a two-panel GUI that supports multiple media sources (libraries) as well   |
| as playlists.  Libraries are defined by groups of folders which are scanned for media and     |
| organized by artist, album, and track tags.  Playlists are lists of individual media files.   |
|                                                                                               |
| Created by Adam Honse (CalcProgrammer1), calcprogrammer1@gmail.com, 12/16/2011                |
\*---------------------------------------------------------------------------------------------*/
package com.calcprogrammer1.calctunes;

import android.app.Activity;
import android.view.*;
import android.os.Bundle;
import android.widget.*;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

import com.calcprogrammer1.calctunes.R;
import com.calcprogrammer1.calctunes.LibraryOperations;
import java.io.File;
import java.util.ArrayList;

public class CalcTunesActivity extends Activity
{
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//Class Variables////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	MediaPlayerHandler mediaplayer;
	TextView artisttext;
	TextView albumtext;
	TextView tracktext;
	
	SeekBar trackseek;
	SeekHandler trackseekhandler;
	
	ExpandableListView sourcelist;
	SourceListHandler sourcelisthandler;
	
	ListView mainlist;
    LibraryListHandler mainlisthandler;

    MediaButtonsHandler buttons;
    
    int now_playing = -1;
    int interfaceColor;
    boolean sidebarHidden = false;
    
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //Callbacks//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    MediaButtonsHandlerCallback buttonsCallback = new MediaButtonsHandlerCallback(){
        public void onMediaNextPressed()
        {
            tracktext.setText("Button Pressed");
            ButtonNextClick(null);
        }

        public void onMediaPrevPressed()
        { 
        }

        public void onMediaPlayPausePressed()
        {
        }

        public void onMediaStopPressed()
        {
        }  
    };
    
    MediaPlayerHandlerCallback mediaplayerCallback = new MediaPlayerHandlerCallback(){
        public void onSongFinished()
        {
            ButtonNextClick(null);
        }

        public void onStop()
        {
            artisttext.setText(mediaplayer.current_artist);
            albumtext.setText(mediaplayer.current_album);
            tracktext.setText(mediaplayer.current_title);
        }
    };
    
    LibraryListCallback mainlisthandlerCallback = new LibraryListCallback(){
        public void callback(int position)
        {
            now_playing = position;
            media_initialize(mainlisthandler.getTrack(now_playing));
            mainlisthandler.setHighlightedTrack(now_playing);
        }
    };
    
    SourceListCallback sourcelisthandlerCallback = new SourceListCallback(){
        public void callback(String filename)
        {
            mainlisthandler.setLibrary(LibraryOperations.readLibraryName(filename));
            mainlisthandler.drawList(-1);
            
        }
    };
    
    LibraryScannerTaskCallback scanCompleteCallback = new LibraryScannerTaskCallback(){
        public void onScanComplete()
        {
            updateGuiElements();
            sourcelisthandler.refreshLibraryList();
        }        
    };
    
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //Class Overrides////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        SharedPreferences appSettings = getSharedPreferences("CalcTunes",MODE_PRIVATE);
        interfaceColor = appSettings.getInt("InterfaceColor", Color.DKGRAY);
        buttons = new MediaButtonsHandler(this);
        buttons.setCallback(buttonsCallback);
        
        mediaplayer = new MediaPlayerHandler();
        mediaplayer.setCallback(mediaplayerCallback);
    	
        mainlisthandler = new LibraryListHandler(this, mainlist);
    	mainlisthandler.setCallback(mainlisthandlerCallback);

    	sourcelisthandler = new SourceListHandler(this, sourcelist);
    	sourcelisthandler.setCallback(sourcelisthandlerCallback);
    	
        updateGuiElements();
 
        sourcelisthandler.refreshLibraryList();
    }
    
    @Override
    public void onResume()
    {
        super.onResume();
        buttons.registerButtons();
    }
    
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.main);
        updateGuiElements();
    }
    
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.mainoptionsmenu, menu);
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.createLibrary:
                startActivityForResult(new Intent(this, LibraryBuilderActivity.class), 1);
                break;
                
            case R.id.exitApplication:
                this.finish();
                break;
                
            case R.id.collapseSidebar:
                if(sidebarHidden)
                {
                    sourcelist.setVisibility(View.VISIBLE);
                    sidebarHidden = false;
                }
                else
                {
                    sourcelist.setVisibility(View.GONE);
                    sidebarHidden = true;
                }
                break;
                
            case R.id.openSettings:
                startActivityForResult(new Intent(this, CalcTunesSettingsActivity.class), 2);
                break;
        }
        return true;
    }
        
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(resultCode == Activity.RESULT_OK)
        {
            if(requestCode == 1)
            {
                ArrayList<String> libraryFolders = data.getStringArrayListExtra("libraryFolders");
                
                if(data.getStringExtra("EditFilename") != null)
                {
                    File deleteFile = new File(data.getStringExtra("EditFilename"));
                    deleteFile.delete();
                }
                
                String libraryName = data.getStringExtra("libraryName");
                
                LibraryOperations.saveLibraryFile(libraryName, libraryFolders, LibraryOperations.getLibraryPath(this));
    
                LibraryScannerTask task = new LibraryScannerTask(this);
                task.setCallback(scanCompleteCallback);
                task.execute(libraryName);

                sourcelisthandler.refreshLibraryList();
            }
            else if(requestCode == 2)
            {

                SharedPreferences appSettings = getSharedPreferences("CalcTunes",MODE_PRIVATE);
                interfaceColor = appSettings.getInt("InterfaceColor", Color.DKGRAY);
                updateInterfaceColor(interfaceColor);
            }
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if(keyCode == KeyEvent.KEYCODE_MEDIA_NEXT)
        {
            ButtonNextClick(null);
            return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        {
            ButtonPrevClick(null);
            return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        {
            ButtonPlayPauseClick(null);
            return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_MEDIA_STOP)
        {
            ButtonStopClick(null);
        }
        return false;       
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        if(v == findViewById(R.id.sourceListView))
        {
            menu.add(1, 1, Menu.NONE, "Edit Library");
            menu.add(1, 2, Menu.NONE, "Delete Library");
            menu.add(1, 3, Menu.NONE, "Rescan Library");
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        ExpandableListContextMenuInfo info= (ExpandableListView.ExpandableListContextMenuInfo) item.getMenuInfo();
        int id = (int) info.id;

        switch(item.getItemId())
        {
            case 1:
                Intent intent = new Intent(getBaseContext(), LibraryBuilderActivity.class);
                intent.putExtra("EditFilename", sourcelisthandler.getLibraryList().get(id).filename);
                intent.putExtra("EditName", sourcelisthandler.getLibraryList().get(id).name);
                startActivityForResult(intent, 1);
                break;
            
            case 2:
                File libraryToDelete = new File(sourcelisthandler.getLibraryList().get(id).filename);
                libraryToDelete.delete();
                sourcelisthandler.refreshLibraryList();
                Toast.makeText(this, "Library Deleted", Toast.LENGTH_SHORT).show();
                break;
            
            case 3:
                LibraryScannerTask task = new LibraryScannerTask(this);
                task.setCallback(scanCompleteCallback);
                task.execute(sourcelisthandler.getLibraryList().get(id).name);
                break;
        }
        
        return(super.onOptionsItemSelected(item));
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //Other Activity Functions///////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    

    
    public void updateGuiElements()
    {
        artisttext = (TextView) findViewById(R.id.text_artistname);
        albumtext = (TextView) findViewById(R.id.text_albumname);
        tracktext = (TextView) findViewById(R.id.text_trackname);
        
        artisttext.setText(mediaplayer.current_artist);
        albumtext.setText(mediaplayer.current_album);
        tracktext.setText(mediaplayer.current_title);
        
        trackseek = (SeekBar) findViewById(R.id.seekBar_track);
        trackseekhandler = new SeekHandler(trackseek, mediaplayer);
        
        sourcelist = (ExpandableListView) findViewById(R.id.sourceListView);
        sourcelisthandler.setListView(sourcelist);
        sourcelisthandler.updateList();
        
        registerForContextMenu(sourcelist);
        if(sidebarHidden)
        {
            sourcelist.setVisibility(View.GONE);
        }
        
        mainlist = (ListView) findViewById(R.id.libraryListView);
        mainlisthandler.setListView(mainlist);
        updateInterfaceColor(interfaceColor);
    }
    
    public void media_initialize(String filename)
    {	
            mediaplayer.stopPlayback();
            mediaplayer.initialize(filename);
			artisttext.setText(mediaplayer.current_artist);
			albumtext.setText(mediaplayer.current_album);
			tracktext.setText(mediaplayer.current_title);
    }
   
    public void ButtonStopClick(View view)
    {
        now_playing = -1;
        mainlisthandler.setHighlightedTrack(now_playing);
    	mediaplayer.stopPlayback();
    }
    
    public void ButtonPlayPauseClick(View view)
    {
        if(mediaplayer.isPlaying())
        {
            mediaplayer.pausePlayback();
        }
        else if(mediaplayer.prepared)
        {
            mediaplayer.startPlayback(true);
        }
    }
    
    public void ButtonNextClick(View view)
    {
        mediaplayer.stopPlayback();
        now_playing += 1;
        media_initialize(mainlisthandler.getTrack(now_playing));
        mainlisthandler.setHighlightedTrack(now_playing);
        mediaplayer.startPlayback(true);
    }
    
    public void ButtonPrevClick(View view)
    {
        mediaplayer.stopPlayback();
        now_playing -= 1;
        media_initialize(mainlisthandler.getTrack(now_playing));
        mainlisthandler.setHighlightedTrack(now_playing);
        mediaplayer.startPlayback(true);
    }
    
    public void updateInterfaceColor(int color)
    {
        int[] colors = {Color.BLACK, color};
        GradientDrawable back = new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, colors);
        back.setShape(GradientDrawable.RECTANGLE);
        sourcelist.setBackgroundDrawable(back);
        back = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
        findViewById(R.id.lower_frame).setBackgroundDrawable(back);
        mainlisthandler.setHighlightColor(color);
    }
}