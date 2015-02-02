LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE    := libopencv_java
LOCAL_SRC_FILES := libs/armeabi/libopencv_java.so
LOCAL_MODULE_SUFFIX := .so
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_PATH := $(TARGET_OUT)/lib
LOCAL_MODULE_TAGS := optional
include $(BUILD_PREBUILT)
include $(CLEAR_VARS)
LOCAL_MODULE    := libopencv_info
LOCAL_SRC_FILES := libs/armeabi/libopencv_info.so
LOCAL_MODULE_SUFFIX := .so
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_PATH := $(TARGET_OUT)/lib
LOCAL_MODULE_TAGS := optional
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
## Include opencv and marf library
LOCAL_STATIC_JAVA_LIBRARIES :=  libopencv \
								libmarf
								
LOCAL_REQUIRED_MODULES := libopencv_java libopencv_info
LOCAL_SHARED_LIBRARIES := libopencv_java libopencv_info	

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := VFLock
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform

LOCAL_PROGUARD_FLAG_FILES := proguard.flags
include $(BUILD_PACKAGE)

								
include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libopencv:libs/OpenCV-2.4.10.jar \
										libmarf:libs/marf.jar
										

include $(BUILD_MULTI_PREBUILT)



# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
