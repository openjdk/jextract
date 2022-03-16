default: all

.NOTPARALLEL:

include make/Common.gmk

JEXTRACT_NATIVE_TEST_DIR ?= $(JEXTRACT_IMAGE_NATIVE_TEST_DIR)
JEXTRACT_TEST_JDK ?= $(JEXTRACT_IMAGE_TEST_JDK_DIR)
JEXTRACT_DIR ?= $(JEXTRACT_IMAGE_DIR)

TEST ?= test

all: bundles

TARGETS += all


image images test-image bundles:
	$(MAKE) -f make/Build.gmk $@

TARGETS += image images test-image bundles


verify: test test-integration

TARGETS += verify


test-prebuilt:
	@( \
	    $(MAKE) --no-print-directory -r -R -I make/common/ -f make/RunTestsPrebuilt.gmk \
	        test-prebuilt $(MAKE_ARGS) \
	        JEXTRACT_TEST_JDK="$(JEXTRACT_TEST_JDK)" \
	        JEXTRACT_NATIVE_TEST_DIR="$(JEXTRACT_NATIVE_TEST_DIR)" \
	        JEXTRACT_DIR="$(JEXTRACT_DIR)" \
	        TEST="$(TEST)" \
	)

TARGETS += test-prebuilt


test: image test-image test-prebuilt

TARGETS += test

clean:
	rm -rf $(BUILD_DIR)

TARGETS += clean


.PHONY: default $(TARGETS)
