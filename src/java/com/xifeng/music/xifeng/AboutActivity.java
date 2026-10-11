/*
 * 冬瓜音乐 · 二创开源版
 * Copyright (C) 2026 二次分发者 (https://github.com/BaiX-sudo)
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 本文件为 GPL-3.0-or-later 授权。完整许可文本见仓库根目录的 LICENSE 文件。
 *
 * 说明：用于替代原版由 ApkVmProtect 原生壳渲染的「关于我们」页面。
 */

package com.xifeng.music.xifeng;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class AboutActivity extends Activity {

    private int surface;
    private int onSurface;
    private int onSurfaceVariant;
    private int primary;
    private int outline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean night = appDarkTheme(this);
        surface = night ? 0xFF141218 : 0xFFFFFBFF;
        onSurface = night ? 0xFFE6E1E5 : 0xFF1C1B1F;
        onSurfaceVariant = night ? 0xFFCBC4D0 : 0xFF49454F;
        outline = night ? 0xFF49454F : 0xFFD0CBC8;
        primary = safeAccent(night);

        Window window = getWindow();
        window.setStatusBarColor(surface);
        window.setNavigationBarColor(surface);
        window.getDecorView().setSystemUiVisibility(night ? 0
                : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        setContentView(build());
    }

    private int safeAccent(boolean night) {
        int fallback = night ? 0xFFB98585 : 0xFF904A49;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return fallback;
        }
        try {
            int id = getResources().getIdentifier(
                    "system_accent1_" + (night ? 200 : 600), "color", "android");
            if (id == 0) {
                return fallback;
            }
            int c = getColor(id);
            int bg = night ? 0xFF141218 : 0xFFFFFBFF;
            int dr = Math.abs(Color.red(c) - Color.red(bg));
            int dg = Math.abs(Color.green(c) - Color.green(bg));
            int db = Math.abs(Color.blue(c) - Color.blue(bg));
            return (Math.max(dr, Math.max(dg, db)) < 90) ? fallback : c;
        } catch (Throwable t) {
            return fallback;
        }
    }

    private static boolean appDarkTheme(Activity act) {
        return (act.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable pill(int color, float radiusDp, int strokeColor, int strokeWidth) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        if (strokeWidth > 0) {
            d.setStroke(dp(strokeWidth), strokeColor);
        }
        return d;
    }

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) {
            t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        t.setIncludeFontPadding(false);
        return t;
    }

    private LinearLayout.LayoutParams lp(int w, int h, int topMargin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        if (topMargin != 0) {
            p.topMargin = topMargin;
        }
        return p;
    }

    private Bitmap circularBitmap(int resId, int size) {
        try {
            Bitmap raw = BitmapFactory.decodeResource(getResources(), resId);
            if (raw == null) {
                android.graphics.drawable.Drawable d = getResources().getDrawable(resId, getTheme());
                if (d == null) {
                    return null;
                }
                raw = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                Canvas tmp = new Canvas(raw);
                d.setBounds(0, 0, size, size);
                d.draw(tmp);
            }
            Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(out);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            c.drawCircle(size / 2f, size / 2f, size / 2f, p);
            p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            c.drawBitmap(raw, null, new Rect(0, 0, size, size), p);
            return out;
        } catch (Throwable t) {
            return null;
        }
    }

    private ImageView circle(int resId, int sizeDp) {
        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Bitmap bmp = resId == 0 ? null : circularBitmap(resId, dp(sizeDp) * 3);
        if (bmp != null) {
            iv.setImageBitmap(bmp);
        } else {
            iv.setBackground(pill(primary, sizeDp / 2f, 0, 0));
        }
        return iv;
    }

    private int res(String type, String name) {
        return getResources().getIdentifier(name, type, getPackageName());
    }

    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(surface);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), dp(10), dp(16), dp(10));
        root.addView(bar, lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(58), 0));

        TextView back = text("‹", 40, onSurface, false);
        back.setGravity(Gravity.CENTER);
        back.setBackground(pill(Color.TRANSPARENT, 24, 0, 0));
        back.setOnClickListener(new FinishClick());
        bar.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView title = text("关于我们", 22, onSurface, true);
        LinearLayout.LayoutParams tlp = lp(ViewGroup.LayoutParams.WRAP_CONTENT, dp(28), 0);
        tlp.leftMargin = dp(8);
        bar.addView(title, tlp);

        ScrollView scroll = new ScrollView(this);
        scroll.setScrollbarFadingEnabled(false);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(dp(24), dp(16), dp(24), dp(40));
        scroll.addView(col, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        int iconRes = res("mipmap", "ic_launcher");
        if (iconRes == 0) {
            iconRes = res("drawable", "avatar_deepseek");
        }
        col.addView(circle(iconRes, 84), lp(dp(84), dp(84), 0));

        col.addView(text("冬瓜音乐", 30, onSurface, true),
                lp(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36), dp(22)));
        col.addView(text("v1.0.7", 15, onSurfaceVariant, false),
                lp(ViewGroup.LayoutParams.WRAP_CONTENT, dp(20), dp(6)));

        View line = new View(this);
        line.setBackgroundColor(outline);
        col.addView(line, lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(1), dp(36)));

        col.addView(text("开发团队", 17, onSurface, true),
                lp(ViewGroup.LayoutParams.WRAP_CONTENT, dp(24), dp(30)));

        col.addView(member("不许人间见白头", "二次分发 · 维护", res("drawable", "avatar_user")),
                lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(76), dp(20)));
        col.addView(member("Deepseek", "技术支持 · 方案", res("drawable", "avatar_deepseek")),
                lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(76), dp(12)));
        col.addView(member("Space Bunny", "运行环境", res("drawable", "avatar_bunny")),
                lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(76), dp(12)));

        TextView note = text("本软件为个人学习与自用的二次分发版本，仅供学习交流。",
                13, onSurfaceVariant, false);
        note.setGravity(Gravity.CENTER);
        col.addView(note, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, dp(34)));

        return root;
    }

    private static final class FinishClick implements View.OnClickListener {
        public void onClick(View v) {
            ((Activity) v.getContext()).finish();
        }
    }

    private View member(String name, String role, int avatarRes) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        row.addView(circle(avatarRes, 46), new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        clp.leftMargin = dp(16);
        row.addView(col, clp);

        col.addView(text(name, 17, onSurface, true),
                lp(ViewGroup.LayoutParams.WRAP_CONTENT, dp(24), 0));
        col.addView(text(role, 13, onSurfaceVariant, false),
                lp(ViewGroup.LayoutParams.WRAP_CONTENT, dp(18), dp(4)));

        return row;
    }
}