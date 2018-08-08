/* *************************************************************************************************
 * IBMatcher.cpp
 *
 * DESCRIPTION:
 *     Android JNI wrapper for IBScanMatcher library
 *     http://www.integratedbiometrics.com
 *
 * NOTES:
 *     Copyright (c) Integrated Biometrics, 2013
 *
 * HISTORY:
 *     2013/03/01  First version.
 ************************************************************************************************ */

#include <jni.h>
#include <stdlib.h>
#include <stdio.h>

#include "IBMatcher.h"

#ifndef _WINDOWS
#include "LinuxPort.h"
#else
#include <Windows.h>
#endif

#include "IBScanUltimate.h"
#include "IBScanUltimateApi_defs.h"
#include "IBScanUltimateApi_err.h"
#include "IBScanUltimateApi.h"
#include "IBScanMatcher.h"
#include "IBScanMatcherApi_defs.h"
#include "IBScanMatcherApi_err.h"
#include "IBScanMatcherApi.h"

#ifdef __android__
#include <android/log.h>
#endif

/***************************************************************************************************
 * LOCAL MACROS
 **************************************************************************************************/

#define DEBUG       0
#define MODULE_NAME "ibmatcher"

#if DEBUG
	#ifdef __android__
		#define ANDROID_LOG(...) __android_log_print(ANDROID_LOG_INFO, MODULE_NAME, __VA_ARGS__)
		#define LOG(s) { ANDROID_LOG s; }
	#else
		#define LOG(s) { printf(MODULE_NAME ": "); printf s; fflush(stdout); }
	#endif
#else
	#define LOG(s)
#endif

#define USE_PARAM(p) (void)p

#define IBMATCHER_JAVA_CLASS_PATH    "com/integratedbiometrics/ibscanmatcher/IBMatcher"
#define IBCOMMON_JAVA_CLASS_PATH     "com/integratedbiometrics/ibscancommon/IBCommon"
#define IBSCANDEVICE_JAVA_CLASS_PATH "com/integratedbiometrics/ibscanultimate/IBScanDevice"
#define IBSCAN_JAVA_CLASS_PATH       "com/integratedbiometrics/ibscanultimate/IBScan"
#define STRING_CLASS_PATH            "java/lang/String"

/***************************************************************************************************
 * LOCAL VARIABLES
 **************************************************************************************************/

static jclass    Class_IBMatcher                                     = NULL;
static jfieldID  FieldID_IBMatcher_handleNative                      = NULL;

static jclass    Class_IBMatcher_NativeError                         = NULL;
static jfieldID  FieldID_IBMatcher_NativeError_code                  = NULL;

static jclass    Class_IBMatcher_SdkVersion                          = NULL;
static jmethodID MethodID_IBMatcher_SdkVersion_SdkVersion            = NULL;

static jclass    Class_IBCommon_ImageDataExt                         = NULL;
static jmethodID MethodID_IBCommon_ImageDataExt_ImageDataExt         = NULL;
static jfieldID  FieldID_IBCommon_ImageDataExt_imageFormat           = NULL;
static jfieldID  FieldID_IBCommon_ImageDataExt_impressionType        = NULL;
static jfieldID  FieldID_IBCommon_ImageDataExt_fingerPosition        = NULL;
static jfieldID  FieldID_IBCommon_ImageDataExt_captureDeviceTechId   = NULL;
static jfieldID  FieldID_IBCommon_ImageDataExt_captureDeviceVendorId = NULL;
static jfieldID  FieldID_IBCommon_ImageDataExt_captureDeviceTypeId   = NULL;
static jfieldID  FieldID_IBCommon_ImageDataExt_scanSamplingX         = NULL;
static jfieldID  FieldID_IBCommon_ImageDataExt_scanSamplingY         = NULL;
static jfieldID  FieldID_IBCommon_ImageDataExt_imageSamplingX        = NULL;
static jfieldID  FieldID_IBCommon_ImageDataExt_imageSamplingY        = NULL;
static jfieldID  FieldID_IBCommon_ImageDataExt_imageSizeX            = NULL;
static jfieldID  FieldID_IBCommon_ImageDataExt_imageSizeY            = NULL;
static jfieldID  FieldID_IBCommon_ImageDataExt_scaleUnit             = NULL;
static jfieldID  FieldID_IBCommon_ImageDataExt_bitDepth              = NULL;
static jfieldID  FieldID_IBCommon_ImageDataExt_imageData             = NULL;

static jclass    Class_IBMatcher_Template                            = NULL;
static jmethodID MethodID_IBMatcher_Template_Template                = NULL;
static jfieldID  FieldID_IBMatcher_Template_version                  = NULL;
static jfieldID  FieldID_IBMatcher_Template_fingerPosition           = NULL;
static jfieldID  FieldID_IBMatcher_Template_impressionType           = NULL;
static jfieldID  FieldID_IBMatcher_Template_captureDeviceTechId      = NULL;
static jfieldID  FieldID_IBMatcher_Template_captureDeviceVendorId    = NULL;
static jfieldID  FieldID_IBMatcher_Template_captureDeviceTypeId      = NULL;
static jfieldID  FieldID_IBMatcher_Template_imageSamplingX           = NULL;
static jfieldID  FieldID_IBMatcher_Template_imageSamplingY           = NULL;
static jfieldID  FieldID_IBMatcher_Template_imageSizeX               = NULL;
static jfieldID  FieldID_IBMatcher_Template_imageSizeY               = NULL;
static jfieldID  FieldID_IBMatcher_Template_minutiae                 = NULL;
static jfieldID  FieldID_IBMatcher_Template_reserved                 = NULL;

static jclass    Class_IBCommon_ImageFormat                          = NULL;
static jmethodID MethodID_IBCommon_ImageFormat_toCode                = NULL;

static jclass    Class_IBCommon_ImpressionType                       = NULL;
static jmethodID MethodID_IBCommon_ImpressionType_toCode             = NULL;

static jclass    Class_IBCommon_FingerPosition                       = NULL;
static jmethodID MethodID_IBCommon_FingerPosition_toCode             = NULL;

static jclass    Class_IBCommon_CaptureDeviceTechId                  = NULL;
static jmethodID MethodID_IBCommon_CaptureDeviceTechId_toCode        = NULL;

static jclass    Class_IBMatcher_TemplateVersion                     = NULL;
static jmethodID MethodID_IBMatcher_TemplateVersion_toCode           = NULL;

/***************************************************************************************************
 * LOCAL FUNCTION PROTOTYPES
 **************************************************************************************************/

static jclass    findClass            (JNIEnv *pEnv, const char *name, BOOL *pOk);
static jfieldID  getFieldID           (JNIEnv *pEnv, jclass cclass, const char *name, const char *sig, BOOL *pOk);
static jmethodID getMethodID          (JNIEnv *pEnv, jclass cclass, const char *name, const char *sig, BOOL *pOk);
static void      setNativeError       (JNIEnv *pEnv, jobject obj, int code);
static int       getHandle            (JNIEnv *pEnv, jobject obj);
static jobject   convertImageDataExt  (JNIEnv *pEnv, const IBSM_ImageData *pImage);
static BOOL      unconvertImageDataExt(JNIEnv *pEnv, jobject imageDataExt, IBSM_ImageData *pImageExt);
static jobject   convertTemplate      (JNIEnv *pEnv, const IBSM_Template *pTemplate);
static BOOL      unconvertTemplate    (JNIEnv *pEnv, jobject ttemplate, IBSM_Template *pTemplate);

/***************************************************************************************************
 * GLOBAL FUNCTIONS
 **************************************************************************************************/

/*
 * Handle load of library.
 */
jboolean IBMatcher_OnLoad
    (JavaVM *vm,
     void   *reserved)
{
    JNIEnv *pEnv;
    jint    res;
    BOOL    ok = TRUE;

    USE_PARAM(reserved);

    LOG(("%s\n", __FUNCTION__));

    /* Cached classes, field IDs, and method IDs. */
    res = vm->GetEnv((void **)&pEnv, JNI_VERSION_1_2);
    if (res < 0)
    {
        LOG(("unable to get JNIEnv reference\n"));
        ok = FALSE;
    }
    else
    {
        /* Cache classes, field IDs, and method IDs. */
        Class_IBMatcher = findClass(pEnv, IBMATCHER_JAVA_CLASS_PATH, &ok);
        if (Class_IBMatcher != NULL)
        {
        	FieldID_IBMatcher_handleNative = getFieldID (pEnv, Class_IBMatcher, "m_handleNative", "I",     &ok);
        }
        Class_IBMatcher_NativeError = findClass(pEnv, IBMATCHER_JAVA_CLASS_PATH "$NativeError", &ok);
        if (Class_IBMatcher_NativeError != NULL)
        {
            FieldID_IBMatcher_NativeError_code = getFieldID(pEnv, Class_IBMatcher_NativeError, "code", "I", &ok);
        }
        Class_IBMatcher_SdkVersion = findClass(pEnv, IBMATCHER_JAVA_CLASS_PATH "$SdkVersion", &ok);
        if (Class_IBMatcher_SdkVersion != NULL)
        {
            MethodID_IBMatcher_SdkVersion_SdkVersion = getMethodID(pEnv, Class_IBMatcher_SdkVersion, "<init>", "(L" STRING_CLASS_PATH ";L" STRING_CLASS_PATH ";)V", &ok);
        }
        Class_IBCommon_ImageDataExt = findClass(pEnv, IBCOMMON_JAVA_CLASS_PATH "$ImageDataExt", &ok);
        if (Class_IBCommon_ImageDataExt != NULL)
        {
            MethodID_IBCommon_ImageDataExt_ImageDataExt         = getMethodID(pEnv, Class_IBCommon_ImageDataExt, "<init>", "(IIIISSSSSSSSBB[B)V", &ok);
            FieldID_IBCommon_ImageDataExt_imageFormat           = getFieldID(pEnv, Class_IBCommon_ImageDataExt, "imageFormat",           "L" IBCOMMON_JAVA_CLASS_PATH "$ImageFormat;", &ok);
            FieldID_IBCommon_ImageDataExt_impressionType        = getFieldID(pEnv, Class_IBCommon_ImageDataExt, "impressionType",        "L" IBCOMMON_JAVA_CLASS_PATH "$ImpressionType;", &ok);
            FieldID_IBCommon_ImageDataExt_fingerPosition        = getFieldID(pEnv, Class_IBCommon_ImageDataExt, "fingerPosition",        "L" IBCOMMON_JAVA_CLASS_PATH "$FingerPosition;", &ok);
            FieldID_IBCommon_ImageDataExt_captureDeviceTechId   = getFieldID(pEnv, Class_IBCommon_ImageDataExt, "captureDeviceTechId",   "L" IBCOMMON_JAVA_CLASS_PATH "$CaptureDeviceTechId;", &ok);
            FieldID_IBCommon_ImageDataExt_captureDeviceVendorId = getFieldID(pEnv, Class_IBCommon_ImageDataExt, "captureDeviceVendorId", "S",  &ok);
            FieldID_IBCommon_ImageDataExt_captureDeviceTypeId   = getFieldID(pEnv, Class_IBCommon_ImageDataExt, "captureDeviceTypeId",   "S",  &ok);
            FieldID_IBCommon_ImageDataExt_scanSamplingX         = getFieldID(pEnv, Class_IBCommon_ImageDataExt, "scanSamplingX",         "S",  &ok);
            FieldID_IBCommon_ImageDataExt_scanSamplingY         = getFieldID(pEnv, Class_IBCommon_ImageDataExt, "scanSamplingY",         "S",  &ok);
            FieldID_IBCommon_ImageDataExt_imageSamplingX        = getFieldID(pEnv, Class_IBCommon_ImageDataExt, "imageSamplingX",        "S",  &ok);
            FieldID_IBCommon_ImageDataExt_imageSamplingY        = getFieldID(pEnv, Class_IBCommon_ImageDataExt, "imageSamplingY",        "S",  &ok);
            FieldID_IBCommon_ImageDataExt_imageSizeX            = getFieldID(pEnv, Class_IBCommon_ImageDataExt, "imageSizeX",            "S",  &ok);
            FieldID_IBCommon_ImageDataExt_imageSizeY            = getFieldID(pEnv, Class_IBCommon_ImageDataExt, "imageSizeY",            "S",  &ok);
            FieldID_IBCommon_ImageDataExt_scaleUnit             = getFieldID(pEnv, Class_IBCommon_ImageDataExt, "scaleUnit",             "B",  &ok);
            FieldID_IBCommon_ImageDataExt_bitDepth              = getFieldID(pEnv, Class_IBCommon_ImageDataExt, "bitDepth",              "B",  &ok);
            FieldID_IBCommon_ImageDataExt_imageData             = getFieldID(pEnv, Class_IBCommon_ImageDataExt, "imageData",             "[B", &ok);
        }
		Class_IBMatcher_Template = findClass(pEnv, IBMATCHER_JAVA_CLASS_PATH "$Template", &ok);
        if (Class_IBMatcher_Template != NULL)
        {
            MethodID_IBMatcher_Template_Template             = getMethodID(pEnv, Class_IBMatcher_Template, "<init>", "(IIIISSSSSS[BI)V", &ok);
            FieldID_IBMatcher_Template_version               = getFieldID(pEnv, Class_IBMatcher_Template, "version",               "L" IBMATCHER_JAVA_CLASS_PATH "$TemplateVersion;", &ok);
            FieldID_IBMatcher_Template_fingerPosition        = getFieldID(pEnv, Class_IBMatcher_Template, "fingerPosition",        "L" IBCOMMON_JAVA_CLASS_PATH "$FingerPosition;", &ok);
            FieldID_IBMatcher_Template_impressionType        = getFieldID(pEnv, Class_IBMatcher_Template, "impressionType",        "L" IBCOMMON_JAVA_CLASS_PATH "$ImpressionType;", &ok);
            FieldID_IBMatcher_Template_captureDeviceTechId   = getFieldID(pEnv, Class_IBMatcher_Template, "captureDeviceTechId",   "L" IBCOMMON_JAVA_CLASS_PATH "$CaptureDeviceTechId;", &ok);
            FieldID_IBMatcher_Template_captureDeviceVendorId = getFieldID(pEnv, Class_IBMatcher_Template, "captureDeviceVendorId", "S",  &ok);
            FieldID_IBMatcher_Template_captureDeviceTypeId   = getFieldID(pEnv, Class_IBMatcher_Template, "captureDeviceTypeId",   "S",  &ok);
            FieldID_IBMatcher_Template_imageSamplingX        = getFieldID(pEnv, Class_IBMatcher_Template, "imageSamplingX",        "S",  &ok);
            FieldID_IBMatcher_Template_imageSamplingY        = getFieldID(pEnv, Class_IBMatcher_Template, "imageSamplingY",        "S",  &ok);
            FieldID_IBMatcher_Template_imageSizeX            = getFieldID(pEnv, Class_IBMatcher_Template, "imageSizeX",            "S",  &ok);
            FieldID_IBMatcher_Template_imageSizeY            = getFieldID(pEnv, Class_IBMatcher_Template, "imageSizeY",            "S",  &ok);
            FieldID_IBMatcher_Template_minutiae              = getFieldID(pEnv, Class_IBMatcher_Template, "minutiae",              "[B", &ok);
            FieldID_IBMatcher_Template_reserved              = getFieldID(pEnv, Class_IBMatcher_Template, "reserved",              "I", &ok);
        }
        Class_IBCommon_ImageFormat = findClass(pEnv, IBCOMMON_JAVA_CLASS_PATH "$ImageFormat", &ok);
        if (Class_IBCommon_ImageFormat != NULL)
        {
        	MethodID_IBCommon_ImageFormat_toCode = getMethodID(pEnv, Class_IBCommon_ImageFormat, "toCode", "()I", &ok);
        }
        Class_IBCommon_ImpressionType = findClass(pEnv, IBCOMMON_JAVA_CLASS_PATH "$ImpressionType", &ok);
        if (Class_IBCommon_ImpressionType != NULL)
        {
        	MethodID_IBCommon_ImpressionType_toCode = getMethodID(pEnv, Class_IBCommon_ImpressionType, "toCode", "()I", &ok);
        }
        Class_IBCommon_FingerPosition = findClass(pEnv, IBCOMMON_JAVA_CLASS_PATH "$FingerPosition", &ok);
        if (Class_IBCommon_FingerPosition != NULL)
        {
        	MethodID_IBCommon_FingerPosition_toCode = getMethodID(pEnv, Class_IBCommon_FingerPosition, "toCode", "()I", &ok);
        }
        Class_IBCommon_CaptureDeviceTechId = findClass(pEnv, IBCOMMON_JAVA_CLASS_PATH "$CaptureDeviceTechId", &ok);
        if (Class_IBCommon_CaptureDeviceTechId != NULL)
        {
        	MethodID_IBCommon_CaptureDeviceTechId_toCode = getMethodID(pEnv, Class_IBCommon_CaptureDeviceTechId, "toCode", "()I", &ok);
        }
        Class_IBMatcher_TemplateVersion = findClass(pEnv, IBMATCHER_JAVA_CLASS_PATH "$TemplateVersion", &ok);
        if (Class_IBMatcher_TemplateVersion != NULL)
        {
        	MethodID_IBMatcher_TemplateVersion_toCode = getMethodID(pEnv, Class_IBMatcher_TemplateVersion, "toCode", "()I", &ok);
        }
    }

    /* Indicate whether initialization was successful. */
    return (ok);
}

/*
 * Handle load of library.
 */
JNIEXPORT jint JNICALL JNI_OnLoad(
    JavaVM *vm,
    void   *reserved)
{
	LOG(("%s\n", __FUNCTION__));

    IBMatcher_OnLoad(vm, reserved);

    return (JNI_VERSION_1_2);
}

/***************************************************************************************************
 * JNI FUNCTIONS
 **************************************************************************************************/

/*
 * private native int initNative();
 */
JNIEXPORT jint JNICALL IBMATCHER_BUILD_JNI_FNCT(initNative)(
    JNIEnv  *pEnv,
	jobject  tthis)
{
    int matcherHandle = -1;

	LOG(("%s\n", __FUNCTION__));

	USE_PARAM(pEnv);
	USE_PARAM(tthis);

    (void)IBSM_OpenMatcher(&matcherHandle);

    return (matcherHandle);
}

/*
 * private native SdkVersion getSdkVersionNative(NativeError error);
 */
JNIEXPORT jobject JNICALL IBMATCHER_BUILD_JNI_FNCT(getSdkVersionNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jobject  error)
{
    IBSM_SDKVersion verInfo;
    int             nRc;
    jobject         verInfoJ = NULL;

    USE_PARAM(tthis);

    LOG(("%s\n", __FUNCTION__));

    memset(&verInfo, 0, sizeof(verInfo));
    nRc = IBSM_GetSDKVersion(&verInfo);
    setNativeError(pEnv, error, nRc);

    if (nRc >= IBSU_STATUS_OK)
    {
        jstring product;

        product = pEnv->NewStringUTF(verInfo.Product);
        if (product == NULL)
        {
            setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
            LOG(("%s: unable to allocate string\n", __FUNCTION__));
        }
        else
        {
            jstring file;

            file = pEnv->NewStringUTF(verInfo.File);
            if (file == NULL)
            {
                setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
                LOG(("%s: unable to allocate string\n", __FUNCTION__));
            }
            else
            {
                verInfoJ = pEnv->NewObject(Class_IBMatcher_SdkVersion, MethodID_IBMatcher_SdkVersion_SdkVersion, product, file);
                if (verInfoJ == NULL)
                {
                    setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
                    LOG(("%s: unable to allocate SdkVersion object\n", __FUNCTION__));
                }
            }
        }
    }

	return (verInfoJ);
}

/*
 * private native Template extractTemplateNative(ImageData imageData, NativeError error);
 */
JNIEXPORT jobject JNICALL IBMATCHER_BUILD_JNI_FNCT(extractTemplateNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jobject  imageDataExt,
	jobject  error)
{
    IBSM_ImageData imageDataExtC;
    jobject        ttemplateJ = NULL;
    BOOL           ok;

    LOG(("%s\n", __FUNCTION__));

    /* Extract C image data from Java.  We will need to free the buffer later. */
    ok = unconvertImageDataExt(pEnv, imageDataExt, &imageDataExtC);
    if (!ok)
    {
        setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
    	LOG(("%s: unable to unconvert image\n", __FUNCTION__));
    }
    else
    {
        int           nRc;
        int           matcherHandle;
        IBSM_Template ttemplateC;

        memset(&ttemplateC, 0, sizeof(IBSM_Template));
        matcherHandle = getHandle(pEnv, tthis);
    	nRc = IBSM_ExtractTemplate(matcherHandle, imageDataExtC, &ttemplateC);
        setNativeError(pEnv, error, nRc);

        if (nRc == IBSM_STATUS_OK)
        {
        	ttemplateJ = convertTemplate(pEnv, &ttemplateC);
        	if (ttemplateJ == NULL)
        	{
                setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
            	LOG(("%s: unable to convert template\n", __FUNCTION__));
        	}
        }

        /* Free the buffer. */
        free(imageDataExtC.ImageData);
    }

    return (ttemplateJ);
}

/*
 * private native ImageDataExt compressImageNative(ImageDataExt imageDataExt, int imageFormatCode,
 * 	   NativeError error);
 */
JNIEXPORT jobject JNICALL IBMATCHER_BUILD_JNI_FNCT(compressImageNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jobject  imageDataExt,
	jint     imageFormatCode,
	jobject  error)
{
    IBSM_ImageData imageDataExtC;
    jobject        imageDataExtOutJ = NULL;
    BOOL           ok;

    LOG(("%s\n", __FUNCTION__));

    /* Extract C image data from Java.  We will need to free the buffer later. */
    ok = unconvertImageDataExt(pEnv, imageDataExt, &imageDataExtC);
    if (!ok)
    {
        setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
    	LOG(("%s: unable to unconvert image\n", __FUNCTION__));
    }
    else
    {
		int nRc;
		int matcherHandle;
		IBSM_ImageData imageDataExtOutC;

		memset(&imageDataExtOutC, 0, sizeof(IBSM_ImageData));

		matcherHandle = getHandle(pEnv, tthis);
		nRc = IBSM_CompressImage(matcherHandle, imageDataExtC, &imageDataExtOutC, (IBSM_ImageFormat)imageFormatCode);
		setNativeError(pEnv, error, nRc);

		if (nRc == IBSM_STATUS_OK)
		{
			imageDataExtOutJ = convertImageDataExt(pEnv, &imageDataExtOutC);
			if (imageDataExtOutJ == NULL)
			{
		        setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
		    	LOG(("%s: unable to convert image\n", __FUNCTION__));
			}
		}

        /* Free the buffer. */
		free(imageDataExtC.ImageData);
    }

    return (imageDataExtOutJ);
}

/*
 * private native ImageDataExt decompressImageNative(ImageData imageData, NativeError error);
 */
JNIEXPORT jobject JNICALL IBMATCHER_BUILD_JNI_FNCT(decompressImageNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jobject  imageDataExt,
	jobject  error)
{
    IBSM_ImageData imageDataExtC;
    jobject        imageDataExtOutJ = NULL;
    BOOL           ok;

    LOG(("%s\n", __FUNCTION__));

    /* Extract C image data from Java.  We will need to free the buffer later. */
    ok = unconvertImageDataExt(pEnv, imageDataExt, &imageDataExtC);
    if (!ok)
    {
        setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
    	LOG(("%s: unable to unconvert image\n", __FUNCTION__));
    }
    else
    {
		int nRc;
		int matcherHandle;
		IBSM_ImageData imageDataExtOutC;

		memset(&imageDataExtOutC, 0, sizeof(IBSM_ImageData));

		matcherHandle = getHandle(pEnv, tthis);
		nRc = IBSM_DecompressImage(matcherHandle, imageDataExtC, &imageDataExtOutC);
		setNativeError(pEnv, error, nRc);

		if (nRc == IBSM_STATUS_OK)
		{
			imageDataExtOutJ = convertImageDataExt(pEnv, &imageDataExtOutC);
			if (imageDataExtOutJ == NULL)
			{
		        setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
		    	LOG(("%s: unable to convert image\n", __FUNCTION__));
			}
		}

        /* Free the buffer. */
		free(imageDataExtC.ImageData);
    }

    return (imageDataExtOutJ);
}

/*
 * private native boolean saveImageNative(ImageDataExt imageDataExt, String filePath,
 *     NativeError error);
 */
JNIEXPORT jboolean JNICALL IBMATCHER_BUILD_JNI_FNCT(saveImageNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jobject  imageDataExt,
	jstring  filePath,
	jobject  error)
{
	IBSM_ImageData imageDataExtC;
	BOOL           ok;
	jboolean       okJ = JNI_FALSE;

	LOG(("%s\n", __FUNCTION__));

    /* Extract C image data from Java.  We will need to free the buffer later. */
	ok = unconvertImageDataExt(pEnv, imageDataExt, &imageDataExtC);
	if (!ok)
	{
        setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
    	LOG(("%s: unable to unconvert image\n", __FUNCTION__));
	}
	else
	{
		char *filePathC;

		filePathC = (char *)pEnv->GetStringUTFChars(filePath, NULL);
		if (filePathC == NULL)
		{
			setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
			LOG(("%s: unable to get string characters\n", __FUNCTION__));
		}
		else
		{
			int matcherHandle;
			int nRc;

			matcherHandle = getHandle(pEnv, tthis);

			nRc = IBSM_SaveImageData(matcherHandle, filePathC, imageDataExtC);
			setNativeError(pEnv, error, nRc);

			pEnv->ReleaseStringUTFChars(filePath, filePathC);
		}

        /* Free the buffer. */
		free(imageDataExtC.ImageData);
	}

	return (okJ);
}

/*
 * private native ImageDataExt loadImageNative(String filePath, NativeError error);
 */
JNIEXPORT jobject JNICALL IBMATCHER_BUILD_JNI_FNCT(loadImageNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jstring  filePath,
	jobject  error)
{
	jobject imageDataExt = NULL;
	char   *filePathC;

	LOG(("%s\n", __FUNCTION__));

	filePathC = (char *)pEnv->GetStringUTFChars(filePath, NULL);
	if (filePathC == NULL)
	{
		setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
		LOG(("%s: unable to get string characters\n", __FUNCTION__));
	}
	else
	{
		int            matcherHandle;
		int            nRc;
		IBSM_ImageData imageDataExtC;

		matcherHandle = getHandle(pEnv, tthis);

		memset(&imageDataExtC, 0, sizeof(imageDataExtC));
		nRc = IBSM_OpenImageData(matcherHandle, filePathC, &imageDataExtC);
		setNativeError(pEnv, error, nRc);

		if (nRc == IBSM_STATUS_OK)
		{
			imageDataExt = convertImageDataExt(pEnv, &imageDataExtC);
			if (imageDataExt == NULL)
			{
				setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
				LOG(("%s: unable to convert image\n", __FUNCTION__));
			}
		}

		pEnv->ReleaseStringUTFChars(filePath, filePathC);
	}

	return (imageDataExt);
}

/*
 * private native boolean saveImageAsFirNative(ImageDataExt imageDataExt, String filePath,
 *		NativeError error);
 */
JNIEXPORT jboolean JNICALL IBMATCHER_BUILD_JNI_FNCT(saveImageAsFirNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jobject  imageDataExt,
	jstring  filePath,
	jobject  error)
{
	IBSM_ImageData imageDataExtC;
	BOOL           ok;
	jboolean       okJ = JNI_FALSE;

	LOG(("%s\n", __FUNCTION__));

    /* Extract C image data from Java.  We will need to free the buffer later. */
	ok = unconvertImageDataExt(pEnv, imageDataExt, &imageDataExtC);
	if (!ok)
	{
        setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
    	LOG(("%s: unable to unconvert image\n", __FUNCTION__));
	}
	else
	{
		int     matcherHandle;
		int     nRc;
		ISO_FIR fir;

		matcherHandle = getHandle(pEnv, tthis);

		memset(&fir, 0, sizeof(fir));
		nRc = IBSM_ConvertImage_IBSMtoISO(matcherHandle, imageDataExtC, &fir);
		setNativeError(pEnv, error, nRc);

		if (nRc == IBSM_STATUS_OK)
		{
			char *filePathC;

			filePathC = (char *)pEnv->GetStringUTFChars(filePath, NULL);
			if (filePathC == NULL)
			{
		        setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
		    	LOG(("%s: unable to get string characters\n", __FUNCTION__));
			}
			else
			{
				nRc = IBSM_SaveFIR(matcherHandle, filePathC, fir);
				setNativeError(pEnv, error, nRc);

				pEnv->ReleaseStringUTFChars(filePath, filePathC);
			}
		}

        /* Free the buffer. */
		free(imageDataExtC.ImageData);
	}

	return (okJ);
}

/*
 * private native ImageDataExt loadImageFromFirNative(String filePath, NativeError error);
 */
JNIEXPORT jobject JNICALL IBMATCHER_BUILD_JNI_FNCT(loadImageFromFirNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jstring  filePath,
	jobject  error)
{
	jobject imageDataExt = NULL;
	char   *filePathC;

	LOG(("%s\n", __FUNCTION__));

	filePathC = (char *)pEnv->GetStringUTFChars(filePath, NULL);
	if (filePathC == NULL)
	{
		setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
		LOG(("%s: unable to get string characters\n", __FUNCTION__));
	}
	else
	{
		int     matcherHandle;
		int     nRc;
		ISO_FIR fir;

		matcherHandle = getHandle(pEnv, tthis);

		memset(&fir, 0, sizeof(fir));
		nRc = IBSM_OpenFIR(matcherHandle, filePathC, &fir);
		setNativeError(pEnv, error, nRc);

		if (nRc == IBSM_STATUS_OK)
		{
			IBSM_ImageData *imageDataExtArray = NULL;
			int             imageCount = 0;

			nRc = IBSM_ConvertImage_ISOtoIBSM(matcherHandle, fir, &imageDataExtArray, &imageCount);
			setNativeError(pEnv, error, nRc);

			if (nRc == IBSM_STATUS_OK)
			{
				if (imageCount > 0)
				{
					/* Only convert first image. */
					imageDataExt = convertImageDataExt(pEnv, imageDataExtArray);
					if (imageDataExt == NULL)
					{
				        setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
				    	LOG(("%s: unable to convert image\n", __FUNCTION__));
					}
				}
				else
				{
					setNativeError(pEnv, error, IBSM_ERR_EXTRACTION_FAILED);
			    	LOG(("%s: no image in ISO data\n", __FUNCTION__));
				}
			}
		}

		pEnv->ReleaseStringUTFChars(filePath, filePathC);
	}

	return (imageDataExt);
}

/*
 * private native boolean saveTemplateNative(Template template, String filePath, NativeError error);
 */
JNIEXPORT jboolean JNICALL IBMATCHER_BUILD_JNI_FNCT(saveTemplateNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jobject  ttemplate,
	jstring  filePath,
	jobject  error)
{
	IBSM_Template ttemplateC;
	BOOL          ok;
	jboolean      okJ = JNI_FALSE;

	LOG(("%s\n", __FUNCTION__));

	ok = unconvertTemplate(pEnv, ttemplate, &ttemplateC);
	if (!ok)
	{
        setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
    	LOG(("%s: unable to unconvert template\n", __FUNCTION__));
	}
	else
	{
		char *filePathC;

		filePathC = (char *)pEnv->GetStringUTFChars(filePath, NULL);
		if (filePathC == NULL)
		{
			setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
			LOG(("%s: unable to get string characters\n", __FUNCTION__));
		}
		else
		{
			int matcherHandle;
			int nRc;

			matcherHandle = getHandle(pEnv, tthis);
			nRc           = IBSM_SaveTemplate(matcherHandle, filePathC, ttemplateC);
			setNativeError(pEnv, error, nRc);

			pEnv->ReleaseStringUTFChars(filePath, filePathC);
		}
	}

	return (okJ);
}

/*
 * private native Template loadTemplateNative(String filePath, NativeError error);
 */
JNIEXPORT jobject JNICALL IBMATCHER_BUILD_JNI_FNCT(loadTemplateNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jstring  filePath,
	jobject  error)
{
	jobject ttemplate = NULL;
	char   *filePathC;

	LOG(("%s\n", __FUNCTION__));

	filePathC = (char *)pEnv->GetStringUTFChars(filePath, NULL);
	if (filePathC == NULL)
	{
		setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
		LOG(("%s: unable to get string characters\n", __FUNCTION__));
	}
	else
	{
		int           matcherHandle;
		int           nRc;
		IBSM_Template ttemplateC;

		matcherHandle = getHandle(pEnv, tthis);

		memset(&ttemplateC, 0, sizeof(ttemplateC));
		nRc = IBSM_OpenTemplate(matcherHandle, filePathC, &ttemplateC);
		setNativeError(pEnv, error, nRc);

		if (nRc == IBSM_STATUS_OK)
		{
			ttemplate = convertTemplate(pEnv, &ttemplateC);
			if (ttemplate == NULL)
			{
				setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
				LOG(("%s: unable to convert template\n", __FUNCTION__));
			}
		}

		pEnv->ReleaseStringUTFChars(filePath, filePathC);
	}

	return (ttemplate);
}

/*
 * private native boolean saveTemplateAsFmrNative(Template template, String filePath,
 *		NativeError error);
 */
JNIEXPORT jboolean JNICALL IBMATCHER_BUILD_JNI_FNCT(saveTemplateAsFmrNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jobject  ttemplate,
	jstring  filePath,
	jobject  error)
{
	IBSM_Template ttemplateC;
	BOOL          ok;
	jboolean      okJ = JNI_FALSE;

	LOG(("%s\n", __FUNCTION__));

	ok = unconvertTemplate(pEnv, ttemplate, &ttemplateC);
	if (!ok)
	{
        setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
    	LOG(("%s: unable to unconvert template\n", __FUNCTION__));
	}
	else
	{
		int     matcherHandle;
		int     nRc;
		ISO_FMR fmr;

		matcherHandle = getHandle(pEnv, tthis);

		memset(&fmr, 0, sizeof(fmr));
		nRc = IBSM_ConvertTemplate_IBSMtoISO(matcherHandle, ttemplateC, &fmr);
		setNativeError(pEnv, error, nRc);

		if (nRc == IBSM_STATUS_OK)
		{
			char *filePathC;

			filePathC = (char *)pEnv->GetStringUTFChars(filePath, NULL);
			if (filePathC == NULL)
			{
		        setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
		    	LOG(("%s: unable to get string characters\n", __FUNCTION__));
			}
			else
			{
				nRc = IBSM_SaveFMR(matcherHandle, filePathC, fmr);
				setNativeError(pEnv, error, nRc);

				pEnv->ReleaseStringUTFChars(filePath, filePathC);
			}
		}
	}

	return (okJ);
}

/*
 * private native Template loadTemplateFromFmrNative(String filePath, NativeError error);
 */
JNIEXPORT jobject JNICALL IBMATCHER_BUILD_JNI_FNCT(loadTemplateFromFmrNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jstring  filePath,
	jobject  error)
{
	jobject ttemplate = NULL;
	char   *filePathC;

	LOG(("%s\n", __FUNCTION__));

	filePathC = (char *)pEnv->GetStringUTFChars(filePath, NULL);
	if (filePathC == NULL)
	{
		setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
		LOG(("%s: unable to get string characters\n", __FUNCTION__));
	}
	else
	{
		int     matcherHandle;
		int     nRc;
		ISO_FMR fmr;

		matcherHandle = getHandle(pEnv, tthis);

		memset(&fmr, 0, sizeof(fmr));
		nRc = IBSM_OpenFMR(matcherHandle, filePathC, &fmr);
		setNativeError(pEnv, error, nRc);

		if (nRc == IBSM_STATUS_OK)
		{
			IBSM_Template *templateArray = NULL;
			int            templateCount = 0;

			nRc = IBSM_ConvertTemplate_ISOtoIBSM(matcherHandle, fmr, &templateArray, &templateCount);
			setNativeError(pEnv, error, nRc);

			if (nRc == IBSM_STATUS_OK)
			{
				if (templateCount > 0)
				{
					/* Only convert first template. */
					ttemplate = convertTemplate(pEnv, templateArray);
					if (ttemplate == NULL)
					{
				        setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
				    	LOG(("%s: unable to convert template\n", __FUNCTION__));
					}
				}
				else
				{
					setNativeError(pEnv, error, IBSM_ERR_EXTRACTION_FAILED);
			    	LOG(("%s: no template in ISO data\n", __FUNCTION__));
				}
			}
		}

		pEnv->ReleaseStringUTFChars(filePath, filePathC);
	}

	return (ttemplate);
}

/*
 * private native int matchTemplatesNative(Template template1, Template template2,
 * 		NativeError error);
 */
JNIEXPORT jint JNICALL IBMATCHER_BUILD_JNI_FNCT(matchTemplatesNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jobject  template1,
	jobject  template2,
	jobject  error)
{
	IBSM_Template template1C;
	int           matchingScore = -1;
	BOOL          ok;

	LOG(("%s\n", __FUNCTION__));

	ok = unconvertTemplate(pEnv, template1, &template1C);
	if (!ok)
	{
        setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
    	LOG(("%s: unable to unconvert first template\n", __FUNCTION__));
	}
	else
	{
		IBSM_Template template2C;

		ok = unconvertTemplate(pEnv, template2, &template2C);
		if (!ok)
		{
	        setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
	    	LOG(("%s: unable to unconvert second template\n", __FUNCTION__));
		}
		else
		{
			int matcherHandle;
			int nRc;

			matcherHandle = getHandle(pEnv, tthis);
			nRc           = IBSM_MatchingTemplate(matcherHandle, template1C, template2C,
								&matchingScore);
			setNativeError(pEnv, error, nRc);
		}
	}

	return (matchingScore);
}

/*
 * private native void setMatchingLevelNative(int matchingLevel, NativeError error);
 */
JNIEXPORT void JNICALL IBMATCHER_BUILD_JNI_FNCT(setMatchingLevelNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jint     matchingLevel,
	jobject  error)
{
	int matcherHandle;
	int nRc;

	matcherHandle = getHandle(pEnv, tthis);
	nRc           = IBSM_SetMatchingLevel(matcherHandle, matchingLevel);
	setNativeError(pEnv, error, nRc);
}

/*
 * private native int getMatchingLevelNative(NativeError error);
 */
JNIEXPORT jint JNICALL IBMATCHER_BUILD_JNI_FNCT(getMatchingLevelNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jobject  error)
{
	int matcherHandle;
	int nRc;
	int matchingLevel = -1;

	matcherHandle = getHandle(pEnv, tthis);
	nRc           = IBSM_GetMatchingLevel(matcherHandle, &matchingLevel);
	setNativeError(pEnv, error, nRc);

	return (matchingLevel);
}

/*
 * private native Template singleEnrollmentNative(ImageDataExt imageDataExt1,
 *		ImageDataExt imageDataExt2, ImageDataExt imageDataExt3, NativeError error);
 */
JNIEXPORT jobject JNICALL IBMATCHER_BUILD_JNI_FNCT(singleEnrollmentNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jobject  imageDataExt1,
	jobject  imageDataExt2,
	jobject  imageDataExt3,
	jobject  error)
{
	IBSM_ImageData imageDataExt1C;
	jobject        ttemplate = NULL;
	BOOL           ok;

	LOG(("%s\n", __FUNCTION__));

	ok = unconvertImageDataExt(pEnv, imageDataExt1, &imageDataExt1C);
	if (!ok)
	{
        setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
    	LOG(("%s: unable to unconvert first image data\n", __FUNCTION__));
	}
	else
	{
		IBSM_ImageData imageDataExt2C;

		ok = unconvertImageDataExt(pEnv, imageDataExt2, &imageDataExt2C);
		if (!ok)
		{
	        setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
	    	LOG(("%s: unable to unconvert second image data\n", __FUNCTION__));
		}
		else
		{
			IBSM_ImageData imageDataExt3C;

			ok = unconvertImageDataExt(pEnv, imageDataExt3, &imageDataExt3C);
			if (!ok)
			{
		        setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
		    	LOG(("%s: unable to unconvert third image data\n", __FUNCTION__));
			}
			else
			{
				int           matcherHandle;
				int           nRc;
				IBSM_Template ttemplateC;

				memset(&ttemplateC, 0, sizeof(ttemplateC));
				matcherHandle = getHandle(pEnv, tthis);
				nRc           = IBSM_SingleEnrollment(matcherHandle, imageDataExt1C, imageDataExt2C,
									imageDataExt3C, &ttemplateC);
				setNativeError(pEnv, error, nRc);

				if (nRc == IBSM_STATUS_OK)
				{
					ttemplate = convertTemplate(pEnv, &ttemplateC);
					if (ttemplate == NULL)
					{
				        setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
				    	LOG(("%s: unable to convert template\n", __FUNCTION__));
					}
				}

				free(imageDataExt3C.ImageData);
			}

			free(imageDataExt2C.ImageData);
		}

		free(imageDataExt1C.ImageData);
	}

	return (ttemplate);
}

/*
 * private native Template multiEnrollmentNative(ImageDataExt imageDataExt1,
 *		ImageDataExt imageDataExt2, ImageDataExt imageDataExt3, NativeError error);
 */
JNIEXPORT jobjectArray JNICALL IBMATCHER_BUILD_JNI_FNCT(multiEnrollmentNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jobject  imageDataExt1,
	jobject  imageDataExt2,
	jobject  imageDataExt3,
	jobject  imageDataExt4,
	jobject  imageDataExt5,
	jobject  imageDataExt6,
	jobject  error)
{
	IBSM_ImageData imageDataExt1C;
	jobjectArray   ttemplates = NULL;
	BOOL           ok;

	LOG(("%s\n", __FUNCTION__));

	ok = unconvertImageDataExt(pEnv, imageDataExt1, &imageDataExt1C);
	if (!ok)
	{
        setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
    	LOG(("%s: unable to unconvert first image data\n", __FUNCTION__));
	}
	else
	{
		IBSM_ImageData imageDataExt2C;

		ok = unconvertImageDataExt(pEnv, imageDataExt2, &imageDataExt2C);
		if (!ok)
		{
	        setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
	    	LOG(("%s: unable to unconvert second image data\n", __FUNCTION__));
		}
		else
		{
			IBSM_ImageData imageDataExt3C;

			ok = unconvertImageDataExt(pEnv, imageDataExt3, &imageDataExt3C);
			if (!ok)
			{
		        setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
		    	LOG(("%s: unable to unconvert third image data\n", __FUNCTION__));
			}
			else
			{
				IBSM_ImageData imageDataExt4C;

				ok = unconvertImageDataExt(pEnv, imageDataExt4, &imageDataExt4C);
				if (!ok)
				{
			        setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
			    	LOG(("%s: unable to unconvert fourth image data\n", __FUNCTION__));
				}
				else
				{
					IBSM_ImageData imageDataExt5C;

					ok = unconvertImageDataExt(pEnv, imageDataExt5, &imageDataExt5C);
					if (!ok)
					{
				        setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
				    	LOG(("%s: unable to unconvert fifth image data\n", __FUNCTION__));
					}
					else
					{
						IBSM_ImageData imageDataExt6C;

						ok = unconvertImageDataExt(pEnv, imageDataExt6, &imageDataExt6C);
						if (!ok)
						{
					        setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
					    	LOG(("%s: unable to unconvert sixth image data\n", __FUNCTION__));
						}
						else
						{
							int           matcherHandle;
							int           nRc;
							IBSM_Template ttemplateC1;
							IBSM_Template ttemplateC2;

							memset(&ttemplateC1, 0, sizeof(ttemplateC1));
							memset(&ttemplateC2, 0, sizeof(ttemplateC2));
							matcherHandle = getHandle(pEnv, tthis);
							nRc           = IBSM_MultiEnrollment(matcherHandle, imageDataExt1C,
												imageDataExt2C, imageDataExt3C, imageDataExt4C,
												imageDataExt5C, imageDataExt6C, &ttemplateC1,
												&ttemplateC2);
							setNativeError(pEnv, error, nRc);

							if (nRc == IBSM_STATUS_OK)
							{
								jobject ttemplate1;

								ttemplate1 = convertTemplate(pEnv, &ttemplateC1);
								if (ttemplate1 == NULL)
								{
									setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
									LOG(("%s: unable to convert first template\n", __FUNCTION__));
								}
								else
								{
									jobject ttemplate2;

									ttemplate2 = convertTemplate(pEnv, &ttemplateC2);
									if (ttemplate2 == NULL)
									{
										setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
										LOG(("%s: unable to convert second template\n", __FUNCTION__));
									}
									else
									{
										ttemplates = pEnv->NewObjectArray(2, Class_IBMatcher_Template, NULL);
										if (ttemplates == NULL)
										{
											setNativeError(pEnv, error, IBSM_ERR_MEM_ALLOC);
											LOG(("%s: unable to allocate array for templates\n", __FUNCTION__));
										}
										else
										{
											pEnv->SetObjectArrayElement(ttemplates, 0, ttemplate1);
											pEnv->SetObjectArrayElement(ttemplates, 1, ttemplate2);
										}
									}
								}
							}

							free(imageDataExt6C.ImageData);
						}

						free(imageDataExt5C.ImageData);
					}

					free(imageDataExt4C.ImageData);
				}

				free(imageDataExt3C.ImageData);
			}

			free(imageDataExt2C.ImageData);
		}

		free(imageDataExt1C.ImageData);
	}

	return (ttemplates);
}

/***************************************************************************************************
 * LOCAL FUNCTIONS (HELPERS)
 **************************************************************************************************/

/*
 * Find a class and log result.
 */
static jclass findClass(
    JNIEnv     *pEnv,
    const char *name,
    BOOL       *pOk)
{
    jclass cclass = NULL;

    if (*pOk == TRUE)
    {
		cclass = pEnv->FindClass(name);
		if (cclass == NULL)
		{
			LOG(("unable to get reference to \"%s\" class\n", name));
			*pOk = FALSE;
		}
		else
		{
			LOG(("got reference to \"%s\" class\n", name));
			cclass = (jclass)pEnv->NewGlobalRef(cclass);
			if (cclass == NULL)
			{
				LOG(("unable to get global reference to \"%s\" class\n", name));
				*pOk = FALSE;
			}
		}
    }
    return (cclass);
}

/*
 * Get a field ID and log result.
 */
static jfieldID getFieldID(
    JNIEnv     *pEnv,
    jclass      cclass,
    const char *name,
    const char *sig,
    BOOL       *pOk)
{
    jfieldID fieldID = NULL;

    if (*pOk == TRUE)
    {
		fieldID = pEnv->GetFieldID(cclass, name, sig);
		if (fieldID == NULL)
		{
			LOG(("unable to get reference to \"%s\" field with signature \"%s\"\n", name, sig));
			*pOk = FALSE;
		}
		else
		{
			LOG(("got reference to \"%s\" field with signature \"%s\"\n", name, sig));
		}
    }
    return (fieldID);
}

/*
 * Get a method ID and log result.
 */
static jmethodID getMethodID(
    JNIEnv     *pEnv,
    jclass      cclass,
    const char *name,
    const char *sig,
    BOOL       *pOk)
{
    jmethodID methodID = NULL;

    if (*pOk == TRUE)
    {
		methodID = pEnv->GetMethodID(cclass, name, sig);
		if (methodID == NULL)
		{
			LOG(("unable to get reference to \"%s\" method with signature \"%s\"\n", name, sig));
			*pOk = FALSE;
		}
		else
		{
			LOG(("got reference to \"%s\" method with signature \"%s\"\n", name, sig));
		}
    }
    return (methodID);
}

/*
 * Set native error.
 */
static void setNativeError(
    JNIEnv  *pEnv,
    jobject  obj,
    int      code)
{
    pEnv->SetIntField(obj, FieldID_IBMatcher_NativeError_code, (jint)code);
}

/*
 * Set native error.
 */
static int getHandle(
    JNIEnv  *pEnv,
    jobject  obj)
{
    jint handle = pEnv->GetIntField(obj, FieldID_IBMatcher_handleNative);

    return (handle);
}

/*
 * Convert C ImageDataExt to Java ImageDataExt.
 */
static jobject convertImageDataExt(
    JNIEnv               *pEnv,
    const IBSM_ImageData *pImage)
{
    jbyteArray bufferJ;
    jobject    imageJ = NULL;

    bufferJ = pEnv->NewByteArray(pImage->ImageDataLength);
    if (bufferJ == NULL)
    {
    	LOG(("unable to allocate data buffer of size %d\n", pImage->ImageDataLength));
    }
    else
    {
        pEnv->SetByteArrayRegion(bufferJ, 0, pImage->ImageDataLength, (const jbyte *)pImage->ImageData);
        imageJ  = pEnv->NewObject(Class_IBCommon_ImageDataExt,
                        MethodID_IBCommon_ImageDataExt_ImageDataExt, (jint)pImage->ImageFormat,
                        (jint)pImage->ImpressionType, (jint)pImage->FingerPosition, (jint)pImage->CaptureDeviceTechID,
                        (jshort)pImage->CaptureDeviceVendorID, (jshort)pImage->CaptureDeviceTypeID,
                        (jshort)pImage->ScanSamplingX, (jshort)pImage->ScanSamplingY, (jshort)pImage->ImageSamplingX,
                        (jshort)pImage->ImageSamplingY, (jshort)pImage->ImageSizeX, (jshort)pImage->ImageSizeY,
                        (jbyte)pImage->ScaleUnit, (jbyte)pImage->BitDepth, bufferJ);
        if (imageJ == NULL)
        {
        	LOG(("unable to allocate ImageData object\n"));
        }
    }

    return (imageJ);
}

/*
 * Convert Java ImageDataExt to C ImageDataExt.
 */
static BOOL unconvertImageDataExt(
	JNIEnv         *pEnv,
	jobject         imageData,
	IBSM_ImageData *pImage)
{
	memset(pImage, 0, sizeof(IBSM_ImageData));
	BOOL       ok = FALSE;
	jbyteArray imageDataJ;

	/* Get buffer field. */
	imageDataJ = (jbyteArray)pEnv->GetObjectField(imageData, FieldID_IBCommon_ImageDataExt_imageData);
	if (imageDataJ == NULL)
	{
        LOG(("%s: unable to get imageData field\n", __FUNCTION__));
	}
	else
	{
		void *buffer;

		buffer = pEnv->GetByteArrayElements(imageDataJ, NULL);
		if (buffer == NULL)
		{
	        LOG(("%s: unable to get imageData field\n", __FUNCTION__));
		}
		else
		{
			jobject temp;

			pImage->ImageDataLength = pEnv->GetArrayLength(imageDataJ);
			pImage->ImageData       = malloc(pImage->ImageDataLength);
			if (pImage->ImageData == NULL)
			{
				LOG(("%s: unable to allocate memory for imageData\n", __FUNCTION__));
			}
			else
			{
				memcpy(pImage->ImageData, buffer, pImage->ImageDataLength);

				/* Get captureDeviceVendorId field. */
				pImage->CaptureDeviceVendorID = pEnv->GetShortField(imageData, FieldID_IBCommon_ImageDataExt_captureDeviceVendorId);
				/* Get captureDeviceTypeId field. */
				pImage->CaptureDeviceTypeID   = pEnv->GetShortField(imageData, FieldID_IBCommon_ImageDataExt_captureDeviceTypeId);
				/* Get scanSamplingX field. */
				pImage->ScanSamplingX         = pEnv->GetShortField(imageData, FieldID_IBCommon_ImageDataExt_scanSamplingX);
				/* Get scanSamplingY field. */
				pImage->ScanSamplingY         = pEnv->GetShortField(imageData, FieldID_IBCommon_ImageDataExt_scanSamplingY);
				/* Get imageSamplingX field. */
				pImage->ImageSamplingX        = pEnv->GetShortField(imageData, FieldID_IBCommon_ImageDataExt_imageSamplingX);
				/* Get imageSamplingY field. */
				pImage->ImageSamplingY        = pEnv->GetShortField(imageData, FieldID_IBCommon_ImageDataExt_imageSamplingY);
				/* Get imageSizeX field. */
				pImage->ImageSizeX            = pEnv->GetShortField(imageData, FieldID_IBCommon_ImageDataExt_imageSizeX);
				/* Get imageSizeY field. */
				pImage->ImageSizeY            = pEnv->GetShortField(imageData, FieldID_IBCommon_ImageDataExt_imageSizeY);
				/* Get scaleUnit field. */
				pImage->ScaleUnit             = pEnv->GetByteField(imageData, FieldID_IBCommon_ImageDataExt_scaleUnit);
				/* Get bitDepth field. */
				pImage->BitDepth              = pEnv->GetByteField(imageData, FieldID_IBCommon_ImageDataExt_bitDepth);

				/* Get imageFormat field. */
				temp = pEnv->GetObjectField(imageData, FieldID_IBCommon_ImageDataExt_imageFormat);
				if (temp == NULL)
				{
					free(pImage->ImageData);
					LOG(("%s: unable to get imageFormat field\n", __FUNCTION__));
				}
				else
				{
					pImage->ImageFormat = (IBSM_ImageFormat)pEnv->CallIntMethod(temp, MethodID_IBCommon_ImageFormat_toCode);

					/* Get impressionType field. */
					temp = pEnv->GetObjectField(imageData, FieldID_IBCommon_ImageDataExt_impressionType);
					if (temp == NULL)
					{
						free(pImage->ImageData);
						LOG(("%s: unable to get impressionType field\n", __FUNCTION__));
					}
					else
					{
						pImage->ImpressionType = (IBSM_ImpressionType)pEnv->CallIntMethod(temp, MethodID_IBCommon_ImpressionType_toCode);

						/* Get fingerPosition field. */
						temp = pEnv->GetObjectField(imageData, FieldID_IBCommon_ImageDataExt_fingerPosition);
						if (temp == NULL)
						{
							free(pImage->ImageData);
							LOG(("%s: unable to get fingerPosition field\n", __FUNCTION__));
						}
						else
						{
							pImage->FingerPosition = (IBSM_FingerPosition)pEnv->CallIntMethod(temp, MethodID_IBCommon_FingerPosition_toCode);

							/* Get captureDeviceTechId field. */
							temp = pEnv->GetObjectField(imageData, FieldID_IBCommon_ImageDataExt_captureDeviceTechId);
							if (temp == NULL)
							{
								free(pImage->ImageData);
								LOG(("%s: unable to get captureDeviceTechId field\n", __FUNCTION__));
							}
							else
							{
								pImage->CaptureDeviceTechID = (IBSM_CaptureDeviceTechID)pEnv->CallIntMethod(temp, MethodID_IBCommon_CaptureDeviceTechId_toCode);

								ok = TRUE;
							}
						}
					}
				}
			}
			pEnv->ReleaseByteArrayElements(imageDataJ, (jbyte *)buffer, 0);
		}
	}

	return (ok);
}

/*
 * Convert C Template to Java Template.
 */
static jobject convertTemplate(
    JNIEnv              *pEnv,
    const IBSM_Template *pTemplate)
{
    jbyteArray minutiaeJ;
    jobject    templateJ = NULL;

    minutiaeJ = pEnv->NewByteArray(sizeof(pTemplate->Minutiae));
    if (minutiaeJ == NULL)
    {
    	LOG(("unable to allocate data buffer of size %d\n", sizeof(pTemplate->Minutiae)));
    }
    else
    {
        pEnv->SetByteArrayRegion(minutiaeJ, 0, sizeof(pTemplate->Minutiae), (const jbyte *)pTemplate->Minutiae);
        templateJ  = pEnv->NewObject(Class_IBMatcher_Template,
                        MethodID_IBMatcher_Template_Template, (jint)pTemplate->Version,
                        (jint)pTemplate->FingerPosition, (jint)pTemplate->ImpressionType,
                        (jint)pTemplate->CaptureDeviceTechID, (jshort)pTemplate->CaptureDeviceVendorID,
                        (jshort)pTemplate->CaptureDeviceTypeID, (jshort)pTemplate->ImageSamplingX,
                        (jshort)pTemplate->ImageSamplingY, (jshort)pTemplate->ImageSizeX,
                        (jshort)pTemplate->ImageSizeY, minutiaeJ, (jint)pTemplate->Reserved);
        if (templateJ == NULL)
        {
        	LOG(("unable to allocate Template object\n"));
        }
    }

    return (templateJ);
}

/*
 * Convert Java ImageDataExt to C ImageDataExt.
 */
static BOOL unconvertTemplate(
	JNIEnv        *pEnv,
	jobject        ttemplate,
	IBSM_Template *pTemplate)
{
	memset(pTemplate, 0, sizeof(IBSM_Template));
	BOOL       ok = FALSE;
	jbyteArray minutiaeJ;

	/* Get minutiae field. */
	minutiaeJ = (jbyteArray)pEnv->GetObjectField(ttemplate, FieldID_IBMatcher_Template_minutiae);
	if (minutiaeJ == NULL)
	{
        LOG(("%s: unable to minutiae field\n", __FUNCTION__));
	}
	else
	{
		jbyte *minutiaeC = pEnv->GetByteArrayElements(minutiaeJ, NULL);
		if (minutiaeC == NULL)
		{
	        LOG(("%s: unable to get minutiae field\n", __FUNCTION__));
		}
		else
		{
			jobject temp;

			memcpy(pTemplate->Minutiae, minutiaeC, pEnv->GetArrayLength(minutiaeJ));
			pEnv->ReleaseByteArrayElements(minutiaeJ, minutiaeC, 0);

			/* Get captureDeviceVendorId field. */
			pTemplate->CaptureDeviceVendorID = pEnv->GetShortField(ttemplate, FieldID_IBMatcher_Template_captureDeviceVendorId);
			/* Get captureDeviceTypeId field. */
			pTemplate->CaptureDeviceTypeID   = pEnv->GetShortField(ttemplate, FieldID_IBMatcher_Template_captureDeviceTypeId);
			/* Get imageSamplingX field. */
			pTemplate->ImageSamplingX        = pEnv->GetShortField(ttemplate, FieldID_IBMatcher_Template_imageSamplingX);
			/* Get imageSamplingY field. */
			pTemplate->ImageSamplingY        = pEnv->GetShortField(ttemplate, FieldID_IBMatcher_Template_imageSamplingY);
			/* Get imageSizeX field. */
			pTemplate->ImageSizeX            = pEnv->GetShortField(ttemplate, FieldID_IBMatcher_Template_imageSizeX);
			/* Get imageSizeY field. */
			pTemplate->ImageSizeY            = pEnv->GetShortField(ttemplate, FieldID_IBMatcher_Template_imageSizeY);
			/* Get reserved field. */
			pTemplate->Reserved              = pEnv->GetIntField(ttemplate, FieldID_IBMatcher_Template_reserved);

			/* Get imageFormat field. */
			temp = pEnv->GetObjectField(ttemplate, FieldID_IBMatcher_Template_version);
			if (temp == NULL)
			{
		        LOG(("%s: unable to get version field\n", __FUNCTION__));
			}
			else
			{
				pTemplate->Version = (IBSM_TemplateVersion)pEnv->CallIntMethod(temp, MethodID_IBMatcher_TemplateVersion_toCode);

				/* Get impressionType field. */
				temp = pEnv->GetObjectField(ttemplate, FieldID_IBMatcher_Template_impressionType);
				if (temp == NULL)
				{
			        LOG(("%s: unable to get impressionType field\n", __FUNCTION__));
				}
				else
				{
					pTemplate->ImpressionType = (IBSM_ImpressionType)pEnv->CallIntMethod(temp, MethodID_IBCommon_ImpressionType_toCode);

					/* Get fingerPosition field. */
					temp = pEnv->GetObjectField(ttemplate, FieldID_IBMatcher_Template_fingerPosition);
					if (temp == NULL)
					{
				        LOG(("%s: unable to get fingerPosition field\n", __FUNCTION__));
					}
					else
					{
						pTemplate->FingerPosition = (IBSM_FingerPosition)pEnv->CallIntMethod(temp, MethodID_IBCommon_FingerPosition_toCode);

						/* Get captureDeviceTechId field. */
						temp = pEnv->GetObjectField(ttemplate, FieldID_IBMatcher_Template_captureDeviceTechId);
						if (temp == NULL)
						{
					        LOG(("%s: unable to get captureDeviceTechId field\n", __FUNCTION__));
						}
						else
						{
							pTemplate->CaptureDeviceTechID = (IBSM_CaptureDeviceTechID)pEnv->CallIntMethod(temp, MethodID_IBCommon_CaptureDeviceTechId_toCode);

							ok = TRUE;
						}
					}
				}
			}
		}
	}

	return (ok);
}
