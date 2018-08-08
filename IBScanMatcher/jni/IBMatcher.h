/* *************************************************************************************************
 * IBMatcher.h
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

#ifndef IBMATCHER_H_
#define IBMATCHER_H_

#ifdef __cplusplus
extern "C" {
#endif

/***************************************************************************************************
 * GLOBAL MACROS
 **************************************************************************************************/

#define IBMATCHER_BUILD_JNI_FNCT(name) Java_com_integratedbiometrics_ibscanmatcher_IBMatcher_##name

/***************************************************************************************************
 * GLOBAL FUNCTION PROTOTYPES
 **************************************************************************************************/

/*
 * Handle load of library.
 */
jboolean IBMatcher_OnLoad(
    JavaVM *vm,
    void   *reserved);

/*
 * Handle load of library.
 */
JNIEXPORT jint JNICALL JNI_OnLoad(
    JavaVM *vm,
    void   *reserved);

/***************************************************************************************************
 * JNI FUNCTION PROTOTYPES
 **************************************************************************************************/

/*
 * private native int initNative();
 */
JNIEXPORT jint JNICALL IBMATCHER_BUILD_JNI_FNCT(initNative)(
    JNIEnv  *pEnv,
	jobject  tthis);

/*
 * private native SdkVersion getSdkVersionNative(NativeError error);
 */
JNIEXPORT jobject JNICALL IBMATCHER_BUILD_JNI_FNCT(getSdkVersionNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jobject  error);

/*
 * private native Template extractTemplateNative(ImageDataExt imageDataExt, NativeError error);
 */
JNIEXPORT jobject JNICALL IBMATCHER_BUILD_JNI_FNCT(extractTemplateNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jobject  imageDataExt,
	jobject  error);

/*
 * private native ImageDataExt compressImageNative(ImageDataExt imageDataExt, int imageFormatCode,
 *     NativeError error);
 */
JNIEXPORT jobject JNICALL IBMATCHER_BUILD_JNI_FNCT(compressImageNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jobject  imageDataExt,
	jint     imageFormatCode,
	jobject  error);

/*
 * private native ImageDataExt decompressImageNative(ImageDataExt imageDataExt, NativeError error);
 */
JNIEXPORT jobject JNICALL IBMATCHER_BUILD_JNI_FNCT(decompressImageNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jobject  imageDataExt,
	jobject  error);

/*
 * private native boolean saveImageNative(ImageDataExt imageDataExt, String filePath,
 *		NativeError error);
 */
JNIEXPORT jboolean JNICALL IBMATCHER_BUILD_JNI_FNCT(saveImageNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jobject  imageDataExt,
	jstring  filePath,
	jobject  error);

/*
 * private native ImageDataExt loadImageNative(String filePath, NativeError error);
 */
JNIEXPORT jobject JNICALL IBMATCHER_BUILD_JNI_FNCT(loadImageNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jstring  filePath,
	jobject  error);

/*
 * private native boolean saveImageAsFirNative(ImageDataExt imageDataExt, String filePath,
 *		NativeError error);
 */
JNIEXPORT jboolean JNICALL IBMATCHER_BUILD_JNI_FNCT(saveImageAsFirNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jobject  imageDataExt,
	jstring  filePath,
	jobject  error);

/*
 * private native ImageDataExt loadImageFromFirNative(String filePath, NativeError error);
 */
JNIEXPORT jobject JNICALL IBMATCHER_BUILD_JNI_FNCT(loadImageFromFirNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jstring  filePath,
	jobject  error);

/*
 * private native boolean saveTemplateNative(Template template, String filePath, NativeError error);
 */
JNIEXPORT jboolean JNICALL IBMATCHER_BUILD_JNI_FNCT(saveTemplateNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jobject  ttemplate,
	jstring  filePath,
	jobject  error);

/*
 * private native Template loadTemplateNative(String filePath, NativeError error);
 */
JNIEXPORT jobject JNICALL IBMATCHER_BUILD_JNI_FNCT(loadTemplateNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jstring  filePath,
	jobject  error);

/*
 * private native boolean saveTemplateAsFmrNative(Template template, String filePath,
 *		NativeError error);
 */
JNIEXPORT jboolean JNICALL IBMATCHER_BUILD_JNI_FNCT(saveTemplateAsFmrNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jobject  ttemplate,
	jstring  filePath,
	jobject  error);

/*
 * private native Template loadTemplateFromFmrNative(String filePath, NativeError error);
 */
JNIEXPORT jobject JNICALL IBMATCHER_BUILD_JNI_FNCT(loadTemplateFromFmrNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jstring  filePath,
	jobject  error);

/*
 * private native int matchTemplatesNative(Template template1, Template template2,
 * 		NativeError error);
 */
JNIEXPORT jint JNICALL IBMATCHER_BUILD_JNI_FNCT(matchTemplatesNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jobject  template1,
	jobject  template2,
	jobject  error);

/*
 * private native void setMatchingLevelNative(int matchingLevel, NativeError error);
 */
JNIEXPORT void JNICALL IBMATCHER_BUILD_JNI_FNCT(setMatchingLevelNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jint     matchingLevel,
	jobject  error);

/*
 * private native int getMatchingLevelNative(NativeError error);
 */
JNIEXPORT jint JNICALL IBMATCHER_BUILD_JNI_FNCT(getMatchingLevelNative)(
    JNIEnv  *pEnv,
	jobject  tthis,
	jobject  error);

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
	jobject  error);

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
	jobject  error);

#ifdef __cplusplus
} //extern "C" {
#endif

#endif /* IBSCANDEVICE_H_ */
