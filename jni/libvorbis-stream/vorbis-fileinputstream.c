/**
 * 	A java interface (a la inputstream) to read ogg vorbis files.
 * 	http://svn.xiph.org/trunk/vorbis/examples/vorbisfile_example.c
 */

#include <jni.h>
#include <stdlib.h>
#include <stdio.h>
#include <time.h>
#include <math.h>

#include <vorbis/vorbisfile.h>
#include <stream/util.h>

/* This is arbitrary, If you don't like it, change it */
#define MAX_INPUTSTREAMS 8

struct input_stream {
	FILE * 				fh;
	OggVorbis_File 		vf;
	int					section;
	int					length;
};
static struct input_stream input_streams[MAX_INPUTSTREAMS];

jint Java_org_xiph_vorbis_stream_VorbisFileInputStream_create(
		JNIEnv* env,
		jobject this,
		jstring path,
		jobject info
		)
{
	int ret;	/* Debugging variable */
	jfieldID channels_field, sample_rate_field, length_field;	/* JNI field ID */
	jclass cls = (*env)->GetObjectClass(env, info);
	int stream_idx;
	struct input_stream * iptr;
	vorbis_info * vi;

	/* Find an unused input_stream */
	for (stream_idx = 0; stream_idx < MAX_INPUTSTREAMS; stream_idx++) {
		if (input_streams[stream_idx].fh == NULL) {
			const jbyte * pchars = (*env)->GetStringUTFChars(env, path, NULL);
			if (pchars == NULL) {
				/* Exception Already thrown */
				return;
			}
			/* We found one! */
			iptr = &input_streams[stream_idx];
			iptr->fh = fopen(pchars, "r");
			if (iptr->fh == NULL) {
				JNU_ThrowByName(env, "java/io/IOException", "Error Creating File Handle", 0);
				return;
			}
			(*env)->ReleaseStringUTFChars(env, path, pchars);
			break;
		}
	}

	if (stream_idx == MAX_INPUTSTREAMS) {
		JNU_ThrowByName(env, "java/io/IOException",
				"Too Many Vorbis InputStreams", stream_idx);
		return;
	}

	/* Open the stream */
	ret = ov_open(iptr->fh, &iptr->vf, NULL, 0);
	if (ret < 0) {
		JNU_ThrowByName(env, "java/io/IOException",
				"Vorbis File Corrupt", ret);
		fclose(iptr->fh);
		iptr->fh = NULL;
		return;
	}

	channels_field = (*env)->GetFieldID(env, cls, "channels", "I");
	sample_rate_field = (*env)->GetFieldID(env, cls, "sampleRate", "I");
	length_field = (*env)->GetFieldID(env, cls, "length", "J");
	if (channels_field == NULL || sample_rate_field == NULL) {
		JNU_ThrowByName(env, "java/lang/Exception",
				"Native Field Misnamed", 0);
		ov_clear(&iptr->vf);
		fclose(iptr->fh);
		iptr->fh = NULL;
		return;
	}

	vi = ov_info(&iptr->vf, -1);

	iptr->section = 0;
	iptr->length = ov_pcm_total(&iptr->vf, -1);

	/* Populate basic stream info into the VorbisInfo object. */
	(*env)->SetIntField(env, info, channels_field, vi->channels);
	(*env)->SetIntField(env, info, sample_rate_field, vi->rate);
	(*env)->SetLongField(env, info, length_field, iptr->length);


	return stream_idx;
}

jint Java_org_xiph_vorbis_stream_VorbisFileInputStream_readStreamIdx(
		JNIEnv* 	env,
		jobject 	this,
		jint		sidx,
		jshortArray pcm,
		jint 		offset,
		jint 		length
		)
{
	long ret;
	struct input_stream * iptr = &input_streams[sidx];

	jshort * pcmShorts 			= (*env)->GetShortArrayElements(env, pcm, NULL);
	int maxLength				= (*env)->GetArrayLength(env,pcm);

	/* Do the battery of validation checks. */
	if (offset + length > maxLength) {
		JNU_ThrowByName(env, "java/lang/ArrayIndexOutOfBoundsException",
				"No data was written to the buffer",
				offset + length - 1);
		return;
	}

	if (sidx >= MAX_INPUTSTREAMS || sidx < 0 || iptr->fh == NULL) {
		JNU_ThrowByName(env, "java/io/IOException", "Invalid Stream Index", sidx);
		return;
	}

	if (length > 0) {
		ret = ov_read(&iptr->vf, (char *)(pcmShorts + offset), length, 0, 2, 1, &iptr->section);
		/* -1 is EOF */
		if (ret == 0) {
			ret = -1;
		}
		else if (ret < 0) {
			if (ret == OV_EBADLINK) {
				JNU_ThrowByName(env, "java/io/IOException", "Corrupt bitstream section!", iptr->section);
				return;
			}
		}
	}
	else {
		ret = 0;
	}
	/* Apparently sample rates can change inside the stream... We may need to account for that. */

	(*env)->ReleaseShortArrayElements(env, pcm, pcmShorts, 0);
	return ret >> 1;
}

jlong Java_org_xiph_vorbis_stream_VorbisFileInputStream_skipStreamIdx(
		JNIEnv* 	env,
		jobject 	this,
		jint		sidx,
		jlong 		offset
		)
{
	struct input_stream * iptr = &input_streams[sidx];
	long ret;
	if (sidx >= MAX_INPUTSTREAMS || sidx < 0 || iptr->fh == NULL) {
		JNU_ThrowByName(env, "java/io/IOException", "Invalid Stream Index", sidx);
		return;
	}

	ret = ov_pcm_seek_lap(&iptr->vf, offset);

	if (ret == OV_EREAD) {
		JNU_ThrowByName(env, "java/io/IOException", "Read ERROR", ret);
		return;
	}
	else if (ret != 0){
		JNU_ThrowByName(env, "java/io/IOException", "Vorbis Seek Error code: ", ret);
		return;
	}

	return ret;

}

void Java_org_xiph_vorbis_stream_VorbisFileInputStream_closeStreamIdx(
		JNIEnv* 	env,
		jobject 	this,
		jint		sidx
		)
{
	struct input_stream * iptr = &input_streams[sidx];
	if (sidx >= MAX_INPUTSTREAMS || sidx < 0 || iptr->fh == NULL) {
		JNU_ThrowByName(env, "java/io/IOException", "Invalid Stream Index", sidx);
		return;
	}
	ov_clear(&iptr->vf);
	fclose(iptr->fh);
	iptr->fh = NULL;
}



