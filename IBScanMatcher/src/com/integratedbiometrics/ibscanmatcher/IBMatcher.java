/* *************************************************************************************************
 * IBMatcher.java
 * 
 * DESCRIPTION:
 *     Android Java wrapper for IBScanMatcher library
 *     http://www.integratedbiometrics.com
 *
 * NOTES:
 *     Copyright (c) Integrated Biometrics, 2013
 *     
 * HISTORY:
 *     2013/03/08  First version.
 ************************************************************************************************ */

package com.integratedbiometrics.ibscanmatcher;

import java.nio.ByteBuffer;

import com.integratedbiometrics.ibscancommon.IBCommon.CaptureDeviceTechId;
import com.integratedbiometrics.ibscancommon.IBCommon.FingerPosition;
import com.integratedbiometrics.ibscancommon.IBCommon.ImageDataExt;
import com.integratedbiometrics.ibscancommon.IBCommon.ImageFormat;
import com.integratedbiometrics.ibscancommon.IBCommon.ImpressionType;

import android.graphics.Bitmap;
import android.util.Log;

/**
 * This class encapsulates matcher functionality.  The single instance of this class may be gotten 
 * with <code>getInstance()</code>.  Methods are provided for loading images and templates from 
 * files, saving templates and images to files, and performing standard operations like generating 
 * templates from images, matching templates, and generating enrollment templates.  The input images
 * for template extraction may be provided by the <code>IBScanDevice</code> <code>getResultImageExt()</code>
 * method.  Supported file formats include ISO/IEC 19794-2 (finger minutiae data), ISO/IEC 19794-4 
 * (finger image data), and IB's own image and template formats.
 */
public class IBMatcher 
{
    /* *********************************************************************************************
     * PUBLIC INNER CLASSES
     ******************************************************************************************** */

    /**
     * Container to hold version information.
     */
    public static final class SdkVersion
    {
        /**
         * Product version string.
         */
        public final String product;

        /**
         * File version string.
         */
        public final String file;

        /* Instantiate version & initialize version information. */
        protected SdkVersion(String product, String file)
        {
            this.product = product;
            this.file = file;
        }

        @Override
        public String toString()
        {
            final String s = "Product: " + this.product + "\n" + 
                             "File: "    + this.file    + "\n";
            return (s);
        }
    }

	/**
	 * Container to hold template data together with meta data.
	 */
	public static final class Template
	{
		public final TemplateVersion     version;
		public final FingerPosition      fingerPosition;
		public final ImpressionType      impressionType;
		public final CaptureDeviceTechId captureDeviceTechId;
		public final short               captureDeviceVendorId;
		public final short               captureDeviceTypeId;
		public final short               imageSamplingX;
		public final short               imageSamplingY;
		public final short               imageSizeX;
		public final short               imageSizeY;
		public final byte[]              minutiae;
		public final int                 reserved;
		
		protected Template(int versionCode, int fingerPositionCode, int impressionTypeCode,
				int captureDeviceTechIdCode, short captureDeviceVendorId, short captureDeviceTypeId,
				short imageSamplingX, short imageSamplingY, short imageSizeX, short imageSizeY,
				byte[] minutiae, int reserved)
		{
			this.version               = TemplateVersion.fromCode(versionCode);
			this.fingerPosition        = FingerPosition.fromCode(fingerPositionCode);
			this.impressionType        = ImpressionType.fromCode(impressionTypeCode);
			this.captureDeviceTechId   = CaptureDeviceTechId.fromCode(captureDeviceTechIdCode);
			this.captureDeviceVendorId = captureDeviceVendorId;
			this.captureDeviceTypeId   = captureDeviceTypeId;
			this.imageSamplingX        = imageSamplingX;
			this.imageSamplingY        = imageSamplingY;
			this.imageSizeX            = imageSizeX;
			this.imageSizeY            = imageSizeY;
			this.minutiae              = minutiae;
			this.reserved              = reserved;			
		}
		
		@Override
		public String toString()
		{
			final String s = "Template version = "         + this.version.toString()                           + "\n"
					       + "Impression type = "          + this.impressionType.toString()                    + "\n"
					       + "Finger position = "          + this.fingerPosition.toString()                    + "\n"
					       + "Capture device tech ID = "   + this.captureDeviceTechId.toString()               + "\n"
					       + "Capture device vendor ID = " + this.captureDeviceVendorId                        + "\n"
					       + "Capture device type ID = "   + this.captureDeviceTypeId                          + "\n"
					       + "Image sampling = "           + this.imageSamplingX + " x " + this.imageSamplingY + "\n"
					       + "Image size = "               + this.imageSizeX     + " x " + this.imageSizeY     + "\n";
			return (s);
		}
	}

	/**
     * Template version.
     */
    public static enum TemplateVersion
    {
    	IBISDK_0(0),
    	IBISDK_1(1),
    	IBISDK_2(2),
    	IBISDK_3(3),
    	NEW_0(16);

        /* Native value for enumeration. */
        private final int code;

        TemplateVersion(int code)
        {
            this.code = code;
        }

        /* Find Java object from native value. */
        protected static TemplateVersion fromCode(int code)
        {
            for (TemplateVersion t : TemplateVersion.values())
            {
                if (t.code == code)
                {
                    return (t);
                }
            }
            return (null);
        }

        /* Get native value for Java object. */
        protected int toCode()
        {
            return (this.code);
        }
    }

    /* *********************************************************************************************
     * (OBJECT) PUBLIC INTERFACE
     ******************************************************************************************** */
	
    /**
     * Obtains product and software version information.
     * 
     * @return SDK version
     * @throws IBMatcherException
     */
    public SdkVersion getSdkVersion() throws IBMatcherException
    {
        final NativeError error   = new NativeError();
        final SdkVersion  version;
		synchronized(this) /* only one thread can access the matcher at a time. */
		{
			version = getSdkVersionNative(error);
		}
        handleError(error); /* throws exception if necessary */

        return (version);
    }
    
	/**
	 * Extract template from image data.
	 * 
	 * @param imageDataExt  image data from which template will be extracted
	 * @return              extracted template, if no error; <code>null</code> otherwise
	 * @throws              IBMatcherException
	 */
	public Template extractTemplate(ImageDataExt imageDataExt) throws IBMatcherException
	{
		/* Check for invalid argument. */
		if (imageDataExt == null)
		{
        	logPrintWarning(getMethodName() + ": received null imageDataExt");
    		throw (new IllegalArgumentException("Received null imageDataExt"));
		}
		
        final NativeError error = new NativeError();
        final Template    template;
		synchronized(this) /* only one thread can access the matcher at a time. */
		{
			template = extractTemplateNative(imageDataExt, error);
		}
        handleError(error); /* throws exception if necessary */

        /* Check for library or JNI interface error. */
        if ((template == null) || (template.captureDeviceTechId == null) || 
        		(template.fingerPosition == null) || (template.version == null) || 
        		(template.impressionType == null) || (template.minutiae == null))
        {
            logPrintError(getMethodName() + ": null or invalid template returned from native code");

            error.code = IBMatcherException.Type.COMMAND_FAILED.toCode();
            handleError(error);
        }

        return (template);
	}
	
	/**
	 * Compress image data.
	 * 
	 * @param imageDataExt image data to compress
	 * @param imageFormat  format of compressed image data
	 * @return             compressed image data
	 * @throws             IBMatcherException
	 */
	public ImageDataExt compressImage(ImageDataExt imageDataExt, ImageFormat imageFormat) throws IBMatcherException
	{
		/* Check for invalid argument. */
		if (imageDataExt == null)
		{
        	logPrintWarning(getMethodName() + ": received null imageDataExt");
    		throw (new IllegalArgumentException("Received null imageDataExt"));
		}
		if (imageFormat == null)
		{
        	logPrintWarning(getMethodName() + ": received null imageFormat");
    		throw (new IllegalArgumentException("Received null imageFormat"));
		}		
		
        final NativeError  error = new NativeError();
        final ImageDataExt imageDataExtOut;
		synchronized(this) /* only one thread can access the matcher at a time. */
		{
			imageDataExtOut = compressImageNative(imageDataExt, imageFormat.toCode(), error);
		}
        handleError(error); /* throws exception if necessary */
        
        return (imageDataExtOut);
	}
	
	/**
	 * Decompress image data.
	 * 
	 * @param imageDataExt  image data to decompress
	 * @return              decompressed image data
	 * @throws              IBMatcherException
	 */
	public ImageDataExt decompressImage(ImageDataExt imageDataExt) throws IBMatcherException
	{
		/* Check for invalid argument. */
		if (imageDataExt == null)
		{
        	logPrintWarning(getMethodName() + ": received null imageDataExt");
    		throw (new IllegalArgumentException("Received null imageDataExt"));		
		}
		
		final NativeError  error = new NativeError();
		final ImageDataExt imageDataExtOut;
		synchronized(this) /* only one thread can access the matcher at a time. */
		{
			 imageDataExtOut = decompressImageNative(imageDataExt, error);
		}
		handleError(error); /* throws exception if necessary */
	
		return (imageDataExtOut);
	}
	
    /**
     * Convert image data to Bitmap.
     * 
     * @param imageDataExt  image data to convert
     * @return              bitmap if successful; otherwise <code>null</code>
     */
    public Bitmap convertImageToBitmap(ImageDataExt imageDataExt)
    {
    	if (imageDataExt == null)
    	{
        	logPrintWarning(getMethodName() + ": received null imageDataExt");
    		throw (new IllegalArgumentException("Received null imageDataExt"));		
    	}
    
    	final Bitmap bitmap = Bitmap.createBitmap(imageDataExt.imageSizeX, imageDataExt.imageSizeY, Bitmap.Config.ARGB_8888);
    	
		if (bitmap != null)
		{
        	final byte[] imageBuffer = new byte[imageDataExt.imageSizeX * imageDataExt.imageSizeY * 4];
        	/* 
        	 * The image in the buffer is flipped vertically from what the Bitmap class expects; 
        	 * we will flip it to compensate while moving it into the buffer. 
        	 */
    		for (int y = 0; y < imageDataExt.imageSizeY; y++) 
    		{
    			for (int x = 0; x < imageDataExt.imageSizeX; x++) 
    			{
    				imageBuffer[((y * imageDataExt.imageSizeX) + x) * 4] = 
    						imageBuffer[((y * imageDataExt.imageSizeX) + x) * 4 + 1] = 
    								imageBuffer[((y * imageDataExt.imageSizeX) + x) * 4 + 2] = 
    										imageDataExt.imageData[(imageDataExt.imageSizeY - y - 1) * imageDataExt.imageSizeX + x];
    				imageBuffer[((y * imageDataExt.imageSizeX) + x) * 4 + 3] = (byte)255;
    			}
    		}        	
    		bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(imageBuffer));
		}
		
		return (bitmap);
    }
    
	/**
	 * Save image to file in IBSM image format.
	 * 
	 * @param imageDataExt  image to save
	 * @param filePath      path of IBSM image file
	 * @return              <code>true</code> if image was saved; <code>false</code> otherwise
	 * @throws              IBMatcherException
	 */
    public boolean saveImage(ImageDataExt imageDataExt, String filePath) throws IBMatcherException
    {
		/* Check for invalid argument. */
		if (imageDataExt == null)
		{
        	logPrintWarning(getMethodName() + ": received null imageDataExt");
    		throw (new IllegalArgumentException("Received null imageDataExt"));
		}
		if (filePath == null)
		{
        	logPrintWarning(getMethodName() + ": received null filePath");
    		throw (new IllegalArgumentException("Received null filePath"));
		}
				
        final NativeError error = new NativeError();
        final boolean     saved;
		synchronized(this) /* only one thread can access the matcher at a time. */
		{
			saved = saveImageNative(imageDataExt, filePath, error);
		}
        handleError(error); /* throws exception if necessary */

        return (saved);
    }

	/**
	 * Load image from file in IBSM image format.
	 * 
	 * @param filePath  path of IBSM image file
	 * @return          loaded image, if successful; <code>null</code> otherwise
	 * @throws          IBMatcherException
	 */
    public ImageDataExt loadImage(String filePath) throws IBMatcherException
    {
		/* Check for invalid argument. */
		if (filePath == null)
		{
        	logPrintWarning(getMethodName() + ": received null filePath");
    		throw (new IllegalArgumentException("Received null filePath"));
		}
		
		final NativeError  error = new NativeError();
		final ImageDataExt imageDataExt;
		synchronized(this) /* only one thread can access the matcher at a time. */
		{
			imageDataExt = loadImageNative(filePath, error);
		}
		handleError(error); /* throws exception if necessary */
		
		return (imageDataExt);
    } 	
	
	/**
	 * Save image to file as ISO/IEC 19794-4 Fingerprint Image Record (FIR).
	 * 
	 * @param imageDataExt  image to save
	 * @param filePath      path of image FIR file
	 * @return              <code>true</code> if image was saved; <code>false</code> otherwise
	 * @throws              IBMatcherException
	 */
	public boolean saveImageAsFir(ImageDataExt imageDataExt, String filePath) throws IBMatcherException
	{
		/* Check for invalid argument. */
		if (imageDataExt == null)
		{
        	logPrintWarning(getMethodName() + ": received null imageDataExt");
    		throw (new IllegalArgumentException("Received null imageDataExt"));
		}
		if (filePath == null)
		{
        	logPrintWarning(getMethodName() + ": received null filePath");
    		throw (new IllegalArgumentException("Received null filePath"));
		}
				
        final NativeError error = new NativeError();
        final boolean     saved;
		synchronized(this) /* only one thread can access the matcher at a time. */
		{
			saved = saveImageAsFirNative(imageDataExt, filePath, error);
		}
        handleError(error); /* throws exception if necessary */

        return (saved);
	}
	
	/**
	 * Load image from file with ISO/IEC 19794-4 Fingerprint Image Record (FIR).
	 * 
	 * @param filePath  path of image FIR file
	 * @return          loaded image, if successful; <code>null</code> otherwise
	 * @throws          IBMatcherException
	 */
	public ImageDataExt loadImageFromFir(String filePath) throws IBMatcherException
	{
		/* Check for invalid argument. */
		if (filePath == null)
		{
        	logPrintWarning(getMethodName() + ": received null filePath");
    		throw (new IllegalArgumentException("Received null filePath"));
		}
		
		final NativeError  error = new NativeError();
		final ImageDataExt imageDataExt;
		synchronized(this) /* only one thread can access the matcher at a time. */
		{
			imageDataExt = loadImageFromFirNative(filePath, error);
		}
		handleError(error); /* throws exception if necessary */
		
		return (imageDataExt);
	}
	
	/**
	 * Save template to file in IBSM template format.
	 * 
	 * @param template  template to save
	 * @param filePath  path of IBSM template file
	 * @return          <code>true</code> if template was saved; <code>false</code> otherwise
	 * @throws          IBMatcherException
	 */
	public boolean saveTemplate(Template template, String filePath) throws IBMatcherException
	{
		/* Check for invalid argument. */
		if (template == null)
		{
        	logPrintWarning(getMethodName() + ": received null template");
    		throw (new IllegalArgumentException("Received null template"));
		}
		if (filePath == null)
		{
        	logPrintWarning(getMethodName() + ": received null filePath");
    		throw (new IllegalArgumentException("Received null filePath"));
		}
				
        final NativeError error = new NativeError();
        final boolean     saved;
		synchronized(this) /* only one thread can access the matcher at a time. */
		{
			saved = saveTemplateNative(template, filePath, error);
		}
        handleError(error); /* throws exception if necessary */

        return (saved);
	}
	
	/**
	 * Load template from file in IBSM template format.
	 * 
	 * @param filePath  path of IBSM template file
	 * @return          loaded template, if successful; <code>null</code> otherwise
	 * @throws          IBMatcherException
	 */
	public Template loadTemplate(String filePath) throws IBMatcherException
	{
		/* Check for invalid argument. */
		if (filePath == null)
		{
        	logPrintWarning(getMethodName() + ": received null filePath");
    		throw (new IllegalArgumentException("Received null filePath"));
		}
		
		final NativeError error = new NativeError();
		final Template    template;
		synchronized(this) /* only one thread can access the matcher at a time. */
		{
			template = loadTemplateNative(filePath, error);
		}
		handleError(error); /* throws exception if necessary */
		
		return (template);
	}
	
	/**
	 * Save template to file as ISO/IEC 19794-2 Fingerprint Minutiae Record (FMR).
	 * 
	 * @param template  template to save
	 * @param filePath  path of template FMR file
	 * @return          <code>true</code> if template was saved; <code>false</code> otherwise
	 * @throws          IBMatcherException
	 */
	public boolean saveTemplateAsFmr(Template template, String filePath) throws IBMatcherException
	{
		/* Check for invalid argument. */
		if (template == null)
		{
        	logPrintWarning(getMethodName() + ": received null template");
    		throw (new IllegalArgumentException("Received null template"));
		}
		if (filePath == null)
		{
        	logPrintWarning(getMethodName() + ": received null filePath");
    		throw (new IllegalArgumentException("Received null filePath"));
		}
				
        final NativeError error = new NativeError();
        final boolean     saved;
		synchronized(this) /* only one thread can access the matcher at a time. */
		{
			saved = saveTemplateAsFmrNative(template, filePath, error);
		}
        handleError(error); /* throws exception if necessary */

        return (saved);
	}
	
	/**
	 * Load template from file with ISO/IEC 19794-2 Fingerprint Minutiae Record (FMR).
	 * 
	 * @param filePath  path of template FMR file
	 * @return          loaded template, if successful; <code>null</code> otherwise
	 * @throws          IBMatcherException
	 */
	public Template loadTemplateFromFmr(String filePath) throws IBMatcherException
	{
		/* Check for invalid argument. */
		if (filePath == null)
		{
        	logPrintWarning(getMethodName() + ": received null filePath");
    		throw (new IllegalArgumentException("Received null filePath"));
		}
		
		final NativeError error = new NativeError();
		final Template    template;
		synchronized(this) /* only one thread can access the matcher at a time. */
		{
			template = loadTemplateFromFmrNative(filePath, error);
		}
		handleError(error); /* throws exception if necessary */
		
		return (template);
	}
	
	/**
	 * Determine whether two templates match.  The templates must match to the level set with 
	 * setMatchingLevel().
	 * 
	 * @param template1  first template
	 * @param template2  second template
	 * @return           matching score.  If match is better than the configured matching level, a 
	 *                   non-zero matching score will be returned.  Otherwise, a positive matching 
	 *                   score will be returned.
	 * @throws           IBMatcherException
	 */
	public int matchTemplates(Template template1, Template template2) throws IBMatcherException
	{
		/* Check for invalid argument. */
		if (template1 == null)
		{
			logPrintWarning(getMethodName() + ": received null first template");
			throw (new IllegalArgumentException("Received null first template"));
		}
		if (template2 == null)
		{
			logPrintWarning(getMethodName() + ": received null second template");
			throw (new IllegalArgumentException("Received null second template"));
		}
		
		final NativeError error = new NativeError();
		final int         matchingScore;
		synchronized(this) /* only one thread can access the matcher at a time. */
		{
			matchingScore = matchTemplatesNative(template1, template2, error);
		}
		handleError(error); /* throws exception if necessary */
		
		return (matchingScore);
	}
	
	/**
	 * Set matching level.  This level is used when matching templates (with <code>matchTemplates()</code>)
	 * or generating enrollment templates (with <code>singleEnrollment()</code> or <code>multiEnrollment()</code>).
	 * 
	 * @param matchingLevel  matching level required, between 1 (loosest) and 7 (strictest),
	 *                       inclusive
	 * @throws               IBMatcherException
	 */
	public void setMatchingLevel(int matchingLevel) throws IBMatcherException
	{
		final NativeError error = new NativeError();
		synchronized(this) /* only one thread can access the matcher at a time. */
		{
			setMatchingLevelNative(matchingLevel, error);
		}
		handleError(error); /* throws exception if necessary */
	}
	
	/**
	 * Get matching level.  This level is used when matching templates (with <code>matchTemplates()</code>)
	 * or generating enrollment templates (with <code>singleEnrollment()</code> or <code>multiEnrollment()</code>).
	 * 
	 * @return matching level required, between 1 (loosest) and 7 (strictest), inclusive
	 * @throws IBMatcherException
	 */
	public int getMatchingLevel() throws IBMatcherException
	{
		final NativeError error = new NativeError();
		final int         matchingLevel;
		synchronized(this) /* only one thread can access the matcher at a time. */
		{
			matchingLevel = getMatchingLevelNative(error);
		}
		handleError(error); /* throws exception if necessary */
		
		return (matchingLevel);
	}	
	
	/**
	 * Generate one enrollment template from three image data.  Templates extracted from each pair 
	 * of images must match to the level set with <code>setMatchingLevel()</code>.  The template 
	 * extracted from the most representative image will be returned.
	 * 
	 * @param imageDataExt1  first image data
	 * @param imageDataExt2  second image data
	 * @param imageDataExt3  third image data
	 * @return               template
	 * @throws               IBMatcherException
	 */
	public Template singleEnrollment(ImageDataExt imageDataExt1, ImageDataExt imageDataExt2,
			ImageDataExt imageDataExt3) throws IBMatcherException
	{
		/* Check for invalid argument. */
		if (imageDataExt1 == null)
		{
			logPrintWarning(getMethodName() + ": received null first image");
			throw (new IllegalArgumentException("Received null first image"));
		}
		if (imageDataExt2 == null)
		{
			logPrintWarning(getMethodName() + ": received null second image");
			throw (new IllegalArgumentException("Received null second image"));
		}
		if (imageDataExt3 == null)
		{
			logPrintWarning(getMethodName() + ": received null third image");
			throw (new IllegalArgumentException("Received null third image"));
		}

		final NativeError error = new NativeError();
		final Template    template;
		synchronized(this) /* only one thread can access the matcher at a time. */
		{
			 template = singleEnrollmentNative(imageDataExt1, imageDataExt2, imageDataExt3, 
					 error);
		}
		handleError(error); /* throws exception if necessary */
		
		return (template);
	}
	
	/**
	 * Generate two enrollment templates from six image data.  Templates extracted from sufficient 
	 * pairs of images will be expected to match to the level set with <code>setMatchingLevel()</code>.  
	 * The templates extracted from the most representative images will be returned.
	 * 
	 * @param imageDataExt1  first image data
	 * @param imageDataExt2  second image data
	 * @param imageDataExt3  third image data
	 * @param imageDataExt4  fourth image data
	 * @param imageDataExt5  fifth image data
	 * @param imageDataExt6  sixth image data
	 * @return               array of two templates
	 * @throws               IBMatcherException
	 */
	public Template[] multiEnrollment(ImageDataExt imageDataExt1, ImageDataExt imageDataExt2,
			ImageDataExt imageDataExt3, ImageDataExt imageDataExt4, ImageDataExt imageDataExt5,
			ImageDataExt imageDataExt6) throws IBMatcherException
	{
		/* Check for invalid argument. */
		if (imageDataExt1 == null)
		{
			logPrintWarning(getMethodName() + ": received null first image");
			throw (new IllegalArgumentException("Received null first image"));
		}
		if (imageDataExt2 == null)
		{
			logPrintWarning(getMethodName() + ": received null second image");
			throw (new IllegalArgumentException("Received null second image"));
		}
		if (imageDataExt3 == null)
		{
			logPrintWarning(getMethodName() + ": received null third image");
			throw (new IllegalArgumentException("Received null third image"));
		}
		if (imageDataExt4 == null)
		{
			logPrintWarning(getMethodName() + ": received null fourth image");
			throw (new IllegalArgumentException("Received null fourth image"));
		}
		if (imageDataExt5 == null)
		{
			logPrintWarning(getMethodName() + ": received null fifth image");
			throw (new IllegalArgumentException("Received null fifth image"));
		}
		if (imageDataExt6 == null)
		{
			logPrintWarning(getMethodName() + ": received null sixth image");
			throw (new IllegalArgumentException("Received null sixth image"));
		}

		final NativeError error = new NativeError();
		final Template[]  templates;
		synchronized(this) /* only one thread can access the matcher at a time. */
		{
			templates = multiEnrollmentNative(imageDataExt1, imageDataExt2, imageDataExt3, 
				imageDataExt4, imageDataExt5, imageDataExt6, error);
		}
		handleError(error); /* throws exception if necessary */
		
		return (templates);
	}
		
    /* *********************************************************************************************
     * (CLASS) PUBLIC INTERFACE
     ******************************************************************************************** */
	
	/**
	 * Capture device type IDs.
	 */
	public static final short CAPTURE_DEVICE_TYPE_ID_UNKNOWN     = 0x0000;
	public static final short CAPTURE_DEVICE_TYPE_ID_CURVE       = 0x1001;
	public static final short CAPTURE_DEVICE_TYPE_ID_WATSON      = 0x1005;
	public static final short CAPTURE_DEVICE_TYPE_ID_SHERLOCK    = 0x0010;
	public static final short CAPTURE_DEVICE_TYPE_ID_WATSON_MINI = 0x0020;
	public static final short CAPTURE_DEVICE_TYPE_ID_COLUMBO     = 0x0030;
	public static final short CAPTURE_DEVICE_TYPE_ID_HOLMES      = 0x0040;
	
	/**
	 * Scale unit types.
	 */
	public static final byte SCALE_UNIT_INCH       = 0x01;
	public static final byte SCALE_UNIT_CENTIMETER = 0x02;

	/**
	 * Capture device vendor IDs.
	 */
	public static final short CAPTURE_DEVICE_VENDOR_ID_UNREPORTED         = 0x0000;
	public static final short CAPTURE_DEVICE_VENDOR_INTEGRATED_BIOMETRICS = (short)0xABCD;
	
    /**
     * Get single instance of class.
     * 
     * @return  single instance of <code>IBMatcher</code>
     */
    public static synchronized IBMatcher getInstance()
    {
        if (IBMatcher.m_instance == null)
        {
        	IBMatcher.m_instance = new IBMatcher();
        }

        return (IBMatcher.m_instance);
    }

    /* *********************************************************************************************
     * PROTECTED INNER CLASSES
     ******************************************************************************************** */

    /*
     *  Container for native error value.
     */
    protected static final class NativeError
    {
        public int code = 0;
    }

    /* *********************************************************************************************
     * PRIVATE INTERFACE
     ******************************************************************************************** */

    /*
     *  Protected default constructor to prevent external instantiation.
     */
    private IBMatcher()
    {
    	this.m_handleNative = initNative();
    }
    
    /*
     *  Singleton matcher object.
     */
    private static IBMatcher m_instance = null;

    /*
     *  The handle for this matcher.  Accessed from native code.
     */
    private final int m_handleNative;

    /*
     *  Handle error from native method.
     */
    private static void handleError(NativeError error) throws IBMatcherException
    {
        if (error.code != 0)
        {
            IBMatcherException.Type type;

            type = IBMatcherException.Type.fromCode(error.code);
            if (type == null)
            {
            	logPrintError(getMethodName() + ": unrecognized error code (" + error.code + ") returned from native code");
            	type = IBMatcherException.Type.COMMAND_FAILED;
            }
            throw (new IBMatcherException(type));
        }
    }
    
    /*
     *  Log warning to System.out.
     */
    private static void logPrintWarning(String ln)
    {
        Log.w("IBMatcher", ln);
    }
    
    /*
     *  Log error to System.out.
     */
    private static void logPrintError(String ln)
    {
        Log.e("IBMatcher", ln);
    }
           
    /*
     * The stack index at which a caller method's name will be found.
     */
    private static int METHOD_STACK_INDEX;

    /*
     *  Get name of method caller.
     */
    private static String getMethodName() 
    {
    	StackTraceElement[] stackTrace;
    	String              name;
    	
    	stackTrace = Thread.currentThread().getStackTrace();
    	/* Sanity check the index, though it should always be within bounds. */
    	if (stackTrace.length > METHOD_STACK_INDEX)
    	{
    		name = stackTrace[METHOD_STACK_INDEX].getMethodName();
    	}
    	else
    	{
    		name = "?";
    	}
        return (name);
    }

    /* *********************************************************************************************
     * NATIVE METHODS
     ******************************************************************************************** */

    /* Native method for constructor. */
    private native int initNative();
    
    /* Native method for getSDKVersion(). */
    private native SdkVersion getSdkVersionNative(NativeError error);

    /* Native method for extractTemplate(). */
    private native Template extractTemplateNative(ImageDataExt imageDataExt, NativeError error);

    /* Native method for compressImage(). */
    private native ImageDataExt compressImageNative(ImageDataExt imageDataExt, int imageFormatCode, 
    		NativeError error);
    
    /* Native method for decompressImage(). */
    private native ImageDataExt decompressImageNative(ImageDataExt imageDataExt, NativeError error);
    
    /* Native method for saveImage(). */
    private native boolean saveImageNative(ImageDataExt imageDataExt, String filePath, 
    		NativeError error);
    
    /* Native method for loadImage(). */
    private native ImageDataExt loadImageNative(String filePath, NativeError error);
    
    /* Native method for saveImageAsFir(). */
    private native boolean saveImageAsFirNative(ImageDataExt imageDataExt, String filePath, 
    		NativeError error);
    
    /* Native method for loadImageFromFir(). */
    private native ImageDataExt loadImageFromFirNative(String filePath, NativeError error);
    
    /* Native method for saveTemplate(). */
    private native boolean saveTemplateNative(Template template, String filePath, 
    		NativeError error);
    
    /* Native method for loadTemplate(). */
    private native Template loadTemplateNative(String filePath, NativeError error);
    
    /* Native method for saveTemplateAsFmr(). */
    private native boolean saveTemplateAsFmrNative(Template template, String filePath, 
    		NativeError error);
    
    /* Native method for loadTemplateFromFmr(). */
    private native Template loadTemplateFromFmrNative(String filePath, NativeError error);
    
	/* Native method for matchTemplates(). */
	private native int matchTemplatesNative(Template template1, Template template2, NativeError error);
	
	/* Native method for setMatchingLevel(). */
	private native void setMatchingLevelNative(int matchingLevel, NativeError error);
	
	/* Native method for getMatchingLevel(). */
	private native int getMatchingLevelNative(NativeError error);
	
	/* Native method for singleEnrollment(). */
	private native Template singleEnrollmentNative(ImageDataExt imageDataExt1, 
			ImageDataExt imageDataExt2, ImageDataExt imageDataExt3, NativeError error);
			
	/* Native method for multiEnrollment(). */
	private native Template[] multiEnrollmentNative(ImageDataExt imageDataExt1, 
			ImageDataExt imageDataExt2, ImageDataExt imageDataExt3, ImageDataExt imageDataExt4, 
			ImageDataExt imageDataExt5, ImageDataExt imageDataExt6, NativeError error);
	
    /* *********************************************************************************************
     * STATIC BLOCKS
     ******************************************************************************************** */
    
    /*
     *  Helper block to get method name for debug messages. 
     */
    static 
    {
        int i = 0;
        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) 
        {
            i++;
            if (ste.getClassName().equals(IBMatcher.class.getName())) 
            {
                break;
            }
        }
        METHOD_STACK_INDEX = i;
    }
    
    /* 
     * Load native library.
     */
    static
    {
        System.loadLibrary("ibscanmatcher");
        System.loadLibrary("ibscanmatcherjni");
    }
}
