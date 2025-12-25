package com.swordshield.content.controller;

import com.swordshield.common.Response;
import com.swordshield.content.service.FileServiceClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/content")
@Validated
@Tag(name = "海报", description = "动态生成海报并可上传到文件服务")
public class PosterController {
    private static final Logger log = LoggerFactory.getLogger(PosterController.class);
    private final FileServiceClient fileServiceClient;

    public PosterController(FileServiceClient fileServiceClient) {
        this.fileServiceClient = fileServiceClient;
    }

    @PostMapping(value = "/poster", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(summary = "生成海报", description = "根据标题/副标题生成品牌海报，可返回 base64 或上传文件服务")
	    public Response<String> poster(@RequestParam(name = "title") @NotBlank(message = "title 不能为空") String title,
	                                   @RequestParam(name = "subtitle") @NotBlank(message = "subtitle 不能为空") String subtitle,
	                                   @RequestHeader(name = "Authorization", required = false) String authorization) throws Exception {
	        BufferedImage img = new BufferedImage(800, 1200, BufferedImage.TYPE_INT_ARGB);
	        Graphics2D g = img.createGraphics();
	        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
	        
	        int width = img.getWidth();
	        int height = img.getHeight();

	        // Theme colors aligned with the web UI
	        Color bgTop = new Color(227, 238, 255);      // #e3eeff
	        Color bgBase = new Color(243, 246, 251);     // #f3f6fb
	        Color cardBg = new Color(255, 255, 255, 245);
	        Color cardBorder = new Color(191, 219, 254, 230); // rgba(191,219,254,.9)
	        Color textPrimary = new Color(17, 24, 39);   // #111827
	        Color textMuted = new Color(75, 85, 99);     // #4b5563
	        Color accent1 = new Color(37, 99, 235);      // #2563eb
	        Color accent2 = new Color(56, 189, 248);     // #38bdf8

	        // Slight per-title variation within the same palette
	        int colorSeed = title.hashCode();
	        java.util.Random rand = new java.util.Random(colorSeed);
	        float accentShift = (rand.nextInt(21) - 10) / 100f; // [-0.10, +0.10]
	        Color accentSoft = new Color(
	                clampColor(224 + Math.round(20 * accentShift)),
	                clampColor(236 + Math.round(10 * accentShift)),
	                255
	        );

	        // Background radial wash like the app shell
	        java.awt.RadialGradientPaint bg = new java.awt.RadialGradientPaint(
	                new java.awt.geom.Point2D.Float(width * 0.35f, 0),
	                Math.max(width, height) * 1.05f,
	                new float[]{0f, 0.55f, 1f},
	                new Color[]{accentSoft, bgTop, bgBase}
	        );
	        g.setPaint(bg);
	        g.fillRect(0, 0, width, height);

	        // Subtle decorative dots/bubbles
	        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.14f));
	        for (int i = 0; i < 10; i++) {
	            int radius = 70 + rand.nextInt(140);
	            int cx = rand.nextInt(width);
	            int cy = rand.nextInt(height);
	            g.setPaint(new GradientPaint(
	                    cx - radius, cy - radius, new Color(accent2.getRed(), accent2.getGreen(), accent2.getBlue(), 80),
	                    cx + radius, cy + radius, new Color(accent1.getRed(), accent1.getGreen(), accent1.getBlue(), 40)
	            ));
	            g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
	        }
	        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

	        // Header bar
	        int headerH = 96;
	        g.setColor(new Color(255, 255, 255, 210));
	        g.fillRect(0, 0, width, headerH);
	        g.setColor(new Color(15, 23, 42, 18));
	        g.fillRect(0, headerH - 1, width, 1);

	        // Brand mark (rounded square + gradient)
	        int markSize = 62;
	        int markX = 36;
	        int markY = 18;
	        g.setColor(new Color(15, 23, 42, 28));
	        g.fillRoundRect(markX + 2, markY + 6, markSize, markSize, 18, 18);
	        java.awt.RadialGradientPaint mark = new java.awt.RadialGradientPaint(
	                new java.awt.geom.Point2D.Float(markX + markSize * 0.35f, markY + markSize * 0.15f),
	                markSize * 0.95f,
	                new float[]{0f, 0.55f, 1f},
	                new Color[]{accent1, accent2, new Color(229, 240, 255)}
	        );
	        g.setPaint(mark);
	        g.fillRoundRect(markX, markY, markSize, markSize, 18, 18);
	        g.setColor(new Color(191, 219, 254, 210));
	        g.drawRoundRect(markX, markY, markSize, markSize, 18, 18);

	        g.setFont(new Font("SansSerif", Font.BOLD, 26));
	        g.setColor(new Color(249, 250, 251));
	        String markText = "SS";
	        FontMetrics markFm = g.getFontMetrics();
	        int markTx = markX + (markSize - markFm.stringWidth(markText)) / 2;
	        int markTy = markY + (markSize - markFm.getHeight()) / 2 + markFm.getAscent();
	        g.drawString(markText, markTx, markTy);

	        // Brand text
	        g.setFont(new Font("SansSerif", Font.BOLD, 22));
	        g.setColor(textPrimary);
	        String brand = "西湖论剑";
	        FontMetrics brandFm = g.getFontMetrics();
	        int brandTextX = markX + markSize + 16;
	        int brandTextY = markY + 30;
	        g.drawString(brand, brandTextX, brandTextY);

	        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
	        g.setColor(new Color(156, 163, 175)); // #9ca3af
	        String brandEn = "DIGITAL SECURITY CONFERENCE";
	        g.drawString(brandEn, brandTextX, brandTextY + 22);

	        // Main content card
	        int cardX = 56;
	        int cardY = 170;
	        int cardW = width - cardX * 2;
	        int cardH = 860;
	        int cardR = 28;

	        // shadow
	        g.setColor(new Color(15, 23, 42, 28));
	        g.fillRoundRect(cardX + 2, cardY + 14, cardW, cardH, cardR, cardR);

	        g.setColor(cardBg);
	        g.fillRoundRect(cardX, cardY, cardW, cardH, cardR, cardR);
	        g.setColor(cardBorder);
	        g.drawRoundRect(cardX, cardY, cardW, cardH, cardR, cardR);

	        // Top accent strip inside card
	        g.setPaint(new GradientPaint(cardX, cardY, accent1, cardX + cardW, cardY, accent2));
	        g.fillRoundRect(cardX, cardY, cardW, 10, cardR, cardR);

	        // Tag pill
	        int pillX = cardX + 28;
	        int pillY = cardY + 28;
	        g.setColor(new Color(224, 236, 255));
	        g.fillRoundRect(pillX, pillY, 120, 34, 18, 18);
	        g.setColor(new Color(37, 99, 235));
	        g.setFont(new Font("SansSerif", Font.BOLD, 14));
	        g.drawString("智能海报", pillX + 20, pillY + 23);

	        // Title
	        Font titleFont = new Font("SansSerif", Font.BOLD, 52);
	        g.setFont(titleFont);
	        FontMetrics titleFm = g.getFontMetrics(titleFont);
	        int textMaxW = cardW - 80;
	        String[] titleLines = wrapText(title, titleFm, textMaxW);
	        int titleY = pillY + 110;
	        int titleLineH = 64;
	        for (String line : titleLines) {
	            int x = cardX + (cardW - titleFm.stringWidth(line)) / 2;
	            g.setColor(textPrimary);
	            g.drawString(line, x, titleY);
	            titleY += titleLineH;
	        }

	        // Accent underline
	        int underlineW = Math.min(280, cardW - 120);
	        int underlineX = cardX + (cardW - underlineW) / 2;
	        int underlineY = titleY - 24;
	        g.setPaint(new GradientPaint(underlineX, underlineY, accent1, underlineX + underlineW, underlineY, accent2));
	        g.fillRoundRect(underlineX, underlineY, underlineW, 6, 6, 6);

	        // Subtitle
	        Font subFont = new Font("SansSerif", Font.PLAIN, 26);
	        g.setFont(subFont);
	        FontMetrics subFm = g.getFontMetrics(subFont);
	        String[] subLines = wrapText(subtitle, subFm, textMaxW);
	        int subY = underlineY + 52;
	        int subLineH = 42;
	        for (String line : subLines) {
	            int x = cardX + (cardW - subFm.stringWidth(line)) / 2;
	            g.setColor(textMuted);
	            g.drawString(line, x, subY);
	            subY += subLineH;
	        }

	        // Middle content to reduce whitespace
	        int contentTopY = subY + 18;

	        // Layout: centered content + bottom-right QR block (avoid overlaps)
	        int innerPad = 28;
	        int gutter = 18;
	        int qrSize = 100;
	        int qrPad = 18;
	        int qrContainerH = qrSize + qrPad * 2 + 46;
	        int qrContainerW = qrSize + qrPad * 2;
	        int qrRightPad = 18;
	        int qrBottomPad = 18;

	        // QR block anchored to bottom-right inside card
	        int qrContainerX = cardX + cardW - qrRightPad - qrContainerW;
	        int qrContainerY = cardY + cardH - qrBottomPad - qrContainerH;
	        int qrX = qrContainerX + qrPad;
	        int qrY = qrContainerY + qrPad;

	        // Reserve some space above the footer for readability
	        int footerTopY = cardY + cardH - innerPad - 64;
	        int contentBottomY = Math.min(qrContainerY - 22, footerTopY - 18);

	        // Feature pills (2x2) centered in the card (above QR/footer)
	        String[] features = new String[]{"会议议程", "嘉宾分享", "安全前沿", "现场互动"};
	        int pillH = 52;
	        int pillW = Math.min(260, (cardW - innerPad * 2 - gutter) / 2);
	        int pillsTotalH = pillH * 2 + 14;
	        int pillsGroupW = pillW * 2 + gutter;
	        int pillsX = cardX + (cardW - pillsGroupW) / 2;
	        int pillsMaxY = contentBottomY - pillsTotalH;
	        int pillsMinY = contentTopY + 210;
	        int pillsY = Math.max(pillsMinY, pillsMaxY);
	        if (pillsY > pillsMaxY) {
	            pillsY = pillsMaxY;
	        }

	        // Compute shield size based on available height above the pills.
	        int shieldGap = 22;
	        int shieldY = contentTopY + 6;
	        int shieldHMax = pillsY - shieldGap - shieldY;
	        int shieldH = Math.min(420, shieldHMax);
	        if (shieldH < 120) {
	            shieldH = 0;
	        }
	        if (shieldH > 0) {
	            shieldH = Math.max(220, shieldH);
	            int shieldW = Math.max(220, Math.min(340, Math.round(shieldH * 0.92f)));
	            int shieldX = cardX + (cardW - shieldW) / 2;
	            drawShieldOutline(g, shieldX, shieldY, shieldW, shieldH, accent1, 0.11f);
	            drawShieldOutline(g, shieldX + 12, shieldY + 16, shieldW - 24, shieldH - 32, accent2, 0.09f);
	        }

	        // Ensure pills never overlap the shield (in case of tight layouts)
	        int pillsMinFromShield = shieldY + shieldH + shieldGap;
	        if (pillsY < pillsMinFromShield) {
	            pillsY = pillsMinFromShield;
	        }
	        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
	        FontMetrics featFm = g.getFontMetrics();
	        for (int i = 0; i < features.length; i++) {
	            int col = i % 2;
	            int row = i / 2;
	            int x = pillsX + col * (pillW + gutter);
	            int y = pillsY + row * (pillH + 14);

	            g.setColor(new Color(239, 246, 255)); // #eff6ff
	            g.fillRoundRect(x, y, pillW, pillH, 18, 18);
	            g.setColor(new Color(37, 99, 235, 90));
	            g.drawRoundRect(x, y, pillW, pillH, 18, 18);

	            // Center dot + label as a group inside the pill
	            String label = features[i];
	            int dotR = 10;
	            int dotCy = y + pillH / 2;
	            int labelW = featFm.stringWidth(label);
	            int contentW = dotR * 2 + 10 + labelW;
	            int startX = x + (pillW - contentW) / 2;
	            int dotCx = startX + dotR;
	            g.setPaint(new GradientPaint(dotCx - dotR, dotCy - dotR, accent1, dotCx + dotR, dotCy + dotR, accent2));
	            g.fillOval(dotCx - dotR, dotCy - dotR, dotR * 2, dotR * 2);

	            g.setColor(textPrimary);
	            int tx = startX + dotR * 2 + 10;
	            int ty = y + (pillH - featFm.getHeight()) / 2 + featFm.getAscent();
	            g.drawString(label, tx, ty);
	        }

	        // QR-style placeholder block (right column)
	        g.setColor(new Color(255, 255, 255, 255));
	        g.fillRoundRect(qrContainerX, qrContainerY, qrContainerW, qrContainerH, 18, 18);
	        g.setColor(new Color(37, 99, 235, 70));
	        g.drawRoundRect(qrContainerX, qrContainerY, qrContainerW, qrContainerH, 18, 18);
	        drawQrPlaceholder(g, qrX, qrY, qrSize, colorSeed);
	        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
	        g.setColor(new Color(107, 114, 128));
	        g.drawString("扫码查看详情", qrContainerX + 18, qrY + qrSize + 28);

	        // Footer meta inside card
	        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
	        g.setColor(new Color(107, 114, 128)); // #6b7280
	        String timestamp = java.time.LocalDateTime.now().format(
	                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
	        );
	        String footer = "西湖论剑·数字安全大会";
	        FontMetrics metaFm = g.getFontMetrics();
	        int metaY = cardY + cardH - 54;
	        g.drawString(timestamp, cardX + innerPad, metaY);
	        g.drawString(footer, cardX + innerPad, metaY + 24);
	        
	        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] bytes = baos.toByteArray();

        if (authorization == null || authorization.isBlank()) {
            String dataUrl = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(bytes);
            return Response.ok(dataUrl);
        }
        try {
            String filename = "poster-" + Instant.now().toEpochMilli() + ".png";
            String url = fileServiceClient.uploadAndPresign(bytes, filename, authorization, 600);
            if (url != null && !url.isBlank()) {
                return Response.ok(url);
            }
        } catch (Exception e) {
            log.warn("Poster upload failed", e);
        }
        String dataUrl = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(bytes);
        return Response.ok(dataUrl);
    }
    
	    private String[] wrapText(String text, FontMetrics fm, int maxWidth) {
	        if (fm.stringWidth(text) <= maxWidth) {
	            return new String[]{text};
	        }
	        
	        java.util.List<String> lines = new java.util.ArrayList<>();
	        boolean hasSpaces = text.matches(".*\\s+.*");
	        String[] words = hasSpaces ? text.trim().split("\\s+") : null;
	        StringBuilder currentLine = new StringBuilder();

	        if (hasSpaces) {
	            for (String word : words) {
	                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
	                if (fm.stringWidth(testLine) <= maxWidth) {
	                    if (currentLine.length() > 0) currentLine.append(" ");
	                    currentLine.append(word);
	                    continue;
	                }
	                if (currentLine.length() > 0) {
	                    lines.add(currentLine.toString());
	                    currentLine = new StringBuilder();
	                }
	                if (fm.stringWidth(word) <= maxWidth) {
	                    currentLine.append(word);
	                    continue;
	                }
	                // If a single word is too long, fallback to character wrapping.
	                for (String part : wrapByCharacters(word, fm, maxWidth)) {
	                    lines.add(part);
	                }
	            }
	        } else {
	            for (String part : wrapByCharacters(text, fm, maxWidth)) {
	                lines.add(part);
	            }
	            return lines.toArray(new String[0]);
	        }
	        
	        if (currentLine.length() > 0) {
	            lines.add(currentLine.toString());
	        }
	        
	        return lines.toArray(new String[0]);
	    }

	    private java.util.List<String> wrapByCharacters(String text, FontMetrics fm, int maxWidth) {
	        java.util.List<String> out = new java.util.ArrayList<>();
	        StringBuilder line = new StringBuilder();
	        for (int i = 0; i < text.length(); i++) {
	            String ch = String.valueOf(text.charAt(i));
	            String test = line + ch;
	            if (fm.stringWidth(test) <= maxWidth) {
	                line.append(ch);
	                continue;
	            }
	            if (line.length() > 0) {
	                out.add(line.toString());
	                line = new StringBuilder(ch);
	                continue;
	            }
	            // Extremely narrow maxWidth: still emit something.
	            out.add(ch);
	        }
	        if (line.length() > 0) {
	            out.add(line.toString());
	        }
	        return out;
	    }

	    private int clampColor(int v) {
	        return Math.max(0, Math.min(255, v));
	    }

	    private void drawShieldOutline(Graphics2D g, int x, int y, int w, int h, Color baseColor, float alpha) {
	        java.awt.geom.GeneralPath path = new java.awt.geom.GeneralPath();
	        float left = x;
	        float top = y;
	        float right = x + w;
	        float bottom = y + h;
	        float midX = x + w / 2f;

	        // A simple shield silhouette
	        path.moveTo(midX, top);
	        path.curveTo(left + w * 0.22f, top + h * 0.02f, left + w * 0.06f, top + h * 0.20f, left + w * 0.10f, top + h * 0.36f);
	        path.curveTo(left + w * 0.16f, top + h * 0.64f, left + w * 0.30f, top + h * 0.84f, midX, bottom);
	        path.curveTo(right - w * 0.30f, top + h * 0.84f, right - w * 0.16f, top + h * 0.64f, right - w * 0.10f, top + h * 0.36f);
	        path.curveTo(right - w * 0.06f, top + h * 0.20f, right - w * 0.22f, top + h * 0.02f, midX, top);
	        path.closePath();

	        Stroke old = g.getStroke();
	        Composite oldComp = g.getComposite();
	        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
	        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
	        g.setColor(baseColor);
	        g.draw(path);
	        g.setComposite(oldComp);
	        g.setStroke(old);
	    }

	    private void drawQrPlaceholder(Graphics2D g, int x, int y, int size, int seed) {
	        java.util.Random random = new java.util.Random(seed ^ 0x9E3779B9);
	        int cell = 6;
	        int grid = size / cell;
	        int usable = grid * cell;

	        // Background
	        g.setColor(new Color(255, 255, 255));
	        g.fillRect(x, y, usable, usable);
	        g.setColor(new Color(17, 24, 39, 24));
	        g.drawRect(x, y, usable, usable);

	        // Finder patterns
	        drawFinder(g, x + cell, y + cell, cell);
	        drawFinder(g, x + usable - cell * 8, y + cell, cell);
	        drawFinder(g, x + cell, y + usable - cell * 8, cell);

	        // Random modules (avoid finder zones)
	        g.setColor(new Color(17, 24, 39));
	        for (int r = 0; r < grid; r++) {
	            for (int c = 0; c < grid; c++) {
	                if (isInFinderZone(r, c, grid)) continue;
	                if (random.nextFloat() < 0.32f) {
	                    g.fillRect(x + c * cell, y + r * cell, cell, cell);
	                }
	            }
	        }
	    }

	    private void drawFinder(Graphics2D g, int x, int y, int cell) {
	        g.setColor(new Color(17, 24, 39));
	        g.fillRect(x, y, cell * 7, cell * 7);
	        g.setColor(Color.WHITE);
	        g.fillRect(x + cell, y + cell, cell * 5, cell * 5);
	        g.setColor(new Color(17, 24, 39));
	        g.fillRect(x + cell * 2, y + cell * 2, cell * 3, cell * 3);
	    }

	    private boolean isInFinderZone(int r, int c, int grid) {
	        boolean topLeft = r <= 8 && c <= 8;
	        boolean topRight = r <= 8 && c >= grid - 9;
	        boolean bottomLeft = r >= grid - 9 && c <= 8;
	        return topLeft || topRight || bottomLeft;
	    }
}
