cmake_minimum_required(VERSION 3.4.1)

# Пути к предкомпилированной статической библиотеке libvpx и его заголовочным файлам.
set(LIBVPX_ROOT "${CMAKE_SOURCE_DIR}/libvpx-prebuilt")
set(LIBVPX_INCLUDE "${LIBVPX_ROOT}/include")

set(LIBWEBM_ROOT "${CMAKE_SOURCE_DIR}/libwebm-prebuilt")
set(LIBWEBM_INCLUDE "${LIBWEBM_ROOT}/include")

set(LIBYUV_ROOT "${CMAKE_SOURCE_DIR}/libyuv-prebuilt")
set(LIBYUV_INCLUDE "${LIBYUV_ROOT}/include")

set(LIBFFMPEG_ROOT "${CMAKE_SOURCE_DIR}/libffmpeg-prebuilt")
set(LIBFFMPEG_INCLUDE "${LIBFFMPEG_ROOT}/include")

# Добавляем пути для заголовочных файлов libvpx и FFmpeg.
include_directories(${LIBVPX_INCLUDE} ${LIBWEBM_INCLUDE} ${LIBYUV_INCLUDE} ${LIBFFMPEG_INCLUDE})

# Импорт статической библиотеки libvpx для нужного ABI.
if(${ANDROID_ABI} STREQUAL "armeabi-v7a")
    set(libvpx_path "${LIBVPX_ROOT}/armeabi-v7a/lib/libvpx.a")
elseif(${ANDROID_ABI} STREQUAL "arm64-v8a")
    set(libvpx_path "${LIBVPX_ROOT}/arm64-v8a/lib/libvpx.a")
elseif(${ANDROID_ABI} STREQUAL "x86")
    set(libvpx_path "${LIBVPX_ROOT}/x86/lib/libvpx.a")
elseif(${ANDROID_ABI} STREQUAL "x86_64")
    set(libvpx_path "${LIBVPX_ROOT}/x86_64/lib/libvpx.a")
endif()

add_library(libvpx STATIC IMPORTED)
set_target_properties(libvpx PROPERTIES IMPORTED_LOCATION ${libvpx_path})


if(${ANDROID_ABI} STREQUAL "armeabi-v7a")
    set(libwebm_path "${LIBWEBM_ROOT}/armeabi-v7a/libwebm.a")
elseif(${ANDROID_ABI} STREQUAL "arm64-v8a")
    set(libwebm_path "${LIBWEBM_ROOT}/arm64-v8a/libwebm.a")
elseif(${ANDROID_ABI} STREQUAL "x86")
    set(libwebm_path "${LIBWEBM_ROOT}/x86/libwebm.a")
elseif(${ANDROID_ABI} STREQUAL "x86_64")
    set(libwebm_path "${LIBWEBM_ROOT}/x86_64/libwebm.a")
endif()

add_library(webm STATIC IMPORTED)
set_target_properties(webm PROPERTIES IMPORTED_LOCATION ${libwebm_path})

if(${ANDROID_ABI} STREQUAL "armeabi-v7a")
    set(libyuv_path "${LIBYUV_ROOT}/armeabi-v7a/libyuv.a")
elseif(${ANDROID_ABI} STREQUAL "arm64-v8a")
    set(libyuv_path "${LIBYUV_ROOT}/arm64-v8a/libyuv.a")
elseif(${ANDROID_ABI} STREQUAL "x86")
    set(libyuv_path "${LIBYUV_ROOT}/x86/libyuv.a")
elseif(${ANDROID_ABI} STREQUAL "x86_64")
    set(libyuv_path "${LIBYUV_ROOT}/x86_64/libyuv.a")
endif()

add_library(yuv STATIC IMPORTED)
set_target_properties(yuv PROPERTIES IMPORTED_LOCATION ${libyuv_path})

if(${ANDROID_ABI} STREQUAL "armeabi-v7a")
    set(libavformat_path "${LIBFFMPEG_ROOT}/armeabi-v7a/lib/libavformat.a")
    set(libavcodec_path "${LIBFFMPEG_ROOT}/armeabi-v7a/lib/libavcodec.a")
    set(libavutil_path "${LIBFFMPEG_ROOT}/armeabi-v7a/lib/libavutil.a")
    set(libavfilter_path "${LIBFFMPEG_ROOT}/armeabi-v7a/lib/libavfilter.a")
    set(libavdevice_path "${LIBFFMPEG_ROOT}/armeabi-v7a/lib/libavdevice.a")
    set(libswscale_path "${LIBFFMPEG_ROOT}/armeabi-v7a/lib/libswscale.a")
    set(libswresample_path "${LIBFFMPEG_ROOT}/armeabi-v7a/lib/libswresample.a")
elseif(${ANDROID_ABI} STREQUAL "arm64-v8a")
    set(libavformat_path "${LIBFFMPEG_ROOT}/arm64-v8a/lib/libavformat.a")
    set(libavcodec_path "${LIBFFMPEG_ROOT}/arm64-v8a/lib/libavcodec.a")
    set(libavutil_path "${LIBFFMPEG_ROOT}/arm64-v8a/lib/libavutil.a")
    set(libavfilter_path "${LIBFFMPEG_ROOT}/arm64-v8a/lib/libavfilter.a")
    set(libavdevice_path "${LIBFFMPEG_ROOT}/arm64-v8a/lib/libavdevice.a")
    set(libswscale_path "${LIBFFMPEG_ROOT}/arm64-v8a/lib/libswscale.a")
    set(libswresample_path "${LIBFFMPEG_ROOT}/arm64-v8a/lib/libswresample.a")
elseif(${ANDROID_ABI} STREQUAL "x86")
    set(libavformat_path "${LIBFFMPEG_ROOT}/x86/lib/libavformat.a")
    set(libavcodec_path "${LIBFFMPEG_ROOT}/x86/lib/libavcodec.a")
    set(libavutil_path "${LIBFFMPEG_ROOT}/x86/lib/libavutil.a")
    set(libavfilter_path "${LIBFFMPEG_ROOT}/x86/lib/libavfilter.a")
    set(libavdevice_path "${LIBFFMPEG_ROOT}/x86/lib/libavdevice.a")
    set(libswscale_path "${LIBFFMPEG_ROOT}/x86/lib/libswscale.a")
    set(libswresample_path "${LIBFFMPEG_ROOT}/x86/lib/libswresample.a")
elseif(${ANDROID_ABI} STREQUAL "x86_64")
    set(libavformat_path "${LIBFFMPEG_ROOT}/x86_64/lib/libavformat.a")
    set(libavcodec_path "${LIBFFMPEG_ROOT}/x86_64/lib/libavcodec.a")
    set(libavutil_path "${LIBFFMPEG_ROOT}/x86_64/lib/libavutil.a")
    set(libavfilter_path "${LIBFFMPEG_ROOT}/x86_64/lib/libavfilter.a")
    set(libavdevice_path "${LIBFFMPEG_ROOT}/x86_64/lib/libavdevice.a")
    set(libswscale_path "${LIBFFMPEG_ROOT}/x86_64/lib/libswscale.a")
    set(libswresample_path "${LIBFFMPEG_ROOT}/x86_64/lib/libswresample.a")
endif()
# Импортируем статические библиотеки FFmpeg.
add_library(avformat STATIC IMPORTED)
set_target_properties(avformat PROPERTIES IMPORTED_LOCATION ${libavformat_path})

add_library(avcodec STATIC IMPORTED)
set_target_properties(avcodec PROPERTIES IMPORTED_LOCATION ${libavcodec_path})

add_library(avutil STATIC IMPORTED)
set_target_properties(avutil PROPERTIES IMPORTED_LOCATION ${libavutil_path})

add_library(avfilter STATIC IMPORTED)
set_target_properties(avfilter PROPERTIES IMPORTED_LOCATION ${libavfilter_path})

add_library(avdevice STATIC IMPORTED)
set_target_properties(avdevice PROPERTIES IMPORTED_LOCATION ${libavdevice_path})

add_library(swscale STATIC IMPORTED)
set_target_properties(swscale PROPERTIES IMPORTED_LOCATION ${libswscale_path})

add_library(swresample STATIC IMPORTED)
set_target_properties(swresample PROPERTIES IMPORTED_LOCATION ${libswresample_path})



# Добавляем основной нативный модуль, например, vp9player.cpp.
add_library(vp9player SHARED vp9player.cpp)

# Линкуем нативный модуль с импортированными статическими библиотеками
# FFmpeg (avformat, avcodec, avutil), libvpx и системными библиотеками.
target_link_libraries(vp9player
    libvpx
    android
    webm
    yuv
    avformat
    avcodec
    avutil
    avfilter
    avdevice
    swscale
    swresample
    z
    log
)
