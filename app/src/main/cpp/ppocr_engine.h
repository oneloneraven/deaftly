#pragma once

#include <android/asset_manager.h>

#include <cstdint>
#include <string>
#include <vector>

#include "net.h"

namespace ppocr {

struct Word {
    std::string text;
    float left = 0, top = 0, right = 0, bottom = 0;
    float confidence = 0;
};

struct Line {
    std::string text;
    float quad[8] = {};
    float confidence = 0;
    std::vector<Word> words;
};

struct Timings {
    double prepareMs = 0;
    double detectMs = 0;
    double postprocessMs = 0;
    double recogniseMs = 0;
};

struct Config {
    std::string detParam, detBin, recParam, recBin, dictPath;
    int detSide = 960;
    int threads = 4;
    bool fp16 = true;
};

class Engine {
public:
    bool load(AAssetManager* assets, const Config& config);

    void run(const uint8_t* rgba, int width, int height, int strideBytes, int rotationDegrees);

    const std::vector<Line>& lines() const { return lines_; }
    const Timings& timings() const { return timings_; }

private:
    struct Candidate {
        float quad[8];
        float score;
    };

    void detect(const uint8_t* rgb, int width, int height, std::vector<Candidate>& out);
    void recogniseLine(const uint8_t* rgb, int width, int height, const Candidate& candidate, Line& line) const;

    ncnn::Net det_;
    ncnn::Net rec_;
    std::vector<std::string> dict_;
    Config config_;
    std::vector<Line> lines_;
    Timings timings_;
};

}  // namespace ppocr
