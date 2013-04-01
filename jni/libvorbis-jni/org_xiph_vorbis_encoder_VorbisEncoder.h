#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <math.h>
#include <vorbis/vorbisenc.h>
#include <android/log.h>

#ifndef _Included_org_xiph_vorbis_encoder_VorbisEncoder
#define _Included_org_xiph_vorbis_encoder_VorbisEncoder
#ifdef __cplusplus
extern "C" {
#endif

//Throws an exception to the java layer with th specified error code and stops the encode feed
void throwEncodeException(JNIEnv *env, const int code, jobject* vorbisDataFeed, jmethodID* stopMethodId);

//Starts the encode feed
void startEncodeFeed(JNIEnv *env, jobject *vorbisDataFeed, jmethodID* startMethodId);

//Stops the vorbis data feed
void stopEncodeFeed(JNIEnv *env, jobject* vorbisDataFeed, jmethodID* stopMethodId);

//Reads pcm data from the jni callback
long readPCMDataFromEncoderDataFeed(JNIEnv *env, jobject* encoderDataFeed, jmethodID* readPCMDataMethodId, char* buffer, int length);

//Writes the vorbis data to the Java layer
int writeVorbisDataToEncoderDataFeed(JNIEnv *env, jobject* encoderDataFeed, jmethodID* writeVorbisDataMethodId, char* buffer, int bytes);

JNIEXPORT int JNICALL Java_org_xiph_vorbis_encoder_VorbisEncoder_startEncoding
(JNIEnv *env, jclass cls, jlong sampleRate, jlong channels, jfloat quality, jobject encoderDataFeed);

#ifdef __cplusplus
}
#endif
#endif