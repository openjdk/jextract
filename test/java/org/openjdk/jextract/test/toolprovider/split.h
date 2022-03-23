#ifdef _WIN64
#define EXPORT __declspec(dllexport)
#else
#define EXPORT
#endif

EXPORT int a();
EXPORT int b();
EXPORT int c();
EXPORT int d();
EXPORT int e();
