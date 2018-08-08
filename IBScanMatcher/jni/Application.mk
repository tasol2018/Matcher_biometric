APP_MODULES = ibscanmatcherjni ibscanmatcher
APP_ABI    := armeabi armeabi-v7a
APP_STL    := stlport_static

ifeq ($(NDK_DEBUG), 1)
    APP_OPTIM = debug
    APP_CFLAGS = -g -O0
else
    APP_OPTIM = release
    APP_CFLAGS = -O3
endif

