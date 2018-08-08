/* *************************************************************************************************
 * IBHelpers.java
 *
 * DESCRIPTION:
 *     Helper functions for IBScanMatcher
 *     http://www.integratedbiometrics.com
 *
 * NOTES:
 *     Copyright (c) Integrated Biometrics, 2012-2013
 *     
 * HISTORY:
 *     2013/03/22  First version.
 ************************************************************************************************ */

package com.facerec.tasol.tasolbiometricdemo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.util.Log;

import com.integratedbiometrics.ibscancommon.IBCommon;
import com.integratedbiometrics.ibscancommon.IBCommon.ImageDataExt;
import com.integratedbiometrics.ibscanmatcher.IBMatcher;
import com.integratedbiometrics.ibscanmatcher.IBMatcherException;
import com.integratedbiometrics.ibscanmatcher.IBMatcher.Template;

/**
 * Class with static helper functions.
 */
public class IBHelpers 
{
	private static final String HELPERS_TAG = "IBHelpers";

	/**
	 * Create PNG file from image.
	 * 
	 * @param imageData  image data to write to file
	 * @param file       file to create
	 * @return           <code>true</code> if file was created; <code>false</code> otherwise
	 */
	public static boolean createPng(ImageDataExt imageData, File file)
	{
		boolean created = false;
		
		try 
		{
			final IBMatcher        ibMatcher  = IBMatcher.getInstance();
			final FileOutputStream ostream    = new FileOutputStream(file);
			final Bitmap           lastBitmap = ibMatcher.convertImageToBitmap(imageData);
			lastBitmap.compress(CompressFormat.PNG, 100, ostream);
			ostream.close();
			created = true;
		} 
		catch (IOException ioe) 
		{
			Log.e(HELPERS_TAG, "Could not create image for e-mail");
		}		

		return (created);
	}
	
	/**
	 * Create WSQ file from image.
	 * 
	 * @param imageData  image data to write to file
	 * @param file       file to create
	 * @return           <code>true</code> if file was created; <code>false</code> otherwise
	 */
	public static boolean createWsq(ImageDataExt imageData, File file)
	{
		boolean created = false;
		
		try
		{
			final IBMatcher    ibMatcher           = IBMatcher.getInstance();
			final ImageDataExt compressedImageData = ibMatcher.compressImage(imageData, IBCommon.ImageFormat.WSQ);

			try
			{
				FileOutputStream stream = new FileOutputStream(file);
				stream.write(compressedImageData.imageData);
		        stream.close();
			
				created = true;
			}
			catch (IOException ioe)
			{
				Log.e(HELPERS_TAG, "Could not create image");										
			}
		}
		catch (IBMatcherException ibse)
		{
			Log.e(HELPERS_TAG, "Could not create image " + ibse.getType().toString());					
		}

		return (created);
	}

	/**
	 * Create FIR file from image.
	 * 
	 * @param imageData  image data to write to file
	 * @param file       file to create
	 * @return           <code>true</code> if file was created; <code>false</code> otherwise
	 */
	public static boolean createFir(ImageDataExt imageData, File file)
	{
		boolean created = false;
		
		try
		{
			final IBMatcher ibMatcher = IBMatcher.getInstance();
			ibMatcher.saveImageAsFir(imageData, file.getAbsolutePath());
		
			created = true;
		}
		catch (IBMatcherException ibse)
		{
			Log.e(HELPERS_TAG, "Could not create image " + ibse.getType().toString());					
		}

		return (created);
	}
	
	/**
	 * Create FMR file from image.
	 * 
	 * @param imageData  image data from which to create template to write to file
	 * @param file       file to create
	 * @return           <code>true</code> if file was created; <code>false</code> otherwise
	 */
	public static boolean createFmr(ImageDataExt imageData, File file)
	{
		boolean created = false;
		
		try
		{
			final IBMatcher ibMatcher = IBMatcher.getInstance();
			final Template  template  = ibMatcher.extractTemplate(imageData);
			
			try
			{
				ibMatcher.saveTemplateAsFmr(template, file.getAbsolutePath());

				created = true;
			}
			catch (IBMatcherException ibse)
			{
				Log.e(HELPERS_TAG, "Could not create image " + ibse.getType().toString());											
			}
		}
		catch (IBMatcherException ibse)
		{
			Log.e(HELPERS_TAG, "Could not create template for image " + ibse.getType().toString());					
		}

		return (created);
	}
	
	/**
	 * Create FMR file from template.
	 * 
	 * @param template  template to write to file
	 * @param file      file to create
	 * @return          <code>true</code> if file was created; <code>false</code> otherwise
	 */
	public static boolean createFmr(Template template, File file)
	{
		boolean created = false;
		
		try
		{
			final IBMatcher ibMatcher = IBMatcher.getInstance();
			ibMatcher.saveTemplateAsFmr(template, file.getAbsolutePath());

			created = true;
		}
		catch (IBMatcherException ibse)
		{
			Log.e(HELPERS_TAG, "Could not create template for image " + ibse.getType().toString());					
		}

		return (created);
	}

	/**
	 * Create IBSM image file from image.
	 * 
	 * @param imageData  image data to write to file
	 * @param file       file to create
	 * @return           <code>true</code> if file was created; <code>false</code> otherwise
	 */
	public static boolean createIbsmImage(ImageDataExt imageData, File file)
	{
		boolean created = false;
		
		try
		{
			final IBMatcher ibMatcher = IBMatcher.getInstance();
			ibMatcher.saveImage(imageData, file.getAbsolutePath());
		
			created = true;
		}
		catch (IBMatcherException ibse)
		{
			Log.e(HELPERS_TAG, "Could not create image " + ibse.getType().toString());					
		}

		return (created);
	}

	/**
	 * Create IBSM template file from image.
	 * 
	 * @param imageData  image data from which to create template to write to file
	 * @param file       file to create
	 * @return           <code>true</code> if file was created; <code>false</code> otherwise
	 */
	public static boolean createIbsmTemplate(ImageDataExt imageData, File file)
	{
		boolean created = false;
		
		try
		{
			final IBMatcher ibMatcher = IBMatcher.getInstance();
			final Template  template  = ibMatcher.extractTemplate(imageData);
			
			try
			{
				ibMatcher.saveTemplate(template, file.getAbsolutePath());

				created = true;
			}
			catch (IBMatcherException ibse)
			{
				Log.e(HELPERS_TAG, "Could not create image " + ibse.getType().toString());											
			}
		}
		catch (IBMatcherException ibse)
		{
			Log.e(HELPERS_TAG, "Could not create template for image " + ibse.getType().toString());					
		}

		return (created);
	}

	/**
	 * Create IBSM template file from template.
	 * 
	 * @param template  template to write to file
	 * @param file      file to create
	 * @return          <code>true</code> if file was created; <code>false</code> otherwise
	 */
	public static boolean createIbsmTemplate(Template template, File file)
	{
		boolean created = false;
		
		try
		{
			final IBMatcher ibMatcher = IBMatcher.getInstance();
			ibMatcher.saveTemplate(template, file.getAbsolutePath());

			created = true;
		}
		catch (IBMatcherException ibse)
		{
			Log.e(HELPERS_TAG, "Could not create template for image " + ibse.getType().toString());					
		}

		return (created);
	}
}
