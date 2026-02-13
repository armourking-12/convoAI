#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_duddleTech_convoAI_MainActivity_getApiKey(JNIEnv* env, jobject /* this */) {
    std::string api_key = "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";
    return env->NewStringUTF(api_key.c_str());
}