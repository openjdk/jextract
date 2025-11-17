cmake_minimum_required(VERSION 3.14)
project(TestSupportLibs)

option(TEST_SOURCE_ROOT "src directory with test files")
# Force XL C on AIX
if(${CMAKE_SYSTEM_NAME} MATCHES "AIX")
    set(CMAKE_C_COMPILER /opt/IBM/xlC/13.1.3/bin/xlc)
    set(CMAKE_CXX_COMPILER /opt/IBM/xlC/13.1.3/bin/xlC)
endif()
file(GLOB_RECURSE TEST_LIBS ${TEST_SOURCE_ROOT}/*lib*.c)

foreach(TEST_LIB ${TEST_LIBS})
    message(STATUS "Processing test lib: ${TEST_LIB}")

    string(REGEX REPLACE ".*lib([A-Za-z0-9_]+)\.c(pp)?" "\\1" LIB_NAME ${TEST_LIB})
    string(REGEX REPLACE "(.*)/lib[A-Za-z0-9_]+\.c(pp)?" "\\1" PARENT_DIR ${TEST_LIB})
    message(STATUS "Lib name: ${LIB_NAME}")
    if(${CMAKE_SYSTEM_NAME} MATCHES "AIX")
    	add_library(${LIB_NAME} SHARED ${TEST_LIB})
    	set_target_properties(${LIB_NAME} PROPERTIES
        	PREFIX "lib"
        	SUFFIX ".a"
        	COMPILE_FLAGS "-q64  -qhalt=e"
        	LINK_FLAGS "-q64 -G -Wl,-bnoentry,-bexpall"
    	)
    else()
    	add_library(${LIB_NAME} SHARED ${TEST_LIB})
    endif()
    

    target_include_directories(${LIB_NAME}
      PRIVATE
        ${PARENT_DIR}
    )

    # Link against libm to resolve sqrt() and other math symbols
    target_link_libraries(${LIB_NAME} m)

    install(
     TARGETS
       ${LIB_NAME}
    )
endforeach()


