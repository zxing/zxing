#ifndef _LIBICONV_H
#define _LIBICONV_H
#include <stddef.h>
#ifdef __cplusplus
extern "C" {
#endif
typedef void* iconv_t;
iconv_t iconv_open(const char *tocode, const char *fromcode);
int iconv_close(iconv_t cd);
size_t iconv(iconv_t cd, char **inbuf, size_t *inbytesleft, char **outbuf, size_t *outbytesleft);
#ifdef __cplusplus
}
#endif
#endif//_LIBICONV_H