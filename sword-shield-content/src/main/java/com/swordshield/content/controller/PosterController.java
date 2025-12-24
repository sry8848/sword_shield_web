package com.swordshield.content.controller;

import com.swordshield.common.Response;
import com.swordshield.file.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/content")
@Tag(name = "海报", description = "动态生成海报并可上传到文件服务")
public class PosterController {
    @Autowired(required = false)
    private FileService fileService;

    @PostMapping(value = "/poster", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(summary = "生成海报", description = "根据标题/副标题生成品牌海报，可返回 base64 或上传文件服务")
    public Response<String> poster(@RequestParam(name = "title") String title,
                                   @RequestParam(name = "subtitle") String subtitle) throws Exception {
        BufferedImage img = new BufferedImage(800, 1200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        int width = img.getWidth();
        int height = img.getHeight();
        
        // Generate unique color scheme based on title hash
        int colorSeed = title.hashCode();
        java.util.Random rand = new java.util.Random(colorSeed);
        
        // Premium gradient background with Douyin-inspired colors
        Color brandColor1 = new Color(255, 0, 80);   // #ff0050 - Douyin pink
        Color brandColor2 = new Color(0, 242, 234);  // #00f2ea - Douyin cyan
        Color brandColor3 = new Color(0, 0, 0);      // #000000 - Black accent
        
        // Create multi-layer gradient
        GradientPaint mainGradient = new GradientPaint(
            0, 0, brandColor1,
            width, height, brandColor2
        );
        g.setPaint(mainGradient);
        g.fillRect(0, 0, width, height);
        
        // Add decorative geometric shapes for uniqueness
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
        
        // Random circles
        for (int i = 0; i < 5; i++) {
            int cx = rand.nextInt(width);
            int cy = rand.nextInt(height);
            int radius = 100 + rand.nextInt(200);
            g.setColor(new Color(255, 255, 255, 30));
            g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
        }
        
        // Decorative triangles
        for (int i = 0; i < 3; i++) {
            int[] xPoints = {rand.nextInt(width), rand.nextInt(width), rand.nextInt(width)};
            int[] yPoints = {rand.nextInt(height), rand.nextInt(height), rand.nextInt(height)};
            g.setColor(new Color(255, 255, 255, 20));
            g.fillPolygon(xPoints, yPoints, 3);
        }
        
        // Reset composite
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        
        // Add top decorative bar
        g.setColor(new Color(255, 255, 255, 80));
        g.fillRect(0, 0, width, 8);
        
        // Add shield icon at top
        g.setColor(new Color(255, 255, 255, 200));
        g.setFont(new Font("SansSerif", Font.BOLD, 80));
        String shieldIcon = "🛡️";
        FontMetrics iconFm = g.getFontMetrics();
        int iconX = (width - iconFm.stringWidth(shieldIcon)) / 2;
        g.drawString(shieldIcon, iconX, 150);
        
        // Draw "西湖论剑" branding
        g.setFont(new Font("SansSerif", Font.BOLD, 32));
        g.setColor(new Color(255, 255, 255, 220));
        String branding = "西湖论剑";
        FontMetrics brandFm = g.getFontMetrics();
        int brandX = (width - brandFm.stringWidth(branding)) / 2;
        g.drawString(branding, brandX, 200);
        
        // Add decorative line under branding
        g.setStroke(new BasicStroke(2));
        g.setColor(new Color(255, 255, 255, 150));
        int lineY = 220;
        g.drawLine(width/2 - 100, lineY, width/2 + 100, lineY);
        
        // Main title with enhanced styling
        Font titleFont = new Font("SansSerif", Font.BOLD, 56);
        g.setFont(titleFont);
        FontMetrics fmTitle = g.getFontMetrics(titleFont);
        
        // Wrap title if too long
        String[] titleLines = wrapText(title, fmTitle, width - 120);
        int titleStartY = 350;
        
        for (int i = 0; i < titleLines.length; i++) {
            int titleW = fmTitle.stringWidth(titleLines[i]);
            int titleX = (width - titleW) / 2;
            int titleY = titleStartY + (i * 70);
            
            // Draw title with glow effect
            g.setColor(new Color(255, 255, 255, 80));
            for (int j = 8; j > 0; j--) {
                g.drawString(titleLines[i], titleX - j/2, titleY - j/2);
            }
            
            // Draw main title
            g.setColor(Color.WHITE);
            g.drawString(titleLines[i], titleX, titleY);
        }
        
        // Subtitle with box background
        Font subFont = new Font("SansSerif", Font.PLAIN, 32);
        g.setFont(subFont);
        FontMetrics fmSub = g.getFontMetrics(subFont);
        String[] subLines = wrapText(subtitle, fmSub, width - 120);
        
        int subStartY = titleStartY + (titleLines.length * 70) + 80;
        
        for (int i = 0; i < subLines.length; i++) {
            int subW = fmSub.stringWidth(subLines[i]);
            int subX = (width - subW) / 2;
            int subY = subStartY + (i * 50);
            
            // Background box for subtitle
            g.setColor(new Color(255, 255, 255, 30));
            g.fillRoundRect(subX - 20, subY - 35, subW + 40, 50, 25, 25);
            
            // Subtitle text
            g.setColor(new Color(255, 255, 255, 230));
            g.drawString(subLines[i], subX, subY);
        }
        
        // Add timestamp at bottom
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g.setColor(new Color(255, 255, 255, 180));
        String timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        );
        FontMetrics timeFm = g.getFontMetrics();
        int timeX = (width - timeFm.stringWidth(timestamp)) / 2;
        g.drawString(timestamp, timeX, height - 100);
        
        // Add decorative bottom section
        g.setColor(new Color(255, 255, 255, 50));
        int[] bottomX = {0, width, width, 0};
        int[] bottomY = {height - 60, height - 60, height, height};
        g.fillPolygon(bottomX, bottomY, 4);
        
        // Add "Digital Security Conference" text at bottom
        g.setFont(new Font("SansSerif", Font.ITALIC, 18));
        g.setColor(new Color(255, 255, 255, 200));
        String footer = "西湖论剑·数字安全大会";
        FontMetrics footerFm = g.getFontMetrics();
        int footerX = (width - footerFm.stringWidth(footer)) / 2;
        g.drawString(footer, footerX, height - 30);
        
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] bytes = baos.toByteArray();

        if (fileService != null) {
            String objectName = "poster-" + Instant.now().toEpochMilli() + ".png";
            fileService.putObject(objectName, objectName, "image/png", bytes.length, new ByteArrayInputStream(bytes), null);
            String url = fileService.presignedGet(objectName, 600);
            return Response.ok(url);
        }
        String dataUrl = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(bytes);
        return Response.ok(dataUrl);
    }
    
    private String[] wrapText(String text, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(text) <= maxWidth) {
            return new String[]{text};
        }
        
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (fm.stringWidth(testLine) <= maxWidth) {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines.toArray(new String[0]);
    }
}
