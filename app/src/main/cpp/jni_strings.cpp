#include "jni_strings.h"

#include <cstdint>
#include <vector>

std::string toUtf8(JNIEnv* env, jstring value) {
    const jsize length = env->GetStringLength(value);
    const jchar* chars = env->GetStringChars(value, nullptr);
    std::string out;
    for (jsize i = 0; i < length; i++) {
        uint32_t cp = chars[i];
        if (cp >= 0xD800 && cp < 0xDC00 && i + 1 < length) {
            cp = 0x10000 + ((cp - 0xD800) << 10) + (chars[++i] - 0xDC00);
        }
        if (cp < 0x80) {
            out += (char)cp;
        } else if (cp < 0x800) {
            out += (char)(0xC0 | (cp >> 6));
            out += (char)(0x80 | (cp & 0x3F));
        } else if (cp < 0x10000) {
            out += (char)(0xE0 | (cp >> 12));
            out += (char)(0x80 | ((cp >> 6) & 0x3F));
            out += (char)(0x80 | (cp & 0x3F));
        } else {
            out += (char)(0xF0 | (cp >> 18));
            out += (char)(0x80 | ((cp >> 12) & 0x3F));
            out += (char)(0x80 | ((cp >> 6) & 0x3F));
            out += (char)(0x80 | (cp & 0x3F));
        }
    }
    env->ReleaseStringChars(value, chars);
    return out;
}

jstring toJString(JNIEnv* env, const std::string& utf8) {
    std::vector<jchar> units;
    units.reserve(utf8.size());
    size_t i = 0;
    while (i < utf8.size()) {
        const uint8_t b = (uint8_t)utf8[i];
        uint32_t cp;
        int extra;
        if (b < 0x80) {
            cp = b;
            extra = 0;
        } else if ((b >> 5) == 0x6) {
            cp = b & 0x1F;
            extra = 1;
        } else if ((b >> 4) == 0xE) {
            cp = b & 0x0F;
            extra = 2;
        } else {
            cp = b & 0x07;
            extra = 3;
        }
        i++;
        for (int k = 0; k < extra && i < utf8.size(); k++, i++) cp = (cp << 6) | ((uint8_t)utf8[i] & 0x3F);
        if (cp >= 0x10000) {
            cp -= 0x10000;
            units.push_back((jchar)(0xD800 + (cp >> 10)));
            units.push_back((jchar)(0xDC00 + (cp & 0x3FF)));
        } else {
            units.push_back((jchar)cp);
        }
    }
    return env->NewString(units.data(), (jsize)units.size());
}
