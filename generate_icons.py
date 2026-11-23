import os
import math
from PIL import Image, ImageDraw, ImageFilter

def create_alarm_icon(size=512):
    # RGBA image
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # 1. Gradient Yumaloq Fon (Squircle Background)
    # Ranglar: Neon Indigo (#4F46E5) dan To'q Binafsha (#1E1B4B) gacha
    radius = int(size * 0.22)
    padding = int(size * 0.05)
    
    # Asosiy fon
    for i in range(size - 2 * padding):
        ratio = i / (size - 2 * padding)
        r = int(79 * (1 - ratio) + 30 * ratio)
        g = int(70 * (1 - ratio) + 27 * ratio)
        b = int(229 * (1 - ratio) + 75 * ratio)
        draw.line(
            [(padding, padding + i), (size - padding, padding + i)],
            fill=(r, g, b, 255)
        )
    
    # Mask rounded rectangle
    mask = Image.new("L", (size, size), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.rounded_rectangle(
        [padding, padding, size - padding, size - padding],
        radius=radius,
        fill=255
    )
    
    bg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    bg.paste(img, (0, 0), mask=mask)
    img = bg
    draw = ImageDraw.Draw(img)

    # 2. Budilnik oyoqchalari (Feet)
    foot_color = (226, 232, 240, 255)
    # Chap oyoq
    draw.polygon([(160, 390), (135, 435), (155, 442), (185, 400)], fill=foot_color)
    # O'ng oyoq
    draw.polygon([(352, 390), (377, 435), (357, 442), (327, 400)], fill=foot_color)

    # 3. Budilnik qo'ng'iroqlari (Amber / Gold Bells)
    bell_color = (245, 158, 11, 255) # Oltin sariq
    bell_highlight = (251, 191, 36, 255)
    # Chap qo'ng'iroq
    draw.pieslice([110, 110, 210, 210], 120, 300, fill=bell_color)
    draw.arc([110, 110, 210, 210], 120, 300, fill=bell_highlight, width=6)
    # O'ng qo'ng'iroq
    draw.pieslice([302, 110, 402, 210], 240, 60, fill=bell_color)
    draw.arc([302, 110, 402, 210], 240, 60, fill=bell_highlight, width=6)

    # Yuqori tugma (Top hammer/button)
    draw.rounded_rectangle([236, 120, 276, 150], radius=8, fill=(241, 245, 249, 255))

    # 4. Asosiy Soat Tanasi (Clock Body) - Katta oq/krem disk
    cx, cy = 256, 280
    r_outer = 135
    # Tashqi qirrasi (Soya va hoshiya)
    draw.ellipse([cx - r_outer, cy - r_outer, cx + r_outer, cy + r_outer], fill=(255, 255, 255, 255))
    draw.ellipse([cx - r_outer + 8, cy - r_outer + 8, cx + r_outer - 8, cy + r_outer - 8], fill=(248, 250, 252, 255))

    # Soat siferblati (Inner dial)
    r_dial = 115
    draw.ellipse([cx - r_dial, cy - r_dial, cx + r_dial, cy + r_dial], fill=(255, 255, 255, 255))

    # Soat belgilari (12 ta nuqta/chiziq)
    for hour in range(12):
        angle = math.radians(hour * 30 - 90)
        x1 = cx + (r_dial - 16) * math.cos(angle)
        y1 = cy + (r_dial - 16) * math.sin(angle)
        x2 = cx + (r_dial - 6) * math.cos(angle)
        y2 = cy + (r_dial - 6) * math.sin(angle)
        w = 5 if hour % 3 == 0 else 3
        c = (30, 27, 75, 255) if hour % 3 == 0 else (148, 163, 184, 255)
        draw.line([(x1, y1), (x2, y2)], fill=c, width=w)

    # 5. Soat Millari (Hands at 10:10)
    # Soat mili (qisqa va qalin - 10 ga qaragan)
    h_angle = math.radians(10 * 30 - 90 + 5)
    hx = cx + 55 * math.cos(h_angle)
    hy = cy + 55 * math.sin(h_angle)
    draw.line([(cx, cy), (hx, hy)], fill=(30, 27, 75, 255), width=8)

    # Daqiqa mili (uzoq - 2 ga qaragan)
    m_angle = math.radians(2 * 30 - 90)
    mx = cx + 80 * math.cos(m_angle)
    my = cy + 80 * math.sin(m_angle)
    draw.line([(cx, cy), (mx, my)], fill=(79, 70, 229, 255), width=6)

    # Markaziy nuqta
    draw.ellipse([cx - 8, cy - 8, cx + 8, cy + 8], fill=(239, 68, 68, 255)) # Qizil markaz

    # 6. Pastki o'ng burchakda "Bajarildi" Yashil Nishoni (Vazifalar ramzi)
    badge_x, badge_y = 350, 360
    badge_r = 42
    # Soya
    draw.ellipse([badge_x - badge_r - 2, badge_y - badge_r - 2, badge_x + badge_r + 2, badge_y + badge_r + 2], fill=(255, 255, 255, 255))
    # Yashil doira
    draw.ellipse([badge_x - badge_r, badge_y - badge_r, badge_x + badge_r, badge_y + badge_r], fill=(16, 185, 129, 255))
    # Oq galochka (Checkmark)
    draw.line([(badge_x - 18, badge_y), (badge_x - 6, badge_y + 14)], fill=(255, 255, 255, 255), width=7)
    draw.line([(badge_x - 6, badge_y + 14), (badge_x + 20, badge_y - 12)], fill=(255, 255, 255, 255), width=7)

    return img

def create_round_icon(base_icon):
    size = base_icon.width
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse([int(size * 0.05), int(size * 0.05), int(size * 0.95), int(size * 0.95)], fill=255)
    round_img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    round_img.paste(base_icon, (0, 0), mask=mask)
    return round_img

# Barcha o'lchamlarda eksport qilish
base_icon = create_alarm_icon(512)
round_icon = create_round_icon(base_icon)

res_dir = r"C:\Users\admin\.gemini\antigravity\scratch\task-alarm\app\src\main\res"

sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192
}

for folder, s in sizes.items():
    dir_path = os.path.join(res_dir, folder)
    os.makedirs(dir_path, exist_ok=True)
    
    # Standart icon
    resized = base_icon.resize((s, s), Image.Resampling.LANCZOS)
    resized.save(os.path.join(dir_path, "ic_launcher.png"), "PNG")
    
    # Round icon
    resized_round = round_icon.resize((s, s), Image.Resampling.LANCZOS)
    resized_round.save(os.path.join(dir_path, "ic_launcher_round.png"), "PNG")

print("BARCHA ICONLAR YARATILDI!")
