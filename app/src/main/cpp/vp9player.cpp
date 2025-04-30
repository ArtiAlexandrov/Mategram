#include <jni.h>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <thread>
#include <atomic>
#include <chrono>
#include <cstdio>
#include <cstring>
#include <string>
#include <mutex>
#include <vector>

extern "C" {
// Заголовки FFmpeg (обязательно включать в extern "C")
#include "libffmpeg-prebuilt/include/libavformat/avformat.h"
#include "libffmpeg-prebuilt/include/libavcodec/avcodec.h"
#include "libffmpeg-prebuilt/include/libavutil/imgutils.h"
#include "libffmpeg-prebuilt/include/libavutil/avutil.h"
#include "libffmpeg-prebuilt/include/libavutil/frame.h"
}

#include "libyuv.h" // Подключаем libyuv для преобразования

#define LOG_TAG "FFmpegPlayer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

class FFmpegPlayer {
public:
    FFmpegPlayer(ANativeWindow* window, const std::string& videoPath)
            : mWindow(window), mVideoPath(videoPath), mStopDecoding(false) {
        LOGI("Инициализация FFmpegPlayer: файл=%s", videoPath.c_str());
    }

    ~FFmpegPlayer() {
        stop();
        if (mWindow) {
            LOGI("Освобождение ANativeWindow");
            ANativeWindow_release(mWindow);
            mWindow = nullptr;
        }
    }

    void start() {
        LOGI("Запуск воспроизведения FFmpegPlayer");
        mStopDecoding = false;
        mDecodingThread = std::thread(&FFmpegPlayer::decodingLoop, this);
    }

    void stop() {
        LOGI("Остановка FFmpegPlayer");
        mStopDecoding = true;
        if (mDecodingThread.joinable()) {
            mDecodingThread.join();
        }
    }

private:
    void decodingLoop() {
        AVFormatContext* fmt_ctx = nullptr;
        if (avformat_open_input(&fmt_ctx, mVideoPath.c_str(), nullptr, nullptr) != 0) {
            LOGE("Не удалось открыть входной файл: %s", mVideoPath.c_str());
            return;
        }
        if (avformat_find_stream_info(fmt_ctx, nullptr) < 0) {
            LOGE("Не удалось найти информацию о потоках");
            avformat_close_input(&fmt_ctx);
            return;
        }

        // Поиск видеопотока
        int videoStreamIndex = -1;
        for (unsigned int i = 0; i < fmt_ctx->nb_streams; ++i) {
            if (fmt_ctx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
                videoStreamIndex = i;
                break;
            }
        }
        if (videoStreamIndex == -1) {
            LOGE("Видеопоток не найден");
            avformat_close_input(&fmt_ctx);
            return;
        }

        AVCodecParameters* codecpar = fmt_ctx->streams[videoStreamIndex]->codecpar;
        LOGI("Pixel format код: %u", codecpar->format);

        auto* codec = const_cast<AVCodec *>(avcodec_find_decoder_by_name("libvpx-vp9"));

        // Создание контекста декодера
        AVCodecContext* codec_ctx = avcodec_alloc_context3(codec);
        if (!codec_ctx) {
            LOGE("Не удалось создать контекст декодера");
            avformat_close_input(&fmt_ctx);
            return;
        }
        if (avcodec_parameters_to_context(codec_ctx, codecpar) < 0) {
            LOGE("Не удалось скопировать параметры в контекст декодера");
            avcodec_free_context(&codec_ctx);
            avformat_close_input(&fmt_ctx);
            return;
        }
        if (avcodec_open2(codec_ctx, codec, nullptr) < 0) {
            LOGE("Не удалось открыть декодер");
            avcodec_free_context(&codec_ctx);
            avformat_close_input(&fmt_ctx);
            return;
        }

        // Выделяем кадр для декодера
        AVFrame* frame = av_frame_alloc();
        if (!frame) {
            LOGE("Ошибка выделения AVFrame");
            avcodec_free_context(&codec_ctx);
            avformat_close_input(&fmt_ctx);
            return;
        }

        AVPacket packet;
        av_init_packet(&packet);

        // Основной цикл декодирования с зацикливанием
        while (!mStopDecoding) {
            int ret = av_read_frame(fmt_ctx, &packet);
            if (ret < 0) {
                // Если достигнут конец файла, выполняем переход к началу
                if (ret == AVERROR_EOF) {
                    LOGI("Достигнут конец файла. Зацикливаем видео...");
                    // Переводим формат контекста к начальной точке.
                    if (av_seek_frame(fmt_ctx, videoStreamIndex, 0, AVSEEK_FLAG_BACKWARD) < 0) {
                        LOGE("Ошибка зацикливания: не удалось выполнить seek");
                        break;
                    }
                    // Сбрасываем внутренние буферы декодера, чтобы избежать артефактов
                    avcodec_flush_buffers(codec_ctx);
                    continue;
                } else {
                    LOGE("Ошибка чтения кадра: %d", ret);
                    break;
                }
            }
            if (packet.stream_index == videoStreamIndex) {
                if (avcodec_send_packet(codec_ctx, &packet) < 0) {
                    LOGE("Ошибка отправки пакета в декодер");
                    av_packet_unref(&packet);
                    continue;
                }

                while (avcodec_receive_frame(codec_ctx, frame) == 0) {
                    const char *pix_fmt_name = av_get_pix_fmt_name(static_cast<AVPixelFormat>(frame->format));
                    LOGI("Frame pixel format: %s", pix_fmt_name);

                    if (frame->format != AV_PIX_FMT_YUV420P && frame->format != AV_PIX_FMT_YUVA420P) {
                        LOGE("Неподдерживаемый формат: %d. Ожидается YUV420P/YUVA420P", frame->format);
                        continue;
                    }

                    int width = frame->width;
                    int height = frame->height;
                    std::vector<uint8_t> argbBuffer(width * height * 4);
                    int dst_stride = width * 4;
                    int conv_result;
                    if (pix_fmt_name == "yuv420p") {
                        conv_result = libyuv::I420ToABGR(
                                frame->data[0], frame->linesize[0], // Y
                                frame->data[1], frame->linesize[1], // U
                                frame->data[2], frame->linesize[2], // V
                                argbBuffer.data(), dst_stride,
                                width, height);
                    } else {
                        conv_result = libyuv::I420AlphaToABGR(
                                frame->data[0], frame->linesize[0], // Y
                                frame->data[1], frame->linesize[1], // U
                                frame->data[2], frame->linesize[2], // V
                                frame->data[3], frame->linesize[3], // Alpha
                                argbBuffer.data(), dst_stride,
                                width, height, 1);
                    }

                    LOGI("Frame DATA3: %p", frame->data[3]);
                    if (conv_result != 0) {
                        LOGE("Ошибка конвертации libyuv: %d", conv_result);
                    } else {
                        renderFrame(argbBuffer.data(), width, height);
                    }

                    // Задержка для имитации ~30 fps
                    std::this_thread::sleep_for(std::chrono::milliseconds(33));
                    if (mStopDecoding) break;
                }
            }
            av_packet_unref(&packet);
        }

        // Освобождение ресурсов
        av_frame_free(&frame);
        avcodec_free_context(&codec_ctx);
        avformat_close_input(&fmt_ctx);
    }

    void renderFrame(const uint8_t* argbBuffer, int width, int height) {
        std::lock_guard<std::mutex> lock(mWindowMutex);
        if (!mWindow) {
            LOGE("ANativeWindow не инициализировано");
            return;
        }
        // Устанавливаем геометрию окна под формат RGBA (ABGR данные можно передавать как 32-битный RGBA)
        if (ANativeWindow_setBuffersGeometry(mWindow, width, height, WINDOW_FORMAT_RGBA_8888) < 0) {
            LOGE("Ошибка установки геометрии ANativeWindow");
            return;
        }
        ANativeWindow_Buffer windowBuffer;
        if (ANativeWindow_lock(mWindow, &windowBuffer, nullptr) != 0) {
            LOGE("Ошибка блокировки ANativeWindow");
            return;
        }
        for (int y = 0; y < height; y++) {
            uint8_t* dst = reinterpret_cast<uint8_t*>(windowBuffer.bits) + y * windowBuffer.stride * 4;
            const uint8_t* src = argbBuffer + y * width * 4;
            memcpy(dst, src, width * 4);
        }
        if (ANativeWindow_unlockAndPost(mWindow) != 0) {
            LOGE("Ошибка разблокировки ANativeWindow");
        }
    }

    ANativeWindow* mWindow;
    std::string mVideoPath;
    std::atomic<bool> mStopDecoding;
    std::thread mDecodingThread;
    std::mutex mWindowMutex;
};

extern "C"
JNIEXPORT jlong JNICALL
Java_com_xxcactussell_mategram_ui_Vp9Player_nativeCreate(JNIEnv *env, jobject /* this */,
                                                         jstring jVideoPath, jobject jSurface) {
    const char* cVideoPath = env->GetStringUTFChars(jVideoPath, nullptr);
    std::string videoPathStr(cVideoPath);
    env->ReleaseStringUTFChars(jVideoPath, cVideoPath);

    // Получаем ANativeWindow из Surface
    ANativeWindow* window = ANativeWindow_fromSurface(env, jSurface);
    if (window) {
        ANativeWindow_acquire(window);
    } else {
        LOGE("Не удалось получить ANativeWindow");
    }

    auto* player = new FFmpegPlayer(window, videoPathStr);
    return reinterpret_cast<jlong>(player);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_xxcactussell_mategram_ui_Vp9Player_nativeStart(JNIEnv *env, jobject /* this */,
                                                        jlong handle) {
    auto* player = reinterpret_cast<FFmpegPlayer*>(handle);
    if (player) {
        player->start();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_xxcactussell_mategram_ui_Vp9Player_nativeStop(JNIEnv *env, jobject /* this */,
                                                       jlong handle) {
    auto* player = reinterpret_cast<FFmpegPlayer*>(handle);
    if (player) {
        player->stop();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_xxcactussell_mategram_ui_Vp9Player_nativeDestroy(JNIEnv *env, jobject /* this */,
                                                          jlong handle) {
    auto* player = reinterpret_cast<FFmpegPlayer*>(handle);
    delete player;
}
