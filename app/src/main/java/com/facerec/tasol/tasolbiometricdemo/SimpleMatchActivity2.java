/* *************************************************************************************************
 * SimpleMatchActivity2.java
 *
 * DESCRIPTION:
 *     SimpleMatch demo app for IBScanMatcher
 *     http://www.integratedbiometrics.com
 *
 * NOTES:
 *     Copyright (c) Integrated Biometrics, 2012-2013
 *     
 * HISTORY:
 *     2013/03/22  First version, adapted from SimpleScanActivity.java.
 ************************************************************************************************ */

package com.facerec.tasol.tasolbiometricdemo;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.integratedbiometrics.ibscancommon.IBCommon;
import com.integratedbiometrics.ibscancommon.IBCommon.ImageDataExt;
import com.integratedbiometrics.ibscanmatcher.IBMatcher;
import com.integratedbiometrics.ibscanmatcher.IBMatcher.SdkVersion;
import com.integratedbiometrics.ibscanmatcher.IBMatcher.Template;
import com.integratedbiometrics.ibscanmatcher.IBMatcherException;
import com.integratedbiometrics.ibscanultimate.IBScan;
import com.integratedbiometrics.ibscanultimate.IBScan.DeviceDesc;
import com.integratedbiometrics.ibscanultimate.IBScanDevice;
import com.integratedbiometrics.ibscanultimate.IBScanDevice.FingerCountState;
import com.integratedbiometrics.ibscanultimate.IBScanDevice.FingerQualityState;
import com.integratedbiometrics.ibscanultimate.IBScanDevice.ImageData;
import com.integratedbiometrics.ibscanultimate.IBScanDevice.ImageResolution;
import com.integratedbiometrics.ibscanultimate.IBScanDevice.ImageType;
import com.integratedbiometrics.ibscanultimate.IBScanDevice.PlatenState;
import com.integratedbiometrics.ibscanultimate.IBScanDevice.PropertyId;
import com.integratedbiometrics.ibscanultimate.IBScanDevice.RollingData;
import com.integratedbiometrics.ibscanultimate.IBScanDevice.SegmentPosition;
import com.integratedbiometrics.ibscanultimate.IBScanDeviceListener;
import com.integratedbiometrics.ibscanultimate.IBScanException;
import com.integratedbiometrics.ibscanultimate.IBScanListener;

/**
 * Main activity for SimpleScan application.  Capture on a single connected scanner can be started
 * and stopped.  After an acquisition is complete, long-clicking on the small preview window will
 * allow the image to be e-mailed or show a larger view of the image.
 */
public class SimpleMatchActivity2 extends Activity implements IBScanListener, IBScanDeviceListener
{
	/* *********************************************************************************************
	 * PRIVATE CLASSES
	 ******************************************************************************************** */

	/*
	 * Enum representing the application state.  The application will move between states based on 
	 * callbacks from the IBScan library.  There are two methods associated with each state.  The 
	 * "transition" method is call by the app to move to a new state and sends a message to a 
	 * handler.  The "handleTransition" method is called by that handler to effect the transition.
	 */
	private static enum AppState
	{
		NO_SCANNER_ATTACHED,
		SCANNER_ATTACHED,
		REFRESH,
		INITIALIZING,
		INITIALIZED,
		CLOSING,
		STARTING_CAPTURE,
		CAPTURING,
		STOPPING_CAPTURE,
		IMAGE_CAPTURED,
		COMMUNICATION_BREAK;
	}
	
	/*
	 * Enum representing output types for e-mailing an image.
	 */
	private static enum OutputFormat
	{
		PNG,
		WSQ,
		FIR,
		FMR,
		IBSM_IMAGE,
		IBSM_TEMPLATE;	
	}
	
	/* 
	 * Enum representing actions that can be triggered with the start button.
	 */
	 private static enum ActionType
	 {
		 CAPTURE,
		 MATCH,
		 SINGLE_ENROLL,
		 MULTI_ENROLL;
	 }
	
	/*
	 * This class wraps the data saved by the app for configuration changes.
	 */
	private class AppData
	{
		/* The state of the application. */
		public AppState state                      = AppState.NO_SCANNER_ATTACHED;
		
		/* The type of capture currently selected. */
		public int      captureType                = CAPTURE_TYPE_INVALID;
		
		/* The current contents of the status TextView. */
		public String   status                     = STATUS_DEFAULT;
		
		/* The current contents of the frame time TextView. */
		public String   frameTime                  = FRAME_TIME_DEFAULT;
		
		/* The current contents of the actionstate TextView. */
		public String   actionState                = ACTION_STATE_DEFAULT;
		
		/* The current image displayed in the image preview ImageView. */
		public Bitmap   imageBitmap                = null;
		
		/* The current background colors of the finger quality TextViews. */
		public int[]    fingerQualityColors        = new int[]
				{FINGER_QUALITY_NOT_PRESENT_COLOR, FINGER_QUALITY_NOT_PRESENT_COLOR, 
				 FINGER_QUALITY_NOT_PRESENT_COLOR, FINGER_QUALITY_NOT_PRESENT_COLOR};
				 
		/* Indicates whether the image preview ImageView can be long-clicked. */
		public boolean  imagePreviewImageClickable = false;
		
		/* The current contents of the device description TextView. */
		public String   description                = NO_DEVICE_DESCRIPTION_STRING;
		
		/* The current background color of the device description TextView. */
		public int      descriptionColor           = NO_DEVICE_DESCRIPTION_COLOR;
		
		/* The current device count displayed in the device count TextView. */
		public int      deviceCount                = 0;
	}
	
	/* *********************************************************************************************
	 * PRIVATE CONSTANTS
	 ******************************************************************************************** */

	/* The tag used for Android log messages from this app. */
	private static final String SIMPLE_MATCH_TAG                 = "Simple Match";
	
	/* The default value of the status TextView. */
	private static final String STATUS_DEFAULT                   = "";
	
	/* The default value of the frame time TextView. */
	private static final String FRAME_TIME_DEFAULT               = "n/a";
	
	/* The default value of the action state TextView. */
	private static final String ACTION_STATE_DEFAULT             = "";
	
	/* The default user name in the user create dialog TextView. */
	private static final String USER_NAME_DEFAULT                = "";
	
	/* The default user description in the user create dialog TextView. */
	private static final String USER_DESCRIPTION_DEFAULT         = "";

	/* The default file name for images and templates for e-mail. */
	private static final String FILE_NAME_DEFAULT                = "output";

	/* The value of AppData.captureType when the capture type has never been set. */
	private static final int    CAPTURE_TYPE_INVALID             = -1;
	
	/* The number of finger qualities set in the preview image. */
	private static final int    FINGER_QUALITIES_COUNT           = 4;
	
	/* The description in the device description TextView when no device is attached. */
	private static final String NO_DEVICE_DESCRIPTION_STRING     = "(no scanner)";
	
	/* The background color of the device description TextView when no device is attached. */
	private static final int    NO_DEVICE_DESCRIPTION_COLOR      = Color.RED;
	
	/* The background color of the device description TextView when a device is attached. */
	private static final int    DEVICE_DESCRIPTION_COLOR         = Color.GRAY;
	
	/* The delay between resubmissions of messages when trying to stop capture. */
	private static final int    STOPPING_CAPTURE_DELAY_MILLIS    = 250;
	
	/* The device index which will be initialized. */
	private static final int    INITIALIZING_DEVICE_INDEX        = 0;
	
	/* The background color of the preview image ImageView. */
	private static final int    PREVIEW_IMAGE_BACKGROUND         = Color.LTGRAY;
	
	/* The background color of a finger quality TextView when the finger is not present. */
	private static final int    FINGER_QUALITY_NOT_PRESENT_COLOR = Color.LTGRAY;

	/* The background color of a finger quality TextView when the finger is good quality. */
	private static final int    FINGER_QUALITY_GOOD_COLOR        = Color.GREEN;

	/* The background color of a finger quality TextView when the finger is fair quality. */
	private static final int    FINGER_QUALITY_FAIR_COLOR        = Color.YELLOW;

	/* The background color of a finger quality TextView when the finger is poor quality. */
	private static final int    FINGER_QUALITY_POOR_COLOR        = Color.RED;
	
	/* *********************************************************************************************
	 * PRIVATE FIELDS (UI COMPONENTS)
	 ******************************************************************************************** */

	private TextView       m_txtDeviceCount;
	private TextView       m_txtStatus;                   
	private TextView       m_txtDesciption;                    
	private TextView       m_txtFrameTime;      
	private TextView       m_txtActionState;      
	private ImageView      m_imagePreviewImage;                    
	private TextView[]     m_txtFingerQuality = new TextView[FINGER_QUALITIES_COUNT];
	private TextView       m_txtSDKVersion;
	private Spinner        m_spinnerCaptureType;
	private Button         m_startCaptureBtn;
	private Button         m_stopCaptureBtn;
	private Button         m_openScannerBtn;
	private Button         m_closeScannerBtn;
	private Button         m_refreshBtn;
	private Dialog         m_enlargedDialog;
	private Dialog         m_databaseDialog;
	private Spinner        m_spinnerActionType;
	private ProgressDialog m_progressDialog;
	private Button         m_viewDatabaseBtn;
	
	/* *********************************************************************************************
	 * PRIVATE FIELDS
	 ******************************************************************************************** */

	/* 
	 * A handle to the single instance of the IBScan class that will be the primary interface to
	 * the library, for operations like getting the number of scanners (getDeviceCount()) and 
	 * opening scanners (openDeviceAsync()). 
	 */
	private IBScan             m_ibScan;

	/*
	 * A handle to the single instance of the IBMatcher class that will be the primary interface 
	 * for matcher functionality, for operations like converting an image to a template 
	 * (convertToTemplate()) or compressing to WSQ (compressImage()).
	 */
	private IBMatcher          m_ibMatcher;
	
	/*
	 * The database in which templates are stored for matching.
	 */
	 private IBMatcherDatabase m_ibMatcherDatabase;
	
	/* 
	 * A handle to the open IBScanDevice (if any) that will be the interface for getting data from
	 * the open scanner, including capturing the image (beginCaptureImage(), cancelCaptureImage()),
	 * the type of image being captured, and the action being executed (e.g., capture or enroll).
	 */
	private IBScanDevice       m_ibScanDevice;
	private ImageType          m_imageType;
	private ActionType         m_actionType;
	private int                m_imagesCaptured;
	  
	/*
	 * An object that will play a sound when the image capture has completed.
	 */
	private PlaySound          m_beeper = new PlaySound();
	
	/* 
	 * Information retained to show view or perform analysis of result.
	 */
	private ImageDataExt[]     m_lastImageExts;
	private ImageDataExt       m_lastImageExt;
	
	/*
	 * Information retained for orientation changes.
	 */
	private AppData            m_savedData = new AppData();
	
	/* *********************************************************************************************
	 * INHERITED INTERFACE (Activity OVERRIDES)
	 ******************************************************************************************** */

    /*
     * Called when the activity is started.
     */
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		   
		this.m_ibScan = IBScan.getInstance(this.getApplicationContext());
		this.m_ibScan.setScanListener(this);
	    
		this.m_ibMatcher = IBMatcher.getInstance();
		
		this.m_ibMatcherDatabase = new IBMatcherDatabase(this.getApplicationContext());
		
		Resources r = Resources.getSystem();
		Configuration config = r.getConfiguration();
		onConfigurationChanged(config);
		
		resetButtonsForState(AppState.NO_SCANNER_ATTACHED);
		transitionToRefresh();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) 
	{
		super.onConfigurationChanged(newConfig);

		if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) 
		{
			setContentView(R.layout.ib_scan_port_matcher);
		} 
		else
		{
			setContentView(R.layout.ib_scan_land_matcher);
		}
		
		/* Initialize UI fields for new orientation. */
		initUIFields(); 
		
		/* Populate UI with data from old orientation. */
		populateUI();  
	}
	
	/*
	 * Release driver resources.
	 */
	@Override
	protected void onDestroy() 
	{
		super.onDestroy();
	}

	@Override
	public void onBackPressed() 
	{
		exitApp(this);
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() 
	{
		/* 
		 * For the moment, we do not restore the current operation after a configuration change; 
		 * just close any open device.
		 */
		this.m_scanHandler.removeCallbacksAndMessages(null);
        if (this.m_ibScanDevice != null)
        {
        	/* Try to cancel any active capture. */
        	try
        	{
        		boolean isActive = this.m_ibScanDevice.isCaptureActive();
        		if (isActive)
        		{
        			this.m_ibScanDevice.cancelCaptureImage();
        		}
        	}
        	catch (IBScanException ibse)
        	{
        		Log.e(SIMPLE_MATCH_TAG, "error canceling capture " + ibse.getType().toString());        		
        	}
        	/* Try to close any open device. */
        	try
        	{
        		this.m_ibScanDevice.close();
        	}
        	catch (IBScanException ibse)
        	{
        		Log.e(SIMPLE_MATCH_TAG, "error closing device " + ibse.getType().toString());
        	}
        	this.m_ibScanDevice = null;
        }
        
        return (null);
	}
	
	/* *********************************************************************************************
	 * PRIVATE METHODS
	 ******************************************************************************************** */
	
	/*
	 * Initialize UI fields for new orientation.
	 */
	private void initUIFields()
	{
		this.m_txtDeviceCount      = (TextView) findViewById(R.id.device_count);
		this.m_txtStatus           = (TextView) findViewById(R.id.status);
		this.m_txtDesciption       = (TextView) findViewById(R.id.description);

		/* Hard-coded for four finger qualities. */
		this.m_txtFingerQuality[0] = (TextView) findViewById(R.id.scan_states_color1);
		this.m_txtFingerQuality[1] = (TextView) findViewById(R.id.scan_states_color2);
		this.m_txtFingerQuality[2] = (TextView) findViewById(R.id.scan_states_color3);
		this.m_txtFingerQuality[3] = (TextView) findViewById(R.id.scan_states_color4);

		this.m_txtFrameTime        = (TextView) findViewById(R.id.frame_time);

		this.m_txtActionState      = (TextView) findViewById(R.id.action_state);

		this.m_txtSDKVersion       = (TextView) findViewById(R.id.version);

		this.m_imagePreviewImage   = (ImageView) findViewById(R.id.preview_image);
		this.m_imagePreviewImage.setOnLongClickListener(this.m_imagePreviewImageLongClickListener);
		this.m_imagePreviewImage.setBackgroundColor(PREVIEW_IMAGE_BACKGROUND);
		
		this.m_stopCaptureBtn      = (Button) findViewById(R.id.stop_capture_btn);
		this.m_stopCaptureBtn.setOnClickListener(this.m_stopCaptureBtnClickListener);

		this.m_startCaptureBtn     = (Button) findViewById(R.id.start_capture_btn);
		this.m_startCaptureBtn.setOnClickListener(this.m_startCaptureBtnClickListener);
		
		this.m_openScannerBtn      = (Button) findViewById(R.id.open_scanner_btn);
		this.m_openScannerBtn.setOnClickListener(this.m_openScannerBtnClickListener);
		
		this.m_closeScannerBtn     = (Button) findViewById(R.id.close_scanner_btn);
		this.m_closeScannerBtn.setOnClickListener(this.m_closeScannerBtnClickListener);
		
		this.m_refreshBtn          = (Button) findViewById(R.id.refresh_btn);
		this.m_refreshBtn.setOnClickListener(this.m_refreshBtnClickListener);
		
		this.m_spinnerCaptureType  = (Spinner) findViewById(R.id.capture_type);
		final ArrayAdapter<CharSequence> adapterCapture = new ArrayAdapter<CharSequence>(this, 
				android.R.layout.simple_spinner_item, new CharSequence[] { });
		adapterCapture.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		this.m_spinnerCaptureType.setAdapter(adapterCapture);
		this.m_spinnerCaptureType.setOnItemSelectedListener(this.m_captureTypeItemSelectedListener);
		
		this.m_spinnerActionType   = (Spinner) findViewById(R.id.action_type);
		final ArrayAdapter<CharSequence> adapterAction = new ArrayAdapter<CharSequence>(this, 
				android.R.layout.simple_spinner_item, 
				new CharSequence[] {"Capture", "Match", "Single enroll", "Multi enroll" });
		adapterAction.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		this.m_spinnerActionType.setAdapter(adapterAction);
		
		this.m_viewDatabaseBtn     = (Button) findViewById(R.id.view_database_btn);
		this.m_viewDatabaseBtn.setOnClickListener(this.m_viewDatabaseBtnClickListener);
	}
		
	/*
	 * Populate UI with data from old orientation.
	 */
	private void populateUI()
	{
		resetButtonsForState(this.m_savedData.state);

		setSDKVersionInfo();
		setDeviceCount(this.m_savedData.deviceCount);
		setDescription(this.m_savedData.description, this.m_savedData.descriptionColor);

		if (this.m_savedData.status != null)
		{
			this.m_txtStatus.setText(this.m_savedData.status);
		}
		if (this.m_savedData.frameTime != null)
		{
			this.m_txtFrameTime.setText(this.m_savedData.frameTime);
		}
		if (this.m_savedData.actionState != null)
		{
			this.m_txtActionState.setText(this.m_savedData.actionState);
		}
		if (this.m_savedData.imageBitmap != null) 
		{
			this.m_imagePreviewImage.setImageBitmap(this.m_savedData.imageBitmap);
		}
		
		for (int i = 0; i < FINGER_QUALITIES_COUNT; i++)
		{
			this.m_txtFingerQuality[i].setBackgroundColor(this.m_savedData.fingerQualityColors[i]);
		}

		if (this.m_savedData.captureType != CAPTURE_TYPE_INVALID)
		{
			this.m_spinnerCaptureType.setSelection(this.m_savedData.captureType);
		}
		
		this.m_imagePreviewImage.setLongClickable(this.m_savedData.imagePreviewImageClickable);
	}	

	/*
	 * Show Toast message on UI thread.
	 */
	private void showToastOnUiThread(final String message, final int duration)
	{
		runOnUiThread(new Runnable() 
		{ 
			@Override
			public void run()
			{
				Toast toast = Toast.makeText(getApplicationContext(), message, duration);
				toast.show();				
			}
		});		
	}
 
	/*
	 * Set SDK version in SDK version text field.
	 */
	private void setSDKVersionInfo() 
	{
		String txtValue;

		try
		{
			SdkVersion sdkVersion;
			
			sdkVersion = this.m_ibMatcher.getSdkVersion();			
			txtValue   = "SDK version: " + sdkVersion.file;
		}
		catch (IBMatcherException ibse)
		{
			txtValue = "(failure)"; 
		}
		
		/* Make sure this occurs on the UI thread. */
		final String txtValueTemp = txtValue;
		runOnUiThread(new Runnable() 
		{
			@Override
			public void run()
			{
				SimpleMatchActivity2.this.m_txtSDKVersion.setText(txtValueTemp);				
			}
		});
	}

	/*
	 * Set description of header in finger print image box.
	 */
	private void setDescription(final String description, final int color) 
	{
		this.m_savedData.description = description;
		this.m_savedData.descriptionColor = color;
		
		/* Make sure this occurs on the UI thread. */
		runOnUiThread(new Runnable() 
		{
			@Override
			public void run()
			{
				SimpleMatchActivity2.this.m_txtDesciption.setText(description);
				SimpleMatchActivity2.this.m_txtDesciption.setBackgroundColor(color);
			}
		});
	}

	/*
	 * Set device count in device count text box.
	 */
	private void setDeviceCount(final int deviceCount) 
	{
		this.m_savedData.deviceCount = deviceCount;
		
		/* Make sure this occurs on the UI thread. */
		runOnUiThread(new Runnable() 
		{
			@Override
			public void run()
			{
				SimpleMatchActivity2.this.m_txtDeviceCount.setText("" + deviceCount);
			}
		});
	}

	/*
	 * Set status in status field.  Save value for orientation change.
	 */
	private void setStatus(final String s)
	{
		this.m_savedData.status = s;
		
		/* Make sure this occurs on the UI thread. */
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				SimpleMatchActivity2.this.m_txtStatus.setText(s);				
			}
		});
	}	
	
	/*
	 * Set frame time in frame time field.  Save value for orientation change.
	 */
	private void setFrameTime(final String s)
	{
		this.m_savedData.frameTime = s;
		
		/* Make sure this occurs on the UI thread. */
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				SimpleMatchActivity2.this.m_txtFrameTime.setText(s);		
			}
		});
	}

	/* 
	 * Set action state in action state field.  Save value for orientation change.
	 */
	private void setActionState(final String s)
	{
		this.m_savedData.actionState = s;
		
		/* Make sure this occurs on the UI thread. */
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				SimpleMatchActivity2.this.m_txtActionState.setText(s);		
			}
		});
	}
	
	/*
	 * Set capture types.
	 */
	private void setCaptureTypes(final String[] captureTypes)
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(SimpleMatchActivity2.this, 
						android.R.layout.simple_spinner_item, captureTypes);
				adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				SimpleMatchActivity2.this.m_spinnerCaptureType.setAdapter(adapter);				
			}
		});
	}
	
	/*
	 * Show progress dialog.
	 */
	private void showProgressDialog(final String title)
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				SimpleMatchActivity2.this.m_progressDialog = new ProgressDialog(SimpleMatchActivity2.this);
				SimpleMatchActivity2.this.m_progressDialog.setIndeterminate(true);
				SimpleMatchActivity2.this.m_progressDialog.setTitle(title);
				SimpleMatchActivity2.this.m_progressDialog.show();
			}
		});
	}

	/*
	 * Hide progress dialog.
	 */
	private void hideProgressDialog()
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				SimpleMatchActivity2.this.m_progressDialog.hide();
			}
		});
	}
	
	/*
	 * Show enlarged image in popup window.
	 */		
	private void showEnlargedImage()
	{
		/* 
		 * Sanity check.  Make sure the image exists.
		 */
		if (this.m_lastImageExt == null)
		{
			showToastOnUiThread("No last image information", Toast.LENGTH_SHORT);
			return;
		}
		
		this.m_enlargedDialog = new Dialog(this, R.style.Enlarged);
		this.m_enlargedDialog.setContentView(R.layout.enlarged);
		this.m_enlargedDialog.setCancelable(false);
		
		final Bitmap    bitmap       = this.m_ibMatcher.convertImageToBitmap(this.m_lastImageExt);
		final ImageView enlargedView = (ImageView) this.m_enlargedDialog.findViewById(R.id.enlarged_image);
		enlargedView.setImageBitmap(bitmap);
		enlargedView.setOnClickListener(this.m_enlargedImageClickListener);
		
		this.m_enlargedDialog.show();
	}
	
	/*
	 * Show database in popup window.
	 */		
	private void showDatabase()
	{
		this.m_databaseDialog = new Dialog(this, R.style.Enlarged);
		this.m_databaseDialog.setContentView(R.layout.database_list);
		this.m_databaseDialog.setCancelable(false);
		
		/* 
		 * This should be done in the background with a cursor loader to avoid blocking the UI thread. 
		 */
		final Cursor cursor = this.m_ibMatcherDatabase.getCursor();
		
		/* 
		 * Setup database list view.  Create adapter to map data between the cursor and list items
		 * in the view.  
		 */
		final ListView databaseView = (ListView) this.m_databaseDialog.findViewById(android.R.id.list);
		
		final SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this.m_databaseDialog.getContext(),
                R.layout.database_list_item,
                cursor, 
                new String[] {IBMatcherDatabase.COLUMN_NAME_NAME, 
                	IBMatcherDatabase.COLUMN_NAME_CREATE_DATE, 
                	IBMatcherDatabase.COLUMN_NAME_MODIFY_DATE,
                	IBMatcherDatabase.COLUMN_NAME_DESCRIPTION},
                new int[] {R.id.database_name, R.id.database_create, R.id.database_modify, 
                	R.id.database_description},
                0);
                
        /*
         * Set a view binder to specially format data from the cursor.
         */
        adapter.setViewBinder(new ViewBinder() 
        {
			@Override
			public boolean setViewValue(final View view, final Cursor cursor, final int columnIndex) 
			{
				if (columnIndex == IBMatcherDatabase.CURSOR_INDEX_CREATE_DATE)
				{
					final TextView textView  = (TextView) view.findViewById(R.id.database_create);
					final Long     dateValue = cursor.getLong(columnIndex);
					final Date     date      = new Date(dateValue.longValue());
					textView.setText(DateFormat.getDateInstance().format(date) 
							+ " " + DateFormat.getTimeInstance().format(date));
					return (true);
				}
				else if (columnIndex == IBMatcherDatabase.CURSOR_INDEX_MODIFY_DATE)
				{
					final TextView textView  = (TextView) view.findViewById(R.id.database_modify);
					final Long     dateValue = cursor.getLong(columnIndex);
					final Date     date      = new Date(dateValue.longValue());
					textView.setText(DateFormat.getDateInstance().format(date) 
							+ " " + DateFormat.getTimeInstance().format(date));
					return (true);
				}	
	
				return (false);
			}
        });
        databaseView.setAdapter(adapter);
        
        /*
         * Populate information about database.
         */
        final TextView databaseSize = (TextView) this.m_databaseDialog.findViewById(R.id.database_list_size);
        databaseSize.setText("" + this.m_ibMatcherDatabase.getSize() + " bytes"); 
        
        /*
         * Assign handlers for the buttons in the view to hide the dialog and clear the database.
         */
        final Button buttonDone = (Button) this.m_databaseDialog.findViewById(R.id.database_list_done);
        buttonDone.setOnClickListener(this.m_databaseDoneClickListener);
        
        final Button buttonClear = (Button) this.m_databaseDialog.findViewById(R.id.database_list_clear);
        buttonClear.setOnClickListener(this.m_databaseClearClickListener);
        
		this.m_databaseDialog.show();
	}
	
	/*
	 * Compress the image and attach it to an e-mail using an installed e-mail client. 
	 */
	private void sendImageInEmail(final ImageDataExt imageData, final String fileName, 
			final OutputFormat format) 
	{
		File    file    = new File(Environment.getExternalStorageDirectory().getPath() + "/" + fileName);
		boolean created = false;

		try
		{
			file.createNewFile();
	
			/* Compress image to temporary file. */
			switch (format)
			{
				case PNG:
					created = IBHelpers.createPng(imageData, file);
					break;
			
				case WSQ:
					created = IBHelpers.createWsq(imageData, file);
					break;
			
				case FIR:
					created = IBHelpers.createFir(imageData, file);
					break;
				
				case FMR:
					created = IBHelpers.createFmr(imageData, file);
					break;
					
				case IBSM_IMAGE:
					created = IBHelpers.createIbsmImage(imageData, file);
					break;
					
				case IBSM_TEMPLATE:
					created = IBHelpers.createIbsmTemplate(imageData, file);
					break;
			}
		} 
		catch (IOException ioe)
		{
			showToastOnUiThread("Could not create image for e-mail " + ioe.toString(), Toast.LENGTH_LONG);											
		}
		
		/* If file was created, send the e-mail. */
		if (created)
		{
			attachAndSendEmail(Uri.fromFile(file), "Fingerprint Image", fileName);
		}
	}
	
	/*
	 * Compress the image and attach it to an e-mail using an installed e-mail client. 
	 */
	private void sendImageInEmail(final Template template, final String fileName, 
			final OutputFormat format) 
	{
		File    file    = new File(Environment.getExternalStorageDirectory().getPath() + "/" + fileName);
		boolean created = false;

		try
		{
			file.createNewFile();
	
			/* Compress image to temporary file. */
			switch (format)
			{
				case FMR:
					created = IBHelpers.createFmr(template, file);
					break;
					
				case IBSM_TEMPLATE:
					created = IBHelpers.createIbsmTemplate(template, file);
					break;
			}
		} 
		catch (IOException ioe)
		{
			showToastOnUiThread("Could not create image for e-mail " + ioe.toString(), Toast.LENGTH_LONG);											
		}
		
		/* If file was created, send the e-mail. */
		if (created)
		{
			attachAndSendEmail(Uri.fromFile(file), "Fingerprint Image", fileName);
		}
	}
	
	/*
	 * Attach file to e-mail and send.
	 */
	private void attachAndSendEmail(final Uri uri, final String subject, final String message)
	{
	 	final Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("message/rfc822");
		i.putExtra(Intent.EXTRA_EMAIL,   new String[] { "" });
		i.putExtra(Intent.EXTRA_SUBJECT, subject);
		i.putExtra(Intent.EXTRA_STREAM,  uri);
		i.putExtra(Intent.EXTRA_TEXT,    message);

		try 
		{
			startActivity(Intent.createChooser(i, "Send mail..."));
		} 
		catch (ActivityNotFoundException anfe) 
		{
			showToastOnUiThread("There are no e-mail clients installed", Toast.LENGTH_LONG);
		}
	}
	
	/*
	 * Prompt to send e-mail with image.
	 */
	private void promptForEmail(final ImageDataExt imageData)
	{
		/* The dialog must be shown from the UI thread. */
		runOnUiThread(new Runnable() 
		{ 
			@Override
			public void run()
			{
		    	final AlertDialog.Builder builder = new AlertDialog.Builder(SimpleMatchActivity2.this);
		    	final String formats[] = {"PNG", "WSQ", "Fingerprint Image Record", 
		    			"Fingerprint Minutiae Record", "IBSM image", "IBSM template"};
		    	final String extensions[] = {"png", "wsq", "fir", "fmr", "ibsm_image", "ibsm_template"}; 
		    	builder.setTitle("Select format").setItems(formats, new DialogInterface.OnClickListener() 
		    		{
		    			@Override
						public void onClick(final DialogInterface dialog, final int which) 
		    			{
		    				final OutputFormat format = OutputFormat.values()[which];
		
		    				final LayoutInflater      inflater     = SimpleMatchActivity2.this.getLayoutInflater();
		    				final View                fileNameView = inflater.inflate(R.layout.file_name_dialog, null);
		    				final AlertDialog.Builder builder      = new AlertDialog.Builder(SimpleMatchActivity2.this)
		    					.setView(fileNameView)
		        	            .setTitle("Enter file name")
		        	            .setPositiveButton("OK", new DialogInterface.OnClickListener() 
		        	            {
									@Override
									public void onClick(final DialogInterface dialog, final int which) 
									{
										final EditText text     = (EditText) fileNameView.findViewById(R.id.file_name);
										final String   fileName = text.getText().toString();
										
										/* E-mail image in background thread. */
										Thread threadEmail = new Thread() 
										{
											@Override
											public void run()
											{
												sendImageInEmail(imageData, fileName, format);													
											}
										};
										threadEmail.start();
									}
								})
								.setNegativeButton("Cancel", null);
		    				EditText text = (EditText) fileNameView.findViewById(R.id.file_name);
		    				text.setText(FILE_NAME_DEFAULT + "." + extensions[which]);
		    				
			            	builder.create().show();
						}
					});
		    	builder.create().show();
			}
		});
	}
	
	/*
	 * Prompt to send e-mail with image.
	 */
	private void promptForEmail(final Template template)
	{
		/* The dialog must be shown from the UI thread. */
		runOnUiThread(new Runnable() 
		{ 
			@Override
			public void run()
			{
		    	final AlertDialog.Builder builder = new AlertDialog.Builder(SimpleMatchActivity2.this);
		    	final String formats[] = {"Fingerprint Minutiae Record", "IBSM template"};
		    	final String extensions[] = {"fmr", "ibsm_template"}; 
		    	builder.setTitle("Select format").setItems(formats, new DialogInterface.OnClickListener() 
		    		{
		    			@Override
						public void onClick(final DialogInterface dialog, final int which) 
		    			{
		    				final OutputFormat format = (which == 0) ? OutputFormat.FMR : OutputFormat.IBSM_TEMPLATE;
		
		    				final LayoutInflater      inflater     = SimpleMatchActivity2.this.getLayoutInflater();
		    				final View                fileNameView = inflater.inflate(R.layout.file_name_dialog, null);
		    				final AlertDialog.Builder builder      = new AlertDialog.Builder(SimpleMatchActivity2.this)
		    					.setView(fileNameView)
		        	            .setTitle("Enter file name")
		        	            .setPositiveButton("OK", new DialogInterface.OnClickListener() 
		        	            {
									@Override
									public void onClick(final DialogInterface dialog, final int which) 
									{
										final EditText text     = (EditText) fileNameView.findViewById(R.id.file_name);
										final String   fileName = text.getText().toString();
										
										/* E-mail image in background thread. */
										Thread threadEmail = new Thread() 
										{
											@Override
											public void run()
											{
												sendImageInEmail(template, fileName, format);													
											}
										};
										threadEmail.start();
									}
								})
								.setNegativeButton("Cancel", null);
		    				EditText text = (EditText) fileNameView.findViewById(R.id.file_name);
		    				text.setText(FILE_NAME_DEFAULT + "." + extensions[which]);
		    				
			            	builder.create().show();
						}
					});
		    	builder.create().show();
			}
		});
	}
	
	/*
	 * Prompt for enrollment.
	 */
	private void promptForEnroll(final Template template)
	{
		/* The dialog must be shown from the UI thread. */
		runOnUiThread(new Runnable() 
		{ 
			@Override
			public void run()
			{
				/*
				 * Prompt for details about user, including name and description.
				 */
				final LayoutInflater      inflater        = SimpleMatchActivity2.this.getLayoutInflater();
				final View                userDetailsView = inflater.inflate(R.layout.user_create_dialog, null);
				final AlertDialog.Builder builder         = new AlertDialog.Builder(SimpleMatchActivity2.this)
					.setView(userDetailsView)
    	            .setTitle("Enter user details")
    	            .setPositiveButton("OK", new DialogInterface.OnClickListener() 
    	            {
						@Override
						public void onClick(final DialogInterface dialog, final int which) 
						{
							final EditText textUserName = (EditText) userDetailsView.findViewById(R.id.user_name);
							final String   userName     = textUserName.getText().toString();
							
							final EditText textUserDescription = (EditText) userDetailsView.findViewById(R.id.user_description);
							final String   userDescription     = textUserDescription.getText().toString();
							
							/* Enroll in background thread. */
							Thread threadEnroll = new Thread() 
							{
								@Override
								public void run()
								{
									/* 
									 * Check whether user with this name already exists.  If so, 
									 * prompt to update the user's existing entry.
									 */
									final IBMatcherDatabase.Entry entryFind = SimpleMatchActivity2.this
											.m_ibMatcherDatabase.find(userName);
									
									if (entryFind != null)
									{
										/* The dialog must be shown from the UI thread. */
										runOnUiThread(new Runnable() 
										{ 
											@Override
											public void run()
											{
												final AlertDialog.Builder builder = new AlertDialog.Builder(SimpleMatchActivity2.this)
													.setTitle("Could not enroll user")
													.setMessage("User already enrolled.  Would you like to update user's entry?")
													.setPositiveButton("Yes", new DialogInterface.OnClickListener() 
													{
														@Override
														public void onClick(DialogInterface dialog, int which) 
														{
															/* 
															 * Update this user's entry.
															 */
															final boolean ok = SimpleMatchActivity2.this
																	.m_ibMatcherDatabase.update(userName, userDescription, template);
		
															if (ok)
															{
																showAlert("User updated", "");
															}
															else
															{
																showAlert("Could not update user", "");
															}
														}
													})
													.setNegativeButton("No", null);    	            
												builder.create().show();
											}
										});
									}
									else
									{
										/* 
										 * This user does not have an entry.  Enroll now.
										 */
										final boolean ok = SimpleMatchActivity2.this
												.m_ibMatcherDatabase.enroll(userName, userDescription, template);
										
										if (ok)
										{
											showAlert("User now enrolled", "");
										}
										else
										{
											showAlert("Could not enroll user", "");
										}
									}
								}
							};
							threadEnroll.start();
						}
					})
					.setNegativeButton("Cancel", null);
				final EditText textUserName = (EditText) userDetailsView.findViewById(R.id.user_name);
				textUserName.setText(USER_NAME_DEFAULT);
				
				final EditText textUserDescription = (EditText) userDetailsView.findViewById(R.id.user_description);
				textUserDescription.setText(USER_DESCRIPTION_DEFAULT);
				
            	builder.create().show();
			}
		});
	}
	
	/*
	 * Prompt for enrollment.
	 */
	private void promptForUpdate(final Template template)
	{
		/* The dialog must be shown from the UI thread. */
		runOnUiThread(new Runnable() 
		{ 
			@Override
			public void run()
			{
				/* 
				 * Prompt for details about user, including name (which will be used to look up 
				 * existing entry) and description (which will also be updated).
				 */
				final LayoutInflater      inflater        = SimpleMatchActivity2.this.getLayoutInflater();
				final View                userDetailsView = inflater.inflate(R.layout.user_create_dialog, null);
				final AlertDialog.Builder builder         = new AlertDialog.Builder(SimpleMatchActivity2.this)
					.setView(userDetailsView)
    	            .setTitle("Enter user details")
    	            .setPositiveButton("OK", new DialogInterface.OnClickListener() 
    	            {
						@Override
						public void onClick(final DialogInterface dialog, final int which) 
						{
							final EditText textUserName = (EditText) userDetailsView.findViewById(R.id.user_name);
							final String   userName     = textUserName.getText().toString();
							
							final EditText textUserDescription = (EditText) userDetailsView.findViewById(R.id.user_description);
							final String   userDescription     = textUserDescription.getText().toString();
							
							/* Update in background thread. */
							Thread threadUpdate = new Thread() 
							{
								@Override
								public void run()
								{
									/*
									 * Try to find user.  If user cannot be found, prompt to add 
									 * new entry.
									 */
									final IBMatcherDatabase.Entry entry = SimpleMatchActivity2.this
											.m_ibMatcherDatabase.find(userName);
									
									if (entry == null)
									{
										/* The dialog must be shown from the UI thread. */
										runOnUiThread(new Runnable() 
										{ 
											@Override
											public void run()
											{
												final AlertDialog.Builder builder = new AlertDialog.Builder(SimpleMatchActivity2.this)
													.setTitle("Could not enroll user")
													.setMessage("No user with name \"" + userName + "\" enrolled.  Would you like to add an entry for this user?")
													.setPositiveButton("Yes", new DialogInterface.OnClickListener() 
													{
														@Override
														public void onClick(final DialogInterface dialog, final int which) 
														{
															/*
															 * Add new entry for this user.
															 */
															final boolean ok = SimpleMatchActivity2.this
																	.m_ibMatcherDatabase.enroll(userName, userDescription, template);;
					
															if (ok)
															{
																showAlert("User now enrolled", "");
															}
															else
															{
																showAlert("Could not enroll user", "");
															}
														}
													})
													.setNegativeButton("No", null);    	            
												
												builder.create().show();
											}
										});
									}
									else
									{
										/*
										 * Update existing entry.
										 */
										final boolean ok = SimpleMatchActivity2.this
												.m_ibMatcherDatabase.update(userName, userDescription, template);
										
										if (ok)
										{
											showAlert("User updated", "");
										}
										else
										{
											showAlert("Could not update user", "");
										}
									}
								}
							};
							threadUpdate.start();
						}
					})
					.setNegativeButton("Cancel", null);
				final EditText textUserName = (EditText) userDetailsView.findViewById(R.id.user_name);
				textUserName.setText(USER_NAME_DEFAULT);
				
				final EditText textUserDescription = (EditText) userDetailsView.findViewById(R.id.user_description);
				textUserDescription.setText(USER_DESCRIPTION_DEFAULT);
				
            	builder.create().show();
			}
		});
	}

	/*
	 * Prompt for enrollment action.
	 */
	private void promptForEnrollAction(final Template template)
	{
		/* The dialog must be shown from the UI thread. */
		runOnUiThread(new Runnable() 
		{ 
			@Override
			public void run()
			{
		    	final AlertDialog.Builder builder = new AlertDialog.Builder(SimpleMatchActivity2.this);
		    	final String actions[] = {"Enroll new user", "Update existing user", "E-mail template"};
		    	builder.setTitle("Select action").setItems(actions, new DialogInterface.OnClickListener() 
		    		{
		    			@Override
						public void onClick(final DialogInterface dialog, final int which) 
		    			{
		    				switch (which)
		    				{
			    				case 0:
			    					promptForEnroll(template);
			    					break;
			    					
			    				case 1:
			    					promptForUpdate(template);
			    					break;
			    					
			    				case 2:
			    					promptForEmail(template);
			    					break;		    				
		    				}
						}
					});
		    	builder.create().show();
			}
		});
	}
	
	/*
	 * Show information about match.
	 */
	private void showMatch(final IBMatcherDatabase.Entry entry)
	{
		/* The dialog must be shown from the UI thread. */
		runOnUiThread(new Runnable() 
		{ 
			@Override
			public void run()
			{
				if (entry != null)
				{
					final LayoutInflater      inflater        = SimpleMatchActivity2.this.getLayoutInflater();
					final View                userDetailsView = inflater.inflate(R.layout.user_details_dialog, null);
					final AlertDialog.Builder builder         = new AlertDialog.Builder(SimpleMatchActivity2.this)
						.setView(userDetailsView)
	    	            .setTitle("Matching user details")
	    	            .setPositiveButton("OK", null);

					final TextView textUserName = (TextView) userDetailsView.findViewById(R.id.user_details_name);
					textUserName.setText(entry.getName());
					
					final TextView textUserDescription = (TextView) userDetailsView.findViewById(R.id.user_details_description);
					textUserDescription.setText(entry.getDescription());
					
					final TextView textCreateDate = (TextView) userDetailsView.findViewById(R.id.user_details_create_date);
					textCreateDate.setText(DateFormat.getDateInstance().format(entry.getCreateDate()) 
							+ " " + DateFormat.getTimeInstance().format(entry.getCreateDate()));

					final TextView textModifyDate = (TextView) userDetailsView.findViewById(R.id.user_details_modify_date);
					textModifyDate.setText(DateFormat.getDateInstance().format(entry.getModifyDate()) 
							+ " " + DateFormat.getTimeInstance().format(entry.getModifyDate()));
					
					final TextView textMatchScore = (TextView) userDetailsView.findViewById(R.id.user_details_match_score);
					textMatchScore.setText("" + entry.getMatchScore());

	            	builder.create().show();
				}
				else
				{
					showAlert("No matching user", "");
				}								
			}
		});
	}
	
	/*
	 * Show alert.
	 */
	private void showAlert(final String title, final String message)
	{
		/* The dialog must be shown from the UI thread. */
		runOnUiThread(new Runnable() 
		{ 
			@Override
			public void run()
			{
				final AlertDialog.Builder builder = new AlertDialog.Builder(SimpleMatchActivity2.this)
					.setTitle(title)
					.setMessage(message)
	            	.setPositiveButton("OK", null);    	            
				builder.create().show();
			}
		});
	}
	
	/*
	 * Exit application.
	 */
	private static void exitApp(final Activity ac) 
	{
		ac.moveTaskToBack(true);
		ac.finish();
		android.os.Process.killProcess(android.os.Process.myPid());
	}

	/* *********************************************************************************************
	 * PRIVATE METHODS (SCANNING STATE MACHINE)
	 ******************************************************************************************** */
	
	/*
	 * A handler to process state transition messages.
	 */
	private Handler m_scanHandler = new Handler(new Handler.Callback() 
	{		
		@Override
		public boolean handleMessage(final Message msg) 
		{
			final AppState nextState = AppState.values()[msg.what];
			
			switch (nextState)
			{
				case NO_SCANNER_ATTACHED:
					handleTransitionToNoScannerAttached();
					break;		
					
				case SCANNER_ATTACHED:
				{
					String deviceDesc  = (String)msg.obj;
					int    deviceCount = msg.arg1;
					handleTransitionToScannerAttached(deviceDesc, deviceCount);
					break;	
				}
				
				case REFRESH:
					handleTransitionToRefresh();
					break;
					
				case INITIALIZING:
				{
					final int deviceIndex = msg.arg1;
					handleTransitionToInitializing(deviceIndex);
					break;	
				}
				
				case INITIALIZED:
				{
					IBScanDevice scanDevice = (IBScanDevice)msg.obj;
					handleTransitionToInitialized(scanDevice);
					break;
				}
				
				case CLOSING:
					handleTransitionToClosing();
					break;	
				
				case STARTING_CAPTURE:
				{
					final ActionType actionType = (ActionType)msg.obj;
					handleTransitionToStartingCapture(actionType);
					break;	
				}
				
				case CAPTURING:
					handleTransitionToCapturing();
					break;	
					
				case STOPPING_CAPTURE:
					handleTransitionToStoppingCapture();
					break;
					
				case IMAGE_CAPTURED:
				{
					final Object[] objects = (Object[])msg.obj;
					final ImageData   imageData       = (ImageData)objects[0];
					final ImageType   imageType       = (ImageType)objects[1];
					final ImageData[] splitImageArray = (ImageData[])objects[2];
					handleTransitionToImageCaptured(imageData, imageType, splitImageArray);
					break;
				}
				
				case COMMUNICATION_BREAK:
					handleTransitionToCommunicationBreak();
					break;
			}
			
			return (false);
		}
	});
			
	/*
	 * Transition to no ### state.
	 */
	private void transitionToNoScannerAttached()
	{
		final Message msg = this.m_scanHandler.obtainMessage(AppState.NO_SCANNER_ATTACHED.ordinal());
		this.m_scanHandler.sendMessage(msg);
	}
	private void transitionToScannerAttached(final String deviceDesc, final int deviceCount)
	{
		final Message msg = this.m_scanHandler.obtainMessage(AppState.SCANNER_ATTACHED.ordinal(), deviceCount, 0, deviceDesc);
		this.m_scanHandler.sendMessage(msg);
	}
	private void transitionToRefresh()
	{
		final Message msg = this.m_scanHandler.obtainMessage(AppState.REFRESH.ordinal());
		this.m_scanHandler.sendMessage(msg);
	}
	private void transitionToInitializing(final int deviceIndex)
	{
		final Message msg = this.m_scanHandler.obtainMessage(AppState.INITIALIZING.ordinal(), deviceIndex, 0);
		this.m_scanHandler.sendMessage(msg);
	}
	private void transitionToInitialized(final IBScanDevice device)
	{
		final Message msg = this.m_scanHandler.obtainMessage(AppState.INITIALIZED.ordinal(), device);
		this.m_scanHandler.sendMessage(msg);
	}
	private void transitionToClosing()
	{
		final Message msg = this.m_scanHandler.obtainMessage(AppState.CLOSING.ordinal());
		this.m_scanHandler.sendMessage(msg);
	}
	private void transitionToStartingCapture(final ActionType actionType)
	{
		final Message msg = this.m_scanHandler.obtainMessage(AppState.STARTING_CAPTURE.ordinal(), actionType);
		this.m_scanHandler.sendMessage(msg);
	}
	private void transitionToCapturing()
	{
		final Message msg = this.m_scanHandler.obtainMessage(AppState.CAPTURING.ordinal());
		this.m_scanHandler.sendMessage(msg);
	}
	private void transitionToStoppingCapture()
	{
		final Message msg = this.m_scanHandler.obtainMessage(AppState.STOPPING_CAPTURE.ordinal());
		this.m_scanHandler.sendMessage(msg);
	}
	private void transitionToStoppingCaptureWithDelay(final int delayMillis)
	{
		final Message msg = this.m_scanHandler.obtainMessage(AppState.STOPPING_CAPTURE.ordinal());
		this.m_scanHandler.sendMessageDelayed(msg, delayMillis);
	}
	private void transitionToImageCaptured(final ImageData image, final ImageType imageType, 
			final ImageData[] splitImageArray)
	{
		final Message msg = this.m_scanHandler.obtainMessage(AppState.IMAGE_CAPTURED.ordinal(), 0, 0, 
				new Object[] {image, imageType, splitImageArray} );
		this.m_scanHandler.sendMessage(msg);
	}
	private void transitionToCommunicationBreak()
	{
		final Message msg = this.m_scanHandler.obtainMessage(AppState.COMMUNICATION_BREAK.ordinal());
		this.m_scanHandler.sendMessage(msg);
	}
		
	/* 
	 * Handle transition to no scanner attached state. 
	 */
	private void handleTransitionToNoScannerAttached()
	{
		/* Sanity check state. */
		switch (this.m_savedData.state)
		{
			case REFRESH:
				break;
			default:
				Log.e(SIMPLE_MATCH_TAG, "Received unexpected transition to NO_SCANNER_ATTACHED from " + this.m_savedData.state.toString());
				return;
		}

		/* Move to this state. */
		this.m_savedData.state = AppState.NO_SCANNER_ATTACHED;			

		/* Setup UI for state. */
		resetButtonsForState(AppState.NO_SCANNER_ATTACHED);
		setStatus("no scanners");
		setFrameTime(FRAME_TIME_DEFAULT);
		setDeviceCount(0);
		setDescription(NO_DEVICE_DESCRIPTION_STRING, NO_DEVICE_DESCRIPTION_COLOR);

		/*
		 * We will stay in this state until a scanner is attached or the user presses the "Refresh".
		 */
	}
	
	/*
	 * Handle transition to scanner attached state.
	 */
	private void handleTransitionToScannerAttached(final String deviceDesc, final int deviceCount)
	{
		/* Sanity check state. */
		switch (this.m_savedData.state)
		{
			case REFRESH:
				break;
			default:
				Log.e(SIMPLE_MATCH_TAG, "Received unexpected transition to SCANNER_ATTACHED from " + this.m_savedData.state.toString());
				return;
		}
		
		/* Move to this state. */
		this.m_savedData.state = AppState.SCANNER_ATTACHED;
		
		/* Setup UI for state. */
		resetButtonsForState(AppState.SCANNER_ATTACHED);
		setStatus("uninitialized");
		setFrameTime(FRAME_TIME_DEFAULT);
		setDeviceCount(deviceCount);
		setDescription(deviceDesc, DEVICE_DESCRIPTION_COLOR);
		
		/*
		 * We will stay in this state until the scanner is detached, the user presses the "Refresh"
		 * button, or the user presses the "Start" button.
		 */
	}
	
	/*
	 * Handle transition to refresh state.
	 */
	private void handleTransitionToRefresh()
	{
		/* Sanity check state. */
		switch (this.m_savedData.state)
		{
			case NO_SCANNER_ATTACHED:
			case SCANNER_ATTACHED:
			case CLOSING:
				break;
			case INITIALIZED:
				/*
				 * If the initialized device has been disconnected, transition to closing, then 
				 * refresh.
				 */
				if (this.m_ibScanDevice != null)
				{
					try
					{
						/* Just a test call. */
						this.m_ibScanDevice.isCaptureActive();
					}
					catch (IBScanException ibse)
					{
						transitionToClosing();						
					}
				}
				return;
			case INITIALIZING:
			case STARTING_CAPTURE:
			case CAPTURING:
			case STOPPING_CAPTURE:
			case IMAGE_CAPTURED:
			case COMMUNICATION_BREAK:
				/* 
				 * These transitions is ignored to preserve UI state.  The CLOSING state will transition to 
				 * REFRESH.
				 */
				return;
			case REFRESH:
				/*
				 * This transition can occur when multiple events (button presses, device count changed 
				 * callbacks occur).  We assume the last execution will transition to the correct state.
				 */
				return;
			default:
				Log.e(SIMPLE_MATCH_TAG, "Received unexpected transition to REFRESH from " + this.m_savedData.state.toString());
				return;
		}
		
		/* Move to this state. */
		this.m_savedData.state = AppState.REFRESH;
		
		/* Setup UI for state. */
		resetButtonsForState(AppState.REFRESH);
		setStatus("refreshing");
		setFrameTime(FRAME_TIME_DEFAULT);
		setActionState("");
		setCaptureTypes(new String[0]);
		
		/*
		 * Make sure there are no USB devices attached that are IB scanners for which permission has
		 * not been granted.  For any that are found, request permission; we should receive a 
		 * callback when permission is granted or denied and then when IBScan recognizes that new
		 * devices are connected, which will result in another refresh.
		 */
    	final UsbManager                 manager        = (UsbManager)this.getApplicationContext().getSystemService(Context.USB_SERVICE);
    	final HashMap<String, UsbDevice> deviceList     = manager.getDeviceList();
		final Iterator<UsbDevice>        deviceIterator = deviceList.values().iterator();
		while (deviceIterator.hasNext())
		{
		    final UsbDevice device       = deviceIterator.next();
		    final boolean   isScanDevice = IBScan.isScanDevice(device);		    
		    if (isScanDevice)
		    {
		    	final boolean hasPermission = manager.hasPermission(device);
		    	if (!hasPermission)
		    	{
		    		this.m_ibScan.requestPermission(device.getDeviceId());
		    	}
		    }
		}
		
		/* 
		 * Determine the next state according to device count.  A state transition always occurs,
		 * either to NO_SCANNER_ATTACHED or SCANNER_ATTACHED.
		 */
		try
		{
			final int deviceCount = this.m_ibScan.getDeviceCount();
			if (deviceCount > 0)
			{
				try
				{
					final DeviceDesc deviceDesc = this.m_ibScan.getDeviceDescription(INITIALIZING_DEVICE_INDEX);
					transitionToScannerAttached(deviceDesc.productName + " - " + deviceDesc.serialNumber, deviceCount);
				}
				catch (IBScanException ibse)
				{
					Log.e(SIMPLE_MATCH_TAG, "Received exception getting device description " + ibse.getType().toString());
					transitionToNoScannerAttached();					
				}
			}
			else
			{
				transitionToNoScannerAttached();
			}
		}
		catch (IBScanException ibse)
		{
			Log.e(SIMPLE_MATCH_TAG, "Received exception getting device count " + ibse.getType().toString());
			transitionToNoScannerAttached();
		}
	}
	
	/*
	 * Handle transition to initializing state.
	 */
	private void handleTransitionToInitializing(final int deviceIndex)
	{
		/* Sanity check state. */
		switch (this.m_savedData.state)
		{
			case SCANNER_ATTACHED:
				break;
			default:
				Log.e(SIMPLE_MATCH_TAG, "Received unexpected transition to INITIALIZING from " + this.m_savedData.state.toString());
				return;
		}
		
		/* Move to this state. */
		this.m_savedData.state = AppState.INITIALIZING;

		/* Setup UI for state. */
		resetButtonsForState(AppState.INITIALIZING);
		setStatus("initializing");
		setFrameTime(FRAME_TIME_DEFAULT);
		
		/* Clear saved information from last scan and prevent long clicks on the image viewer. */
		this.m_imagePreviewImage.setLongClickable(false);
		this.m_savedData.imagePreviewImageClickable = false;
		this.m_lastImageExts = null;
		this.m_lastImageExt  = null;
		
		/* Start device initialization. */
		try
		{
			/* While the device is being opened, callbacks for initialization progress will be 
			 * received.  When the device is open, another callback will be received and capture can
			 * begin.  We will stay in this state until capture begins.
			 */
			this.m_ibScan.openDeviceAsync(deviceIndex);			
		}
		catch (IBScanException ibse)
		{
			/* Device initialization failed.  Go to closing. */
			showToastOnUiThread("Could not initialize device with exception " + ibse.getType().toString(), Toast.LENGTH_SHORT);
			transitionToClosing();
		}
	}
	
	/*
	 * Handle transition to starting capture state.
	 */
	private void handleTransitionToInitialized(final IBScanDevice device)
	{
		/* Sanity check state. */
		switch (this.m_savedData.state)
		{
			case INITIALIZING:
			case STARTING_CAPTURE:
			case STOPPING_CAPTURE:
			case IMAGE_CAPTURED:
				break;
			default:
				Log.e(SIMPLE_MATCH_TAG, "Received unexpected transition to INITIALIZED from " + this.m_savedData.state.toString());
				return;
		}
		
		/* Move to this state. */
		this.m_savedData.state = AppState.INITIALIZED;
		
		/* Setup the UI for state. */
		resetButtonsForState(AppState.INITIALIZED);
		setStatus("initialized");
		setFrameTime(FRAME_TIME_DEFAULT);
		
		/* If the device is null, we have already passed through this state. */
		if (device != null)
		{
			/* Enable power save mode. */
			try
			{
				device.setProperty(PropertyId.ENABLE_POWER_SAVE_MODE, "TRUE");
			}
			catch (IBScanException ibse)
			{
				/* 
				 * We could not enable power save mode. This is was non-essential, so we continue on and 
				 * see whether we can start capture. 
				 */
				Log.e(SIMPLE_MATCH_TAG, "Could not begin enable power save mode " + ibse.getType().toString());			
			}
			
			/* Get list of acceptable capture types. */
			Vector<String> typeVector = new Vector<String>();
			for (ImageType imageType : ImageType.values())
			{
				try
				{
					boolean available = device.isCaptureAvailable(imageType, ImageResolution.RESOLUTION_500);
					if (available)
					{
						typeVector.add(imageType.toDescription());
					}
				}
				catch (IBScanException ibse)
				{
					Log.e(SIMPLE_MATCH_TAG, "Could not check capture availability " + ibse.getType().toString());					
				}
			}
			String[] typeArray = new String[0];
			typeArray = typeVector.toArray(typeArray);
			setCaptureTypes(typeArray);
			/* Save device. */
			this.m_ibScanDevice = device;
		}
		
		/*
		 * Stay in this state waiting to perform scans. 
		 */
	}

	/*
	 * Handle transition to closing state.
	 */
	private void handleTransitionToClosing()
	{
		/* Sanity check state. */
		switch (this.m_savedData.state)
		{
			case INITIALIZING:
			case INITIALIZED:
			case COMMUNICATION_BREAK:
				break;
			default:
				Log.e(SIMPLE_MATCH_TAG, "Received unexpected transition to CLOSING from " + this.m_savedData.state.toString());
				return;
		}
		
		/* Move to this state. */
		this.m_savedData.state = AppState.CLOSING;
		
		/* Setup the UI for state. */
		resetButtonsForState(AppState.CLOSING);
		setStatus("closing");
		setFrameTime(FRAME_TIME_DEFAULT);
		
		/* Close & null device. */
		if (this.m_ibScanDevice != null)
		{
			try
			{
				this.m_ibScanDevice.close();
			}
			catch (IBScanException ibse)
			{
				Log.e(SIMPLE_MATCH_TAG, "Could not close device " + ibse.getType().toString());				
			}
			this.m_ibScanDevice = null;
		}
		
		/*
		 * Refresh the list of devices. 
		 */
		transitionToRefresh();
	}

	/*
	 * Handle transition to starting capture state.
	 */
	private void handleTransitionToStartingCapture(final ActionType actionType)
	{
		/* Sanity check state. */
		switch (this.m_savedData.state)
		{
			case INITIALIZED:
			case IMAGE_CAPTURED:
				break;
			default:
				Log.e(SIMPLE_MATCH_TAG, "Received unexpected transition to STARTING_CAPTURE from " + this.m_savedData.state.toString());
				return;
		}
		
		/* Move to this state. */
		this.m_savedData.state = AppState.STARTING_CAPTURE;
		
		/* Setup the UI for state. */
		resetButtonsForState(AppState.STARTING_CAPTURE);
		setStatus("starting");
		setFrameTime(FRAME_TIME_DEFAULT);
		
		final int[] imageCounts = {1, 1, 3, 6};		
		final int   imageCount  = (actionType != null) ? imageCounts[actionType.ordinal()] : imageCounts[this.m_actionType.ordinal()];
		if (actionType != null)
		{
			this.m_imagesCaptured = 0;
			this.m_actionType     = actionType;
			this.m_lastImageExts  = new ImageDataExt[imageCount];
		}
		setActionState("capturing image " + (this.m_imagesCaptured + 1) + " of " + imageCount);
		
		try 
		{
			ImageType imageType = ImageType.TYPE_NONE;
			
			for (ImageType imageTypeTemp : ImageType.values())
			{
				if (((CharSequence)this.m_spinnerCaptureType.getSelectedItem()).equals(imageTypeTemp.toDescription()))
				{
					imageType = imageTypeTemp;
					break;
				}
			}
			
			/* 
			 * Begin capturing an image.  While the image is being captured, we will receive
			 * preview images through callbacks.  At the end of the capture, we will recieve a
			 * final image. 
			 */
			this.m_ibScanDevice.beginCaptureImage(imageType, ImageResolution.RESOLUTION_500, 
					IBScanDevice.OPTION_AUTO_CAPTURE | IBScanDevice.OPTION_AUTO_CONTRAST);

			/* Save this device and image type for later use. */
			this.m_imageType = imageType;
			transitionToCapturing();
		}
		catch(IBScanException ibse)
		{
			/* We could not begin capturing.  Go to back to initialized. */
			showToastOnUiThread("Could not begin capturing with error " + ibse.getType().toString(), Toast.LENGTH_SHORT);
			transitionToInitialized(null);
		}
	}
	
	/*
	 * Handle transition to capturing state.
	 */
	private void handleTransitionToCapturing()
	{
		/* Sanity check state. */
		switch (this.m_savedData.state)
		{
			case STARTING_CAPTURE:
				break;
			default:
				Log.e(SIMPLE_MATCH_TAG, "Received unexpected transition to CAPTURING from " + this.m_savedData.state.toString());
				return;
		}
		
		/* Move to this state. */
		this.m_savedData.state = AppState.CAPTURING;
		
		/* Setup UI for state. */
		resetButtonsForState(AppState.CAPTURING);
		setStatus("capturing");
		setFrameTime(FRAME_TIME_DEFAULT);
		
		/* 
		 * We will start receiving callbacks for preview images and finger count and quality 
		 * changes. 
		 */
		this.m_ibScanDevice.setScanDeviceListener(this);		
		showToastOnUiThread("Now capturing...put a finger on the sensor", Toast.LENGTH_SHORT);
		
		/*
		 * We stay in this state until a good-quality image with the correct number of fingers is 
		 * obtained, an error occurs (such as a communication break), or the user presses the "Stop"
		 * button.
		 */
	}

	/*
	 * Handle transition to stopping capture state.
	 */
	private void handleTransitionToStoppingCapture()
	{
		/* Sanity check state. */
		switch (this.m_savedData.state)
		{
			case CAPTURING:
			case STOPPING_CAPTURE:
				break;
			default:
				Log.e(SIMPLE_MATCH_TAG, "Received unexpected transition to STOPPING_CAPTURE from " + this.m_savedData.state.toString());
				return;
		}

		/* Move to this state. */
		this.m_savedData.state = AppState.STOPPING_CAPTURE;		
		
		/* Setup UI for state. */
		resetButtonsForState(AppState.STOPPING_CAPTURE);
		setStatus("stopping");
		setFrameTime(FRAME_TIME_DEFAULT);
		setActionState("capture stopped");

		/* Cancel capture if necessary. */
		boolean done = false;		
		try
		{
			final boolean active = this.m_ibScanDevice.isCaptureActive();

			if (!active)
			{
				/* Capture has already stopped.  Let's transition to the refresh state. */
				showToastOnUiThread("Capture stopped", Toast.LENGTH_SHORT);
				done = true;
			} 
			else
			{
				try
				{
					/* Cancel capturing the image. */
					this.m_ibScanDevice.cancelCaptureImage();									
				}
				catch (IBScanException ibse)
				{
					showToastOnUiThread("Could not cancel capturing with error " + ibse.getType().toString(), Toast.LENGTH_SHORT);	
					done = true;
				}
			}			
		}
		catch (IBScanException ibse)
		{
			/* An error occurred.  Let's try to refresh. */
			showToastOnUiThread("Could not query capture active state " + ibse.getType().toString(), Toast.LENGTH_SHORT);
			done = true;
		}
		
		/*
		 * On error or capture not active, transition to initialized.
		 */
		if (done)
		{
			transitionToInitialized(null);
		}
		/* 
		 *  We must wait for this to complete, so we will resubmit this transition with a delay.
		 */
		else
		{
			transitionToStoppingCaptureWithDelay(STOPPING_CAPTURE_DELAY_MILLIS);
		}
	}
	
	/* 
	 * Handle transition to image captured state.
	 */
	private void handleTransitionToImageCaptured(final ImageData image, 
			final ImageType imageType, final ImageData[] splitImageArray)
	{
		/* Sanity check state. */
		switch (this.m_savedData.state)
		{
			case CAPTURING:
				break;
			default:
				showToastOnUiThread("Received unexpected transition to STOPPING_CAPTURE from " + this.m_savedData.state.toString(), Toast.LENGTH_SHORT);
				return;
		}
		
		/* Move to this state. */
		this.m_savedData.state = AppState.IMAGE_CAPTURED;		
		
		/* Setup UI for state. */
		resetButtonsForState(AppState.IMAGE_CAPTURED);
		setStatus("captured");
		setFrameTime(FRAME_TIME_DEFAULT);

		this.m_beeper.playSound();

		/* 
		 * Update the state for this action and save data. 
		 */
		final int[] imageCounts = {1, 1, 3, 6};		
		final int   imageCount  = imageCounts[this.m_actionType.ordinal()];
		try
		{
			Object[] imageInfoExt = this.m_ibScanDevice.getResultImageExt(IBCommon.FingerPosition.UNKNOWN);
		
			this.m_lastImageExt                         = (ImageDataExt)imageInfoExt[0];
			this.m_lastImageExts[this.m_imagesCaptured] = (ImageDataExt)imageInfoExt[0];

		}
		catch (IBScanException ibse)
		{
			this.m_lastImageExt                         = null;
			this.m_lastImageExts[this.m_imagesCaptured] = null;
			showToastOnUiThread("Error creating imageDataExt " + ibse.getType().toString(), Toast.LENGTH_SHORT);
		}
		
		this.m_imagesCaptured++;
		
		if (this.m_imagesCaptured < imageCount)
		{
			/* 
			 * If there are more images to capture for this action, transition back to capturing. 
			 */
			setActionState("captured image " + this.m_imagesCaptured + " of " + imageCount);
			transitionToStartingCapture(null);
		}
		else
		{
			final String[] actionNames = {"capture", "match", "single enrollment", "multiple enrollment"};
		
			/* 
			 * Save information in case we later show the enlarged image and allow long clicks on the
			 * image view to show that view. 
			 */
			setActionState("performed " + actionNames[this.m_actionType.ordinal()]);
			this.m_savedData.imagePreviewImageClickable = true;
			this.m_imagePreviewImage.setLongClickable(true);
				
			/* 
			 * Prompt user to handle data in action-appropriate manner. 
			 */
			switch (this.m_actionType)
			{
				/* Calculate NFIQ score. */
				case CAPTURE:
				{
					final Thread threadNfiq = new Thread() 
					{
						@Override
						public void run()
						{
							try
							{
								final int nfiqScore = SimpleMatchActivity2.this.m_ibScanDevice.calculateNfiqScore(image);
								showToastOnUiThread("NFIQ score for print is " + nfiqScore, Toast.LENGTH_SHORT);
							}
							catch (IBScanException ibse)
							{
								showToastOnUiThread("Error calculating NFIQ score " + ibse.getType().toString(), Toast.LENGTH_SHORT);
							}
						}
					};
					threadNfiq.start();
					
					break;
				}
					
				/* See whether the image matches a template in the database. */
				case MATCH:
				{
					final ImageDataExt[] images = this.m_lastImageExts;

					showProgressDialog("Processing...");
					
					/* Match on background thread. */
					final Thread threadMatch = new Thread()
					{
						@Override
						public void run()
						{
							try 
							{
								final Template              template = SimpleMatchActivity2.this.m_ibMatcher.extractTemplate(images[0]);								
								final IBMatcherDatabase.Entry entry  = SimpleMatchActivity2.this.m_ibMatcherDatabase.match(template);
								hideProgressDialog();
								
								showMatch(entry);
								
							}
							catch (IBMatcherException ibme)
							{
								hideProgressDialog();								
								showAlert("Could not match", "Error generating template");
								Log.e(SIMPLE_MATCH_TAG, "Error generating template for " + ibme.getType().toString());
							}
						}								
					};
					threadMatch.start();
				
				}
				break;
					
				/* Generate a template from the images. */
				case SINGLE_ENROLL:
				{
					final ImageDataExt[] images = this.m_lastImageExts;
				
					showProgressDialog("Processing...");
				
					final Thread threadEnroll = new Thread()
					{
						@Override
						public void run()
						{
							try 
							{
								final Template template = SimpleMatchActivity2.this.m_ibMatcher.singleEnrollment(
										images[0], images[1], images[2]);
								hideProgressDialog();
								
								promptForEnrollAction(template);
							}
							catch (IBMatcherException ibme)
							{
								hideProgressDialog();
								showAlert("Could not enroll user", "Error generating template.  Please retry.");
								Log.e(SIMPLE_MATCH_TAG, "Error generating template for " + ibme.getType().toString());
							}
						}								
					};
					threadEnroll.start();
					
					break;
				}
					
				/* Generate a template from the images. */
				case MULTI_ENROLL:
				{
					final ImageDataExt[] images = this.m_lastImageExts;
				
					showProgressDialog("Processing...");
				
					final Thread threadEnroll = new Thread()
					{
						@Override
						public void run()
						{
							try 
							{
								final Template[] templates = SimpleMatchActivity2.this.m_ibMatcher.multiEnrollment(
										images[0], images[1], images[2], images[3], images[4], images[5]);
								hideProgressDialog();

								promptForEnrollAction(templates[0]);
							}
							catch (IBMatcherException ibme)
							{
								hideProgressDialog();
								showAlert("Could not enroll user", "Error generating template.  Please retry.");
								Log.e(SIMPLE_MATCH_TAG, "Error generating template for " + ibme.getType().toString());
							}
						}								
					};
					threadEnroll.start();
					
					break;
				}
			}
			
			/* Move back to initialized state. */
			transitionToInitialized(null);		
		}		
	}
		
	/*
	 * Handle transition to communication break.
	 */
	private void handleTransitionToCommunicationBreak()
	{
		/* Sanity check state. */
		switch (this.m_savedData.state)
		{
			case CAPTURING:
			case STOPPING_CAPTURE:
			case INITIALIZED:
				break;
			default:
				Log.e(SIMPLE_MATCH_TAG, "Received unexpected transition to COMMUNICATION_BREAK from " + this.m_savedData.state.toString());
				return;
		}
		
		/* Move to this state. */
		this.m_savedData.state = AppState.COMMUNICATION_BREAK;
		
		/* Setup UI for this state. */
		resetButtonsForState(AppState.COMMUNICATION_BREAK);
		setStatus("comm break");
		setFrameTime(FRAME_TIME_DEFAULT);
		
		/* Transition to closing, then to refresh. */
		transitionToClosing();
	}

	/*
	 * Reset the stop, start, and refresh buttons for the state.
	 */
	private void resetButtonsForState(final AppState state)
	{
		                              /* NO_SCAN, SCANNER, REFRESH, INITIALIZING, INITIALIZED, CLOSING, STARTING, CAPTURING, STOPPING, CAPTURED, BREAK */
		final boolean[] stopStates    = {false,   false,   false,   false,        false,       false,   false,    true,      false,    false,    false};
		final boolean[] startStates   = {false,   false,   false,   false,        true,        false,   false,    false,     false,    false,    false};
		final boolean[] refreshStates = {true,    true,    false,   false,        false,       false,   false,    false,     false,    false,    false};
		final boolean[] captureStates = {false,   false,   false,   false,        true,        false,   false,    false,     false,    false,    false};
		final boolean[] openStates    = {false,   true,    false,   false,        false,       false,   false,    false,     false,    false,    false};
		final boolean[] closeStates   = {false,   false,   false,   false,        true,        false,   false,    false,     false,    false,    false};
		final boolean[] actionStates  = closeStates;
		final boolean[] viewStates    = {true,    true,    false,   false,        true,        false,   false,    false,     false,    false,    false};
		
		final boolean stopButtonClickable     = stopStates[state.ordinal()];
		final boolean startButtonClickable    = startStates[state.ordinal()];
		final boolean refreshButtonClickable  = refreshStates[state.ordinal()];
		final boolean captureSpinnerClickable = captureStates[state.ordinal()];
		final boolean openButtonClickable     = openStates[state.ordinal()];
		final boolean closeButtonClickable    = closeStates[state.ordinal()];
		final boolean actionSpinnerClickable  = actionStates[state.ordinal()];
		final boolean viewButtonClickable     = viewStates[state.ordinal()];
		
		/* Make sure the update occurs from the UI thread. */
		runOnUiThread(new Runnable() 
		{
			@Override
			public void run()
			{
				SimpleMatchActivity2.this.m_stopCaptureBtn.setEnabled(stopButtonClickable);
				SimpleMatchActivity2.this.m_stopCaptureBtn.setClickable(stopButtonClickable);
				
				SimpleMatchActivity2.this.m_startCaptureBtn.setEnabled(startButtonClickable);
				SimpleMatchActivity2.this.m_startCaptureBtn.setClickable(startButtonClickable);

				SimpleMatchActivity2.this.m_refreshBtn.setEnabled(refreshButtonClickable);
				SimpleMatchActivity2.this.m_refreshBtn.setClickable(refreshButtonClickable);		
				
				SimpleMatchActivity2.this.m_spinnerCaptureType.setEnabled(captureSpinnerClickable);
				SimpleMatchActivity2.this.m_spinnerCaptureType.setClickable(captureSpinnerClickable);
				
				SimpleMatchActivity2.this.m_openScannerBtn.setEnabled(openButtonClickable);
				SimpleMatchActivity2.this.m_openScannerBtn.setClickable(openButtonClickable);

				SimpleMatchActivity2.this.m_closeScannerBtn.setEnabled(closeButtonClickable);
				SimpleMatchActivity2.this.m_closeScannerBtn.setClickable(closeButtonClickable);	
				
				SimpleMatchActivity2.this.m_spinnerActionType.setEnabled(actionSpinnerClickable);
				SimpleMatchActivity2.this.m_spinnerActionType.setClickable(actionSpinnerClickable);	
				
				SimpleMatchActivity2.this.m_viewDatabaseBtn.setEnabled(viewButtonClickable);
				SimpleMatchActivity2.this.m_viewDatabaseBtn.setClickable(viewButtonClickable);		
			}
		});
	}

	/* *********************************************************************************************
	 * EVENT HANDLERS
	 ******************************************************************************************** */
	
	/* 
	 * Handle click on "Start capture" button.
	 */
	private OnClickListener m_startCaptureBtnClickListener = new OnClickListener() 
	{
		@Override
		public void onClick(final View v) 
		{
			/* Sanity check.  Make sure we are in a proper state. */
			switch (SimpleMatchActivity2.this.m_savedData.state)
			{
				case INITIALIZED:
					break;	
				default:
					Log.e(SIMPLE_MATCH_TAG, "Received unexpected start button event in state " + SimpleMatchActivity2.this.m_savedData.state.toString());
					return;
			}		
				
			final int        position = SimpleMatchActivity2.this.m_spinnerActionType.getSelectedItemPosition();
			final ActionType actionType = (position == Spinner.INVALID_POSITION) ? ActionType.values()[0] : ActionType.values()[position];
				
	
			/* Transition to capturing state. */
			transitionToStartingCapture(actionType);
		}
	};
	
	/*
	 * Handle click on "Stop capture" button. 
	 */	
	private OnClickListener m_stopCaptureBtnClickListener = new OnClickListener() 
	{
		@Override
		public void onClick(final View v) 
		{
			/* Sanity check.  Make sure we are in a proper state. */
			switch (SimpleMatchActivity2.this.m_savedData.state)
			{
				case CAPTURING:
					break;	
				default:
					Log.e(SIMPLE_MATCH_TAG, "Received unexpected stop button event in state " + SimpleMatchActivity2.this.m_savedData.state.toString());
					return;
			}		
	
			/* Transition to stopping capture state. */
			transitionToStoppingCapture();
		}
	};
	
	/*
	 * Handle click on "Open" button. 
	 */	
	private OnClickListener m_openScannerBtnClickListener = new OnClickListener() 
	{
		@Override
		public void onClick(final View v) 
		{
			/* Sanity check.  Make sure we are in a proper state. */
			switch (SimpleMatchActivity2.this.m_savedData.state)
			{
				case SCANNER_ATTACHED:
					break;	
				default:
					Log.e(SIMPLE_MATCH_TAG, "Received unexpected open button event in state " + SimpleMatchActivity2.this.m_savedData.state.toString());
					return;
			}		
	
			/* Transition to initializing state. */
			transitionToInitializing(INITIALIZING_DEVICE_INDEX);
		}
	};
	
	/*
	 * Handle click on "Close" button. 
	 */	
	private OnClickListener m_closeScannerBtnClickListener = new OnClickListener() 
	{
		@Override
		public void onClick(final View v) 
		{
			/* Sanity check.  Make sure we are in a proper state. */
			switch (SimpleMatchActivity2.this.m_savedData.state)
			{
				case INITIALIZED:
					break;	
				default:
					Log.e(SIMPLE_MATCH_TAG, "Received unexpected close button event in state " + SimpleMatchActivity2.this.m_savedData.state.toString());
					return;
			}		
	
			/* Transition to closing state. */
			transitionToClosing();
		}
	};
	
	/*
	 * Handle long clicks on the image view.
	 */
	private OnLongClickListener m_imagePreviewImageLongClickListener = new OnLongClickListener()
	{
		/*
		 * When the image view is long-clicked, show a popup menu.
		 */
		@Override
		public boolean onLongClick(final View v) 
		{
			final PopupMenu popup = new PopupMenu(SimpleMatchActivity2.this, SimpleMatchActivity2.this.m_txtDesciption);
		    popup.setOnMenuItemClickListener(new OnMenuItemClickListener() 
		    {
		    	/*
		    	 * Handle click on a menu item.
		    	 */
				@Override
				public boolean onMenuItemClick(final MenuItem item) 
				{
			        switch (item.getItemId()) 
			        {
			            case R.id.email_image:
			            	promptForEmail(SimpleMatchActivity2.this.m_lastImageExt);
			                return (true);
			            case R.id.enlarge:
			            	showEnlargedImage();
			            	return (true);
			            default:
			            	return (false);
			        }
				}
		    	
		    });
		    
		    final MenuInflater inflater = popup.getMenuInflater();
		    inflater.inflate(R.menu.scanimage_menu, popup.getMenu());
		    popup.show();
		    
			return (true);
		}		
	};

	/*
	 * Handle click on the "Refresh" button
	 */
	private OnClickListener m_refreshBtnClickListener = new OnClickListener() 
	{
		@Override
		public void onClick(final View v) 
		{
			/* Sanity check.  Make sure we are in a proper state. */
			switch (SimpleMatchActivity2.this.m_savedData.state)
			{
				case NO_SCANNER_ATTACHED:
				case SCANNER_ATTACHED:
					break;		
				default:
					Log.e(SIMPLE_MATCH_TAG, "Received unexpected refresh button event in state " + SimpleMatchActivity2.this.m_savedData.state.toString());
					return;
			}
			
			/* Transition to refresh state. */
			transitionToRefresh();		
		}
	};

	/*
	 * Handle click on the spinner that determine the scan type.
	 */
	private OnItemSelectedListener m_captureTypeItemSelectedListener = new OnItemSelectedListener()
	{
		@Override
		public void onItemSelected(final AdapterView<?> parent, final View view, final int pos, 
				final long id)		
		{
			/* Save capture type for screen orientation change. */
			SimpleMatchActivity2.this.m_savedData.captureType = pos;
		}
		
		@Override
		public void onNothingSelected(final AdapterView<?> parent)
		{
			SimpleMatchActivity2.this.m_savedData.captureType = CAPTURE_TYPE_INVALID;
		}
	};
	
	/*
	 * Hide the enlarged dialog, if it exists.
	 */
	private OnClickListener m_enlargedImageClickListener = new OnClickListener() 
	{
		@Override
		public void onClick(final View v) 
		{
			if (SimpleMatchActivity2.this.m_enlargedDialog != null)
			{
				SimpleMatchActivity2.this.m_enlargedDialog.hide();
				SimpleMatchActivity2.this.m_enlargedDialog = null;
			}
		}	
	};
	
	/*
	 * Hide the database dialog, if it exists.
	 */
	private OnClickListener m_databaseDoneClickListener = new OnClickListener() 
	{
		@Override
		public void onClick(final View v) 
		{
			if (SimpleMatchActivity2.this.m_databaseDialog != null)
			{
				SimpleMatchActivity2.this.m_databaseDialog.hide();
				SimpleMatchActivity2.this.m_databaseDialog = null;
			}
		}	
	};
	
	/*
	 * Clear the database.
	 */
	private OnClickListener m_databaseClearClickListener = new OnClickListener() 
	{
		@Override
		public void onClick(final View v) 
		{
			SimpleMatchActivity2.this.m_ibMatcherDatabase.clear();
			
			final Cursor cursor = SimpleMatchActivity2.this.m_ibMatcherDatabase.getCursor();
			if (SimpleMatchActivity2.this.m_databaseDialog != null)
			{
				final ListView            listView = (ListView) SimpleMatchActivity2.this.m_databaseDialog.findViewById(android.R.id.list);
				final SimpleCursorAdapter adapter  = (SimpleCursorAdapter) listView.getAdapter();
				adapter.changeCursor(cursor);
				
		        /*
		         * Update information about database.
		         */
		        final TextView databaseSize = (TextView) SimpleMatchActivity2.this.m_databaseDialog.findViewById(R.id.database_list_size);
		        databaseSize.setText("" + SimpleMatchActivity2.this.m_ibMatcherDatabase.getSize() + " bytes"); 
			}
		}	
	};
	
	/* 
	 * View the database.
	 */
	 private OnClickListener m_viewDatabaseBtnClickListener = new OnClickListener()
	 {
			@Override
			public void onClick(final View v) 
			{
				/* Show the dialog with the database. */
				showDatabase();
			}	
	 };
	
	/* *********************************************************************************************
	 * IBScanListener METHODS
	 ******************************************************************************************** */
	
	@Override
	public void scanDeviceAttached(final int deviceId) 
	{
		showToastOnUiThread("Device " + deviceId + " attached", Toast.LENGTH_SHORT);
		
		/* 
		 * Check whether we have permission to access this device.  Request permission so it will
		 * appear as an IB scanner. 
		 */
		final boolean hasPermission = SimpleMatchActivity2.this.m_ibScan.hasPermission(deviceId);
		if (!hasPermission)
		{ 
			SimpleMatchActivity2.this.m_ibScan.requestPermission(deviceId);
		}
	}

	@Override
	public void scanDeviceDetached(final int deviceId) 
	{
		/*
		 * A device has been detached.  We should also receive a scanDeviceCountChanged() callback,
		 * whereupon we can refresh the display.  If our device has detached while scanning, we 
		 * should receive a deviceCommunicationBreak() callback as well.
		 */
		showToastOnUiThread("Device " + deviceId + " detached", Toast.LENGTH_SHORT);
	}

	@Override
	public void scanDevicePermissionGranted(final int deviceId, final boolean granted) 
	{
		if (granted)
		{
			/*
			 * This device should appear as an IB scanner.  We can wait for the scanDeviceCountChanged()
			 * callback to refresh the display.
			 */
			showToastOnUiThread("Permission granted to device " + deviceId, Toast.LENGTH_SHORT);
		}
		else
		{
			showToastOnUiThread("Permission denied to device " + deviceId, Toast.LENGTH_SHORT);
		}
	}	

	@Override
	public void scanDeviceCountChanged(final int deviceCount)
	{
		final String verb   = (deviceCount == 1) ? "is" : "are";
		final String plural = (deviceCount == 1) ? ""   : "s";
		showToastOnUiThread("There " + verb + " now " + deviceCount + " accessible device" + plural, Toast.LENGTH_SHORT);

		/*
		 * The number of recognized accessible scanners has changed.  If there are not zero scanners
		 * and we were not already in the SCANNER_ATTACHED state, let's go there.
		 */
		transitionToRefresh();
	}
	
	@Override
	public void scanDeviceInitProgress(final int deviceIndex, final int progressValue) 
	{
		setStatus("init " + progressValue + "%");
	}

	@Override
	public void scanDeviceOpenComplete(final int deviceIndex, final IBScanDevice device, 
			final IBScanException exception) 
	{
		if (device != null)
		{
			/*
			 * The device has now finished initializing.  We can start capturing an image.
			 */
			showToastOnUiThread("Device " + deviceIndex + " is now initialized", Toast.LENGTH_SHORT);			
			transitionToInitialized(device);
		}
		else
		{
			/*
			 * Initialization failed.  Let's report the error, clean up, and refresh.
			 */
			String error = (exception == null) ? "(unknown)" : exception.getType().toString();
			showToastOnUiThread("Device " + deviceIndex + " could not be initialized with error " + error, Toast.LENGTH_SHORT);			
			transitionToClosing();
		}
	}
	
	/* *********************************************************************************************
	 * IBScanDeviceListener METHODS
	 ******************************************************************************************** */

	@Override
	public void deviceCommunicationBroken(final IBScanDevice device) 
	{
		/*
		 * A communication break occurred with a scanner during capture.  Let's cleanup after the 
		 * break and then refresh.
		 */
		showToastOnUiThread("Communication break with device", Toast.LENGTH_SHORT);	
		transitionToCommunicationBreak();
	}

	@Override
	public void deviceFingerCountChanged(final IBScanDevice device, final FingerCountState fingerState) 
	{
		/* TODO: UPDATE DESCRIPTION OF FINGER COUNT */
		switch (fingerState)
		{
			default:
			case FINGER_COUNT_OK:
				setStatus("capturing");
				break;
			case TOO_MANY_FINGERS:
				setStatus("too many fingers");
				break;
			case TOO_FEW_FINGERS:
				setStatus("too few fingers");
				break;
			case NON_FINGER:
				setStatus("non-finger");
				break;
		}
	}

	@Override
	public void deviceAcquisitionBegun(final IBScanDevice device, final ImageType imageType) 
	{
		
		if (imageType.equals(ImageType.ROLL_SINGLE_FINGER))
		{
		showToastOnUiThread("Beginning acquisition...roll finger left", Toast.LENGTH_SHORT);
	}

	}

	@Override
	public void deviceAcquisitionCompleted(final IBScanDevice device, final ImageType imageType) 
	{
		if (imageType.equals(ImageType.ROLL_SINGLE_FINGER))
		{
		showToastOnUiThread("Completed acquisition...roll finger right", Toast.LENGTH_SHORT);
		}
		
		
	}

	@Override
	public void deviceFingerQualityChanged(final IBScanDevice device, final FingerQualityState[] fingerQualities) 
	{
		/* Make sure this occurs on the UI thread. */
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				/* Determine colors for each finger in finger qualities array. */
				for (int i = 0; i < fingerQualities.length; i++)
				{
					int color;
					
					switch(fingerQualities[i])
					{
						default:
						case FINGER_NOT_PRESENT:
							color = FINGER_QUALITY_NOT_PRESENT_COLOR;
							break;
						case GOOD:
							color = FINGER_QUALITY_GOOD_COLOR;
							break;
						case FAIR:
							color = FINGER_QUALITY_FAIR_COLOR;
							break;
						case POOR:
							color = FINGER_QUALITY_POOR_COLOR;
							break;
					}
					/* Sanity check.  Make sure marker for this finger exists. */
					if (i < SimpleMatchActivity2.this.m_txtFingerQuality.length)
					{
						SimpleMatchActivity2.this.m_savedData.fingerQualityColors[i] = color;
						SimpleMatchActivity2.this.m_txtFingerQuality[i].setBackgroundColor(color);
					}
				}
				/* If marker exists for more fingers, color then "not present". */
				for (int i = fingerQualities.length; i < SimpleMatchActivity2.this.m_txtFingerQuality.length; i++)
				{
					SimpleMatchActivity2.this.m_savedData.fingerQualityColors[i] = FINGER_QUALITY_NOT_PRESENT_COLOR;
					SimpleMatchActivity2.this.m_txtFingerQuality[i].setBackgroundColor(FINGER_QUALITY_NOT_PRESENT_COLOR);
				}
			}
		});
	}

	@Override
	public void deviceImagePreviewAvailable(final IBScanDevice device, final ImageData image) 
	{
		/*
		 * Preserve aspect ratio of image while resizing.
		 */
		int dstWidth      = this.m_imagePreviewImage.getWidth();
		int dstHeight     = this.m_imagePreviewImage.getHeight();
		int dstHeightTemp = (dstWidth * image.height) / image.width;
		if (dstHeightTemp > dstHeight)
		{
			dstWidth = (dstHeight * image.width) / image.height;
		}
		else
		{
			dstHeight = dstHeightTemp;
		}
		
		/*
		 * Get scaled image, perhaps with rolling lines displayed.
		 */
		final Bitmap bitmapScaled;
		
		if (this.m_imageType.equals(ImageType.ROLL_SINGLE_FINGER))
		{
			RollingData rollingData;
			try
			{
				rollingData = this.m_ibScanDevice.getRollingInfo();
			}
			catch (IBScanException ibse)
			{
				rollingData = null;
				Log.e("Simple Scan", "failure getting rolling line " + ibse.getType().toString());
			}
			if (rollingData != null)
			{
				bitmapScaled = image.toBitmapScaled(dstWidth, dstHeight, rollingData.rollingState, rollingData.rollingLineX);
			} 
			else
			{
				bitmapScaled = image.toBitmapScaled(dstWidth, dstHeight);				
			}
		}
		else
		{
			bitmapScaled = image.toBitmapScaled(dstWidth, dstHeight);
		}
		if (bitmapScaled != null)
		{
			/* Make sure this occurs on UI thread. */
			runOnUiThread(new Runnable() 
			{
				@Override
				public void run()
				{
					SimpleMatchActivity2.this.setFrameTime(String.format("%1$.3f", image.frameTime));
					SimpleMatchActivity2.this.m_savedData.imageBitmap = bitmapScaled;
					SimpleMatchActivity2.this.m_imagePreviewImage.setImageBitmap(bitmapScaled);
				}
			});
		}
	}

	@Override
	public void deviceImageResultAvailable(final IBScanDevice device, final ImageData image, 
			final ImageType imageType, final ImageData[] splitImageArray) 
	{
		/*
		 * Preserve aspect ratio of image while resizing.
		 */
		int dstWidth      = this.m_imagePreviewImage.getWidth();
		int dstHeight     = this.m_imagePreviewImage.getHeight();
		int dstHeightTemp = (dstWidth * image.height) / image.width;
		if (dstHeightTemp > dstHeight)
		{
			dstWidth = (dstHeight * image.width) / image.height;
		}
		else
		{
			dstHeight = dstHeightTemp;
		}
		
		/*
		 * Display image result.
		 */
		final Bitmap bitmapScaled = image.toBitmapScaled(dstWidth, dstHeight);
		if (bitmapScaled != null)
		{
			/* Make sure this occurs on UI thread. */
			runOnUiThread(new Runnable() 
			{
				@Override
				public void run()
				{
					SimpleMatchActivity2.this.setFrameTime(String.format("%1$.3f", image.frameTime));
					SimpleMatchActivity2.this.m_savedData.imageBitmap = bitmapScaled;
					SimpleMatchActivity2.this.m_imagePreviewImage.setImageBitmap(bitmapScaled);
				}
			});
		}

		/*
		 * Finish out the image acquisition and retain result so that the user can view a larger 
		 * version of the image later.
		 */
		showToastOnUiThread("Image result available", Toast.LENGTH_SHORT);
		transitionToImageCaptured(image, imageType, splitImageArray);
	}

	@Override
	public void devicePlatenStateChanged(final IBScanDevice device, final PlatenState platenState) 
	{
		switch (platenState)
		{
			case HAS_FINGERS:
				setActionState("please remove fingers from platen");
				break;
			
			case CLEARD:
			{
				final int[] imageCounts = {1, 1, 3, 6};		
				final int   imageCount  = imageCounts[this.m_actionType.ordinal()];
				setActionState("capturing image " + (this.m_imagesCaptured + 1) + " of " + imageCount);
				break;
			}
		}
	}

	@Override
	public void deviceWarningReceived(final IBScanDevice device, final IBScanException warning) 
	{
		showToastOnUiThread("Warning received " + warning.getType().toString(), Toast.LENGTH_SHORT);
	}

	@Override
	public void devicePressedKeyButtons(IBScanDevice ibScanDevice, int i) {

	}

	@Override
	public void deviceImageResultExtendedAvailable(IBScanDevice arg0,
			IBScanException arg1, ImageData arg2, ImageType arg3, int arg4,
			ImageData[] arg5, SegmentPosition[] arg6) {
		// TODO Auto-generated method stub
		
	}
}
