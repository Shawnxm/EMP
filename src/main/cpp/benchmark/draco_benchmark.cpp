#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <dirent.h>

#include <vector>
#include <string>

#include <iostream>     // std::cout
#include <fstream>      // std::ifstream

#include "draco/point_cloud/point_cloud_builder.h"
#include "draco/compression/point_cloud/point_cloud_kd_tree_decoder.h"
#include "draco/compression/point_cloud/point_cloud_kd_tree_encoder.h"
#include "draco/compression/encode.h"

using namespace std;
using namespace draco;

#define MAX_LINE_SIZE 4096

struct Statistics{
    int count;
    double compression_ratio[2000];
    double cr_avg;
    double cr_stddev;
    double encoding_time[2000];
    double et_avg;
    double et_stddev;
    double decoding_time[2000];
    double dt_avg;
    double dt_stddev;
};

struct Statistics draco_statistics;

inline void MyAssert(int x, int id) {
	if (!x) {
		printf("Assertion failed. ID=%d\n", id);
		exit(0);
	}
}

double NDKGetTime() {
    struct timespec res;
    clock_gettime(CLOCK_REALTIME, &res);
    double t = res.tv_sec + (double) res.tv_nsec / 1e9f;
    return t;
}

void compression_demo(char* inputfile, int cl, int qb, char* outputfile){
    // allocate 4 MB buffer (only ~130*4*4 KB are needed)
    int32_t num = 1000000;
    float *data = (float*)malloc(num*sizeof(float));

    // pointers
    float *px = data+0;
    float *py = data+1;
    float *pz = data+2;
    float *pr = data+3;

    // load point cloud
    FILE *stream;
    stream = fopen (inputfile,"rb");
    num = fread(data,sizeof(float),num,stream)/4;

    // point cloud builder
    PointCloudBuilder builder;
	builder.Start(num);
	const int pos_att_id = builder.AddAttribute(GeometryAttribute::POSITION, 3, DT_FLOAT32);
	const int intensity_att_id = builder.AddAttribute(GeometryAttribute::GENERIC, 1, DT_FLOAT32);
    for (int index = 0; index < num; index++) {
		std::array<float, 3> point;
		point[0] = *px;
		point[1] = *py;
		point[2] = *pz;
		builder.SetAttributeValueForPoint(pos_att_id, PointIndex(index), &(point)[0]);

		std::array<float, 1> intensity;
		intensity[0] = *pr;
        // std::cout << intensity[0] << std::endl;
		builder.SetAttributeValueForPoint(intensity_att_id, PointIndex(index), &(intensity)[0]);

        if (index < 5) {
          printf("Point %d: %f %f %f %.2f\n", index, *px, *py, *pz, *pr);
        }
        // std::cout << *px << " " << *py << " " << *pz << " " << *pr << std::endl;

        px+=4; py+=4; pz+=4; pr+=4;
	}
	std::unique_ptr<PointCloud> pc = builder.Finalize(false);

    fclose(stream);

    // encode/decode using kd-tree
    int compression_level = cl;
	EncoderBuffer buffer;
	PointCloudKdTreeEncoder encoder;
	EncoderOptions options = EncoderOptions::CreateDefaultOptions();
	options.SetGlobalInt("quantization_bits", qb);
	options.SetSpeed(10 - compression_level, 10 - compression_level);
	encoder.SetPointCloud(*pc);
	double t1 = NDKGetTime();
	MyAssert(encoder.Encode(options, &buffer).ok(), 2001);
	double t2 = NDKGetTime();

    draco_statistics.compression_ratio[draco_statistics.count] = buffer.size()/(num * (12.0f + 4.0f));
    draco_statistics.cr_avg += buffer.size()/(num * (12.0f + 4.0f));
    draco_statistics.encoding_time[draco_statistics.count] = (t2 - t1);
    draco_statistics.et_avg += (t2 - t1);

    printf("%s\n", inputfile);
    printf("Compression level: %d\n", compression_level);
	printf("KD-Tree Compression ratio: %.6f \
        \noriginal bytes: %d \
        \ncompressioned bytes: %d \
        \nKD-tree Encoding time: %.6fs\n", \
        buffer.size()/(num * (12.0f + 4.0f)), num * (12 + 4), (int)buffer.size(), t2 - t1);
      
	DecoderBuffer dec_buffer;
	dec_buffer.Init(buffer.data(), buffer.size());
	PointCloudKdTreeDecoder decoder;

	std::unique_ptr<PointCloud> out_pc(new PointCloud());
	DecoderOptions dec_options;
	double t3 = NDKGetTime();
	MyAssert(decoder.Decode(dec_options, &dec_buffer, out_pc.get()).ok(), 2002);
	double t4 = NDKGetTime();

    draco_statistics.decoding_time[draco_statistics.count] = (t4 - t3);
    draco_statistics.dt_avg += (t4 - t3);

    printf("KD-Tree Decoding time: %.6fs\n\n", t4 - t3);

    draco_statistics.count += 1;

    // save back to binary
    float *data_out = (float*)malloc(num*4*sizeof(float));

    px = data_out+0;
    py = data_out+1;
    pz = data_out+2;
    pr = data_out+3;

    std::array<float, 3> position_array;
    std::array<float, 1> intensity_array;

    GeometryAttribute *pos = out_pc->attribute(pos_att_id);
    GeometryAttribute *intensity = out_pc->attribute(intensity_att_id);
    for (int index = 0; index < num; index++) {
        pos->GetValue(AttributeValueIndex(index), &position_array);
        intensity->GetValue(AttributeValueIndex(index), &intensity_array);

        *px = position_array[0];
        *py = position_array[1];
        *pz = position_array[2];
        *pr = intensity_array[0];

        px+=4; py+=4; pz+=4; pr+=4;
    }

    FILE *stream_out;
    char save_path[256];
    stream_out = fopen(outputfile, "wb");
    fwrite(data_out, sizeof(float), num*4, stream_out);
    fclose(stream_out);
}

int main(int argc, char** argv) {
	if (argc != 3) {
		printf("Usage: %s cl(compression level:0-10) qb(quantization bits:1-31)\n", argv[0]);
		return -1;
	}
    int cl = atoi(argv[1]);
    int qb = atoi(argv[2]);

    draco_statistics.count = 0;
    draco_statistics.cr_avg = 0;
    draco_statistics.cr_stddev = 0;
    draco_statistics.et_avg = 0;
    draco_statistics.et_stddev = 0;
    draco_statistics.dt_avg = 0;
    draco_statistics.dt_stddev = 0;

    // 2011_09_26_drive_0001_sync
    for(int frame_index = 0; frame_index < 108; frame_index++){
        char inputfile[256];
        sprintf(inputfile, "../../../test/resources/benchmark_data/2011_09_26/2011_09_26_drive_0001_sync/velodyne_points/data/%010d.bin", frame_index);
        char outputfile[256];
        sprintf(outputfile, "../../../test/resources/benchmark_data/2011_09_26/2011_09_26_drive_0001_sync/velodyne_points/processed/%010d.bin", frame_index);
        compression_demo(inputfile, cl, qb, outputfile);
    }

    // 2011_09_26_drive_0002_sync
    for(int frame_index = 0; frame_index < 77; frame_index++){
        char inputfile[256];
        sprintf(inputfile, "../../../test/resources/benchmark_data/2011_09_26/2011_09_26_drive_0002_sync/velodyne_points/data/%010d.bin", frame_index);
        char outputfile[256];
        sprintf(outputfile, "../../../test/resources/benchmark_data/2011_09_26/2011_09_26_drive_0002_sync/velodyne_points/processed/%010d.bin", frame_index);
        compression_demo(inputfile, cl, qb, outputfile);
    }

    // 2011_09_26_drive_0005_sync
    for(int frame_index = 0; frame_index < 154; frame_index++){
        char inputfile[256];
        sprintf(inputfile, "../../../test/resources/benchmark_data/2011_09_26/2011_09_26_drive_0005_sync/velodyne_points/data/%010d.bin", frame_index);
        char outputfile[256];
        sprintf(outputfile, "../../../test/resources/benchmark_data/2011_09_26/2011_09_26_drive_0005_sync/velodyne_points/processed/%010d.bin", frame_index);
        compression_demo(inputfile, cl, qb, outputfile);
    }

    // 2011_09_26_drive_0009_sync
    for(int frame_index = 0; frame_index < 447; frame_index++){
        char inputfile[256];
        if(frame_index >= 177 && frame_index <= 180){
            continue;
        }
        sprintf(inputfile, "../../../test/resources/benchmark_data/2011_09_26/2011_09_26_drive_0009_sync/velodyne_points/data/%010d.bin", frame_index);
        char outputfile[256];
        sprintf(outputfile, "../../../test/resources/benchmark_data/2011_09_26/2011_09_26_drive_0009_sync/velodyne_points/processed/%010d.bin", frame_index);
        compression_demo(inputfile, cl, qb, outputfile);
    }

    // 2011_09_26_drive_0011_sync
    for(int frame_index = 0; frame_index < 233; frame_index++){
        char inputfile[256];
        sprintf(inputfile, "../../../test/resources/benchmark_data/2011_09_26/2011_09_26_drive_0011_sync/velodyne_points/data/%010d.bin", frame_index);
        char outputfile[256];
        sprintf(outputfile, "../../../test/resources/benchmark_data/2011_09_26/2011_09_26_drive_0011_sync/velodyne_points/processed/%010d.bin", frame_index);
        compression_demo(inputfile, cl, qb, outputfile);
    }

    // calculate average
    draco_statistics.cr_avg /= draco_statistics.count;
    draco_statistics.et_avg /= draco_statistics.count;
    draco_statistics.dt_avg /= draco_statistics.count;

    // calculate stddev
    for(int i = 0; i < draco_statistics.count; i++){
        draco_statistics.cr_stddev += (draco_statistics.compression_ratio[i] - draco_statistics.cr_avg) * (draco_statistics.compression_ratio[i] - draco_statistics.cr_avg);
        draco_statistics.et_stddev += (draco_statistics.encoding_time[i] - draco_statistics.et_avg) * (draco_statistics.encoding_time[i] - draco_statistics.et_avg);
        draco_statistics.dt_stddev += (draco_statistics.decoding_time[i] - draco_statistics.dt_avg) * (draco_statistics.decoding_time[i] - draco_statistics.dt_avg);
    }
    draco_statistics.cr_stddev = sqrt(draco_statistics.cr_stddev / draco_statistics.count);
    draco_statistics.et_stddev = sqrt(draco_statistics.et_stddev / draco_statistics.count);
    draco_statistics.dt_stddev = sqrt(draco_statistics.dt_stddev / draco_statistics.count);

	printf("Total Count: %d \
        \nKD-Tree Avg. Compression ratio: %.6f, stddev: %.6f \
        \nKD-tree Avg. Encoding time: %.6fs, stddev: %.6f \
        \nKD-tree Avg. Decoding time: %.6fs, stddev: %.6f\n", \
        draco_statistics.count, \
        draco_statistics.cr_avg, draco_statistics.cr_stddev, \
        draco_statistics.et_avg, draco_statistics.et_stddev, \
        draco_statistics.dt_avg, draco_statistics.dt_stddev);

	return 0;
}
