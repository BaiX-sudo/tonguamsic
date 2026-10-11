/*
 * 冬瓜音乐 · 二创开源版
 * Copyright (C) 2026 二次分发者 (https://github.com/BaiX-sudo)
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 本文件为 GPL-3.0-or-later 授权。完整许可文本见仓库根目录的 LICENSE 文件。
 *
 * 说明：原版公告与强制更新弹窗已由 smali 补丁短路，此处为首次启动时展示的
 * 二次分发声明，代替被移除的原公告 UI。
 */

package com.xifeng.music.xifeng;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.DisplayMetrics;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class Disclaimer {

    private static final String PREFS = "xifeng_disclaimer";
    private static final String KEY_NEVER = "never_show";
    private static final String TARGET_ACTIVITY = "com.xifeng.music.MainActivity";
    private static final int READ_MS = 10000;

    private static final int CARD_BG = 0xFFF6E4E3;
    private static final int TITLE_CG = 0xFF2B2020;
    private static final int BODY_CG = 0xFF625352;
    private static final int PRIMARY = 0xFF904A49;
    private static final int CARD_BG_DARK = 0xFF2B2226;
    private static final int TITLE_DARK = 0xFFF1E4E4;
    private static final int BODY_DARK = 0xFFD3C4C4;
    private static final int PRIMARY_DARK = 0xFFB98585;

    private static boolean installed;
    private static boolean shown;

    public static void install(Application app) {
        if (installed || app == null) {
            return;
        }
        installed = true;
        final SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_NEVER, false)) {
            return;
        }
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            public void onActivityCreated(Activity activity, Bundle b) {}
            public void onActivityStarted(Activity activity) {}
            public void onActivityResumed(Activity activity) {
                tryShow(activity);
            }
            public void onActivityPaused(Activity activity) {}
            public void onActivityStopped(Activity activity) {}
            public void onActivitySaveInstanceState(Activity activity, Bundle b) {}
            public void onActivityDestroyed(Activity activity) {}
        });
    }

    private static void tryShow(final Activity activity) {
        if (shown || activity == null || activity.isFinishing()) {
            return;
        }
        if (!TARGET_ACTIVITY.equals(activity.getClass().getName())) {
            return;
        }
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }
        shown = true;
        window.getDecorView().postDelayed(new Runnable() {
            public void run() {
                if (!activity.isFinishing()) {
                    present(activity);
                }
            }
        }, 800);
    }

    private static int dp(Context context, float value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static GradientDrawable round(int color, float radiusDp, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);
        drawable.setCornerRadius(radiusDp);
        if (strokeWidth > 0) {
            drawable.setStroke(strokeWidth, strokeColor);
        }
        return drawable;
    }

    private static Drawable circleDrawable(Context ctx, String name, int size) {
        try {
            int id = ctx.getResources().getIdentifier(name, "drawable", ctx.getPackageName());
            if (id == 0) {
                return null;
            }
            Bitmap src = BitmapFactory.decodeResource(ctx.getResources(), id);
            if (src == null) {
                return null;
            }
            Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(out);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            c.drawCircle(size / 2f, size / 2f, size / 2f, p);
            p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            c.drawBitmap(src, null, new Rect(0, 0, size, size), p);
BitmapDrawable d = new BitmapDrawable(ctx.getResources(), out);
            d.setBounds(0, 0, size, size);
            return d;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Drawable sizedDrawable(Context ctx, String name, int size) {
        try {
            Drawable hi = circleDrawable(ctx, name, size * 4);
            if (hi == null) {
                return null;
            }
            Bitmap bmp = ((BitmapDrawable) hi).getBitmap();
            Bitmap small = Bitmap.createScaledBitmap(bmp, size, size, true);
            BitmapDrawable d = new BitmapDrawable(ctx.getResources(), small);
            d.setBounds(0, 0, size, size);
            return d;
        } catch (Throwable t) {
            return null;
        }
    }

    private static final class CenterImageSpan extends ImageSpan {
        private final int size;

        CenterImageSpan(Drawable d, int size) {
            super(d, ImageSpan.ALIGN_BASELINE);
            this.size = size;
        }

        @Override
        public void draw(Canvas canvas, CharSequence text, int start, int end, float x,
                         int top, int y, int bottom, Paint paint) {
            Drawable d = getDrawable();
            if (d == null) {
                return;
            }
            d.setBounds(0, 0, size, size);
            float center = y + (paint.ascent() + paint.descent()) / 2f;
            int transY = Math.round(center - size / 2f);
            canvas.save();
            canvas.translate(Math.round(x), transY);
            d.draw(canvas);
            canvas.restore();
        }
    }

    private static void present(final Activity activity) {
        final boolean night = (activity.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        final int cardBg = night ? CARD_BG_DARK : CARD_BG;
        final int titleCg = night ? TITLE_DARK : TITLE_CG;
        final int bodyCg = night ? BODY_DARK : BODY_CG;
        final int primary = night ? PRIMARY_DARK : PRIMARY;
        final int onPrimary = night ? 0xFF2B2226 : Color.WHITE;

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        int pad = dp(activity, 28);
        int radius = dp(activity, 30);

        FrameLayout root = new FrameLayout(activity);
        root.setPadding(pad, pad, pad, pad);

        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(round(cardBg, radius, 0, 0));
        card.setPadding(dp(activity, 24), dp(activity, 24), dp(activity, 24), dp(activity, 20));
        root.addView(card, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(activity);
        title.setText("关于本软件");
        title.setTextColor(titleCg);
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(title);

        TextView subtitle = new TextView(activity);
        subtitle.setText("冬瓜音乐 · 二创说明与免责声明");
        subtitle.setTextColor(bodyCg);
        subtitle.setTextSize(13);
        subtitle.setAlpha(0.85f);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(activity, 6);
        card.addView(subtitle, subLp);

        TextView header = new TextView(activity);
        header.setTextColor(titleCg);
        header.setTextSize(19);
        header.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.setLineSpacing(0f, 1.12f);
        int avSize = dp(activity, 19);
        String obj = "￼";
        String headText = "软件由" + obj + "[不许人间见白头]二次分发";
        SpannableString head = new SpannableString(headText);
        Drawable av = sizedDrawable(activity, "avatar_user", avSize);
        if (av != null) {
            int at = headText.indexOf(obj);
            head.setSpan(new CenterImageSpan(av, avSize), at, at + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        header.setText(head);
        LinearLayout.LayoutParams headLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headLp.topMargin = dp(activity, 16);
        card.addView(header, headLp);

        ScrollView scroll = new ScrollView(activity);
        scroll.setScrollbarFadingEnabled(false);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollLp.topMargin = dp(activity, 16);
        card.addView(scroll, scrollLp);

        TextView body = new TextView(activity);
        body.setText(bodyText());
        body.setTextColor(bodyCg);
        body.setTextSize(14.5f);
        body.setLineSpacing(dp(activity, 5), 1.12f);
        scroll.addView(body, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView hint = new TextView(activity);
        hint.setText("请阅读 10 秒后方可关闭");
        hint.setTextColor(bodyCg);
        hint.setTextSize(13);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintLp.topMargin = dp(activity, 14);
        card.addView(hint, hintLp);

        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionsLp.topMargin = dp(activity, 12);
        card.addView(actions, actionsLp);

        final Button never = makeButton(activity, "不再显示", primary, onPrimary, radius, false);
        final Button close = makeButton(activity, "关闭", Color.TRANSPARENT,
                night ? PRIMARY_DARK : PRIMARY, radius, true);

        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        closeLp.leftMargin = dp(activity, 12);
        actions.addView(close, closeLp);

        actions.addView(never, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        lock(never, 0.4f);
        lock(close, 0.4f);

        final TextView countdownHint = hint;

        never.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit().putBoolean(KEY_NEVER, true).apply();
                dialog.dismiss();
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            public boolean onKey(DialogInterface d, int keyCode, KeyEvent event) {
                return keyCode == KeyEvent.KEYCODE_BACK;
            }
        });

        Window dialogWindow = dialog.getWindow();
        if (dialogWindow != null) {
            dialogWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = dialogWindow.getAttributes();
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.dimAmount = 0.55f;
            dialogWindow.setAttributes(lp);
        }

        dialog.setContentView(root);
        dialog.show();

        if (dialogWindow != null) {
            WindowManager.LayoutParams lp = dialogWindow.getAttributes();
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialogWindow.setAttributes(lp);
        }

        new CountDownTimer(READ_MS, 1000) {
            public void onTick(long millisUntilFinished) {
                long sec = (millisUntilFinished + 999) / 1000;
                countdownHint.setText("请阅读 " + sec + " 秒后方可关闭");
            }

            public void onFinish() {
                countdownHint.setText("已阅读完毕，可关闭");
                unlock(never);
                unlock(close);
            }
        }.start();
    }

    private static Button makeButton(Context context, String label, int bg, int textCg,
                                      float radius, boolean outlined) {
        Button button = new Button(context);
        button.setText(label);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(textCg);
        button.setGravity(Gravity.CENTER);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setPadding(dp(context, 22), dp(context, 11), dp(context, 22), dp(context, 11));
        if (outlined) {
            button.setBackground(round(Color.TRANSPARENT, radius, textCg, dp(context, 1)));
        } else {
            button.setBackground(round(bg, radius, 0, 0));
        }
        return button;
    }

    private static void lock(View view, float alpha) {
        view.setEnabled(false);
        view.setClickable(false);
        view.setAlpha(alpha);
    }

    private static void unlock(View view) {
        view.setEnabled(true);
        view.setClickable(true);
        view.setAlpha(1f);
    }

    private static String bodyText() {
        return "【软件介绍】\n"
                + "冬瓜音乐是一款基于开源项目 InnerTune 深度二创的第三方音乐播放器。"
                + "在原项目的基础上，本软件接入了国内音乐平台的公开数据接口，"
                + "补充了榜单、歌单、分类浏览与站内搜索等功能，"
                + "力求为使用者提供更贴近本地习惯的音乐收听体验。\n\n"

                + "【二创说明】\n"
                + "本软件为个人学习、研究与自用目的而进行的二次开发，"
                + "全程免费、不含任何广告与内购，不用于任何商业用途，"
                + "亦不为任何形式的牟利行为提供支持。\n"
                + "二创人：不许人间见白头\n\n"

                + "【免责声明】\n"
                + "1. 本软件仅供个人学习与交流使用，请勿将其用于商业用途、批量分发或任何形式的牟利行为。\n"
                + "2. 软件内所有歌曲、专辑封面、歌词等内容均来源于第三方平台的公开数据接口，"
                + "其著作权归原作者及相关版权方所有，本软件不主张也不享有这些资源的任何权利。\n"
                + "3. 音乐来源依赖第三方公开接口，可能因平台策略调整而随时变更或失效，"
                + "本软件无法保证任何服务的持续可用性。\n"
                + "4. 请使用者自觉遵守所在国家或地区的法律法规，尊重著作权，"
                + "不得利用本软件从事侵权、传播违法内容等任何违法活动。\n"
                + "5. 因使用本软件所产生的一切后果由使用者自行承担，"
                + "与原作者、二创人及相关开源项目无关。\n"
                + "6. 如权利人认为本软件侵犯其合法权益，请及时告知，"
                + "我们将在核实后第一时间作出处理。\n\n"

                + "【致谢】\n"
                + "感谢原作者 御弟\n"
                + "本软件的一切功能与灵感，均源自原作者「御弟」的开源作品 InnerTune。"
                + "谨以此致以最诚挚的感谢，衷心感谢其无私的分享与付出。";
    }

    private Disclaimer() {
    }
}