#include "DracoHelper.h"
#include "ConsoleLog.h"

#include "draco/point_cloud/point_cloud_builder.h"
#include "draco/compression/point_cloud/point_cloud_kd_tree_decoder.h"
#include "draco/compression/point_cloud/point_cloud_kd_tree_encoder.h"
#include "draco/compression/encode.h"

#include <stdio.h>
#include <stdarg.h>
#include <time.h>

using namespace std;
using namespace draco;

// To enable debugging messages, use the following macro
// #define _EMP_NATIVE_DEBUG
//#define TIME_BUFFER_SIZE 100
//#define LOG_BUFFER_SIZE 2048

char timeBuffer[TIME_BUFFER_SIZE];
char logBuffer[LOG_BUFFER_SIZE];

void ConsoleLog(const char* format, ...) {
#ifdef _EMP_NATIVE_DEBUG
  time_t now;
  time(&now);
  struct tm *localTime = localtime(&now);
  strftime(timeBuffer, TIME_BUFFER_SIZE, "%c", localTime);
  printf("[%s] ", timeBuffer);
  va_list args;
  va_start(args, format);
  vsprintf(logBuffer, format, args);
  va_end(args);
  printf("%s\n", logBuffer);
#endif // _EMP_NATIVE_DEBUG
}

JNIEXPORT jbyteArray JNICALL Java_org_emp_utils_DracoHelper_encode
  (JNIEnv *env, jobject obj, jfloatArray data, jint cl, jint qb) {
  // ConsoleLog("Encoding...");
  jsize size = env->GetArrayLength(data);
  jfloat *points = env->GetFloatArrayElements(data, 0);
  int numPoints = size / 4;
  ConsoleLog("Points: %d", numPoints);

  PointCloudBuilder builder;
  builder.Start(numPoints);
  const int pos_att_id = builder.AddAttribute(GeometryAttribute::POSITION, 3, DT_FLOAT32);
  const int intensity_att_id = builder.AddAttribute(GeometryAttribute::GENERIC, 1, DT_FLOAT32);

  float *px = points;
  float *py = points + 1;
  float *pz = points + 2;
  float *pi = points + 3;

  // ConsoleLog("cl: %d, qb: %d", cl, qb);

  for (int i = 0; i < numPoints; i++) {
    std::array<float, 3> point;
    point[0] = *px;
    point[1] = *py;
    point[2] = *pz;
    if (i < 4) {
      // ConsoleLog("Point %d: %f %f %f", i, point[0], point[1], point[2]);
    }
    builder.SetAttributeValueForPoint(pos_att_id, PointIndex(i), &(point)[0]);

    std::array<float, 1> intensity;
    intensity[0] = *pi;
    builder.SetAttributeValueForPoint(intensity_att_id, PointIndex(i), &(intensity)[0]);

    px += 4;
    py += 4;
    pz += 4;
    pi += 4;
  }
  std::unique_ptr<PointCloud> pointCloud = builder.Finalize(false);

  EncoderBuffer encoderBuffer;
  PointCloudKdTreeEncoder encoder;
  EncoderOptions options = EncoderOptions::CreateDefaultOptions();
  options.SetGlobalInt("quantization_bits", qb);
  options.SetSpeed(10 - cl, 10 - cl);
  encoder.SetPointCloud(*pointCloud);

  bool status = encoder.Encode(options, &encoderBuffer).ok();
  // ConsoleLog("Encode status: %d", status);

  int resultSize = (int) encoderBuffer.size();
  ConsoleLog("Encode bytes: %d", resultSize);
  jbyteArray result = env->NewByteArray(resultSize);
  env->SetByteArrayRegion(
    result, 0, resultSize, reinterpret_cast<jbyte*>((signed char*)encoderBuffer.data()));
  // ConsoleLog("Encoding finished.");
  return result;
}

JNIEXPORT jfloatArray JNICALL Java_org_emp_utils_DracoHelper_decode
  (JNIEnv *env, jobject obj, jbyteArray data) {
  int int_random = rand();
  ConsoleLog("Decoding... (%d)", int_random);
  jsize size = env->GetArrayLength(data);
  ConsoleLog("Decode bytes (%d): %d", int_random, size);
  char* buffer = new char[size];
  env->GetByteArrayRegion(data, 0, size, reinterpret_cast<jbyte*>(buffer));

  DecoderBuffer decoderBuffer;
  decoderBuffer.Init(buffer, size);

  DecoderOptions decoderOptions;
  PointCloudKdTreeDecoder decoder;
  std::unique_ptr<PointCloud> pointCloud(new PointCloud());


  bool status = decoder.Decode(decoderOptions, &decoderBuffer, pointCloud.get()).ok();
  ConsoleLog("Decode status (%d): %d", int_random, status);

  int numPoints = pointCloud->num_points();
  ConsoleLog("Points (%d): %d", int_random, numPoints);

  float *points = (float *)malloc(numPoints * 4 * sizeof(float));

  float *px = points;
  float *py = points + 1;
  float *pz = points + 2;
  float *pi = points + 3;

  std::array<float, 3> positionArray;
  std::array<float, 1> intensityArray;

  PointCloudBuilder builder;
  builder.Start(numPoints);
  const int pos_att_id = builder.AddAttribute(GeometryAttribute::POSITION, 3, DT_FLOAT32);
  const int intensity_att_id = builder.AddAttribute(GeometryAttribute::GENERIC, 1, DT_FLOAT32);

  GeometryAttribute *pos = pointCloud->attribute(pos_att_id);
  GeometryAttribute *intensity = pointCloud->attribute(intensity_att_id);

  for (int i = 0; i < numPoints; i++) {
    pos->GetValue(AttributeValueIndex(i), &positionArray);
    intensity->GetValue(AttributeValueIndex(i), &intensityArray);

    *px = positionArray[0];
    *py = positionArray[1];
    *pz = positionArray[2];
    *pi = intensityArray[0];

    px += 4;
    py += 4;
    pz += 4;
    pi += 4;
  }

  // ConsoleLog("Points array construction done.");
  jfloatArray result;
  result = env->NewFloatArray(numPoints * 4);
  env->SetFloatArrayRegion(result, 0, numPoints * 4, points);
  free(points);
  ConsoleLog("Decoding finished. (%d)", int_random);
  return result;
}

