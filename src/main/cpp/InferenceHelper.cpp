#include "InferenceHelper.h"

#include "pointpillars/inference.cpp"
#include "ConsoleLog.h"

#define NPY_NO_DEPRECATED_API NPY_1_7_API_VERSION
#include <Python.h>
#include <numpy/arrayobject.h>

#include <stdio.h>
#include <stdarg.h>
#include <time.h>
#include <dlfcn.h>

PyObject *pmod, *pclass, *pargs, *pinst, *pName;

using namespace std;

typedef unsigned char BYTE;

JNIEXPORT jlong JNICALL Java_org_emp_utils_InferenceHelper_createNativeObject
  (JNIEnv *env, jobject obj) {
    char *env_val;
    static void *handle = NULL;
    

    string path(getenv("HOME"));
    path.append("/anaconda3/envs/pointpillars/lib/libpython3.7m.so");
    handle = dlopen((char *)path.c_str(), RTLD_NOW | RTLD_GLOBAL);
    ConsoleLog("loaded python lib. %d", handle);
    ConsoleLog("%s", dlerror());

    jlong inference;
    ConsoleLog("helper-inference start");
    inference =(jlong) new Inference();
    ConsoleLog("helper-inference done");
    return inference;
}

static Inference *getObject(JNIEnv *env, jobject obj)
{
    jclass cls = env->GetObjectClass(obj);
    if (!cls)
        env->FatalError("GetObjectClass failed");

    jfieldID nativeInferenceID = env->GetFieldID(cls, "nativeInference", "J");
    if (!nativeInferenceID)
        env->FatalError("GetFieldID failed");

    jlong nativeInference = env->GetLongField(obj, nativeInferenceID);
    return reinterpret_cast<Inference *>(nativeInference);
}

JNIEXPORT jbyteArray JNICALL Java_org_emp_utils_InferenceHelper_executeModel
  (JNIEnv *env, jobject obj, jfloatArray data) {
  Inference *_self = getObject(env, obj);
  jsize size = env->GetArrayLength(data);
  jfloat *points = env->GetFloatArrayElements(data, 0);
  BYTE *result_str = _self->execute_model(points, size);

  int resultSize = strlen((char *)result_str);
  jbyteArray jresult = env->NewByteArray(resultSize);
  env->SetByteArrayRegion(jresult, 0, resultSize, reinterpret_cast<jbyte *>(result_str));
  ConsoleLog("helper-results generated");
  return jresult;
}
