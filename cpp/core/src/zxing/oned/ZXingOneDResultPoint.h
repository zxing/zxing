#pragma once
#include <zxing/ResultPoint.h>  // for ResultPoint

namespace pping {
    namespace oned {
        
        class OneDResultPoint : public ResultPoint {
            
        public:
            OneDResultPoint(float posX, float posY);
        };
    }
}

