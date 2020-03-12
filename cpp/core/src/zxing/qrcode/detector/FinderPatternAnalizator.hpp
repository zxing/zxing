#pragma once

namespace pping {
template <typename T> class Ref;
}  // namespace pping

namespace pping {

namespace qrcode {

class FinderPattern;

class ZXingFinderPatternVector{
public:
    ZXingFinderPatternVector(const Ref<FinderPattern> from, const Ref<FinderPattern> to);

    ~ZXingFinderPatternVector();

    float getNorm();

    float getCosinusAngle(const ZXingFinderPatternVector &other) const;

private:
    float x_;

    float y_;

    float norm_;
};

class FinderPatternAnalizator {
public:
    FinderPatternAnalizator();

    ~FinderPatternAnalizator();

    static float analize(const Ref<FinderPattern> first, const Ref<FinderPattern> second, const Ref<FinderPattern> third);
};

} /* namespace qrcode */

} /* namespace pping */
