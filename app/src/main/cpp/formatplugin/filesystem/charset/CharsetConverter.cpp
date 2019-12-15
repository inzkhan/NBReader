// author : newbiechen
// date : 2019-11-28 20:08
// description : 
//

#include <util/Logger.h>
#include "CharsetConverter.h"

static const std::string TAG = "CharsetConverter";

CharsetConverter::CharsetConverter(const std::string &fromEncoding, const std::string &toEncoding) {
    isEncodingSame = false;

    // TODO：将大写转换成小写

    // 不处理 charset 不支持，或者写错的情况
    mIconvCd = iconv_open(toEncoding.c_str(), fromEncoding.c_str());

    // 如果初始化失败，则 icon cd  为 null
    if (errno == EINVAL) {
        mIconvCd = nullptr;

        //TODO: 直接抛出异常
    }

    // TODO:需要无视大小写

    if (fromEncoding == toEncoding) {
        isEncodingSame = true;
    }
}

CharsetConverter::~CharsetConverter() {
    if (mIconvCd != nullptr) {
        iconv_close(mIconvCd);
    }
}

CharsetConverter::ResultCode
CharsetConverter::convert(CharBuffer &inBuffer, CharBuffer &outBuffer) {

    if (mIconvCd == nullptr) {
        // TODO:未处理，暂时直接返回错误
        Logger::i(TAG, "iconv init error");
        return UNKNOW;
    }

    if (isEncodingSame) {
        // TODO:直接拷贝，还是不处理，没想好 (暂时直接返回错误)
        Logger::i(TAG, "same encoding ");
        return UNKNOW;
    }

    // 获取输入缓冲区的数据长度
    size_t inBufferLen = inBuffer.position();
    // 剩余输出缓冲的长度
    size_t remainOutBufferLen = outBuffer.limit() - outBuffer.position();

    // 如果输入、输出都为 0，则不处理
    if (inBufferLen == 0 || remainOutBufferLen == 0) {
        return SUCCESS;
    }

    // 输入缓冲指针
    char *inBufferPtr = inBuffer.buffer();

    // 输出缓冲指针
    char *outBufferPtr = outBuffer.buffer() + outBuffer.position();

    size_t resultInBufferLen = inBufferLen;
    size_t resultOutBufferLen = remainOutBufferLen;

    // 进行解析操作
    int resultCode = iconv(mIconvCd, &inBufferPtr, &resultInBufferLen, &outBufferPtr,
                           &resultOutBufferLen);

    // 处理 inBuffer

    inBuffer.flip();
    // 指向已处理的区域
    inBuffer.position(inBufferLen - resultInBufferLen);

    // 修改 outBuffer 的 position
    outBuffer.position(outBuffer.position() + (remainOutBufferLen - resultOutBufferLen));

    ResultCode code = SUCCESS;

    // iconv 存在的错误类型只存在这 3 种。
    if (resultCode == -1) {
        switch (errno) {
            // out
            case E2BIG:
                code = OVERFLOW;
                break;
            case EINVAL:
                code = UNDERFLOW;
                break;
            case EILSEQ:
                code = MALFORMED;
                break;
        }
    }

    // 返回结果值
    return code;
}