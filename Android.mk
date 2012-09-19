LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src/com/amlogic/tvutil)
LOCAL_SRC_FILES += $(call all-java-files-under, src/com/amlogic/tvclient)
LOCAL_SRC_FILES += $(call all-java-files-under, src/com/amlogic/tvsubtitle)
LOCAL_SRC_FILES += $(call all-java-files-under, src/com/amlogic/tvactivity)
LOCAL_SRC_FILES += $(call all-java-files-under, src/com/amlogic/tvdataprovider)

LOCAL_SRC_FILES += $(call all-Iaidl-files-under, src/com/amlogic/tvutil)
LOCAL_SRC_FILES += $(call all-Iaidl-files-under, src/com/amlogic/tvclient)
LOCAL_SRC_FILES += $(call all-Iaidl-files-under, src/com/amlogic/tvsubtitle)
LOCAL_SRC_FILES += $(call all-Iaidl-files-under, src/com/amlogic/tvactivity)
LOCAL_SRC_FILES += $(call all-Iaidl-files-under, src/com/amlogic/tvservice)

#LOCAL_SDK_VERSION := current

LOCAL_MODULE:= tvmiddleware
#LOCAL_JAVA_LIBRARIES := 

LOCAL_PROGUARD_ENABLED := disabled
LOCAL_PROGUARD_FLAGS := -include $(LOCAL_PATH)/proguard.flags

# Generate a checksum that will be used in the app to determine whether the
# firmware in /system/etc/firmware needs to be updated.

#include $(BUILD_JAVA_LIBRARY)
include $(BUILD_STATIC_JAVA_LIBRARY)

##################################################

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src/com/amlogic/tvservice)

LOCAL_PACKAGE_NAME := TVService

LOCAL_SDK_VERSION := current

LOCAL_STATIC_JAVA_LIBRARIES := tvmiddleware

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)

##################################################

include $(call all-makefiles-under,$(LOCAL_PATH))
