/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_fourcasters_forec_reconciler_query_history_HistoryDAO */

#ifndef _Included_com_fourcasters_forec_reconciler_query_history_HistoryDAO
#define _Included_com_fourcasters_forec_reconciler_query_history_HistoryDAO
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_fourcasters_forec_reconciler_query_history_HistoryDAO
 * Method:    dbhash
 * Signature: ([C)Z
 */
JNIEXPORT jboolean JNICALL Java_com_fourcasters_forec_reconciler_query_history_HistoryDAO_dbhash
  (JNIEnv *, jobject, jbyteArray);

/*
 * Class:     com_fourcasters_forec_reconciler_query_history_HistoryDAO
 * Method:    offset
 * Signature: ([CIIIII)J
 */
JNIEXPORT jlong JNICALL Java_com_fourcasters_forec_reconciler_query_history_HistoryDAO_offset
  (JNIEnv *, jobject, jbyteArray, jint, jint, jint, jint, jint);

JNIEXPORT jboolean JNICALL Java_com_fourcasters_forec_reconciler_query_history_HistoryDAO_init
  (JNIEnv *, jobject);


#ifdef __cplusplus
}
#endif
#endif