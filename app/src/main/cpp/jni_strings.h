#pragma once

#include <jni.h>

#include <string>

std::string toUtf8(JNIEnv* env, jstring value);

jstring toJString(JNIEnv* env, const std::string& utf8);
