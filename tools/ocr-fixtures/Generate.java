import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Function;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * Generates the OCR fixture set: synthetic phone-camera frames of menus, shop signs, receipts
 * and product labels, each with ground-truth lines and words as JSON.
 *
 * Usage: java tools/ocr-fixtures/Generate.java OUTDIR [OVERLAYDIR]
 */
public class Generate {
    static final long BASE_SEED = 220_000L;
    static final FontRenderContext FRC = new FontRenderContext(null, true, true);
    static final int LAND_W = 1280;
    static final int LAND_H = 960;
    static Path repoRoot;

    enum Align { L, C, R }

    enum Fx { NONE, CHALK, FADE, GLOW }

    enum Ground { WOOD, DARK, PAVEMENT, BRICK, PLASTER }

    enum Face {
        ATKINSON("Atkinson Hyperlegible Next", "app/src/main/res/font/atkinson_hyperlegible_next.ttf", null),
        BRICOLAGE("Bricolage Grotesque", "app/src/main/res/font/bricolage_grotesque.ttf", null),
        OLD_STANDARD("Old Standard TT", "tools/ocr-fixtures/fonts/old-standard-tt/OldStandard-Regular.ttf",
                "tools/ocr-fixtures/fonts/old-standard-tt/OldStandard-Bold.ttf"),
        BARLOW("Barlow Condensed", "tools/ocr-fixtures/fonts/barlow-condensed/BarlowCondensed-Regular.ttf",
                "tools/ocr-fixtures/fonts/barlow-condensed/BarlowCondensed-Bold.ttf"),
        KALAM("Kalam", "tools/ocr-fixtures/fonts/kalam/Kalam-Regular.ttf",
                "tools/ocr-fixtures/fonts/kalam/Kalam-Bold.ttf"),
        COURIER("Courier Prime", "tools/ocr-fixtures/fonts/courier-prime/CourierPrime-Regular.ttf",
                "tools/ocr-fixtures/fonts/courier-prime/CourierPrime-Bold.ttf");

        final String family;
        final String regularPath;
        final String boldPath;
        Font regular;
        Font bold;

        Face(String family, String regularPath, String boldPath) {
            this.family = family;
            this.regularPath = regularPath;
            this.boldPath = boldPath;
        }

        Font font(boolean useBold, double size) {
            try {
                if (regular == null) {
                    regular = Font.createFont(Font.TRUETYPE_FONT, repoRoot.resolve(regularPath).toFile());
                    bold = boldPath == null ? null : Font.createFont(Font.TRUETYPE_FONT, repoRoot.resolve(boldPath).toFile());
                }
            } catch (Exception e) {
                throw new IllegalStateException("Cannot load font " + regularPath, e);
            }
            if (!useBold) {
                return regular.deriveFont((float) size);
            }
            return bold != null ? bold.deriveFont((float) size) : regular.deriveFont(Font.BOLD, (float) size);
        }
    }

    record Sty(Face face, boolean bold, double size, Color color) {
        Font font(double scale) {
            return face.font(bold, size * scale);
        }

        Sty sized(double s) {
            return new Sty(face, bold, s, color);
        }

        Sty colored(Color c) {
            return new Sty(face, bold, size, c);
        }
    }

    record Word(String text, double x0, double x1, double y0, double y1) {}

    record Line(String text, int block, double x0, double x1, double y0, double y1, List<Word> words) {}

    // ---------------------------------------------------------------- noise

    static double hash(int x, int y, long seed) {
        long h = x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ seed * 0x165667B19E3779F9L;
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return (h >>> 11) * (1.0 / (1L << 53));
    }

    static double vnoise(double x, double y, long seed) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        double fx = x - x0;
        double fy = y - y0;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        double a = hash(x0, y0, seed);
        double b = hash(x0 + 1, y0, seed);
        double c = hash(x0, y0 + 1, seed);
        double d = hash(x0 + 1, y0 + 1, seed);
        return (a + (b - a) * fx) + ((c + (d - c) * fx) - (a + (b - a) * fx)) * fy;
    }

    static double fbm(double x, double y, long seed) {
        double sum = 0;
        double amp = 0.5;
        double norm = 0;
        for (int i = 0; i < 4; i++) {
            sum += amp * vnoise(x, y, seed + i * 31L);
            norm += amp;
            x *= 2;
            y *= 2;
            amp /= 2;
        }
        return sum / norm;
    }

    static double smoothstep(double a, double b, double x) {
        double t = Math.max(0, Math.min(1, (x - a) / (b - a)));
        return t * t * (3 - 2 * t);
    }

    static int clamp8(double v) {
        return v < 0 ? 0 : v > 255 ? 255 : (int) (v + 0.5);
    }

    // ---------------------------------------------------------------- gaussian blur

    static void gauss(float[] ch, int w, int h, double sigma) {
        if (sigma < 0.05) {
            return;
        }
        int r = (int) Math.ceil(sigma * 3);
        float[] k = new float[2 * r + 1];
        float sum = 0;
        for (int i = -r; i <= r; i++) {
            k[i + r] = (float) Math.exp(-(i * i) / (2 * sigma * sigma));
            sum += k[i + r];
        }
        for (int i = 0; i < k.length; i++) {
            k[i] /= sum;
        }
        float[] tmp = new float[ch.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float acc = 0;
                for (int i = -r; i <= r; i++) {
                    int xx = Math.max(0, Math.min(w - 1, x + i));
                    acc += ch[y * w + xx] * k[i + r];
                }
                tmp[y * w + x] = acc;
            }
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float acc = 0;
                for (int i = -r; i <= r; i++) {
                    int yy = Math.max(0, Math.min(h - 1, y + i));
                    acc += tmp[yy * w + x] * k[i + r];
                }
                ch[y * w + x] = acc;
            }
        }
    }

    // ---------------------------------------------------------------- scene

    static final class Scene {
        final int w;
        final int h;
        final BufferedImage bg;
        final BufferedImage layer;
        final Graphics2D bgG;
        final Graphics2D g;
        final List<Line> lines = new ArrayList<>();
        final java.util.Set<String> fonts = new java.util.TreeSet<>();
        int block;
        Fx fx = Fx.NONE;
        Color glow = new Color(255, 255, 255);
        long seed;

        Scene(int w, int h, long seed) {
            this.w = w;
            this.h = h;
            this.seed = seed;
            bg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            layer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            bgG = bg.createGraphics();
            g = layer.createGraphics();
            for (Graphics2D gg : new Graphics2D[] {bgG, g}) {
                gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                gg.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                gg.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            }
        }

        void clearText() {
            lines.clear();
            block = 0;
            g.setComposite(java.awt.AlphaComposite.Clear);
            g.fillRect(0, 0, w, h);
            g.setComposite(java.awt.AlphaComposite.SrcOver);
        }

        void surface(Color base, double grain, double mottle) {
            int[] px = ((java.awt.image.DataBufferInt) bg.getRaster().getDataBuffer()).getData();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    double m = (fbm(x * 0.006, y * 0.006, seed + 5) - 0.5) * 2 * mottle;
                    double n = (hash(x, y, seed + 9) - 0.5) * 2 * grain;
                    px[y * w + x] = (clamp8(base.getRed() + m + n) << 16) | (clamp8(base.getGreen() + m + n) << 8)
                            | clamp8(base.getBlue() + m + n);
                }
            }
        }

        void chalkboard(Color base) {
            surface(base, 3, 5);
            int[] px = ((java.awt.image.DataBufferInt) bg.getRaster().getDataBuffer()).getData();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    double dust = Math.pow(fbm(x * 0.004 + 3, y * 0.016, seed + 21), 3) * 70;
                    int p = px[y * w + x];
                    px[y * w + x] = (clamp8(((p >> 16) & 255) + dust) << 16) | (clamp8(((p >> 8) & 255) + dust) << 8)
                            | clamp8((p & 255) + dust);
                }
            }
        }

        void fold(boolean horizontal, double pos, double strength) {
            int[] px = ((java.awt.image.DataBufferInt) bg.getRaster().getDataBuffer()).getData();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    double d = (horizontal ? y : x) - pos;
                    double f = 1 + strength * 0.25 * Math.signum(-d) * Math.exp(-Math.abs(d) / 40.0);
                    f -= strength * Math.exp(-d * d / 3.0);
                    int p = px[y * w + x];
                    px[y * w + x] = (clamp8(((p >> 16) & 255) * f) << 16) | (clamp8(((p >> 8) & 255) * f) << 8)
                            | clamp8((p & 255) * f);
                }
            }
        }

        void ring(double cx, double cy, double r) {
            bgG.setStroke(new BasicStroke(6f));
            bgG.setColor(new Color(110, 70, 30, 46));
            bgG.draw(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
            bgG.setColor(new Color(110, 70, 30, 14));
            bgG.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        }

        void frame(Color c, int thickness, boolean wood) {
            bgG.setColor(c);
            bgG.fillRect(0, 0, w, thickness);
            bgG.fillRect(0, h - thickness, w, thickness);
            bgG.fillRect(0, 0, thickness, h);
            bgG.fillRect(w - thickness, 0, thickness, h);
            if (wood) {
                for (int i = 0; i < thickness * 3; i++) {
                    double t = hash(i, 3, seed + 77);
                    int yy = (int) (t * thickness);
                    bgG.setColor(new Color(0, 0, 0, 25 + (int) (hash(i, 4, seed) * 30)));
                    bgG.drawLine(0, yy, w, yy);
                    bgG.drawLine(0, h - 1 - yy, w, h - 1 - yy);
                    bgG.drawLine(yy, 0, yy, h);
                    bgG.drawLine(w - 1 - yy, 0, w - 1 - yy, h);
                }
            }
            bgG.setColor(new Color(0, 0, 0, 90));
            bgG.setStroke(new BasicStroke(3f));
            bgG.drawRect(thickness, thickness, w - 2 * thickness, h - 2 * thickness);
        }

        void border(Color c, double inset, double thickness) {
            bgG.setColor(c);
            bgG.setStroke(new BasicStroke((float) thickness));
            bgG.draw(new Rectangle2D.Double(inset, inset, w - 2 * inset, h - 2 * inset));
        }

        void drawText(String s, Font f, Color c, double x, double base, Align a) {
            if (s.isEmpty()) {
                return;
            }
            int bad = f.canDisplayUpTo(s);
            if (bad >= 0) {
                throw new IllegalStateException("Font " + f.getFontName() + " cannot draw '" + s.charAt(bad) + "' in: " + s);
            }
            fonts.add(f.getFamily());
            double width = f.getStringBounds(s, FRC).getWidth();
            double x0 = a == Align.L ? x : a == Align.C ? x - width / 2 : x - width;
            g.setFont(f);
            g.setColor(c);
            g.drawString(s, (float) x0, (float) base);

            GlyphVector gv = f.createGlyphVector(FRC, s);
            Rectangle2D vb = gv.getVisualBounds();
            List<Word> words = new ArrayList<>();
            int i = 0;
            while (i < s.length()) {
                if (s.charAt(i) == ' ') {
                    i++;
                    continue;
                }
                int j = i;
                while (j < s.length() && s.charAt(j) != ' ') {
                    j++;
                }
                String word = s.substring(i, j);
                double prefix = i == 0 ? 0 : f.getStringBounds(s.substring(0, i), FRC).getWidth();
                Rectangle2D wb = f.createGlyphVector(FRC, word).getVisualBounds();
                words.add(new Word(word, x0 + prefix + wb.getMinX() - 1, x0 + prefix + wb.getMaxX() + 1,
                        base + wb.getMinY() - 1, base + wb.getMaxY() + 1));
                i = j;
            }
            lines.add(new Line(s, block, x0 + vb.getMinX() - 1, x0 + vb.getMaxX() + 1, base + vb.getMinY() - 1,
                    base + vb.getMaxY() + 1, words));
        }

        void dots(double x0, double x1, double base, double size, Color c) {
            g.setColor(c);
            double step = size * 0.5;
            double r = Math.max(0.9, size * 0.05);
            for (double x = x0; x <= x1; x += step) {
                g.fill(new Ellipse2D.Double(x - r, base - r - size * 0.04, 2 * r, 2 * r));
            }
        }

        void rule(double x0, double x1, double y, double thickness, Color c) {
            g.setColor(c);
            g.fill(new Rectangle2D.Double(x0, y, x1 - x0, thickness));
        }

        void dashed(double x0, double x1, double y, double thickness, Color c) {
            g.setColor(c);
            for (double x = x0; x < x1; x += thickness * 4) {
                g.fill(new Rectangle2D.Double(x, y, thickness * 2.2, thickness));
            }
        }

        BufferedImage compose() {
            int[] la = ((java.awt.image.DataBufferInt) layer.getRaster().getDataBuffer()).getData();
            if (fx == Fx.CHALK || fx == Fx.FADE) {
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int p = la[y * w + x];
                        int a = p >>> 24;
                        if (a == 0) {
                            continue;
                        }
                        double m;
                        if (fx == Fx.CHALK) {
                            m = 0.55 + 0.45 * fbm(x * 0.9, y * 0.9, seed + 40);
                            if (vnoise(x * 2.3, y * 2.3, seed + 41) > 0.72) {
                                m *= 0.25;
                            }
                        } else {
                            m = 0.72 + 0.28 * fbm(x * 0.02, y * 0.5, seed + 42);
                        }
                        la[y * w + x] = (clamp8(a * m) << 24) | (p & 0xFFFFFF);
                    }
                }
            }
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D og = out.createGraphics();
            og.drawImage(bg, 0, 0, null);
            if (fx == Fx.GLOW) {
                float[] alpha = new float[w * h];
                for (int i = 0; i < alpha.length; i++) {
                    alpha[i] = la[i] >>> 24;
                }
                gauss(alpha, w, h, 9);
                BufferedImage gl = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                int[] gp = ((java.awt.image.DataBufferInt) gl.getRaster().getDataBuffer()).getData();
                for (int i = 0; i < gp.length; i++) {
                    gp[i] = (clamp8(alpha[i] * 1.6) << 24) | (glow.getRGB() & 0xFFFFFF);
                }
                og.drawImage(gl, 0, 0, null);
            }
            og.drawImage(layer, 0, 0, null);
            og.dispose();
            return out;
        }
    }

    // ---------------------------------------------------------------- document layout

    static final class El {
        char k;
        String l;
        String r;
        String[] cells;
        double[] fr;
        Sty ls;
        Sty rs;
        Align a = Align.L;
        double v;
        double indent;
        Color c;
    }

    static El head(String t, Sty s, Align a, double before) {
        El e = new El();
        e.k = 'H';
        e.l = t;
        e.ls = s;
        e.a = a;
        e.v = before;
        return e;
    }

    static El row(String l, String r, Sty ls, Sty rs) {
        El e = new El();
        e.k = 'R';
        e.l = l;
        e.r = r;
        e.ls = ls;
        e.rs = rs;
        return e;
    }

    static El para(String t, Sty s, Align a) {
        El e = new El();
        e.k = 'P';
        e.l = t;
        e.ls = s;
        e.a = a;
        return e;
    }

    static El gap(double h) {
        El e = new El();
        e.k = 'G';
        e.v = h;
        return e;
    }

    static El rule(Color c, double thickness) {
        El e = new El();
        e.k = 'X';
        e.c = c;
        e.v = thickness;
        return e;
    }

    static El table(Sty s, double indent, double[] rightEdges, String... cells) {
        El e = new El();
        e.k = 'T';
        e.cells = cells;
        e.fr = rightEdges;
        e.ls = s;
        e.indent = indent;
        return e;
    }

    static double lineHeight(Sty s, double sc) {
        return s.font(sc).getLineMetrics("Hg", FRC).getHeight();
    }

    static double ascent(Sty s, double sc) {
        return s.font(sc).getLineMetrics("Hg", FRC).getAscent();
    }

    static double textW(String t, Sty s, double sc) {
        return s.font(sc).getStringBounds(t, FRC).getWidth();
    }

    static List<String> wrap(String t, Sty s, double sc, double maxW) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : t.split(" ")) {
            String cand = cur.length() == 0 ? word : cur + " " + word;
            if (cur.length() > 0 && textW(cand, s, sc) > maxW) {
                out.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(cand);
            }
        }
        if (cur.length() > 0) {
            out.add(cur.toString());
        }
        return out;
    }

    static final class Doc {
        List<El> header = new ArrayList<>();
        List<El> body = new ArrayList<>();
        List<El> footer = new ArrayList<>();
        int cols = 1;
        double gutter = 44;
        double headerGap = 22;
        double footerGap = 20;
        double maxScale = 3;
        double minScale = 0.25;
        boolean leader;
        boolean centreVertically;
        Color leaderColor = Color.GRAY;

        double elH(El e, double w, double sc, boolean first) {
            switch (e.k) {
                case 'H':
                    return (first ? 0 : e.v * sc) + lineHeight(e.ls, sc);
                case 'R':
                    return Math.max(lineHeight(e.ls, sc), lineHeight(e.rs, sc)) * 1.1;
                case 'T':
                    return lineHeight(e.ls, sc) * 1.12;
                case 'P':
                    return wrap(e.l, e.ls, sc, w).size() * lineHeight(e.ls, sc) * 1.04 + 0.25 * e.ls.size * sc;
                case 'G':
                    return e.v * sc;
                default:
                    return e.v * sc + 8 * sc;
            }
        }

        double needW(El e, double sc) {
            switch (e.k) {
                case 'H':
                    return textW(e.l, e.ls, sc);
                case 'R':
                    return textW(e.l, e.ls, sc) + textW(e.r, e.rs, sc) + e.ls.size * sc * 1.2;
                case 'T': {
                    double need = e.indent * sc + textW(e.cells[0], e.ls, sc);
                    for (int i = 1; i < e.cells.length; i++) {
                        need += textW(e.cells[i], e.ls, sc) + e.ls.size * sc * 0.6;
                    }
                    return need;
                }
                case 'P': {
                    double m = 0;
                    for (String word : e.l.split(" ")) {
                        m = Math.max(m, textW(word, e.ls, sc));
                    }
                    return m;
                }
                default:
                    return 0;
            }
        }

        double stack(List<El> els, double w, double sc) {
            double y = 0;
            boolean first = true;
            for (El e : els) {
                y += elH(e, w, sc, first);
                first = false;
            }
            return y;
        }

        List<List<El>> split(double colW, double sc) {
            List<List<El>> out = new ArrayList<>();
            if (cols == 1) {
                out.add(body);
                return out;
            }
            int best = body.size() / 2;
            double bestCost = Double.MAX_VALUE;
            for (int i = 1; i < body.size(); i++) {
                if (body.get(i).k != 'H') {
                    continue;
                }
                double cost = Math.max(stack(body.subList(0, i), colW, sc), stack(body.subList(i, body.size()), colW, sc));
                if (cost < bestCost) {
                    bestCost = cost;
                    best = i;
                }
            }
            out.add(body.subList(0, best));
            out.add(body.subList(best, body.size()));
            return out;
        }

        double[] measure(double areaW, double sc) {
            double colW = (areaW - gutter * (cols - 1)) / cols;
            double hh = stack(header, areaW, sc);
            double bh = 0;
            double needRatio = 0;
            for (List<El> c : split(colW, sc)) {
                bh = Math.max(bh, stack(c, colW, sc));
                for (El e : c) {
                    needRatio = Math.max(needRatio, needW(e, sc) / colW);
                }
            }
            double fh = stack(footer, areaW, sc);
            for (El e : header) {
                needRatio = Math.max(needRatio, needW(e, sc) / areaW);
            }
            for (El e : footer) {
                needRatio = Math.max(needRatio, needW(e, sc) / areaW);
            }
            double total = hh + (hh > 0 ? headerGap * sc : 0) + bh + (fh > 0 ? footerGap * sc : 0) + fh;
            return new double[] {total, needRatio};
        }

        double fit(double areaW, double areaH) {
            double lo = minScale;
            double hi = maxScale;
            double[] m = measure(areaW, hi);
            if (m[0] <= areaH && m[1] <= 1) {
                return hi;
            }
            for (int i = 0; i < 24; i++) {
                double mid = (lo + hi) / 2;
                m = measure(areaW, mid);
                if (m[0] <= areaH && m[1] <= 1) {
                    lo = mid;
                } else {
                    hi = mid;
                }
            }
            return lo;
        }

        double heightAt(double areaW, double sc) {
            return measure(areaW, sc)[0];
        }

        void render(Scene s, double mx, double my) {
            double areaW = s.w - 2 * mx;
            double areaH = s.h - 2 * my;
            double sc = fit(areaW, areaH);
            double total = measure(areaW, sc)[0];
            double y = my + (centreVertically ? (areaH - total) / 2 : 0);
            s.block = 0;
            y = drawStack(s, header, mx, y, areaW, sc);
            if (!header.isEmpty()) {
                y += headerGap * sc;
            }
            s.block++;
            double colW = (areaW - gutter * (cols - 1)) / cols;
            double bodyTop = y;
            double bodyBottom = y;
            int ci = 0;
            for (List<El> c : split(colW, sc)) {
                double end = drawStack(s, c, mx + ci * (colW + gutter), bodyTop, colW, sc);
                bodyBottom = Math.max(bodyBottom, end);
                ci++;
            }
            if (!footer.isEmpty()) {
                s.block++;
                drawStack(s, footer, mx, bodyBottom + footerGap * sc, areaW, sc);
            }
        }

        double drawStack(Scene s, List<El> els, double x, double y, double w, double sc) {
            boolean first = true;
            for (El e : els) {
                double h = elH(e, w, sc, first);
                switch (e.k) {
                    case 'H': {
                        if (!first) {
                            s.block++;
                        }
                        double top = y + (first ? 0 : e.v * sc);
                        drawAligned(s, e.l, e.ls, sc, x, w, e.a, top + ascent(e.ls, sc));
                        break;
                    }
                    case 'R': {
                        double base = y + Math.max(ascent(e.ls, sc), ascent(e.rs, sc));
                        double rw = textW(e.r, e.rs, sc);
                        double lw = textW(e.l, e.ls, sc);
                        s.drawText(e.l, e.ls.font(sc), e.ls.color, x, base, Align.L);
                        s.drawText(e.r, e.rs.font(sc), e.rs.color, x + w, base, Align.R);
                        if (leader) {
                            double gapPx = e.ls.size * sc * 0.5;
                            s.dots(x + lw + gapPx, x + w - rw - gapPx, base, e.ls.size * sc, leaderColor);
                        }
                        break;
                    }
                    case 'T': {
                        double base = y + ascent(e.ls, sc);
                        s.drawText(e.cells[0], e.ls.font(sc), e.ls.color, x + e.indent * sc, base, Align.L);
                        for (int i = 1; i < e.cells.length; i++) {
                            s.drawText(e.cells[i], e.ls.font(sc), e.ls.color, x + w * e.fr[i - 1], base, Align.R);
                        }
                        break;
                    }
                    case 'P': {
                        double base = y + ascent(e.ls, sc);
                        for (String ln : wrap(e.l, e.ls, sc, w)) {
                            drawAligned(s, ln, e.ls, sc, x, w, e.a, base);
                            base += lineHeight(e.ls, sc) * 1.04;
                        }
                        break;
                    }
                    case 'X':
                        s.rule(x, x + w, y + 4 * sc, e.v * sc, e.c);
                        break;
                    default:
                        break;
                }
                y += h;
                first = false;
            }
            return y;
        }

        void drawAligned(Scene s, String t, Sty st, double sc, double x, double w, Align a, double base) {
            double px = a == Align.L ? x : a == Align.C ? x + w / 2 : x + w;
            s.drawText(t, st.font(sc), st.color, px, base, a);
        }
    }

    static List<El> menu(String text, Sty h, Sty name, Sty price, Sty desc, double sectionGap) {
        List<El> out = new ArrayList<>();
        for (String ln : text.strip().split("\n")) {
            if (ln.startsWith("# ")) {
                out.add(head(ln.substring(2), h, Align.L, sectionGap));
                continue;
            }
            String[] p = ln.split("\\|");
            out.add(row(p[0], p[1], name, price));
            if (p.length > 2) {
                out.add(para(p[2], desc, Align.L));
            }
        }
        return out;
    }

    static int cents(String price) {
        return (int) Math.round(Double.parseDouble(price.replace("£", "")) * 100);
    }

    static String money(int cents) {
        return String.format(Locale.ROOT, "£%d.%02d", cents / 100, cents % 100);
    }

    // ---------------------------------------------------------------- photo model

    static final class Photo {
        double rot;
        double yaw;
        double pitch;
        double zoom = 1.0;
        double dx;
        double dy;
        double blur = 0.7;
        double noise = 2.5;
        double jpeg = 0.85;
        double grad;
        double gradAngle;
        double vig = 0.12;
        double shadow;
        double shAngle;
        double shPos = 0.5;
        double shSoft = 0.15;
        double gain = 1.0;
        double black;
        double[] tint = {1, 1, 1};
        Ground ground = Ground.WOOD;
        String lighting = "even";

        Photo rot(double v) { rot = v; return this; }
        Photo yaw(double v) { yaw = v; return this; }
        Photo pitch(double v) { pitch = v; return this; }
        Photo zoom(double v) { zoom = v; return this; }
        Photo shift(double x, double y) { dx = x; dy = y; return this; }
        Photo blur(double v) { blur = v; return this; }
        Photo noise(double v) { noise = v; return this; }
        Photo jpeg(double v) { jpeg = v; return this; }
        Photo ground(Ground v) { ground = v; return this; }
        Photo tint(double r, double g, double b) { tint = new double[] {r, g, b}; return this; }

        Photo gradient(double strength, double angleDeg) {
            grad = strength;
            gradAngle = Math.toRadians(angleDeg);
            lighting = lighting.equals("low-light") ? lighting : "gradient";
            return this;
        }

        Photo shadow(double strength, double angleDeg, double pos, double soft) {
            shadow = strength;
            shAngle = Math.toRadians(angleDeg);
            shPos = pos;
            shSoft = soft;
            lighting = lighting.equals("low-light") ? lighting : grad != 0 ? "gradient+shadow" : "shadow";
            return this;
        }

        Photo lowLight(double gainV, double blackLevel, double noiseV) {
            gain = gainV;
            black = blackLevel;
            noise = noiseV;
            lighting = "low-light";
            return this;
        }
    }

    record Fixture(String id, String category, boolean portrait, String[] tags, Function<Long, Scene> scene, Photo photo) {}

    // ---------------------------------------------------------------- warp and photo pipeline

    static double[] solveHomography(double[][] s, double[][] d) {
        double[][] a = new double[8][9];
        for (int i = 0; i < 4; i++) {
            double u = s[i][0];
            double v = s[i][1];
            double x = d[i][0];
            double y = d[i][1];
            a[2 * i] = new double[] {u, v, 1, 0, 0, 0, -u * x, -v * x, x};
            a[2 * i + 1] = new double[] {0, 0, 0, u, v, 1, -u * y, -v * y, y};
        }
        for (int c = 0; c < 8; c++) {
            int piv = c;
            for (int r = c + 1; r < 8; r++) {
                if (Math.abs(a[r][c]) > Math.abs(a[piv][c])) {
                    piv = r;
                }
            }
            double[] t = a[c];
            a[c] = a[piv];
            a[piv] = t;
            for (int r = 0; r < 8; r++) {
                if (r == c) {
                    continue;
                }
                double f = a[r][c] / a[c][c];
                for (int k = c; k < 9; k++) {
                    a[r][k] -= f * a[c][k];
                }
            }
        }
        double[] h = new double[9];
        for (int i = 0; i < 8; i++) {
            h[i] = a[i][8] / a[i][i];
        }
        h[8] = 1;
        return h;
    }

    static double[] invert(double[] m) {
        double a = m[0], b = m[1], c = m[2], d = m[3], e = m[4], f = m[5], g = m[6], h = m[7], i = m[8];
        double det = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);
        return new double[] {
            (e * i - f * h) / det, (c * h - b * i) / det, (b * f - c * e) / det,
            (f * g - d * i) / det, (a * i - c * g) / det, (c * d - a * f) / det,
            (d * h - e * g) / det, (b * g - a * h) / det, (a * e - b * d) / det
        };
    }

    static double[] apply(double[] m, double u, double v) {
        double w = m[6] * u + m[7] * v + m[8];
        return new double[] {(m[0] * u + m[1] * v + m[2]) / w, (m[3] * u + m[4] * v + m[5]) / w};
    }

    static double[][] quad(Scene sc, Photo p, double scale, double cx, double cy) {
        double hw = sc.w / 2.0;
        double hh = sc.h / 2.0;
        double[][] pts = {{-hw, -hh}, {hw, -hh}, {hw, hh}, {-hw, hh}};
        double cos = Math.cos(Math.toRadians(p.rot));
        double sin = Math.sin(Math.toRadians(p.rot));
        double[][] out = new double[4][2];
        for (int i = 0; i < 4; i++) {
            double sx = pts[i][0] / hw;
            double sy = pts[i][1] / hh;
            double x = pts[i][0] * (1 - p.pitch * sy) * (1 - 0.35 * p.yaw * sx);
            double y = pts[i][1] * (1 - p.yaw * sx) * (1 - 0.35 * p.pitch * sy);
            out[i][0] = cx + scale * (x * cos - y * sin);
            out[i][1] = cy + scale * (x * sin + y * cos);
        }
        return out;
    }

    static double[][] srcQuad(Scene sc) {
        return new double[][] {{0, 0}, {sc.w, 0}, {sc.w, sc.h}, {0, sc.h}};
    }

    static double[] chooseHomography(Scene sc, Photo p, int fw, int fh) {
        double[][] unit = quad(sc, p, 1, 0, 0);
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (double[] q : unit) {
            minX = Math.min(minX, q[0]);
            maxX = Math.max(maxX, q[0]);
            minY = Math.min(minY, q[1]);
            maxY = Math.max(maxY, q[1]);
        }
        double fitScale = Math.min(fw * 0.97 / (maxX - minX), fh * 0.97 / (maxY - minY));
        double z = p.zoom;
        for (int attempt = 0; attempt < 80; attempt++) {
            double scale = fitScale * z;
            double[] h = solveHomography(srcQuad(sc), quad(sc, p, scale, fw / 2.0 + p.dx, fh / 2.0 + p.dy));
            if (contentInside(sc, h, fw, fh, 10)) {
                return h;
            }
            z *= 0.98;
            if (z < 0.5) {
                break;
            }
        }
        throw new IllegalStateException("Cannot fit content into frame");
    }

    static boolean contentInside(Scene sc, double[] h, int fw, int fh, double margin) {
        for (Line l : sc.lines) {
            for (double[] c : corners(l.x0, l.x1, l.y0, l.y1)) {
                double[] q = apply(h, c[0], c[1]);
                if (q[0] < margin || q[1] < margin || q[0] > fw - margin || q[1] > fh - margin) {
                    return false;
                }
            }
        }
        return true;
    }

    static double[][] corners(double x0, double x1, double y0, double y1) {
        return new double[][] {{x0, y0}, {x1, y0}, {x1, y1}, {x0, y1}};
    }

    static int groundColour(Ground g, double u, double v, long seed) {
        double r;
        double gr;
        double b;
        switch (g) {
            case WOOD: {
                double grain = fbm(u * 0.004, v * 0.11, seed + 1);
                double plank = (Math.floorMod((int) Math.floor(v), 300) < 3) ? 0.45 : 1;
                double f = (0.72 + 0.55 * grain) * plank;
                r = 128 * f;
                gr = 88 * f;
                b = 54 * f;
                break;
            }
            case DARK: {
                double f = 0.8 + 0.4 * fbm(u * 0.02, v * 0.02, seed + 2);
                r = 30 * f;
                gr = 32 * f;
                b = 36 * f;
                break;
            }
            case PAVEMENT: {
                double f = 0.7 + 0.6 * fbm(u * 0.03, v * 0.03, seed + 3);
                f *= 0.9 + 0.2 * hash((int) u, (int) v, seed + 4);
                r = 112 * f;
                gr = 110 * f;
                b = 106 * f;
                break;
            }
            case BRICK: {
                double bh = 44;
                double bw = 118;
                int row = (int) Math.floor(v / bh);
                double uu = u + (row % 2 == 0 ? 0 : bw / 2);
                int col = (int) Math.floor(uu / bw);
                double lu = uu - col * bw;
                double lv = v - row * bh;
                boolean mortar = lu < 5 || lv < 5;
                double var = 0.75 + 0.5 * hash(col, row, seed + 5);
                double tex = 0.88 + 0.24 * fbm(u * 0.15, v * 0.15, seed + 6);
                if (mortar) {
                    r = 168 * tex;
                    gr = 162 * tex;
                    b = 150 * tex;
                } else {
                    r = 134 * var * tex;
                    gr = 62 * var * tex;
                    b = 44 * var * tex;
                }
                break;
            }
            default: {
                double f = 0.9 + 0.2 * fbm(u * 0.01, v * 0.01, seed + 7);
                r = 204 * f;
                gr = 199 * f;
                b = 188 * f;
            }
        }
        return (clamp8(r) << 16) | (clamp8(gr) << 8) | clamp8(b);
    }

    static float[][] warp(BufferedImage scene, double[] h, int fw, int fh, Ground ground, long seed) {
        int sw = scene.getWidth();
        int shh = scene.getHeight();
        int[] sp = scene.getRGB(0, 0, sw, shh, null, 0, sw);
        double[] inv = invert(h);
        float[][] ch = new float[3][fw * fh];
        double[] offs = {0.25, 0.75};
        for (int y = 0; y < fh; y++) {
            for (int x = 0; x < fw; x++) {
                double r = 0, g = 0, b = 0;
                for (double oy : offs) {
                    for (double ox : offs) {
                        double[] uv = apply(inv, x + ox, y + oy);
                        double u = uv[0];
                        double v = uv[1];
                        int c;
                        if (u >= 0 && v >= 0 && u <= sw - 1 && v <= shh - 1) {
                            int u0 = (int) u;
                            int v0 = (int) v;
                            int u1 = Math.min(sw - 1, u0 + 1);
                            int v1 = Math.min(shh - 1, v0 + 1);
                            double fu = u - u0;
                            double fv = v - v0;
                            int c00 = sp[v0 * sw + u0];
                            int c10 = sp[v0 * sw + u1];
                            int c01 = sp[v1 * sw + u0];
                            int c11 = sp[v1 * sw + u1];
                            double rr = 0, gg = 0, bb = 0;
                            for (int s = 0; s < 4; s++) {
                                int cc = s == 0 ? c00 : s == 1 ? c10 : s == 2 ? c01 : c11;
                                double wgt = (s % 2 == 0 ? 1 - fu : fu) * (s < 2 ? 1 - fv : fv);
                                rr += wgt * ((cc >> 16) & 255);
                                gg += wgt * ((cc >> 8) & 255);
                                bb += wgt * (cc & 255);
                            }
                            r += rr;
                            g += gg;
                            b += bb;
                            continue;
                        }
                        c = groundColour(ground, u, v, seed);
                        r += (c >> 16) & 255;
                        g += (c >> 8) & 255;
                        b += c & 255;
                    }
                }
                ch[0][y * fw + x] = (float) (r / 4);
                ch[1][y * fw + x] = (float) (g / 4);
                ch[2][y * fw + x] = (float) (b / 4);
            }
        }
        return ch;
    }

    static BufferedImage develop(float[][] ch, int fw, int fh, Photo p, Random rnd, long seed) {
        for (float[] c : ch) {
            gauss(c, fw, fh, p.blur);
        }
        BufferedImage img = new BufferedImage(fw, fh, BufferedImage.TYPE_INT_RGB);
        int[] out = ((java.awt.image.DataBufferInt) img.getRaster().getDataBuffer()).getData();
        double ca = Math.cos(p.gradAngle);
        double sa = Math.sin(p.gradAngle);
        double sca = Math.cos(p.shAngle);
        double ssa = Math.sin(p.shAngle);
        for (int y = 0; y < fh; y++) {
            for (int x = 0; x < fw; x++) {
                double nx = (x - fw / 2.0) / (fw / 2.0);
                double ny = (y - fh / 2.0) / (fh / 2.0);
                double f = p.gain * (1 + p.grad * (nx * ca + ny * sa)) * (1 - p.vig * (nx * nx + ny * ny) / 2);
                if (p.shadow > 0) {
                    double t = nx * sca + ny * ssa - (p.shPos * 2 - 1) + 0.25 * (fbm(x * 0.004, y * 0.004, seed + 60) - 0.5);
                    f *= 1 - p.shadow * smoothstep(-p.shSoft, p.shSoft, t);
                }
                double lum = 0;
                double[] v = new double[3];
                for (int c = 0; c < 3; c++) {
                    v[c] = p.black + ch[c][y * fw + x] * f * p.tint[c];
                    lum += v[c] / 3;
                }
                double shared = rnd.nextGaussian() * p.noise;
                double gainNoise = 0.45 + 0.55 * Math.sqrt(Math.max(0, lum) / 255.0);
                int rgb = 0;
                for (int c = 0; c < 3; c++) {
                    double n = (shared + rnd.nextGaussian() * p.noise * 0.55) * gainNoise;
                    rgb = (rgb << 8) | clamp8(v[c] + n);
                }
                out[y * fw + x] = rgb;
            }
        }
        return img;
    }

    static void writeJpeg(BufferedImage img, Path file, double quality) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam prm = writer.getDefaultWriteParam();
        prm.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        prm.setCompressionQuality((float) quality);
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(file.toFile())) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(img, null, null), prm);
        } finally {
            writer.dispose();
        }
    }

    // ---------------------------------------------------------------- ground truth

    static String num(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    static String jsonStr(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    static List<double[]> poly(double[] h, double x0, double x1, double y0, double y1) {
        List<double[]> pts = new ArrayList<>();
        for (double[] c : corners(x0, x1, y0, y1)) {
            pts.add(apply(h, c[0], c[1]));
        }
        return pts;
    }

    static String polyJson(List<double[]> pts) {
        StringBuilder sb = new StringBuilder("[");
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i < pts.size(); i++) {
            double[] p = pts.get(i);
            sb.append(i == 0 ? "" : ",").append('[').append(num(p[0])).append(',').append(num(p[1])).append(']');
            minX = Math.min(minX, p[0]);
            maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]);
            maxY = Math.max(maxY, p[1]);
        }
        return sb.append("],\"box\":[").append(num(minX)).append(',').append(num(minY)).append(',').append(num(maxX))
                .append(',').append(num(maxY)).append(']').toString();
    }

    static String groundTruth(Fixture f, Scene sc, Photo p, double[] h, int fw, int fh, String fonts) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"schema\": 1,\n  \"image\": ").append(jsonStr(f.id() + ".jpg"));
        sb.append(",\n  \"width\": ").append(fw).append(",\n  \"height\": ").append(fh);
        sb.append(",\n  \"category\": ").append(jsonStr(f.category()));
        sb.append(",\n  \"tags\": [");
        for (int i = 0; i < f.tags().length; i++) {
            sb.append(i == 0 ? "" : ", ").append(jsonStr(f.tags()[i]));
        }
        sb.append("],\n  \"fonts\": ").append(jsonStr(fonts));
        sb.append(",\n  \"conditions\": {\"rotationDegrees\": ").append(num(p.rot));
        sb.append(", \"perspectiveYaw\": ").append(String.format(Locale.ROOT, "%.2f", p.yaw));
        sb.append(", \"perspectivePitch\": ").append(String.format(Locale.ROOT, "%.2f", p.pitch));
        sb.append(", \"blurSigma\": ").append(String.format(Locale.ROOT, "%.1f", p.blur));
        sb.append(", \"noiseSigma\": ").append(String.format(Locale.ROOT, "%.1f", p.noise));
        sb.append(", \"jpegQuality\": ").append((int) Math.round(p.jpeg * 100));
        sb.append(", \"lighting\": ").append(jsonStr(p.lighting)).append("},\n  \"lines\": [\n");
        for (int i = 0; i < sc.lines.size(); i++) {
            Line l = sc.lines.get(i);
            sb.append("    {\"text\": ").append(jsonStr(l.text())).append(", \"block\": ").append(l.block());
            sb.append(", \"polygon\": ").append(polyJson(poly(h, l.x0(), l.x1(), l.y0(), l.y1())));
            sb.append(", \"words\": [");
            for (int j = 0; j < l.words().size(); j++) {
                Word w = l.words().get(j);
                sb.append(j == 0 ? "" : ", ").append("{\"text\": ").append(jsonStr(w.text()));
                sb.append(", \"polygon\": ").append(polyJson(poly(h, w.x0(), w.x1(), w.y0(), w.y1()))).append('}');
            }
            sb.append("]}").append(i == sc.lines.size() - 1 ? "\n" : ",\n");
        }
        return sb.append("  ]\n}\n").toString();
    }

    static void overlay(BufferedImage img, Scene sc, double[] h, Path file) throws IOException {
        BufferedImage o = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = o.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(img, 0, 0, null);
        for (Line l : sc.lines) {
            g.setColor(new Color(255, 0, 0, 200));
            g.setStroke(new BasicStroke(1.5f));
            List<double[]> pts = poly(h, l.x0(), l.x1(), l.y0(), l.y1());
            for (int i = 0; i < 4; i++) {
                double[] a = pts.get(i);
                double[] b = pts.get((i + 1) % 4);
                g.drawLine((int) a[0], (int) a[1], (int) b[0], (int) b[1]);
            }
            g.setColor(new Color(0, 160, 255, 200));
            for (Word w : l.words()) {
                List<double[]> wp = poly(h, w.x0(), w.x1(), w.y0(), w.y1());
                for (int i = 0; i < 4; i++) {
                    double[] a = wp.get(i);
                    double[] b = wp.get((i + 1) % 4);
                    g.drawLine((int) a[0], (int) a[1], (int) b[0], (int) b[1]);
                }
            }
        }
        g.dispose();
        ImageIO.write(o, "png", file.toFile());
    }

    // ---------------------------------------------------------------- fixture content

    static final Color INK = new Color(34, 30, 28);
    static final Color GREY = new Color(96, 90, 84);
    static final Color WHITE = new Color(246, 244, 238);

    static Doc menuDoc(int cols, boolean leader, String text, Sty h, Sty name, Sty price, Sty desc, double gapSec) {
        Doc d = new Doc();
        d.cols = cols;
        d.leader = leader;
        d.centreVertically = true;
        d.leaderColor = name.color();
        d.body = menu(text, h, name, price, desc, gapSec);
        return d;
    }

    static void titleBlock(Doc d, String title, String sub, Sty ts, Sty ss) {
        d.header.add(head(title, ts, Align.C, 0));
        d.header.add(head(sub, ss, Align.C, 0));
        d.header.add(rule(ss.color(), 2));
    }

    static List<Fixture> fixtures() {
        List<Fixture> all = new ArrayList<>();

        // ---- menus
        all.add(new Fixture("menu-trattoria", "menu", true,
                new String[] {"two-column", "dotted-leaders", "accents", "serif", "dark-on-light", "fold"},
                seed -> {
                    Scene s = new Scene(880, 1180, seed);
                    s.surface(new Color(243, 235, 212), 5, 7);
                    s.fold(true, 590, 0.1);
                    s.ring(740, 1040, 46);
                    Sty hd = new Sty(Face.OLD_STANDARD, true, 34, new Color(120, 28, 24));
                    Sty nm = new Sty(Face.OLD_STANDARD, false, 27, INK);
                    Sty pr = new Sty(Face.OLD_STANDARD, true, 27, INK);
                    Sty ds = new Sty(Face.OLD_STANDARD, false, 20, GREY);
                    Doc d = menuDoc(2, true, """
                            # Antipasti
                            Bruschetta al pomodoro|£6.50|Grilled sourdough, vine tomatoes, basil (V)
                            Burrata e prosciutto|£9.95|Creamy burrata, 24-month Parma ham
                            Calamari fritti|£8.50|Lightly floured, lemon mayonnaise
                            # Primi
                            Pasta arrabbiata|£12.50|Penne, spicy tomato, garlic, chilli (VG)
                            Spaghetti alle vongole|£15.00|Palourde clams, white wine, parsley
                            Tagliatelle al ragù|£13.75|Slow-cooked beef and pork, Parmesan
                            Risotto ai funghi|£14.25|Wild mushrooms, truffle oil (V)
                            # Secondi
                            Pollo alla cacciatora|£16.50|Chicken thigh, peppers, olives
                            Branzino al forno|£19.95|Roast sea bass, capers, tomatoes
                            Aubergine parmigiana|£13.95|Baked layers, mozzarella, basil (V)
                            # Dolci
                            Tiramisù|£6.95|Mascarpone, espresso, cocoa
                            Panna cotta|£6.50|Vanilla cream, seasonal berries
                            Affogato|£5.95|Vanilla gelato, hot espresso
                            """, hd, nm, pr, ds, 30);
                    titleBlock(d, "Trattoria Da Marco", "Cucina italiana · Est. 1987",
                            new Sty(Face.OLD_STANDARD, true, 62, INK), new Sty(Face.OLD_STANDARD, false, 26, GREY));
                    d.footer.add(para("Please tell us about any allergies before ordering. A discretionary 12.5% service charge is added to tables of six or more.",
                            new Sty(Face.OLD_STANDARD, false, 18, GREY), Align.C));
                    d.render(s, 50, 50);
                    return s;
                }, new Photo().rot(-4).yaw(0.04).zoom(1.0).blur(0.8).noise(2.5).gradient(0.12, 20).ground(Ground.WOOD)));

        all.add(new Fixture("menu-cafe-chalkboard", "menu", false,
                new String[] {"chalkboard", "light-on-dark", "handwriting", "wood-frame"},
                seed -> {
                    Scene s = new Scene(1180, 880, seed);
                    s.chalkboard(new Color(36, 52, 46));
                    s.frame(new Color(120, 82, 50), 34, true);
                    s.fx = Fx.CHALK;
                    Sty hd = new Sty(Face.KALAM, true, 36, new Color(250, 222, 120));
                    Sty nm = new Sty(Face.KALAM, false, 28, new Color(240, 238, 230));
                    Sty ds = new Sty(Face.KALAM, false, 21, new Color(205, 210, 200));
                    Doc d = menuDoc(2, true, """
                            # Hot drinks
                            Flat white|£3.20
                            Cappuccino|£3.40
                            Americano|£2.90
                            Hot chocolate|£3.60|With whipped cream
                            Chai latte|£3.80
                            # Cakes
                            Victoria sponge|£3.95
                            Carrot cake|£4.25|Cream cheese frosting
                            Lemon drizzle|£3.75
                            Flapjack (GF)|£2.95
                            # Light bites
                            Cheese & chutney toastie|£5.95
                            Soup of the day|£5.50|With crusty bread
                            Sausage roll|£3.25
                            """, hd, nm, nm, ds, 24);
                    d.leaderColor = new Color(200, 205, 195, 190);
                    titleBlock(d, "Today at The Daily Grind", "Freshly made · Fairly traded",
                            new Sty(Face.KALAM, true, 54, new Color(250, 222, 120)), new Sty(Face.KALAM, false, 26, new Color(205, 210, 200)));
                    d.render(s, 78, 66);
                    return s;
                }, new Photo().rot(3).yaw(0.05).pitch(-0.02).zoom(1.1).blur(1.0).noise(3.2).gradient(0.2, -30).tint(1.04, 1.0, 0.9)));

        all.add(new Fixture("menu-pub-grub", "menu", true,
                new String[] {"single-column", "allergen-codes", "small-print", "sans", "dark-on-light"},
                seed -> {
                    Scene s = new Scene(880, 1180, seed);
                    s.surface(new Color(250, 249, 245), 3, 3);
                    Sty hd = new Sty(Face.ATKINSON, true, 32, new Color(20, 80, 60));
                    Sty nm = new Sty(Face.ATKINSON, true, 25, INK);
                    Sty pr = new Sty(Face.ATKINSON, true, 25, INK);
                    Sty ds = new Sty(Face.ATKINSON, false, 19, GREY);
                    Doc d = menuDoc(1, false, """
                            # Starters
                            Soup of the day|£6.25|Served with warm bread (V) 1,7
                            Scotch egg|£6.95|Free-range egg, black pudding, piccalilli 1,3,10
                            Whitebait|£7.50|Crispy, with tartare sauce 1,3,4,7,10
                            # Mains
                            Fish & chips|£14.95|Beer-battered haddock, chips, mushy peas 1,3,4
                            Steak & ale pie|£15.50|Shortcrust pastry, mash, seasonal greens 1,7
                            Ploughman's|£11.95|Mature cheddar, ham, pickle, bread 1,7,10
                            Beef burger|£14.50|Bacon, cheddar, brioche bun, chips 1,3,7,10
                            Vegetable lasagne|£12.95|Roasted vegetables, béchamel (V) 1,7
                            # Puddings
                            Sticky toffee pudding|£6.50|Toffee sauce, vanilla ice cream 1,3,7
                            Apple crumble|£6.25|Custard or cream 1,7
                            Cheeseboard|£9.50|Three cheeses, chutney, biscuits 1,7,10
                            """, hd, nm, pr, ds, 22);
                    titleBlock(d, "The Plough Inn", "Home-cooked food, served 12:00 – 21:00",
                            new Sty(Face.ATKINSON, true, 54, INK), new Sty(Face.ATKINSON, false, 24, GREY));
                    d.footer.add(para("Allergens: 1 Gluten  3 Egg  4 Fish  7 Milk  10 Mustard. Please ask a member of staff if you have a food allergy or intolerance. All prices include VAT.",
                            new Sty(Face.ATKINSON, false, 15, GREY), Align.L));
                    d.render(s, 56, 52);
                    return s;
                }, new Photo().rot(6).yaw(-0.06).pitch(0.03).blur(0.9).noise(2.8).gradient(0.25, 60).ground(Ground.DARK)));

        all.add(new Fixture("menu-curry-house", "menu", false,
                new String[] {"two-column", "light-on-dark", "dietary-markers", "sans"},
                seed -> {
                    Scene s = new Scene(1180, 880, seed);
                    s.surface(new Color(86, 22, 32), 4, 6);
                    s.border(new Color(222, 184, 90), 22, 3);
                    Sty gold = new Sty(Face.BRICOLAGE, true, 32, new Color(236, 196, 100));
                    Sty nm = new Sty(Face.BRICOLAGE, false, 25, new Color(250, 240, 224));
                    Sty pr = new Sty(Face.BRICOLAGE, true, 25, new Color(236, 196, 100));
                    Sty ds = new Sty(Face.BRICOLAGE, false, 18, new Color(224, 200, 196));
                    Doc d = menuDoc(2, false, """
                            # Starters
                            Onion bhaji (V)|£4.95
                            Chicken tikka|£5.95
                            Lamb samosa|£5.50
                            Paneer shashlik (V)|£6.25
                            # From the tandoor
                            Tandoori mixed grill|£13.95
                            Salmon tikka|£12.50
                            # Curries
                            Chicken tikka masala|£11.95|Creamy tomato and almond sauce
                            Lamb rogan josh|£12.95|Kashmiri chilli, fennel, yoghurt
                            Bombay aloo (VG)|£8.95
                            Saag paneer (V)|£9.95
                            Chana masala (VG)|£8.95
                            # Sides
                            Pilau rice|£3.50
                            Garlic naan|£3.25
                            Peshwari naan|£3.50
                            Mango chutney|£1.00
                            """, gold, nm, pr, ds, 20);
                    titleBlock(d, "Spice Garden", "Authentic Indian cuisine · Takeaway & delivery",
                            new Sty(Face.BRICOLAGE, true, 60, new Color(250, 240, 224)), new Sty(Face.BRICOLAGE, false, 24, new Color(236, 196, 100)));
                    d.render(s, 70, 56);
                    return s;
                }, new Photo().rot(-8).yaw(-0.08).pitch(0.0).zoom(1.05).blur(0.9).noise(3.0).gradient(0.2, 150).ground(Ground.WOOD).tint(1.05, 1.0, 0.88)));

        all.add(new Fixture("menu-brunch", "menu", true,
                new String[] {"single-column", "condensed", "shadow", "accents"},
                seed -> {
                    Scene s = new Scene(880, 1180, seed);
                    s.surface(new Color(252, 250, 244), 3, 4);
                    Sty hd = new Sty(Face.BARLOW, true, 36, new Color(200, 70, 40));
                    Sty nm = new Sty(Face.BARLOW, true, 30, INK);
                    Sty pr = new Sty(Face.BARLOW, false, 30, INK);
                    Sty ds = new Sty(Face.BARLOW, false, 22, GREY);
                    Doc d = menuDoc(1, false, """
                            # Eggs
                            Eggs Benedict|£9.50|Poached eggs, ham, hollandaise on a toasted muffin
                            Eggs Royale|£10.50|Smoked salmon, hollandaise
                            Shakshuka|£9.95|Baked eggs, spiced tomato, feta (V)
                            Full English|£11.95|Bacon, sausage, beans, mushrooms, toast
                            # Sweet
                            Buttermilk pancakes|£8.50|Maple syrup, blueberries, crème fraîche
                            French toast|£8.95|Brioche, streaky bacon, maple
                            Granola bowl|£6.95|Greek yoghurt, honey, fruit (V)
                            # Sides
                            Avocado|£2.50
                            Hash browns|£2.25
                            Sourdough toast|£2.00
                            """, hd, nm, pr, ds, 26);
                    titleBlock(d, "BRUNCH", "Served Saturday & Sunday, 09:00 – 15:00",
                            new Sty(Face.BARLOW, true, 92, INK), new Sty(Face.BARLOW, false, 30, GREY));
                    d.render(s, 70, 60);
                    return s;
                }, new Photo().rot(-5).yaw(0.07).blur(0.8).noise(2.5).gradient(0.1, 90).shadow(0.38, 35, 0.35, 0.12).ground(Ground.PLASTER)));

        all.add(new Fixture("menu-tapas", "menu", false,
                new String[] {"two-column", "accents", "kraft-paper", "rotated"},
                seed -> {
                    Scene s = new Scene(1180, 880, seed);
                    s.surface(new Color(184, 146, 102), 7, 9);
                    Sty hd = new Sty(Face.ATKINSON, true, 32, new Color(96, 28, 20));
                    Sty nm = new Sty(Face.ATKINSON, false, 26, new Color(36, 24, 18));
                    Sty pr = new Sty(Face.ATKINSON, true, 26, new Color(36, 24, 18));
                    Doc d = menuDoc(2, true, """
                            # Para picar
                            Patatas bravas|£5.50
                            Pimientos de Padrón|£5.95
                            Jamón ibérico|£9.50
                            Croquetas de jamón|£6.50
                            Tortilla española|£5.95
                            # Del mar
                            Gambas al ajillo|£8.95
                            Calamares a la romana|£7.50
                            Pulpo a la gallega|£10.50
                            # De la huerta
                            Ensalada de tomate|£5.50
                            Espinacas con garbanzos|£6.25
                            # Postres
                            Crema catalana|£5.95
                            Churros con chocolate|£5.95
                            """, hd, nm, pr, nm, 26);
                    titleBlock(d, "La Taberna", "Tapas & vino · Mesas para compartir",
                            new Sty(Face.ATKINSON, true, 64, new Color(36, 24, 18)), new Sty(Face.ATKINSON, false, 24, new Color(96, 28, 20)));
                    d.render(s, 70, 56);
                    return s;
                }, new Photo().rot(12).yaw(0.03).pitch(0.05).zoom(1.0).blur(1.1).noise(4.0).gradient(0.18, -100).ground(Ground.DARK)));

        all.add(new Fixture("menu-patisserie", "menu", false,
                new String[] {"accents", "serif", "soft-focus", "two-column"},
                seed -> {
                    Scene s = new Scene(1180, 880, seed);
                    s.surface(new Color(250, 236, 232), 4, 6);
                    s.border(new Color(180, 120, 120), 24, 2);
                    Sty hd = new Sty(Face.OLD_STANDARD, true, 34, new Color(150, 60, 70));
                    Sty nm = new Sty(Face.OLD_STANDARD, false, 29, INK);
                    Sty pr = new Sty(Face.OLD_STANDARD, true, 29, INK);
                    Doc d = menuDoc(2, true, """
                            # Viennoiseries
                            Croissant au beurre|£2.75
                            Pain au chocolat|£3.10
                            Pain aux raisins|£3.20
                            # Pâtisseries
                            Crème brûlée|£5.50
                            Éclair au chocolat|£4.25
                            Tarte au citron|£4.95
                            Mille-feuille|£4.95
                            Paris-Brest|£5.25
                            # Boissons
                            Café crème|£3.30
                            Chocolat chaud|£3.75
                            Thé Earl Grey|£2.95
                            """, hd, nm, pr, nm, 30);
                    d.leaderColor = new Color(150, 60, 70);
                    titleBlock(d, "Maison Dupont", "Boulangerie · Pâtisserie",
                            new Sty(Face.OLD_STANDARD, true, 66, INK), new Sty(Face.OLD_STANDARD, false, 28, new Color(150, 60, 70)));
                    d.render(s, 74, 60);
                    return s;
                }, new Photo().rot(-2).yaw(-0.03).blur(1.7).noise(2.2).gradient(0.1, 45).ground(Ground.WOOD)));

        all.add(new Fixture("menu-cocktails", "menu", true,
                new String[] {"low-light", "light-on-dark", "gold-on-black", "noisy"},
                seed -> {
                    Scene s = new Scene(880, 1180, seed);
                    s.surface(new Color(18, 18, 20), 3, 4);
                    Sty gold = new Sty(Face.BRICOLAGE, true, 32, new Color(214, 172, 86));
                    Sty nm = new Sty(Face.BRICOLAGE, false, 27, new Color(240, 232, 214));
                    Sty pr = new Sty(Face.BRICOLAGE, true, 27, new Color(214, 172, 86));
                    Sty ds = new Sty(Face.BRICOLAGE, false, 19, new Color(176, 168, 152));
                    Doc d = menuDoc(1, true, """
                            # Signature
                            Smoked Old Fashioned|£12.50|Bourbon, demerara, orange bitters
                            Espresso Martini|£11.00|Vodka, coffee liqueur, espresso
                            Elderflower Collins|£10.50|Gin, elderflower, lemon, soda
                            Rhubarb Sour|£11.50|Rhubarb gin, lemon, egg white
                            # Classics
                            Negroni|£10.00
                            Margarita|£10.50
                            Mojito|£10.00
                            Aperol Spritz|£9.50
                            # Zero proof
                            Virgin Mojito|£5.50
                            Ginger & Lime Fizz|£5.00
                            """, gold, nm, pr, ds, 28);
                    d.leaderColor = new Color(110, 100, 80);
                    titleBlock(d, "The Velvet Room", "Cocktails",
                            new Sty(Face.BRICOLAGE, true, 60, new Color(240, 232, 214)), new Sty(Face.BRICOLAGE, false, 28, new Color(214, 172, 86)));
                    d.render(s, 64, 58);
                    return s;
                }, new Photo().rot(9).yaw(0.05).pitch(-0.04).blur(1.3).lowLight(0.48, 6, 9.0).gradient(0.3, -60).ground(Ground.DARK).tint(1.08, 1.0, 0.85)));

        all.add(new Fixture("menu-chippy", "menu", false,
                new String[] {"two-column", "dotted-leaders", "condensed", "uppercase", "red-on-white"},
                seed -> {
                    Scene s = new Scene(1180, 880, seed);
                    s.surface(new Color(252, 248, 238), 4, 4);
                    s.border(new Color(190, 30, 36), 20, 6);
                    Color red = new Color(190, 30, 36);
                    Sty hd = new Sty(Face.BARLOW, true, 40, red);
                    Sty nm = new Sty(Face.BARLOW, true, 32, INK);
                    Sty pr = new Sty(Face.BARLOW, true, 32, red);
                    Doc d = menuDoc(2, true, """
                            # FISH
                            COD|£9.80
                            HADDOCK|£9.50
                            PLAICE|£8.95
                            ROCK SALMON|£8.50
                            SCAMPI|£7.90
                            # EXTRAS
                            CHIPS (SMALL)|£2.80
                            CHIPS (LARGE)|£3.60
                            MUSHY PEAS|£1.50
                            CURRY SAUCE|£1.50
                            PICKLED EGG|£1.20
                            BATTERED SAUSAGE|£2.60
                            SAVELOY|£2.20
                            CAN OF DRINK|£1.20
                            """, hd, nm, pr, nm, 28);
                    d.leaderColor = INK;
                    titleBlock(d, "THE GOLDEN FRYER", "TAKEAWAY MENU · OPEN 7 DAYS",
                            new Sty(Face.BARLOW, true, 84, red), new Sty(Face.BARLOW, false, 32, INK));
                    d.render(s, 74, 58);
                    return s;
                }, new Photo().rot(-3).yaw(0.09).pitch(0.0).blur(0.7).noise(2.4).jpeg(0.7).gradient(0.15, 0).ground(Ground.BRICK)));

        all.add(new Fixture("menu-sunday-roast", "menu", true,
                new String[] {"single-column", "serif", "rotated", "perspective"},
                seed -> {
                    Scene s = new Scene(880, 1180, seed);
                    s.surface(new Color(245, 238, 220), 5, 7);
                    s.fold(false, 440, 0.08);
                    Sty hd = new Sty(Face.OLD_STANDARD, true, 36, new Color(80, 50, 20));
                    Sty nm = new Sty(Face.OLD_STANDARD, true, 29, INK);
                    Sty pr = new Sty(Face.OLD_STANDARD, true, 29, INK);
                    Sty ds = new Sty(Face.OLD_STANDARD, false, 22, GREY);
                    Doc d = menuDoc(1, true, """
                            # Roasts
                            Roast beef|£16.95|Yorkshire pudding, roast potatoes, seasonal veg
                            Roast chicken|£14.95|Sage and onion stuffing, pigs in blankets
                            Slow-roast pork belly|£15.95|Crackling, apple sauce
                            Nut roast (VG)|£13.95|Cranberry gravy
                            # Extras
                            Cauliflower cheese|£4.50
                            Extra Yorkshire|£1.50
                            Roast potatoes|£3.75
                            Gravy boat|£1.00
                            # Puddings
                            Sticky toffee pudding|£6.50
                            Rhubarb crumble|£6.25
                            """, hd, nm, pr, ds, 30);
                    d.leaderColor = GREY;
                    titleBlock(d, "Sunday Lunch", "Served 12:00 – 17:00 · Please book by Saturday",
                            new Sty(Face.OLD_STANDARD, true, 76, INK), new Sty(Face.OLD_STANDARD, false, 26, GREY));
                    d.render(s, 66, 58);
                    return s;
                }, new Photo().rot(-13).yaw(-0.1).pitch(0.04).blur(0.9).noise(2.8).gradient(0.2, 120).ground(Ground.WOOD)));

        all.add(new Fixture("menu-kids", "menu", false,
                new String[] {"handwriting", "colourful", "rotated", "sparse"},
                seed -> {
                    Scene s = new Scene(1180, 880, seed);
                    s.surface(new Color(255, 244, 190), 4, 5);
                    Sty hd = new Sty(Face.KALAM, true, 38, new Color(220, 70, 60));
                    Sty nm = new Sty(Face.KALAM, false, 30, new Color(30, 50, 120));
                    Sty pr = new Sty(Face.KALAM, true, 30, new Color(30, 50, 120));
                    Doc d = menuDoc(2, true, """
                            # Little plates
                            Fish fingers & chips|£6.50
                            Mini cheese pizza|£6.00
                            Pasta with tomato sauce|£5.50
                            Chicken nuggets|£6.25
                            # Sweet treats
                            Ice cream (2 scoops)|£3.00
                            Fruit salad|£2.95
                            Chocolate brownie|£3.50
                            # Drinks
                            Apple juice|£1.80
                            Milkshake|£3.20
                            Squash|£1.20
                            """, hd, nm, pr, nm, 26);
                    d.leaderColor = new Color(30, 50, 120);
                    titleBlock(d, "Little Diners' Menu", "For under 12s",
                            new Sty(Face.KALAM, true, 66, new Color(30, 50, 120)), new Sty(Face.KALAM, false, 30, new Color(220, 70, 60)));
                    d.render(s, 70, 54);
                    return s;
                }, new Photo().rot(-12).yaw(0.06).pitch(-0.03).blur(1.0).noise(3.0).gradient(0.2, 200).ground(Ground.PLASTER)));

        all.add(new Fixture("menu-vegan-board", "menu", true,
                new String[] {"chalkboard", "handwriting", "light-on-dark", "single-column"},
                seed -> {
                    Scene s = new Scene(880, 1180, seed);
                    s.chalkboard(new Color(34, 40, 42));
                    s.frame(new Color(96, 70, 48), 30, true);
                    s.fx = Fx.CHALK;
                    Sty hd = new Sty(Face.KALAM, true, 38, new Color(150, 220, 140));
                    Sty nm = new Sty(Face.KALAM, false, 29, new Color(240, 238, 230));
                    Doc d = menuDoc(1, true, """
                            # Plant-based specials
                            Mushroom stroganoff|£11.50
                            Chickpea & spinach curry|£10.95
                            Roast veg Buddha bowl|£10.50
                            Lentil shepherd's pie|£11.25
                            # Sweet
                            Vegan brownie|£4.50
                            Oat milk flat white|£3.50
                            Coconut yoghurt pot|£3.95
                            """, hd, nm, nm, nm, 36);
                    d.leaderColor = new Color(200, 205, 195, 170);
                    titleBlock(d, "Green & Good", "All dishes are 100% vegan",
                            new Sty(Face.KALAM, true, 66, new Color(250, 222, 120)), new Sty(Face.KALAM, false, 28, new Color(205, 210, 200)));
                    d.render(s, 72, 66);
                    return s;
                }, new Photo().rot(6).yaw(-0.04).pitch(0.05).zoom(1.1).blur(1.1).noise(3.4).gradient(0.25, -20).ground(Ground.BRICK)));

        // ---- shop signs
        all.add(sign("sign-bakery", false, new String[] {"gold-on-green", "serif", "brick-wall", "perspective"},
                s -> {
                    s.surface(new Color(22, 62, 46), 3, 5);
                    s.border(new Color(222, 184, 90), 26, 5);
                    s.border(new Color(222, 184, 90), 40, 2);
                    Color gold = new Color(226, 188, 96);
                    Doc d = new Doc();
                    d.centreVertically = true;
                    d.header.add(head("THE CORNER BAKERY", new Sty(Face.OLD_STANDARD, true, 100, gold), Align.C, 0));
                    d.header.add(head("Est. 1952", new Sty(Face.OLD_STANDARD, false, 38, gold), Align.C, 0));
                    d.body.add(gap(26));
                    d.body.add(rule(gold, 2));
                    d.body.add(head("Fresh bread · Cakes · Pastries", new Sty(Face.OLD_STANDARD, false, 44, new Color(244, 236, 214)), Align.C, 22));
                    d.body.add(head("Open daily from 6am", new Sty(Face.OLD_STANDARD, false, 34, gold), Align.C, 10));
                    return d;
                }, new Photo().rot(-5).yaw(0.12).pitch(0.0).zoom(1.1).blur(0.9).noise(2.8).gradient(0.2, -20).ground(Ground.BRICK)));

        all.add(sign("sign-butcher", false, new String[] {"white-on-red", "bold", "bullets"},
                s -> {
                    s.surface(new Color(158, 24, 28), 4, 6);
                    s.border(new Color(246, 240, 230), 24, 4);
                    Color w = new Color(248, 244, 236);
                    Doc d = new Doc();
                    d.centreVertically = true;
                    d.header.add(head("J. HARGREAVES & SON", new Sty(Face.BRICOLAGE, true, 96, w), Align.C, 0));
                    d.header.add(head("FAMILY BUTCHERS", new Sty(Face.BRICOLAGE, false, 54, w), Align.C, 0));
                    d.body.add(gap(20));
                    d.body.add(head("Pies • Sausages • Game • Free-range poultry", new Sty(Face.BRICOLAGE, false, 36, new Color(250, 214, 150)), Align.C, 0));
                    return d;
                }, new Photo().rot(4).yaw(-0.07).pitch(0.03).zoom(1.1).blur(1.0).noise(3.0).gradient(0.2, 70).shadow(0.3, 160, 0.7, 0.2).ground(Ground.BRICK)));

        all.add(sign("sign-pharmacy-hours", true, new String[] {"opening-hours", "table", "white-on-blue", "phone-number"},
                s -> {
                    s.surface(new Color(20, 82, 150), 3, 4);
                    s.border(new Color(240, 244, 250), 28, 4);
                    Color w = new Color(244, 247, 252);
                    Sty rs = new Sty(Face.ATKINSON, false, 40, w);
                    Sty rb = new Sty(Face.ATKINSON, true, 40, w);
                    Doc d = new Doc();
                    d.centreVertically = true;
                    d.header.add(head("Riverside Pharmacy", new Sty(Face.ATKINSON, true, 66, w), Align.C, 0));
                    d.header.add(head("Opening hours", new Sty(Face.ATKINSON, false, 40, new Color(170, 210, 250)), Align.C, 0));
                    d.body.add(gap(14));
                    d.body.add(row("Monday – Friday", "09:00 – 18:00", rs, rb));
                    d.body.add(row("Saturday", "09:00 – 17:30", rs, rb));
                    d.body.add(row("Sunday", "Closed", rs, rb));
                    d.body.add(row("Bank holidays", "10:00 – 16:00", rs, rb));
                    d.body.add(gap(28));
                    d.body.add(rule(w, 2));
                    d.body.add(head("NHS prescriptions · Flu jabs · Health advice", new Sty(Face.ATKINSON, false, 28, w), Align.C, 16));
                    d.body.add(head("Tel: 020 7946 0123", new Sty(Face.ATKINSON, true, 34, w), Align.C, 16));
                    return d;
                }, new Photo().rot(7).yaw(0.05).pitch(0.08).zoom(1.05).blur(0.8).noise(2.5).gradient(0.15, -45).ground(Ground.PLASTER)));

        all.add(sign("sign-barber", false, new String[] {"neon-glow", "low-light", "condensed", "light-on-dark"},
                s -> {
                    s.surface(new Color(10, 10, 14), 2, 3);
                    s.fx = Fx.GLOW;
                    s.glow = new Color(70, 150, 255);
                    Color w = new Color(232, 244, 255);
                    Doc d = new Doc();
                    d.centreVertically = true;
                    d.header.add(head("SHARP & CO.", new Sty(Face.BARLOW, true, 190, w), Align.C, 0));
                    d.header.add(head("BARBERS", new Sty(Face.BARLOW, false, 100, w), Align.C, 0));
                    d.body.add(gap(24));
                    d.body.add(head("Walk-ins welcome · Haircuts from £15", new Sty(Face.BARLOW, false, 52, new Color(190, 220, 255)), Align.C, 0));
                    return d;
                }, new Photo().rot(-6).yaw(0.05).pitch(-0.03).zoom(1.1).blur(1.2).lowLight(0.62, 4, 7.0).ground(Ground.BRICK).tint(0.9, 1.0, 1.1)));

        all.add(sign("sign-chippy", false, new String[] {"yellow-on-blue", "condensed", "bold"},
                s -> {
                    s.surface(new Color(14, 52, 124), 3, 5);
                    s.border(new Color(250, 214, 40), 24, 6);
                    Color y = new Color(252, 218, 48);
                    Doc d = new Doc();
                    d.centreVertically = true;
                    d.header.add(head("THE CODFATHER", new Sty(Face.BARLOW, true, 170, y), Align.C, 0));
                    d.header.add(head("FISH & CHIPS", new Sty(Face.BARLOW, true, 90, new Color(246, 246, 240)), Align.C, 0));
                    d.body.add(gap(20));
                    d.body.add(head("Eat in · Takeaway · Open till 10pm", new Sty(Face.BARLOW, false, 48, y), Align.C, 0));
                    return d;
                }, new Photo().rot(4).yaw(-0.09).pitch(0.0).blur(0.8).noise(2.6).gradient(0.25, 100).ground(Ground.BRICK)));

        all.add(sign("sign-door-closed", true, new String[] {"handwriting", "opening-hours", "card", "rotated"},
                s -> {
                    s.surface(new Color(250, 246, 236), 4, 4);
                    s.border(new Color(160, 30, 36), 26, 5);
                    Color red = new Color(160, 30, 36);
                    Sty rs = new Sty(Face.KALAM, false, 40, INK);
                    Sty rb = new Sty(Face.KALAM, true, 40, INK);
                    Doc d = new Doc();
                    d.centreVertically = true;
                    d.header.add(head("Sorry,", new Sty(Face.KALAM, true, 110, red), Align.C, 0));
                    d.header.add(head("we're closed", new Sty(Face.KALAM, true, 110, red), Align.C, 0));
                    d.body.add(gap(18));
                    d.body.add(head("Back at 2.30pm", new Sty(Face.KALAM, false, 56, INK), Align.C, 0));
                    d.body.add(gap(20));
                    d.body.add(rule(red, 2));
                    d.body.add(head("Opening hours", new Sty(Face.KALAM, true, 38, red), Align.C, 14));
                    d.body.add(row("Mon – Sat", "8am – 5pm", rs, rb));
                    d.body.add(row("Sunday", "Closed", rs, rb));
                    return d;
                }, new Photo().rot(-10).yaw(0.04).pitch(0.06).zoom(1.05).blur(1.0).noise(2.8).gradient(0.12, 30).ground(Ground.WOOD)));

        all.add(sign("sign-pub", false, new String[] {"gold-on-black", "serif", "glow", "low-light", "brick-wall"},
                s -> {
                    s.surface(new Color(14, 14, 14), 2, 3);
                    s.border(new Color(206, 164, 80), 22, 3);
                    s.fx = Fx.GLOW;
                    s.glow = new Color(220, 150, 40);
                    Color gold = new Color(236, 196, 100);
                    Doc d = new Doc();
                    d.centreVertically = true;
                    d.header.add(head("THE ROYAL OAK", new Sty(Face.OLD_STANDARD, true, 130, gold), Align.C, 0));
                    d.header.add(head("FREE HOUSE", new Sty(Face.OLD_STANDARD, false, 60, gold), Align.C, 0));
                    d.body.add(gap(24));
                    d.body.add(head("Real Ales • Fine Wines • Home-cooked food", new Sty(Face.OLD_STANDARD, false, 40, new Color(240, 230, 204)), Align.C, 0));
                    return d;
                }, new Photo().rot(5).yaw(0.1).pitch(0.03).zoom(1.1).blur(1.3).lowLight(0.55, 5, 8.0).ground(Ground.BRICK).tint(1.08, 1.0, 0.82)));

        all.add(sign("sign-hardware", false, new String[] {"black-on-yellow", "condensed", "bold", "rotated"},
                s -> {
                    s.surface(new Color(244, 198, 26), 4, 5);
                    s.border(new Color(24, 24, 24), 22, 8);
                    Color k = new Color(24, 24, 22);
                    Doc d = new Doc();
                    d.centreVertically = true;
                    d.header.add(head("DAVIES HARDWARE", new Sty(Face.BARLOW, true, 180, k), Align.C, 0));
                    d.body.add(gap(16));
                    d.body.add(head("Keys cut while you wait", new Sty(Face.BARLOW, true, 70, k), Align.C, 0));
                    d.body.add(head("Paint • Tools • Garden • Fixings", new Sty(Face.BARLOW, false, 56, k), Align.C, 14));
                    return d;
                }, new Photo().rot(9).yaw(-0.06).pitch(0.04).zoom(1.1).blur(0.8).noise(2.6).gradient(0.22, 10).shadow(0.28, 20, 0.8, 0.15).ground(Ground.PAVEMENT)));

        all.add(sign("sign-newsagent", false, new String[] {"white-on-navy", "sans", "bullets", "small-text"},
                s -> {
                    s.surface(new Color(18, 36, 84), 3, 5);
                    s.border(new Color(240, 240, 240), 24, 3);
                    Color w = new Color(244, 245, 248);
                    Doc d = new Doc();
                    d.centreVertically = true;
                    d.header.add(head("NEWSAGENT", new Sty(Face.ATKINSON, true, 120, w), Align.C, 0));
                    d.header.add(head("& CONVENIENCE", new Sty(Face.ATKINSON, true, 62, new Color(250, 214, 80)), Align.C, 0));
                    d.body.add(gap(20));
                    d.body.add(head("Lottery • Cards • Stamps • Milk & bread", new Sty(Face.ATKINSON, false, 38, w), Align.C, 0));
                    d.body.add(head("Open 6am – 10pm, 7 days", new Sty(Face.ATKINSON, false, 30, new Color(180, 200, 240)), Align.C, 16));
                    return d;
                }, new Photo().rot(-7).yaw(0.08).pitch(-0.04).zoom(1.05).blur(1.0).noise(3.2).gradient(0.25, -120).ground(Ground.BRICK)));

        all.add(sign("sign-a-board", true, new String[] {"chalkboard", "handwriting", "pavement", "prices"},
                s -> {
                    s.chalkboard(new Color(30, 32, 34));
                    s.frame(new Color(70, 50, 34), 30, true);
                    s.fx = Fx.CHALK;
                    Color w = new Color(244, 242, 234);
                    Sty rs = new Sty(Face.KALAM, false, 42, w);
                    Doc d = new Doc();
                    d.centreVertically = true;
                    d.leader = true;
                    d.leaderColor = new Color(200, 205, 195, 170);
                    d.header.add(head("Today's specials", new Sty(Face.KALAM, true, 74, new Color(250, 222, 120)), Align.C, 0));
                    d.body.add(gap(14));
                    d.body.add(row("Soup of the day", "£5.50", rs, rs));
                    d.body.add(row("Jacket potato", "£6.95", rs, rs));
                    d.body.add(row("Quiche & salad", "£7.50", rs, rs));
                    d.body.add(row("Bacon roll", "£3.80", rs, rs));
                    d.body.add(gap(18));
                    d.body.add(head("Fresh coffee 'til 4pm", new Sty(Face.KALAM, true, 46, new Color(150, 220, 140)), Align.C, 0));
                    return d;
                }, new Photo().rot(8).yaw(-0.08).pitch(0.07).zoom(1.08).blur(1.2).noise(3.4).gradient(0.2, 60).shadow(0.3, 90, 0.75, 0.2).ground(Ground.PAVEMENT)));

        // ---- receipts
        all.add(receipt("receipt-supermarket", "supermarket", new String[] {"long", "monospace", "small-print", "narrow"},
                20, new Photo().rot(5).yaw(0.03).pitch(0.04).blur(0.8).noise(2.8).gradient(0.15, 60).ground(Ground.WOOD),
                """
                        Semi-skimmed milk 2L|£1.45
                        Hovis wholemeal loaf|£1.35
                        Free-range eggs x12|£3.10
                        Cheddar mature 400g|£3.75
                        Bananas 0.742kg|£0.82
                        Chicken thighs 1kg|£4.20
                        Baked beans 4pk|£2.40
                        Tea bags 80s|£2.15
                        Penne pasta 500g|£0.95
                        Chopped tomatoes|£0.65
                        Crème fraîche 300ml|£1.20
                        Orange juice 1L|£1.85
                        Dishwasher tablets|£4.50
                        Bag for life|£0.30
                        """,
                new String[] {"FRESHWAY SUPERMARKETS", "Northfield Local", "14 Market Street, Northfield", "Tel 0113 496 0412"},
                new String[] {"Receipt 004217   14/09/2025 17:42", "Card payment (contactless)", "Thank you for shopping with us", "Please keep your receipt"}));

        all.add(receipt("receipt-cafe", "cafe", new String[] {"short", "monospace", "rotated"},
                24, new Photo().rot(-9).yaw(-0.05).pitch(0.03).zoom(1.05).blur(0.9).noise(2.6).gradient(0.2, -80).ground(Ground.DARK),
                """
                        Flat white|£3.20
                        Almond croissant|£3.60
                        Bacon roll|£4.80
                        Orange juice|£3.00
                        """,
                new String[] {"THE DAILY GRIND", "22 Station Road", "Table 7   Server: Priya"},
                new String[] {"Card ending 4821", "09/11/2025 08:17", "Thanks for visiting!", "See you again soon"}));

        all.add(receipt("receipt-pharmacy", "pharmacy", new String[] {"monospace", "faded-ink", "medium"},
                22, new Photo().rot(3).yaw(0.06).pitch(-0.02).blur(1.0).noise(3.0).gradient(0.12, 10).ground(Ground.PLASTER),
                """
                        Paracetamol 500mg 16s|£1.50
                        Ibuprofen 200mg 16s|£2.25
                        Plasters assorted|£2.99
                        Hand cream 50ml|£3.49
                        Cough syrup 150ml|£5.99
                        Throat lozenges|£2.79
                        """,
                new String[] {"RIVERSIDE PHARMACY", "9 Bridge Street", "Pharmacist: A. Khan MRPharmS", "Tel 0161 496 0188"},
                new String[] {"Receipt 11873   02/10/2025 12:05", "Please read the label before use", "Keep out of reach of children"}));

        all.add(receipt("receipt-takeaway", "takeaway", new String[] {"quantities", "monospace", "order-number"},
                23, new Photo().rot(-6).yaw(0.04).pitch(0.05).blur(0.9).noise(3.2).gradient(0.2, 120).shadow(0.3, 30, 0.8, 0.15).ground(Ground.WOOD),
                """
                        1 x Chicken tikka masala|£10.95
                        1 x Pilau rice|£3.20
                        2 x Garlic naan|£6.50
                        1 x Onion bhaji|£4.50
                        Delivery|£2.00
                        """,
                new String[] {"SPICE GARDEN", "ORDER 0417 - DELIVERY", "Tel 0121 496 0733"},
                new String[] {"Paid: cash", "Allergies? Please ask us", "Thank you - enjoy your meal"}));

        all.add(receipt("receipt-petrol", "petrol", new String[] {"numbers", "monospace", "units"},
                23, new Photo().rot(8).yaw(-0.04).pitch(0.02).blur(0.8).noise(2.6).gradient(0.15, -30).ground(Ground.PAVEMENT),
                """
                        Pump 4 Unleaded|£60.26
                        Litres|42.17
                        Price per litre|142.9p
                        Meal deal|£3.50
                        """,
                new String[] {"KINGSWAY SERVICE STATION", "A64 Eastbound", "VAT No 123 4567 89"},
                new String[] {"Card ending 0912", "21/10/2025 07:48", "Drive safely"}));

        all.add(receipt("receipt-restaurant", "restaurant", new String[] {"service-charge", "monospace", "accents"},
                21, new Photo().rot(-4).yaw(0.07).pitch(0.0).blur(0.9).noise(2.8).gradient(0.15, 150).ground(Ground.WOOD),
                """
                        Pasta arrabbiata|£12.50
                        Spaghetti alle vongole|£15.00
                        Branzino al forno|£19.95
                        Tiramisù|£6.95
                        Panna cotta|£6.50
                        Mineral water 75cl|£3.50
                        House red 500ml|£14.50
                        """,
                new String[] {"TRATTORIA DA MARCO", "Table 12   Covers 4", "Server: Luca"},
                new String[] {"Service charge 12.5% included", "Thank you - grazie mille!", "Bill printed 21:34"}));

        all.add(parkingTicket());

        all.add(receipt("receipt-returns", "returns", new String[] {"low-light", "monospace", "refund"},
                22, new Photo().rot(6).yaw(-0.05).pitch(0.04).blur(1.3).lowLight(0.5, 6, 8.0).gradient(0.3, 40).ground(Ground.DARK),
                """
                        Cotton shirt (blue) M|-£24.00
                        Socks 3pk|-£6.50
                        """,
                new String[] {"HIGH STREET FASHION", "RETURN / REFUND", "Reason: wrong size"},
                new String[] {"Refund to original card", "Ref RT-90412   03/11/2025", "Goods returned within 28 days"}));

        // ---- labels
        all.add(label("label-cereal", false, new String[] {"nutrition-table", "ingredients", "small-print", "two-column"}, labelCereal(),
                new Photo().rot(-4).yaw(0.05).pitch(0.03).blur(0.8).noise(2.6).gradient(0.12, 30).ground(Ground.WOOD)));
        all.add(label("label-medicine", false, new String[] {"dosage", "warnings", "small-print", "two-column"}, labelMedicine(),
                new Photo().rot(5).yaw(-0.06).pitch(0.02).blur(0.9).noise(2.6).gradient(0.15, -60).ground(Ground.PLASTER)));
        all.add(label("label-soup-tin", true, new String[] {"light-on-dark", "ingredients", "small-print", "red"}, labelSoup(),
                new Photo().rot(-7).yaw(0.07).pitch(0.03).blur(1.0).noise(3.0).gradient(0.2, 100).ground(Ground.DARK)));
        all.add(label("label-shampoo", true, new String[] {"ingredient-list", "tiny-print", "condensed", "blurred"}, labelShampoo(),
                new Photo().rot(10).yaw(-0.05).pitch(0.05).blur(1.5).noise(3.4).gradient(0.2, 0).ground(Ground.PLASTER)));
        all.add(label("label-crisps", false, new String[] {"nutrition-table", "allergens", "yellow", "small-print"}, labelCrisps(),
                new Photo().rot(-3).yaw(0.08).pitch(0.0).blur(0.9).noise(2.8).jpeg(0.7).gradient(0.15, 160).shadow(0.3, 60, 0.8, 0.15).ground(Ground.WOOD)));
        all.add(label("label-olive-oil", true, new String[] {"serif", "accents", "centred", "product-origin"}, labelOil(),
                new Photo().rot(4).yaw(0.05).pitch(-0.03).blur(0.9).noise(2.6).gradient(0.12, -30).ground(Ground.WOOD)));
        all.add(label("label-tea-box", false, new String[] {"serif", "gold-on-red", "brewing-guide", "small-print"}, labelTea(),
                new Photo().rot(-11).yaw(-0.07).pitch(0.04).blur(1.0).noise(3.0).gradient(0.2, 80).ground(Ground.PLASTER)));
        return all;
    }

    interface SignBuilder {
        Doc build(Scene s);
    }

    static Fixture sign(String id, boolean portrait, String[] tags, SignBuilder b, Photo p) {
        return new Fixture(id, "sign", portrait, tags, seed -> {
            Scene s = portrait ? new Scene(880, 1180, seed) : new Scene(1180, 880, seed);
            Doc d = b.build(s);
            d.render(s, 62, 56);
            return s;
        }, p);
    }

    static Fixture receipt(String id, String name, String[] tags, double size, Photo p, String items, String[] top, String[] bottom) {
        List<String> tg = new ArrayList<>(List.of(tags));
        return new Fixture(id, "receipt", true, tg.toArray(new String[0]), seed -> {
            Color ink = new Color(36, 36, 42);
            Sty body = new Sty(Face.COURIER, false, size, ink);
            Sty bold = new Sty(Face.COURIER, true, size, ink);
            Sty big = new Sty(Face.COURIER, true, size * 1.25, ink);
            Doc d = new Doc();
            d.maxScale = 1;
            d.minScale = 1;
            d.cols = 1;
            for (int i = 0; i < top.length; i++) {
                d.header.add(head(top[i], i == 0 ? big : body, Align.C, 0));
            }
            d.header.add(gap(10));
            d.header.add(rule(ink, 1.5));
            int total = 0;
            for (String ln : items.strip().split("\n")) {
                String[] pr = ln.split("\\|");
                d.body.add(row(pr[0], pr[1], body, body));
                if (pr[1].startsWith("£")) {
                    total += cents(pr[1]);
                } else if (pr[1].startsWith("-£")) {
                    total -= cents(pr[1].substring(1));
                }
            }
            d.body.add(rule(ink, 1.5));
            if (name.equals("restaurant")) {
                int service = (int) Math.round(total * 0.125);
                d.body.add(row("Subtotal", money(total), body, body));
                d.body.add(row("Service 12.5%", money(service), body, body));
                total += service;
            } else if (name.equals("supermarket")) {
                d.body.add(row("Subtotal", money(total), body, body));
                d.body.add(row("VAT A 20%", money((int) Math.round(total / 6.0)), body, body));
            }
            if (name.equals("returns")) {
                d.body.add(row("REFUND", "-" + money(-total), bold, bold));
            } else {
                d.body.add(row("TOTAL", money(total), bold, bold));
            }
            d.footer.add(gap(6));
            for (String b : bottom) {
                d.footer.add(head(b, body, Align.C, 0));
            }
            double width = 560;
            double inner = width - 2 * 36;
            double height = d.heightAt(inner, 1) + 2 * 40;
            Scene s = new Scene((int) width, (int) Math.ceil(height), seed);
            s.surface(new Color(250, 249, 244), 2, 2);
            s.fx = Fx.FADE;
            d.render(s, 36, 40);
            return s;
        }, p);
    }

    static Fixture parkingTicket() {
        return new Fixture("receipt-parking-ticket", "receipt", false,
                new String[] {"ticket", "monospace", "large-type", "perspective"}, seed -> {
                    Color ink = new Color(30, 30, 36);
                    Sty body = new Sty(Face.COURIER, false, 40, ink);
                    Sty bold = new Sty(Face.COURIER, true, 40, ink);
                    Sty big = new Sty(Face.COURIER, true, 58, ink);
                    Doc d = new Doc();
                    d.maxScale = 1;
                    d.minScale = 1;
                    d.header.add(head("PAY & DISPLAY", big, Align.C, 0));
                    d.header.add(head("Riverside Road Car Park", body, Align.C, 0));
                    d.header.add(gap(8));
                    d.header.add(rule(ink, 2));
                    d.body.add(row("Date", "14/09/2025", body, bold));
                    d.body.add(row("Purchased", "13:05", body, bold));
                    d.body.add(row("Valid until", "16:45", body, bold));
                    d.body.add(row("Amount paid", "£3.60", body, bold));
                    d.footer.add(head("Display face up on the dashboard", body.sized(30), Align.C, 0));
                    double w = 780;
                    double h = d.heightAt(w - 100, 1) + 90;
                    Scene s = new Scene((int) w, (int) Math.ceil(h), seed);
                    s.surface(new Color(246, 226, 150), 3, 3);
                    s.fx = Fx.FADE;
                    d.render(s, 50, 45);
                    return s;
                }, new Photo().rot(-8).yaw(0.09).pitch(0.05).zoom(1.15).blur(0.9).noise(2.8).gradient(0.2, 40).ground(Ground.PAVEMENT));
    }

    static Fixture label(String id, boolean portrait, String[] tags, Function<Long, Scene> content, Photo p) {
        return new Fixture(id, "label", portrait, tags, content, p);
    }

    static final double[] NUTRI = {0.72, 1.0};

    static Function<Long, Scene> labelCereal() {
        return seed -> {
            Scene s = new Scene(1180, 880, seed);
            s.surface(new Color(252, 244, 222), 3, 4);
            s.border(new Color(196, 120, 30), 20, 4);
            Color brown = new Color(120, 66, 20);
            Sty t = new Sty(Face.ATKINSON, false, 22, INK);
            Sty tb = new Sty(Face.ATKINSON, true, 22, INK);
            Sty sm = new Sty(Face.ATKINSON, false, 16, new Color(60, 52, 46));
            Doc d = new Doc();
            d.cols = 2;
            d.header.add(head("Honey Nut Crunch", new Sty(Face.BRICOLAGE, true, 66, brown), Align.L, 0));
            d.header.add(head("Wholegrain cereal with honey and almonds", new Sty(Face.ATKINSON, false, 28, INK), Align.L, 0));
            d.body.add(head("Nutrition information", new Sty(Face.ATKINSON, true, 28, brown), Align.L, 0));
            d.body.add(table(tb, 0, NUTRI, "Typical values", "per 100g", "per 40g"));
            d.body.add(rule(INK, 1.2));
            d.body.add(table(t, 0, NUTRI, "Energy", "1590kJ", "636kJ"));
            d.body.add(table(t, 0, NUTRI, "", "377kcal", "151kcal"));
            d.body.add(table(t, 0, NUTRI, "Fat", "5.2g", "2.1g"));
            d.body.add(table(t, 24, NUTRI, "of which saturates", "0.9g", "0.4g"));
            d.body.add(table(t, 0, NUTRI, "Carbohydrate", "71g", "28g"));
            d.body.add(table(t, 24, NUTRI, "of which sugars", "18g", "7.2g"));
            d.body.add(table(t, 0, NUTRI, "Fibre", "7.4g", "3.0g"));
            d.body.add(table(t, 0, NUTRI, "Protein", "9.1g", "3.6g"));
            d.body.add(table(t, 0, NUTRI, "Salt", "0.30g", "0.12g"));
            d.body.add(head("Ingredients", new Sty(Face.ATKINSON, true, 28, brown), Align.L, 0));
            d.body.add(para("Wholegrain OATS (62%), sugar, honey (4%), ALMONDS (3%), sunflower oil, salt, natural flavouring. May contain other NUTS, MILK and SULPHITES.", t, Align.L));
            d.body.add(head("Storage and allergy advice", new Sty(Face.ATKINSON, true, 28, brown), Align.L, 0));
            d.body.add(para("For allergens, including cereals containing GLUTEN, see ingredients in CAPITALS. Store in a cool, dry place. Best before: see top of pack.", sm, Align.L));
            d.body.add(para("This pack contains 14 servings. Not suitable for children under 3 years due to the risk of choking.", sm, Align.L));
            d.maxScale = 1.25;
            d.centreVertically = true;
            d.render(s, 56, 48);
            return s;
        };
    }

    static Function<Long, Scene> labelMedicine() {
        return seed -> {
            Scene s = new Scene(1180, 880, seed);
            s.surface(new Color(250, 252, 252), 2, 3);
            Color teal = new Color(0, 112, 124);
            s.bgG.setColor(teal);
            s.bgG.fillRect(0, 0, 1180, 18);
            s.bgG.fillRect(0, 862, 1180, 18);
            Sty t = new Sty(Face.ATKINSON, false, 21, INK);
            Sty sm = new Sty(Face.ATKINSON, false, 16, new Color(50, 54, 58));
            Doc d = new Doc();
            d.cols = 2;
            d.header.add(head("Paracetamol 500mg Tablets", new Sty(Face.ATKINSON, true, 64, teal), Align.L, 0));
            d.header.add(head("Pain relief · Reduces fever", new Sty(Face.ATKINSON, false, 30, INK), Align.L, 0));
            d.header.add(head("For adults and children over 12 years · 16 tablets", new Sty(Face.ATKINSON, false, 24, INK), Align.L, 0));
            d.body.add(head("How to take", new Sty(Face.ATKINSON, true, 26, teal), Align.L, 0));
            d.body.add(para("Adults and children aged 16 and over: take 1 to 2 tablets every 4 to 6 hours as required. Do not take more than 8 tablets in 24 hours.", t, Align.L));
            d.body.add(para("Children 12 to 15 years: take 1 tablet every 4 to 6 hours as required. Do not take more than 4 doses in 24 hours. Do not give to children under 12.", t, Align.L));
            d.body.add(head("Warnings", new Sty(Face.ATKINSON, true, 26, teal), Align.L, 14));
            d.body.add(para("Do not take with any other paracetamol-containing products. Contact a doctor if symptoms persist for more than 3 days.", t, Align.L));
            d.body.add(head("Storage and information", new Sty(Face.ATKINSON, true, 26, teal), Align.L, 14));
            d.body.add(para("Keep out of the sight and reach of children. Read the enclosed leaflet before use. Store below 25°C in the original package.", sm, Align.L));
            d.body.add(para("Each tablet contains 500mg paracetamol. PL 00000/0000   Batch: AB1234   Expiry: 08/2027", sm, Align.L));
            d.maxScale = 1.25;
            d.centreVertically = true;
            d.render(s, 60, 54);
            return s;
        };
    }

    static Function<Long, Scene> labelSoup() {
        return seed -> {
            Scene s = new Scene(880, 1180, seed);
            s.surface(new Color(170, 28, 30), 4, 6);
            s.border(new Color(250, 236, 200), 26, 3);
            Color cream = new Color(250, 240, 214);
            Sty t = new Sty(Face.BRICOLAGE, false, 22, cream);
            Sty sm = new Sty(Face.BRICOLAGE, false, 17, new Color(244, 220, 200));
            Doc d = new Doc();
            d.header.add(head("Cream of", new Sty(Face.BRICOLAGE, false, 54, cream), Align.C, 0));
            d.header.add(head("Tomato", new Sty(Face.BRICOLAGE, true, 120, cream), Align.C, 0));
            d.header.add(head("SOUP", new Sty(Face.BRICOLAGE, true, 46, new Color(250, 214, 120)), Align.C, 0));
            d.header.add(head("with a hint of basil · Serves 2 · 400g e", new Sty(Face.BRICOLAGE, false, 26, cream), Align.C, 0));
            d.body.add(head("Ingredients", new Sty(Face.BRICOLAGE, true, 26, new Color(250, 214, 120)), Align.L, 0));
            d.body.add(para("Tomatoes (68%), water, cream (MILK) (5%), sugar, modified maize starch, salt, basil, black pepper.", t, Align.L));
            d.body.add(head("Preparation", new Sty(Face.BRICOLAGE, true, 26, new Color(250, 214, 120)), Align.L, 14));
            d.body.add(para("Heat in a pan or microwave. Stir well before serving. Once opened, transfer contents to a non-metallic container and refrigerate. Use within 2 days.", t, Align.L));
            d.body.add(para("Best before end: see base of can. Allergy advice: for allergens, see ingredients in CAPITALS.", sm, Align.L));
            d.maxScale = 1.2;
            d.centreVertically = true;
            d.render(s, 66, 62);
            return s;
        };
    }

    static Function<Long, Scene> labelShampoo() {
        return seed -> {
            Scene s = new Scene(880, 1180, seed);
            s.surface(new Color(240, 244, 240), 2, 3);
            Color green = new Color(34, 92, 66);
            s.bgG.setColor(green);
            s.bgG.fillRect(0, 0, 880, 22);
            Sty t = new Sty(Face.BARLOW, false, 22, INK);
            Sty sm = new Sty(Face.BARLOW, false, 16, new Color(50, 56, 52));
            Doc d = new Doc();
            d.header.add(head("Nourishing", new Sty(Face.BARLOW, true, 70, green), Align.L, 0));
            d.header.add(head("Argan Shampoo", new Sty(Face.BARLOW, true, 70, green), Align.L, 0));
            d.header.add(head("For dry & damaged hair · 250ml", new Sty(Face.BARLOW, false, 30, INK), Align.L, 0));
            d.body.add(head("Directions", new Sty(Face.BARLOW, true, 28, green), Align.L, 0));
            d.body.add(para("Apply to wet hair, massage gently into a lather, then rinse thoroughly. Avoid contact with eyes. If contact occurs, rinse well with water.", t, Align.L));
            d.body.add(head("Ingredients", new Sty(Face.BARLOW, true, 28, green), Align.L, 16));
            d.body.add(para("Aqua, Sodium Laureth Sulfate, Cocamidopropyl Betaine, Glycerin, Argania Spinosa Kernel Oil, Sodium Chloride, Parfum, Citric Acid, Sodium Benzoate, Polyquaternium-10, Linalool, Limonene, Hexyl Cinnamal.", sm, Align.L));
            d.body.add(para("Made in the UK. Please recycle this bottle. Best used within 12 months of opening. Not tested on animals.", sm, Align.L));
            d.maxScale = 1.2;
            d.centreVertically = true;
            d.render(s, 62, 60);
            return s;
        };
    }

    static Function<Long, Scene> labelCrisps() {
        return seed -> {
            Scene s = new Scene(1180, 880, seed);
            s.surface(new Color(250, 214, 40), 4, 6);
            s.border(new Color(30, 60, 150), 20, 5);
            Color navy = new Color(24, 44, 110);
            Sty t = new Sty(Face.BRICOLAGE, false, 20, INK);
            Sty tb = new Sty(Face.BRICOLAGE, true, 20, INK);
            Sty sm = new Sty(Face.BRICOLAGE, false, 15, INK);
            Doc d = new Doc();
            d.cols = 2;
            d.header.add(head("Salt & Vinegar", new Sty(Face.BRICOLAGE, true, 88, navy), Align.L, 0));
            d.header.add(head("Crinkle-cut potato crisps · 34g e", new Sty(Face.BRICOLAGE, false, 30, INK), Align.L, 0));
            d.body.add(head("Ingredients", new Sty(Face.BRICOLAGE, true, 26, navy), Align.L, 0));
            d.body.add(para("Potatoes (62%), Sunflower Oil (33%), Salt & Vinegar Seasoning [Maltodextrin, Salt, Acid (Sodium Acetates, Malic Acid), Flavouring, Barley Malt Vinegar Powder (BARLEY), Yeast Extract].", t, Align.L));
            d.body.add(head("Allergy advice", new Sty(Face.BRICOLAGE, true, 26, navy), Align.L, 14));
            d.body.add(para("For allergens, see ingredients in CAPITALS. Contains BARLEY (gluten). May contain milk.", t, Align.L));
            d.body.add(para("Best before: see top of pack.", sm, Align.L));
            d.body.add(head("Nutrition", new Sty(Face.BRICOLAGE, true, 26, navy), Align.L, 0));
            d.body.add(table(tb, 0, NUTRI, "Typical values", "per 100g", "per 34g"));
            d.body.add(rule(INK, 1.2));
            d.body.add(table(t, 0, NUTRI, "Energy", "2090kJ", "711kJ"));
            d.body.add(table(t, 0, NUTRI, "", "502kcal", "171kcal"));
            d.body.add(table(t, 0, NUTRI, "Fat", "31g", "11g"));
            d.body.add(table(t, 24, NUTRI, "of which saturates", "2.8g", "1.0g"));
            d.body.add(table(t, 0, NUTRI, "Carbohydrate", "49g", "17g"));
            d.body.add(table(t, 0, NUTRI, "Protein", "6.0g", "2.0g"));
            d.body.add(table(t, 0, NUTRI, "Salt", "1.8g", "0.6g"));
            d.maxScale = 1.25;
            d.centreVertically = true;
            d.render(s, 56, 50);
            return s;
        };
    }

    static Function<Long, Scene> labelOil() {
        return seed -> {
            Scene s = new Scene(880, 1180, seed);
            s.surface(new Color(240, 232, 196), 4, 6);
            Color green = new Color(48, 74, 30);
            s.border(green, 28, 4);
            s.border(green, 40, 1.5);
            Doc d = new Doc();
            d.centreVertically = true;
            d.header.add(head("Olio Extra Vergine", new Sty(Face.OLD_STANDARD, true, 72, green), Align.C, 0));
            d.header.add(head("di Oliva", new Sty(Face.OLD_STANDARD, true, 72, green), Align.C, 0));
            d.header.add(gap(10));
            d.header.add(head("Extra Virgin Olive Oil", new Sty(Face.OLD_STANDARD, false, 44, INK), Align.C, 0));
            d.header.add(head("First cold pressed · Qualità superiore", new Sty(Face.OLD_STANDARD, false, 30, INK), Align.C, 0));
            d.body.add(gap(20));
            d.body.add(rule(green, 2));
            d.body.add(head("Product of Italy", new Sty(Face.OLD_STANDARD, true, 36, INK), Align.C, 14));
            d.body.add(head("500ml e", new Sty(Face.OLD_STANDARD, true, 36, INK), Align.C, 6));
            d.body.add(gap(20));
            d.body.add(para("Produced and bottled by Frantoio Rossi, Toscana, Italia. Store in a cool, dark place. Best before end: see neck of bottle.", new Sty(Face.OLD_STANDARD, false, 20, GREY), Align.C));
            d.maxScale = 1.2;
            d.centreVertically = true;
            d.render(s, 70, 66);
            return s;
        };
    }

    static Function<Long, Scene> labelTea() {
        return seed -> {
            Scene s = new Scene(1180, 880, seed);
            s.surface(new Color(122, 24, 28), 4, 6);
            s.border(new Color(222, 184, 90), 22, 3);
            Color gold = new Color(232, 192, 100);
            Color cream = new Color(248, 238, 214);
            Doc d = new Doc();
            d.cols = 2;
            d.header.add(head("Old Mill", new Sty(Face.OLD_STANDARD, false, 40, gold), Align.C, 0));
            d.header.add(head("English Breakfast Tea", new Sty(Face.OLD_STANDARD, true, 84, cream), Align.C, 0));
            d.header.add(head("80 Tea Bags · 250g e", new Sty(Face.OLD_STANDARD, false, 34, gold), Align.C, 0));
            d.body.add(head("A rich, full-bodied blend", new Sty(Face.OLD_STANDARD, true, 28, gold), Align.L, 0));
            d.body.add(para("Carefully blended from Assam and Kenyan teas for a robust, malty cup that stands up to milk.", new Sty(Face.OLD_STANDARD, false, 22, cream), Align.L));
            d.body.add(head("Brewing guide", new Sty(Face.OLD_STANDARD, true, 28, gold), Align.L, 16));
            d.body.add(para("Pour freshly boiled water over one tea bag, leave to brew for 3 to 4 minutes, then add milk to taste.", new Sty(Face.OLD_STANDARD, false, 22, cream), Align.L));
            d.body.add(head("Ingredients", new Sty(Face.OLD_STANDARD, true, 28, gold), Align.L, 0));
            d.body.add(para("Black tea.", new Sty(Face.OLD_STANDARD, false, 22, cream), Align.L));
            d.body.add(para("Best before end: see side of box. Packed in the UK from tea sourced from Kenya and India. Please recycle the box.", new Sty(Face.OLD_STANDARD, false, 17, new Color(226, 206, 186)), Align.L));
            d.maxScale = 1.25;
            d.centreVertically = true;
            d.render(s, 66, 56);
            return s;
        };
    }

    // ---------------------------------------------------------------- driver

    static Path findRoot() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null) {
            if (Files.exists(p.resolve("tools/ocr-fixtures/Generate.java"))) {
                return p;
            }
            p = p.getParent();
        }
        throw new IllegalStateException("Run from inside the repository");
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        if (args.length < 1) {
            System.err.println("Usage: java tools/ocr-fixtures/Generate.java OUTDIR [OVERLAYDIR]");
            System.exit(2);
        }
        repoRoot = findRoot();
        Path out = Path.of(args[0]);
        Path overlayDir = args.length > 1 ? Path.of(args[1]) : null;
        Files.createDirectories(out);
        if (overlayDir != null) {
            Files.createDirectories(overlayDir);
        }
        List<Fixture> all = fixtures();
        int index = 0;
        long bytes = 0;
        for (Fixture f : all) {
            long seed = BASE_SEED + index * 101L;
            index++;
            int fw = f.portrait() ? LAND_H : LAND_W;
            int fh = f.portrait() ? LAND_W : LAND_H;
            Scene sc = f.scene().apply(seed);
            Photo p = f.photo();
            double[] h = chooseHomography(sc, p, fw, fh);
            BufferedImage flat = sc.compose();
            float[][] ch = warp(flat, h, fw, fh, p.ground, seed);
            BufferedImage img = develop(ch, fw, fh, p, new Random(seed), seed);
            Path jpg = out.resolve(f.id() + ".jpg");
            writeJpeg(img, jpg, p.jpeg);
            String json = groundTruth(f, sc, p, h, fw, fh, String.join(", ", sc.fonts));
            Path jsonFile = out.resolve(f.id() + ".json");
            Files.writeString(jsonFile, json, StandardCharsets.UTF_8);
            validate(f, sc, h, fw, fh);
            if (overlayDir != null) {
                overlay(img, sc, h, overlayDir.resolve(f.id() + ".png"));
            }
            long size = Files.size(jpg) + Files.size(jsonFile);
            bytes += size;
            System.out.printf(Locale.ROOT, "%-28s %dx%d  %3d lines  %4d KB%n", f.id(), fw, fh, sc.lines.size(), size / 1024);
        }
        if (all.size() < 30) {
            throw new IllegalStateException("Fewer than 30 fixtures: " + all.size());
        }
        if (bytes > 15L * 1024 * 1024) {
            throw new IllegalStateException("Fixture set is larger than 15 MB: " + bytes);
        }
        System.out.printf(Locale.ROOT, "%d fixtures, %.1f MB%n", all.size(), bytes / 1048576.0);
    }

    static void validate(Fixture f, Scene sc, double[] h, int fw, int fh) {
        if (sc.lines.isEmpty()) {
            throw new IllegalStateException(f.id() + ": no lines");
        }
        for (Line l : sc.lines) {
            if (l.text().isBlank() || l.words().isEmpty()) {
                throw new IllegalStateException(f.id() + ": empty line or no words: " + l.text());
            }
            String joined = String.join(" ", l.words().stream().map(Word::text).toList());
            if (!joined.equals(l.text().trim().replaceAll(" +", " "))) {
                throw new IllegalStateException(f.id() + ": words do not rebuild line: " + l.text());
            }
            for (double[] pt : poly(h, l.x0(), l.x1(), l.y0(), l.y1()).toArray(new double[0][])) {
                if (pt[0] < 0 || pt[1] < 0 || pt[0] > fw || pt[1] > fh) {
                    throw new IllegalStateException(f.id() + ": polygon outside image: " + l.text());
                }
            }
        }
    }
}
