#include <stdio.h>


// To enable debugging messages, use the following macro
// #define _EMP_NATIVE_DEBUG
#define TIME_BUFFER_SIZE 100
#define LOG_BUFFER_SIZE 2048

extern char timeBuffer[TIME_BUFFER_SIZE];
extern char logBuffer[LOG_BUFFER_SIZE];

void ConsoleLog(const char* format, ...);