#define NPY_NO_DEPRECATED_API NPY_1_7_API_VERSION
#include <stdio.h>
#include <string>
#include <time.h>
#include <Python.h>
#include <numpy/arrayobject.h>

typedef unsigned char BYTE;

using namespace std;

void Log(const char * format, ...) {
	char msg[2048];
	va_list argptr;
	va_start(argptr, format);
	vsprintf(msg, format, argptr);
	va_end(argptr);
	fprintf(stderr, "%s\n", msg);
}

double NDKGetTime() {	
	struct timespec res;	
	clock_gettime(CLOCK_REALTIME, &res);	
	double t = res.tv_sec + (double) res.tv_nsec / 1e9f;	
	return t;	
}	

class Inference {
private:
	PyObject *inf_instance, *empty_args;

	void print_error(char* var_name) {
		printf("%s returned NULL\n", var_name);
		printf("The error message is: %s\n", strerror(errno)); 
		PyErr_Print();
		exit(1);
	}

public:
	Inference() {
		// Initialize Python module and fetch Inference class
		char pythonpath[]="PYTHONPATH=second.pytorch/second/edge/"; 
		putenv(pythonpath);
		string pythonhome("PYTHONHOME=");
		pythonhome.append(getenv("HOME"));
		pythonhome.append("/anaconda3/envs/pointpillars");
		putenv((char *)pythonhome.c_str());

		char *env_val;
		env_val = getenv("PYTHONPATH");
		printf("%s\n", env_val);
		env_val = getenv("PYTHONHOME");
		printf("%s\n", env_val);

		printf("cpp - inference start\n");
		
		Py_Initialize();
		PyObject *pmod = PyImport_ImportModule("inference");
		if (!pmod) {
			print_error("pmod");
		}
		PyObject *inf_class = PyObject_GetAttrString(pmod, "Inference");
		if (!inf_class) {
			print_error("inf_class");
		}

		// Construct the Inference instance
		empty_args = Py_BuildValue("()");
		inf_instance = PyEval_CallObject(inf_class, empty_args);

		empty_args = Py_BuildValue("()");
		PyObject* read_config_func = PyObject_GetAttrString(inf_instance, "read_config");
		PyEval_CallObject(read_config_func, empty_args);
		empty_args = Py_BuildValue("()");
		PyObject* build_model_func = PyObject_GetAttrString(inf_instance, "build_model");
		PyEval_CallObject(build_model_func, empty_args);
	}

	void* import_array_wrapper() {
		import_array();
	}

	// void init() {
	// Call methods read_config and build_model
		
	// }

	BYTE* execute_model(float* point_cloud_buf, const long dims) {
		import_array_wrapper();
		BYTE *cstr;
		PyObject* execute_model_func = PyObject_GetAttrString(inf_instance, "execute_model");
		PyObject *point_cloud_array = PyArray_SimpleNewFromData(1, &dims, NPY_FLOAT, point_cloud_buf);
		PyObject *execute_args = PyTuple_New(1);
		PyTuple_SetItem(execute_args, 0, point_cloud_array);
		PyObject *func_call = PyEval_CallObject(execute_model_func, execute_args);
		PyArg_Parse(func_call, "s", &cstr);
		return cstr;
	}
};

int main() {

	FILE *fp;
	int32_t num = 600000;

	fp = fopen("test.bin","rb");
	float *buf1 = (float*)malloc(num*sizeof(float));
	int count1 = fread(buf1, sizeof(float), num, fp);
	fclose(fp);
	
	// PyObject *c, *pargs31, *pargs32, *pargs33;
	// npy_intp dims;

	Inference inf;
	BYTE *result;
	// inf.init();

	double t0 = NDKGetTime();

	result = inf.execute_model(buf1, count1);
	printf("%s\n", result);

	double t1 = NDKGetTime();

	printf("%.6f\n", t1-t0);

	return 0;
}
