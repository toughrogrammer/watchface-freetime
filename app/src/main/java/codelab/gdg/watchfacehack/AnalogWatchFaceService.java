package codelab.gdg.watchfacehack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.Random;

public class AnalogWatchFaceService extends CanvasWatchFaceService {

    private static final Typeface BOLD_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        static final int MSG_UPDATE_TIME = 0;
        static final int INTERACTIVE_UPDATE_RATE_MS = 10;

        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        /* Interactive 모드이면 */
                        if (isVisible() && !isInAmbientMode()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        /* Timezone의 변경을 감지하는 리시버 */
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        /* Timezone 리시버의 등록 여부를 저장하는 변수 */
        boolean mRegisteredTimeZoneReceiver = false;

        Time mTime;

        Bitmap mBackgroundBitmap;
        Bitmap mBackgroundScaledBitmap;
        Bitmap _bitmapUpperBackground;
        Bitmap _bitmapUpperBackgroundScaled;
        Bitmap _bitmapHourPin;
        Bitmap _bitmapMinutePin;

        final int[] FRIENDS_THUMBNAIL_BITMAPS = {
                R.drawable.thumbnail1,
                R.drawable.thumbnail2,
                R.drawable.thumbnail3,
        };
        Bitmap[] _friendsThumbnailBitmaps;
        float[] _friendsOrbitAngle;
        float[] _friendsOrbitRadius;
        float[] _friendsOrbitRotationSpeed;

        final int[] RESOURCES_NUMBER = {
                R.drawable.number_0,
                R.drawable.number_1,
                R.drawable.number_2,
                R.drawable.number_3,
                R.drawable.number_4,
                R.drawable.number_5,
                R.drawable.number_6,
                R.drawable.number_7,
                R.drawable.number_8,
                R.drawable.number_9,
        };
        Bitmap[] _bitmapNumbers;

        long prev_milliseconds;
        float dt;


        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(AnalogWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            mBackgroundBitmap = LoadBitmapFromDrawable(R.drawable.bg);
            _bitmapUpperBackground = LoadBitmapFromDrawable(R.drawable.upper);
            _bitmapHourPin = LoadBitmapFromDrawable(R.drawable.pin_hour);
            _bitmapMinutePin = LoadBitmapFromDrawable(R.drawable.pin_minute);

            _bitmapNumbers = new Bitmap[RESOURCES_NUMBER.length];
            for (int i = 0; i < _bitmapNumbers.length; i++) {
                _bitmapNumbers[i] = LoadBitmapFromDrawable(RESOURCES_NUMBER[i]);
            }

            Random random = new Random();

            _friendsThumbnailBitmaps = new Bitmap[3];
            ArrayList<Integer> listForRandom = new ArrayList<Integer>();
            for (int i = 0; i < _friendsThumbnailBitmaps.length; i++) {
                listForRandom.add(i);
            }
            for (int i = 0; i < _friendsThumbnailBitmaps.length; i++) {
                int randomIndex = random.nextInt(listForRandom.size());
                int n = listForRandom.get(randomIndex);
                listForRandom.remove(randomIndex);
                _friendsThumbnailBitmaps[i] = LoadBitmapFromDrawable(FRIENDS_THUMBNAIL_BITMAPS[n]);
            }

            _friendsOrbitAngle = new float[_friendsThumbnailBitmaps.length];
            for (int i = 0; i < _friendsOrbitAngle.length; i++) {
                _friendsOrbitAngle[i] = random.nextFloat() * 360;
            }

            _friendsOrbitRadius = new float[_friendsThumbnailBitmaps.length];
            for (int i = 0; i < _friendsOrbitRadius.length; i++) {
                _friendsOrbitRadius[i] = random.nextFloat() * 100 + 30;
            }

            _friendsOrbitRotationSpeed = new float[_friendsThumbnailBitmaps.length];
            for (int i = 0; i < _friendsOrbitRotationSpeed.length; i++) {
                _friendsOrbitRotationSpeed[i] = random.nextFloat() * 0.3f + 0.1f;
            }


            mTime = new Time();
        }

        private Bitmap LoadBitmapFromDrawable(int resId) {
            Resources resources = AnalogWatchFaceService.this.getResources();
            Drawable drwable = resources.getDrawable(resId);
            return ((BitmapDrawable) drwable).getBitmap();
        }

        private Paint createTextPaint(int defaultInteractiveColor) {
            return createTextPaint(defaultInteractiveColor, NORMAL_TYPEFACE);
        }

        private Paint createTextPaint(int defaultInteractiveColor, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(defaultInteractiveColor);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (isVisible() && !isInAmbientMode()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                /* Timezone 리시버가 등록되지 않았다면 등록 */
                if (mRegisteredTimeZoneReceiver == false) {
                    mRegisteredTimeZoneReceiver = true;
                    IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
                    AnalogWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
                }
            } else {
                /* Timezone 리시버가 등록되었다면 등록 해제 */
                if (mRegisteredTimeZoneReceiver == true) {
                    mRegisteredTimeZoneReceiver = false;
                    AnalogWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
                }
            }
            updateTimer();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();

            invalidate();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);

            mTime.setToNow();

            long curr = System.currentTimeMillis();
            long diff = curr - prev_milliseconds;
            prev_milliseconds = curr;
            dt = diff * 0.001f;

            int width = bounds.width();
            int height = bounds.height();


            if (mBackgroundScaledBitmap == null
                    || mBackgroundScaledBitmap.getWidth() != width
                    || mBackgroundScaledBitmap.getHeight() != height) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                        width, height, true /* filter */);
            }
            canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);


            /* 중심 좌표를 구합니다. */
            float centerX = width / 2f;
            float centerY = height / 2f;

            DrawFriends(canvas, centerX, centerY);

            if (_bitmapUpperBackgroundScaled == null
                    || _bitmapUpperBackground.getWidth() != width
                    || _bitmapUpperBackground.getHeight() != height) {
                _bitmapUpperBackgroundScaled = Bitmap.createScaledBitmap(_bitmapUpperBackground,
                        width, height, true /* filter */);
            }
            canvas.drawBitmap(_bitmapUpperBackgroundScaled , 0, 0, null);
            DrawClock(canvas, centerX, centerY);
        }

        void DrawFriends(Canvas canvas, float cx, float cy) {
            for (int i = 0; i < _friendsThumbnailBitmaps.length; i++) {
                _friendsOrbitAngle[i] += _friendsOrbitRotationSpeed[i];
            }

            for (int i = 0; i < _friendsThumbnailBitmaps.length; i++) {
                canvas.save();

                float radius = _friendsOrbitRadius[i];

                float angle = (float) Math.toRadians(_friendsOrbitAngle[i]);
                float dx = (float) (radius * Math.cos(angle));
                float dy = (float) (radius * Math.sin(angle));
                Bitmap bitmap = _friendsThumbnailBitmaps[i];
                canvas.drawBitmap(bitmap, cx + dx - bitmap.getWidth() / 2, cy + dy - bitmap.getHeight() / 2, null);

                canvas.restore();
            }
        }

        void DrawClock(Canvas canvas, float centerX, float centerY) {
            canvas.save();

            float minuteRot = mTime.minute * 6 + 180;
            float hourRot = (mTime.hour * 60 + mTime.minute) * 0.5f + 180;

            canvas.save();
            canvas.translate(centerX, centerY);
            canvas.rotate(hourRot);
            canvas.drawBitmap(_bitmapHourPin,
                    -_bitmapHourPin.getWidth() / 2,
                    0,
                    null);
            canvas.restore();

            canvas.save();
            canvas.translate(centerX, centerY);
            canvas.rotate(minuteRot);
            canvas.drawBitmap(_bitmapMinutePin,
                    -_bitmapMinutePin.getWidth() / 2,
                    0,
                    null);
            canvas.restore();

            canvas.restore();


            float width = _bitmapNumbers[0].getWidth();
            float sx = 110.0f;

            int hour = mTime.hour;
            int minute = mTime.minute;
            String stringTime = "";
            if( hour >= 10 ) {
                stringTime += hour;
            } else {
                stringTime += "0" + hour;
            }
            if( minute >= 10 ) {
                stringTime += minute;
            } else {
                stringTime += "0" + minute;
            }
            char[] chars = new char[stringTime.length()];
            stringTime.getChars(0, stringTime.length(), chars, 0);

            for( int i = 0; i < chars.length; i ++ ) {
                int number = Integer.parseInt(chars[i] + "");
                DrawNumber(canvas, number, sx, 28);
                sx += width;
            }
        }

        void DrawNumber(Canvas canvas, int number, float x, float y) {
            Bitmap bitmap = _bitmapNumbers[number];
            canvas.drawBitmap(bitmap, x, y, null);
        }
    }
}