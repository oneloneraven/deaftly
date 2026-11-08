#include "ppocr_engine.h"

#include <algorithm>
#include <atomic>
#include <chrono>
#include <cmath>
#include <thread>

namespace ppocr {

namespace {

constexpr float kBinaryThreshold = 0.3f;
constexpr float kBoxThreshold = 0.6f;
constexpr float kUnclipRatio = 1.5f;
constexpr float kMinSide = 3.f;
constexpr int kDetStride = 32;
constexpr int kRecHeight = 48;
constexpr int kRecMinWidth = 16;
constexpr int kRecMaxWidth = 3200;
constexpr int kMaxCandidates = 1000;

struct P {
    float x, y;
};

double nowMs() {
    return std::chrono::duration<double, std::milli>(std::chrono::steady_clock::now().time_since_epoch()).count();
}

float cross(const P& o, const P& a, const P& b) {
    return (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x);
}

std::vector<P> convexHull(std::vector<P> pts) {
    std::sort(pts.begin(), pts.end(), [](const P& a, const P& b) { return a.x < b.x || (a.x == b.x && a.y < b.y); });
    pts.erase(std::unique(pts.begin(), pts.end(), [](const P& a, const P& b) { return a.x == b.x && a.y == b.y; }), pts.end());
    const size_t n = pts.size();
    if (n < 3) return pts;
    std::vector<P> hull(2 * n);
    size_t k = 0;
    for (size_t i = 0; i < n; i++) {
        while (k >= 2 && cross(hull[k - 2], hull[k - 1], pts[i]) <= 0) k--;
        hull[k++] = pts[i];
    }
    for (size_t i = n - 1, t = k + 1; i > 0; i--) {
        while (k >= t && cross(hull[k - 2], hull[k - 1], pts[i - 1]) <= 0) k--;
        hull[k++] = pts[i - 1];
    }
    hull.resize(k - 1);
    return hull;
}

struct Rect {
    P centre;
    P dir;
    float width;
    float height;
};

bool minAreaRect(const std::vector<P>& hull, Rect& out) {
    if (hull.size() < 3) return false;
    float bestArea = INFINITY;
    for (size_t i = 0; i < hull.size(); i++) {
        const P& a = hull[i];
        const P& b = hull[(i + 1) % hull.size()];
        float ex = b.x - a.x, ey = b.y - a.y;
        const float len = std::sqrt(ex * ex + ey * ey);
        if (len < 1e-6f) continue;
        const float ux = ex / len, uy = ey / len, vx = -uy, vy = ux;
        float minU = INFINITY, maxU = -INFINITY, minV = INFINITY, maxV = -INFINITY;
        for (const P& p : hull) {
            const float u = p.x * ux + p.y * uy;
            const float v = p.x * vx + p.y * vy;
            minU = std::min(minU, u);
            maxU = std::max(maxU, u);
            minV = std::min(minV, v);
            maxV = std::max(maxV, v);
        }
        const float area = (maxU - minU) * (maxV - minV);
        if (area < bestArea) {
            bestArea = area;
            const float cu = (minU + maxU) / 2, cv = (minV + maxV) / 2;
            out.centre = {cu * ux + cv * vx, cu * uy + cv * vy};
            out.dir = {ux, uy};
            out.width = maxU - minU;
            out.height = maxV - minV;
        }
    }
    return bestArea < INFINITY;
}

void makeTextRect(Rect& r) {
    if (r.height > r.width) {
        std::swap(r.width, r.height);
        r.dir = {-r.dir.y, r.dir.x};
    }
    if (r.dir.x < 0 || (r.dir.x == 0 && r.dir.y < 0)) r.dir = {-r.dir.x, -r.dir.y};
}

void sampleChannel(const uint8_t* rgb, int width, int height, float x, float y, int channel, float& out) {
    x = std::min(std::max(x, 0.f), width - 1.f);
    y = std::min(std::max(y, 0.f), height - 1.f);
    const int x0 = (int)x, y0 = (int)y;
    const int x1 = std::min(x0 + 1, width - 1), y1 = std::min(y0 + 1, height - 1);
    const float fx = x - x0, fy = y - y0;
    const float a = rgb[(y0 * width + x0) * 3 + channel], b = rgb[(y0 * width + x1) * 3 + channel];
    const float c = rgb[(y1 * width + x0) * 3 + channel], d = rgb[(y1 * width + x1) * 3 + channel];
    out = (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy;
}

}  // namespace

bool Engine::load(AAssetManager* assets, const Config& config) {
    config_ = config;
    for (ncnn::Net* net : {&det_, &rec_}) {
        net->opt.use_fp16_packed = config.fp16;
        net->opt.use_fp16_storage = config.fp16;
        net->opt.use_fp16_arithmetic = config.fp16;
        net->opt.use_vulkan_compute = false;
    }
    det_.opt.num_threads = std::max(1, config.threads);
    rec_.opt.num_threads = 1;
    if (det_.load_param(assets, config.detParam.c_str()) != 0) return false;
    if (det_.load_model(assets, config.detBin.c_str()) != 0) return false;
    if (rec_.load_param(assets, config.recParam.c_str()) != 0) return false;
    if (rec_.load_model(assets, config.recBin.c_str()) != 0) return false;

    AAsset* asset = AAssetManager_open(assets, config.dictPath.c_str(), AASSET_MODE_BUFFER);
    if (!asset) return false;
    const char* data = static_cast<const char*>(AAsset_getBuffer(asset));
    const size_t size = AAsset_getLength(asset);
    dict_.clear();
    size_t start = 0;
    for (size_t i = 0; i <= size; i++) {
        if (i == size || data[i] == '\n') {
            if (i > start || i < size) dict_.emplace_back(data + start, i - start);
            start = i + 1;
        }
    }
    while (!dict_.empty() && dict_.back().empty()) dict_.pop_back();
    AAsset_close(asset);
    return !dict_.empty();
}

void Engine::run(const uint8_t* rgba, int width, int height, int strideBytes, int rotationDegrees) {
    const double t0 = nowMs();
    const bool quarter = rotationDegrees == 90 || rotationDegrees == 270;
    const int uw = quarter ? height : width;
    const int uh = quarter ? width : height;
    std::vector<uint8_t> rgb((size_t)uw * uh * 3);
    for (int y = 0; y < uh; y++) {
        uint8_t* dst = &rgb[(size_t)y * uw * 3];
        for (int x = 0; x < uw; x++) {
            int sx, sy;
            switch (rotationDegrees) {
                case 90: sx = y; sy = height - 1 - x; break;
                case 180: sx = width - 1 - x; sy = height - 1 - y; break;
                case 270: sx = width - 1 - y; sy = x; break;
                default: sx = x; sy = y; break;
            }
            const uint8_t* src = rgba + (size_t)sy * strideBytes + (size_t)sx * 4;
            dst[0] = src[0];
            dst[1] = src[1];
            dst[2] = src[2];
            dst += 3;
        }
    }
    const double t1 = nowMs();
    timings_ = Timings();
    timings_.prepareMs = t1 - t0;

    std::vector<Candidate> candidates;
    detect(rgb.data(), uw, uh, candidates);
    const double t2 = nowMs();

    std::sort(candidates.begin(), candidates.end(), [](const Candidate& a, const Candidate& b) {
        const float ay = (a.quad[1] + a.quad[7]) / 2, by = (b.quad[1] + b.quad[7]) / 2;
        return ay < by;
    });
    lines_.assign(candidates.size(), Line());
    std::vector<size_t> order(candidates.size());
    for (size_t i = 0; i < order.size(); i++) order[i] = i;
    std::vector<float> widths(candidates.size());
    for (size_t i = 0; i < candidates.size(); i++) {
        const float* q = candidates[i].quad;
        widths[i] = std::hypot(q[2] - q[0], q[3] - q[1]);
    }
    std::sort(order.begin(), order.end(), [&](size_t a, size_t b) { return widths[a] > widths[b]; });
    std::atomic<size_t> next{0};
    auto worker = [&]() {
        for (size_t i = next++; i < order.size(); i = next++) {
            recogniseLine(rgb.data(), uw, uh, candidates[order[i]], lines_[order[i]]);
        }
    };
    const int workers = std::max(1, std::min<int>(config_.threads, (int)candidates.size()));
    std::vector<std::thread> pool;
    for (int i = 1; i < workers; i++) pool.emplace_back(worker);
    worker();
    for (auto& t : pool) t.join();
    lines_.erase(std::remove_if(lines_.begin(), lines_.end(), [](const Line& l) { return l.text.empty(); }), lines_.end());
    const double t3 = nowMs();
    timings_.recogniseMs = t3 - t2;
}

void Engine::detect(const uint8_t* rgb, int width, int height, std::vector<Candidate>& out) {
    const double t0 = nowMs();
    const float scale = std::min(1.f, (float)config_.detSide / (float)std::max(width, height));
    const int nw = std::max(kDetStride, (int)std::lround(width * scale / kDetStride) * kDetStride);
    const int nh = std::max(kDetStride, (int)std::lround(height * scale / kDetStride) * kDetStride);

    ncnn::Mat in = ncnn::Mat::from_pixels_resize(rgb, ncnn::Mat::PIXEL_RGB2BGR, width, height, nw, nh);
    const float mean[3] = {0.485f * 255.f, 0.456f * 255.f, 0.406f * 255.f};
    const float norm[3] = {1.f / 0.229f / 255.f, 1.f / 0.224f / 255.f, 1.f / 0.225f / 255.f};
    in.substract_mean_normalize(mean, norm);

    ncnn::Mat prob;
    {
        ncnn::Extractor ex = det_.create_extractor();
        ex.input("in0", in);
        ex.extract("out0", prob);
    }
    const double t1 = nowMs();
    timings_.detectMs = t1 - t0;

    const int mw = prob.w, mh = prob.h;
    const float* map = prob.channel(0);
    std::vector<uint8_t> visited((size_t)mw * mh, 0);
    std::vector<int> stack, component;
    std::vector<int> rowMin(mh), rowMax(mh);
    const float sx = (float)width / mw, sy = (float)height / mh;

    for (int y0 = 0; y0 < mh; y0++) {
        for (int x0 = 0; x0 < mw; x0++) {
            const int start = y0 * mw + x0;
            if (visited[start] || map[start] <= kBinaryThreshold) continue;
            component.clear();
            stack.clear();
            stack.push_back(start);
            visited[start] = 1;
            double sum = 0;
            int minY = y0, maxY = y0;
            while (!stack.empty()) {
                const int cur = stack.back();
                stack.pop_back();
                component.push_back(cur);
                sum += map[cur];
                const int cy = cur / mw, cx = cur % mw;
                minY = std::min(minY, cy);
                maxY = std::max(maxY, cy);
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        const int ny = cy + dy, nx = cx + dx;
                        if (nx < 0 || ny < 0 || nx >= mw || ny >= mh) continue;
                        const int ni = ny * mw + nx;
                        if (visited[ni] || map[ni] <= kBinaryThreshold) continue;
                        visited[ni] = 1;
                        stack.push_back(ni);
                    }
                }
            }
            if (component.size() < 4) continue;
            const float score = (float)(sum / component.size());
            if (score < kBoxThreshold) continue;
            if ((int)out.size() >= kMaxCandidates) continue;

            for (int y = minY; y <= maxY; y++) {
                rowMin[y] = INT32_MAX;
                rowMax[y] = -1;
            }
            for (int idx : component) {
                const int cy = idx / mw, cx = idx % mw;
                rowMin[cy] = std::min(rowMin[cy], cx);
                rowMax[cy] = std::max(rowMax[cy], cx);
            }
            std::vector<P> pts;
            for (int y = minY; y <= maxY; y++) {
                if (rowMax[y] < 0) continue;
                pts.push_back({(float)rowMin[y], (float)y});
                pts.push_back({(float)rowMin[y], (float)y + 1});
                pts.push_back({(float)rowMax[y] + 1, (float)y});
                pts.push_back({(float)rowMax[y] + 1, (float)y + 1});
            }
            Rect rect;
            if (!minAreaRect(convexHull(pts), rect)) continue;
            makeTextRect(rect);
            if (std::min(rect.width, rect.height) < kMinSide) continue;

            const float distance = rect.width * rect.height * kUnclipRatio / (2.f * (rect.width + rect.height));
            const float hw = rect.width / 2 + distance, hh = rect.height / 2 + distance;
            const P d = rect.dir, n = {-rect.dir.y, rect.dir.x};
            const P corners[4] = {
                {rect.centre.x - d.x * hw - n.x * hh, rect.centre.y - d.y * hw - n.y * hh},
                {rect.centre.x + d.x * hw - n.x * hh, rect.centre.y + d.y * hw - n.y * hh},
                {rect.centre.x + d.x * hw + n.x * hh, rect.centre.y + d.y * hw + n.y * hh},
                {rect.centre.x - d.x * hw + n.x * hh, rect.centre.y - d.y * hw + n.y * hh},
            };
            Candidate c;
            c.score = score;
            for (int i = 0; i < 4; i++) {
                c.quad[i * 2] = std::min(std::max(corners[i].x * sx, 0.f), (float)width);
                c.quad[i * 2 + 1] = std::min(std::max(corners[i].y * sy, 0.f), (float)height);
            }
            out.push_back(c);
        }
    }
    timings_.postprocessMs = nowMs() - t1;
}

void Engine::recogniseLine(const uint8_t* rgb, int width, int height, const Candidate& candidate, Line& line) const {
    const float* q = candidate.quad;
    const P tl{q[0], q[1]}, tr{q[2], q[3]}, bl{q[6], q[7]};
    const float srcWidth = std::hypot(tr.x - tl.x, tr.y - tl.y);
    const float srcHeight = std::max(1.f, std::hypot(bl.x - tl.x, bl.y - tl.y));
    const int cropWidth = std::min(kRecMaxWidth, std::max(kRecMinWidth, (int)std::ceil(kRecHeight * srcWidth / srcHeight)));

    ncnn::Mat in(cropWidth, kRecHeight, 3);
    for (int c = 0; c < 3; c++) {
        float* out = in.channel(2 - c);
        for (int j = 0; j < kRecHeight; j++) {
            const float v = (j + 0.5f) / kRecHeight;
            for (int i = 0; i < cropWidth; i++) {
                const float u = (i + 0.5f) / cropWidth;
                const float x = tl.x + u * (tr.x - tl.x) + v * (bl.x - tl.x) - 0.5f;
                const float y = tl.y + u * (tr.y - tl.y) + v * (bl.y - tl.y) - 0.5f;
                float value;
                sampleChannel(rgb, width, height, x, y, c, value);
                out[j * cropWidth + i] = value / 127.5f - 1.f;
            }
        }
    }

    ncnn::Mat out;
    {
        ncnn::Extractor ex = rec_.create_extractor();
        ex.input("in0", in);
        ex.extract("out0", out);
    }

    struct Token {
        int id, first, last;
        float prob;
    };
    std::vector<Token> tokens;
    int previous = 0;
    for (int t = 0; t < out.h; t++) {
        const float* row = out.row(t);
        int best = 0;
        float bestScore = row[0];
        for (int k = 1; k < out.w; k++) {
            if (row[k] > bestScore) {
                bestScore = row[k];
                best = k;
            }
        }
        if (best != 0) {
            if (best == previous) {
                tokens.back().last = t;
                tokens.back().prob = std::max(tokens.back().prob, bestScore);
            } else {
                tokens.push_back({best, t, t, bestScore});
            }
        }
        previous = best;
    }
    if (tokens.empty()) return;

    const float step = (float)cropWidth / std::max(1, out.h);
    auto centre = [&](const Token& t) { return (t.first + t.last + 1) * 0.5f * step; };
    auto isSpace = [&](const Token& t) { return t.id - 1 < (int)dict_.size() && dict_[t.id - 1] == " "; };

    size_t firstTok = 0, lastTok = tokens.size();
    while (firstTok < lastTok && isSpace(tokens[firstTok])) firstTok++;
    while (lastTok > firstTok && isSpace(tokens[lastTok - 1])) lastTok--;
    if (firstTok == lastTok) return;

    float probSum = 0;
    std::string text;
    for (size_t i = firstTok; i < lastTok; i++) {
        if (tokens[i].id - 1 < (int)dict_.size()) text += dict_[tokens[i].id - 1];
        probSum += tokens[i].prob;
    }
    line.text = text;
    line.confidence = probSum / (lastTok - firstTok);
    for (int i = 0; i < 8; i++) line.quad[i] = q[i];

    struct Span {
        size_t from, to;
    };
    std::vector<Span> spans;
    size_t from = firstTok;
    for (size_t i = firstTok; i <= lastTok; i++) {
        if (i == lastTok || isSpace(tokens[i])) {
            if (i > from) spans.push_back({from, i});
            from = i + 1;
        }
    }
    const float spacing = std::max(step, (centre(tokens[lastTok - 1]) - centre(tokens[firstTok])) / std::max<size_t>(1, lastTok - firstTok - 1));
    for (size_t s = 0; s < spans.size(); s++) {
        const float firstC = centre(tokens[spans[s].from]);
        const float lastC = centre(tokens[spans[s].to - 1]);
        float u0 = firstC - spacing / 2;
        float u1 = lastC + spacing / 2;
        if (s > 0) u0 = std::max(u0, (centre(tokens[spans[s - 1].to - 1]) + firstC) / 2);
        if (s + 1 < spans.size()) u1 = std::min(u1, (lastC + centre(tokens[spans[s + 1].from])) / 2);
        u0 = std::max(0.f, u0) / cropWidth;
        u1 = std::min((float)cropWidth, u1) / cropWidth;
        auto at = [&](float f, float v, float& x, float& y) {
            x = tl.x + f * (tr.x - tl.x) + v * (bl.x - tl.x);
            y = tl.y + f * (tr.y - tl.y) + v * (bl.y - tl.y);
        };
        Word word;
        at(u0, 0, word.quad[0], word.quad[1]);
        at(u1, 0, word.quad[2], word.quad[3]);
        at(u1, 1, word.quad[4], word.quad[5]);
        at(u0, 1, word.quad[6], word.quad[7]);
        float p = 0;
        for (size_t i = spans[s].from; i < spans[s].to; i++) {
            if (tokens[i].id - 1 < (int)dict_.size()) word.text += dict_[tokens[i].id - 1];
            p += tokens[i].prob;
        }
        word.confidence = p / (spans[s].to - spans[s].from);
        line.words.push_back(std::move(word));
    }
}

}  // namespace ppocr
