cmake_minimum_required(VERSION 3.14)
project(TestSupportLibs)

option(TEST_SOURCE_ROOT "src directory with test files")

file(GLOB_RECURSE TEST_LIBS ${TEST_SOURCE_ROOT}/../*lib*.c)

foreach(TEST_LIB ${TEST_LIBS})
    message(STATUS "Processing test lib: ${TEST_LIB}")

    string(REGEX REPLACE ".*lib([A-Za-z0-9_]+)\.c(pp)?" "\\1" LIB_NAME ${TEST_LIB})
    string(REGEX REPLACE "(.*)/lib[A-Za-z0-9_]+\.c(pp)?" "\\1" PARENT_DIR ${TEST_LIB})
    message(STATUS "Lib name: ${LIB_NAME}")

    add_library(${LIB_NAME} SHARED ${TEST_LIB})
    target_include_directories(${LIB_NAME}
      PRIVATE
        ${PARENT_DIR}
    )

    install(
     TARGETS
       ${LIB_NAME}
    )
endforeach()


