#include "Record.h"
#include "Index.h"
#include "com_fourcasters_forec_reconciler_query_history_HistoryDAO.h"
#include <iostream>
#include <fstream>
#include <string>
#include <map>
#include <unordered_map>

using namespace std;

unordered_map<string, Index> indexSet;

int read(const string& filename, map<unsigned long, size_t> &indexes) {
    ifstream filestream(filename);
    string line;
    Record *prev = new Record();
    Record *curr = NULL;
    size_t currPointer = 0;
    while (getline(filestream, line)) {
        curr = new Record(line); //parse
        size_t length = line.length() + 1;
        currPointer += length;
        if (curr->isOnDifferentHour(*prev)) { //date change, we put an index point at every day change
            size_t offset = currPointer - length;
            indexes[curr->gettimestamp()] = offset;
        }
        delete prev;
        prev = curr;
    }
    delete curr;
    return 0;
}

Index dbhash(const string &filename) {
    map<unsigned long, size_t> offsets;
    int res = read(filename, offsets);
    Index idx(filename, offsets);
    return idx;
}

/*
 * Class:     com_fourcasters_forec_reconciler_query_history_HistoryDAO
 * Method:    dbhash
 * Signature: ([C)Z
 */
jboolean JNICALL Java_com_fourcasters_forec_reconciler_query_history_HistoryDAO_dbhash
  (JNIEnv *env, jobject, jbyteArray bytes) {
    cout << "JNI hooked up\n";
    jbyte* byteArray = (*env).GetByteArrayElements(bytes, JNI_FALSE);
    cout << byteArray << "\n";
    size_t length = env->GetArrayLength(bytes);
    string s(reinterpret_cast<char*>(byteArray), length);
    cout << s << "\n";
    indexSet[s] = dbhash(s);
    cout << "Size of the history just indexed: " << indexSet[s].size() << "\n";
    return JNI_TRUE; //has to change t
}


JNIEXPORT jboolean JNICALL Java_com_fourcasters_forec_reconciler_query_history_HistoryDAO_init
  (JNIEnv *, jobject) {
    cout << indexSet.size() << endl;
    return indexSet.size();
}


/*
 * Class:     com_fourcasters_forec_reconciler_query_history_HistoryDAO
 * Method:    offset
 * Signature: ([CIIIII)J
 */
jlong JNICALL Java_com_fourcasters_forec_reconciler_query_history_HistoryDAO_offset
  (JNIEnv *env, jobject, jbyteArray, jint, jint, jint, jint, jint) {
    cout << "JNI hooked up\n";
    return 0;
}