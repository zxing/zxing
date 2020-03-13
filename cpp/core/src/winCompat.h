
#pragma once

#ifndef NAN
#define INFINITY (DBL_MAX+DBL_MAX)
#define NAN (INFINITY-INFINITY)
#endif

/*
inline bool isnan(double x) {
    if (!(x >= 0.) && !(x <=0.)) {
        return true;
    }
    return false;    
}
*/
