/*
 * 冬瓜音乐 · 二创开源版
 * Copyright (C) 2026 二次分发者 (https://github.com/BaiX-sudo)
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 本文件为 GPL-3.0-or-later 授权。完整许可文本见仓库根目录的 LICENSE 文件。
 *
 * 说明：原版客户端的界面逻辑位于 ApkVmProtect 加固的原生库内，无法修改，
 * 因此本项目通过在 MainActivity 的 DecorView 上挂载接管层的方式实现功能替换。
 */

package com.xifeng.music.xifeng;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public final class Guard {

    private static final String TAG = "guard_overlay";
    private static final String LOG = "XFGuard";

    private static final String FEEDBACK_MAIL = "hjl1357275420@hotmail";

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static volatile boolean settingsVisible;
    private static volatile boolean libraryTop;

    public static void install(Application app) {
        app.registerActivityLifecycleCallbacks(new Lifecycle());
    }

    private static int dp(Activity a, float v) {
        return (int) (v * a.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static void attach(final Activity activity) {
        if (activity == null
                || !"com.xifeng.music.MainActivity".equals(activity.getClass().getName())) {
            return;
        }
        View decor = activity.getWindow() == null ? null : activity.getWindow().getDecorView();
        if (!(decor instanceof ViewGroup)) {
            return;
        }
        ViewGroup root = (ViewGroup) decor;
        if (root.findViewWithTag(TAG) != null) {
            return;
        }
        int aboutTop = dp(activity, 407);
        int aboutBottom = dp(activity, 463);
        int bugTop = dp(activity, 347);
        int bugBottom = dp(activity, 402);
        int tipTop = dp(activity, 45);
        int tipBottom = dp(activity, 92);
        int tipLeft = dp(activity, 370);
        int tipRight = dp(activity, 420);
        View overlay = new View(activity);
        overlay.setTag(TAG);
        overlay.setBackgroundColor(Color.TRANSPARENT);
        overlay.setFocusable(false);
        overlay.setClickable(false);
        int pressColor = (activity.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES ? 0x1FFFFFFF : 0x22000000;
        overlay.setOnTouchListener(new TouchHandler(
                activity, root, aboutTop, aboutBottom, bugTop, bugBottom, tipTop, tipBottom,
                tipLeft, tipRight, pressColor));
        root.addView(overlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private static void startPoll(final Activity activity) {
        if (activity == null
                || !"com.xifeng.music.MainActivity".equals(activity.getClass().getName())) {
            return;
        }
        MAIN.removeCallbacksAndMessages(null);
        MAIN.postDelayed(new PollRunnable(activity), 400);
    }

    private static void step(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        int w = activity.getResources().getDisplayMetrics().widthPixels;
        int h = activity.getResources().getDisplayMetrics().heightPixels;
        if (w <= 0 || h <= 0 || activity.getWindow() == null) {
            MAIN.postDelayed(new PollRunnable(activity), 400);
            return;
        }
        Bitmap bmp;
        try {
            bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        } catch (Throwable t) {
            MAIN.postDelayed(new PollRunnable(activity), 400);
            return;
        }
        try {
            PixelCopy.request(activity.getWindow(), bmp, new CopyListener(bmp), MAIN);
        } catch (Throwable t) {
            bmp.recycle();
        }
        MAIN.postDelayed(new PollRunnable(activity), 350);
    }

    private static boolean isSparse(Bitmap bmp) {
        try {
            int w = bmp.getWidth();
            int h = bmp.getHeight();
            if (w <= 0 || h <= 0) {
                return false;
            }
            int minR = 255, maxR = 0, minG = 255, maxG = 0, minB = 255, maxB = 0;
            for (float fy = 0.55f; fy <= 0.801f; fy += 0.05f) {
                int py = (int) (h * fy);
                if (py < 0 || py >= h) {
                    continue;
                }
                for (float fx = 0.12f; fx <= 0.881f; fx += 0.127f) {
                    int px = (int) (w * fx);
                    if (px < 0 || px >= w) {
                        continue;
                    }
                    int p = bmp.getPixel(px, py);
                    minR = Math.min(minR, Color.red(p));
                    maxR = Math.max(maxR, Color.red(p));
                    minG = Math.min(minG, Color.green(p));
                    maxG = Math.max(maxG, Color.green(p));
                    minB = Math.min(minB, Color.blue(p));
                    maxB = Math.max(maxB, Color.blue(p));
                }
            }
            return (maxR - minR) < 26 && (maxG - minG) < 26 && (maxB - minB) < 26;
        } catch (Throwable t) {
            return false;
        }
    }

    static void sendFeedback(Activity activity) {
        String subject = "冬瓜音乐 问题反馈";
        String body = "\n软件版本：v1.0.7\n设备型号："
                + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL
                + "\nAndroid：" + android.os.Build.VERSION.RELEASE
                + "\n\n问题描述：\n";
        Uri uri = Uri.parse("mailto:" + FEEDBACK_MAIL
                + "?subject=" + Uri.encode(subject)
                + "&body=" + Uri.encode(body));
        Intent mail = new Intent(Intent.ACTION_SENDTO, uri);
        try {
            activity.startActivity(mail);
        } catch (Throwable t) {
            Toast.makeText(activity, "未找到可用的邮箱应用", Toast.LENGTH_LONG).show();
        }
    }

    static void openProfile(Activity activity) {
        try {
            Uri uri = Uri.parse("https://github.com/BaiX-sudo");
            Intent web = new Intent(Intent.ACTION_VIEW, uri);
            web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(web);
        } catch (Throwable t) {
            Toast.makeText(activity, "未找到可用的浏览器", Toast.LENGTH_LONG).show();
        }
    }
    private static int diff(int p, int br, int bgc, int bb) {
        return Math.max(Math.abs(android.graphics.Color.red(p) - br),
                Math.max(Math.abs(android.graphics.Color.green(p) - bgc),
                        Math.abs(android.graphics.Color.blue(p) - bb)));
    }

    private static boolean isLibraryTop(Bitmap bmp) {
        try {
            int w = bmp.getWidth();
            int h = bmp.getHeight();
            if (w <= 60 || h <= 60) {
                return false;
            }
            for (int yy = (int) (h * 0.020f); yy <= (int) (h * 0.085f); yy += 4) {
                int bg = bmp.getPixel(8, yy);
                int br = android.graphics.Color.red(bg);
                int bgc = android.graphics.Color.green(bg);
                int bb = android.graphics.Color.blue(bg);

                boolean uniform = true;
                for (int x = (int) (w * 0.40f); x <= (int) (w * 0.80f); x += 6) {
                    if (diff(bmp.getPixel(x, yy), br, bgc, bb) > 10) {
                        uniform = false;
                        break;
                    }
                }
                if (!uniform) {
                    continue;
                }
                boolean left = false;
                for (int x = (int) (w * 0.06f); x <= (int) (w * 0.30f); x += 3) {
                    if (diff(bmp.getPixel(x, yy), br, bgc, bb) > 40) {
                        left = true;
                        break;
                    }
                }
                if (!left) {
                    continue;
                }
                boolean right = false;
                for (int x = (int) (w * 0.87f); x <= (int) (w * 0.99f); x += 3) {
                    if (diff(bmp.getPixel(x, yy), br, bgc, bb) > 60) {
                        right = true;
                        break;
                    }
                }
                if (right) {
                    return true;
                }
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    private static final class TouchHandler implements View.OnTouchListener {
        private final Activity activity;
        private final ViewGroup root;
        private final int slop;
        private final int aboutTop;
        private final int aboutBottom;
        private final int bugTop;
        private final int bugBottom;
        private final int tipTop;
        private final int tipBottom;
        private final int tipLeft;
        private final int tipRight;
        private final int pressColor;

        private float downX;
        private float downY;
        private long downTime;
        private boolean forwarding;
        private boolean tracking;
        private View press;

        TouchHandler(Activity activity, ViewGroup root, int aboutTop, int aboutBottom,
                     int bugTop, int bugBottom, int tipTop, int tipBottom,
                     int tipLeft, int tipRight, int pressColor) {
            this.activity = activity;
            this.root = root;
            this.slop = android.view.ViewConfiguration.get(activity).getScaledTouchSlop();
            this.aboutTop = aboutTop;
            this.aboutBottom = aboutBottom;
            this.bugTop = bugTop;
            this.bugBottom = bugBottom;
            this.tipTop = tipTop;
            this.tipBottom = tipBottom;
            this.tipLeft = tipLeft;
            this.tipRight = tipRight;
            this.pressColor = pressColor;
        }

        private boolean armedAt(float x, float y) {
            int ix = (int) x;
            int iy = (int) y;
            if (libraryTop && ix >= tipLeft && ix <= tipRight && iy >= tipTop && iy <= tipBottom) {
                return true;
            }
            if (settingsVisible) {
                if (iy >= aboutTop && iy <= aboutBottom) {
                    return true;
                }
                if (iy >= bugTop && iy <= bugBottom) {
                    return true;
                }
            }
            return false;
        }

        private boolean perform(float x, float y) {
            int ix = (int) x;
            int iy = (int) y;
            if (libraryTop && ix >= tipLeft && ix <= tipRight
                    && iy >= tipTop && iy <= tipBottom) {
                openProfile(activity);
                return true;
            }
            if (!settingsVisible) {
                return false;
            }
            if (iy >= aboutTop && iy <= aboutBottom) {
                try {
                    activity.startActivity(new Intent(activity, AboutActivity.class));
                    return true;
                } catch (Throwable t) {
                    Log.e(LOG, "about failed " + t);
                    return false;
                }
            }
            if (iy >= bugTop && iy <= bugBottom) {
                sendFeedback(activity);
                return true;
            }
            return false;
        }

        private void showPress(float x, float y) {
            try {
                if (press == null) {
                    press = new View(activity);
                    press.setAlpha(0.0f);
                }
                int size = dp(activity, 52);
                android.widget.FrameLayout.LayoutParams lp =
                        new android.widget.FrameLayout.LayoutParams(size, size);
                lp.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
                lp.leftMargin = (int) x - size / 2;
                lp.topMargin = (int) y - size / 2;
                if (press.getParent() == null) {
                    root.addView(press, lp);
                } else {
                    press.setLayoutParams(lp);
                }
                android.graphics.drawable.GradientDrawable pd =
                        new android.graphics.drawable.GradientDrawable();
                pd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                pd.setColor(pressColor);
                pd.setCornerRadius(size / 2f);
                press.setBackground(pd);
                press.setAlpha(1f);
            } catch (Throwable ignored) {
            }
        }

        private void hidePress() {
            try {
                if (press != null) {
                    press.setAlpha(0f);
                    root.removeView(press);
                }
            } catch (Throwable ignored) {
            }
        }

        private void replay(MotionEvent e) {
            forwarding = true;
            try {
                root.dispatchTouchEvent(e);
            } catch (Throwable ignored) {
            }
            forwarding = false;
        }

        public boolean onTouch(View v, MotionEvent e) {
            if (forwarding) {
                return false;
            }
            int action = e.getActionMasked();

            if (action == MotionEvent.ACTION_DOWN) {
                tracking = false;
                if (!armedAt(e.getX(), e.getY())) {
                    return false;
                }
                tracking = true;
                downX = e.getX();
                downY = e.getY();
                downTime = e.getEventTime();
                showPress(downX, downY);
                return true;
            }

            if (!tracking) {
                return false;
            }

            if (action == MotionEvent.ACTION_MOVE) {
                if (!forwarding) {
                    if (Math.abs(e.getX() - downX) > slop
                            || Math.abs(e.getY() - downY) > slop) {
                        hidePress();
                        forwarding = true;
                        try {
                            MotionEvent d = MotionEvent.obtain(downTime, downTime,
                                    MotionEvent.ACTION_DOWN, downX, downY, 0);
                            root.dispatchTouchEvent(d);
                            d.recycle();
                        } catch (Throwable ignored) {
                        }
                        forwarding = false;
                    } else {
                        return true;
                    }
                }
                replay(e);
                return true;
            }

            if (action == MotionEvent.ACTION_UP) {
                hidePress();
                boolean wasForwarding = forwarding;
                if (wasForwarding) {
                    replay(e);
                    tracking = false;
                    forwarding = false;
                    return true;
                }
                tracking = false;
                if (e.getEventTime() - downTime <= 600) {
                    perform(downX, downY);
                }
                return true;
            }

            if (action == MotionEvent.ACTION_CANCEL) {
                hidePress();
                tracking = false;
                forwarding = false;
                return true;
            }
            return false;
        }
    }
    private static final class PollRunnable implements Runnable {
        private final Activity activity;

        PollRunnable(Activity activity) {
            this.activity = activity;
        }

        public void run() {
            step(activity);
        }
    }

    private static final class CopyListener implements PixelCopy.OnPixelCopyFinishedListener {
        private final Bitmap bmp;

        CopyListener(Bitmap bmp) {
            this.bmp = bmp;
        }

        public void onPixelCopyFinished(int result) {
            if (result == PixelCopy.SUCCESS) {
                settingsVisible = isSparse(bmp);
                boolean lt = isLibraryTop(bmp);
                if (lt != libraryTop) {
                    libraryTop = lt;
                    android.util.Log.e(LOG, "tipBar=" + lt);
                }
            }
            bmp.recycle();
        }
    }

    private static final class Lifecycle implements Application.ActivityLifecycleCallbacks {
        public void onActivityCreated(Activity a, Bundle b) {
        }

        public void onActivityStarted(Activity a) {
        }

        public void onActivityResumed(Activity a) {
            attach(a);
            startPoll(a);
        }

        public void onActivityPaused(Activity a) {
        }

        public void onActivityStopped(Activity a) {
        }

        public void onActivitySaveInstanceState(Activity a, Bundle b) {
        }

        public void onActivityDestroyed(Activity a) {
        }
    }

    private Guard() {
    }
}