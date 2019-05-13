/* Programmer: Nicholas Wertzberger
 * 		Email: wertnick@gmail.com
 *
 */

#include <jni.h>
#include <stdlib.h>
#include <stdio.h>
#include <stream/util.h>

/*
 * http://java.sun.com/docs/books/jni/html/exceptions.html
 */
void
JNU_ThrowByName(JNIEnv *env, const char *name, const char *msg, const int code)
{
	char buf [45];

	sprintf(buf, "%35s: %d", msg, code);

    jclass cls = (*env)->FindClass(env, name);
    /* if cls is NULL, an exception has already been thrown */
    if (cls != NULL) {
        (*env)->ThrowNew(env, cls, buf);
    }
    /* free the local ref */
    (*env)->DeleteLocalRef(env, cls);
}
