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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;

public class AnalogWatchFaceService extends CanvasWatchFaceService {

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
        Paint mHourPaint;
        Paint mMinutePaint;
        Paint mSecondPaint;
        Paint _circleOutlinePaint;

        static final float FRIENDS_ORBIT_RADIUS = 30.0f;
        Bitmap[] _friendsThumbnailBitmaps;
        float _friendsOrbitAngle[] = new float[]{
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f
        };

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

            /* 배경이미지를 로드합니다 */
            Resources resources = AnalogWatchFaceService.this.getResources();
            Drawable backgroundDrawable = resources.getDrawable(R.drawable.bg);
            mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();

            /* 그래픽 객체를 생성합니다 */
            mHourPaint = new Paint();
            mHourPaint.setARGB(255, 200, 200, 200);
            mHourPaint.setStrokeWidth(5.f);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.ROUND);

            mMinutePaint = new Paint();
            mMinutePaint.setARGB(255, 200, 200, 200);
            mMinutePaint.setStrokeWidth(3.f);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.ROUND);

            mSecondPaint = new Paint();
            mSecondPaint.setARGB(255, 255, 0, 0);
            mSecondPaint.setStrokeWidth(2.f);
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStrokeCap(Paint.Cap.ROUND);

            _circleOutlinePaint = new Paint();
            _circleOutlinePaint.setARGB(255, 255, 255, 255);
            _circleOutlinePaint.setStrokeWidth(2.f);
            _circleOutlinePaint.setAntiAlias(true);
            _circleOutlinePaint.setStrokeCap(Paint.Cap.ROUND);
            _circleOutlinePaint.setStyle(Paint.Style.STROKE);

            _friendsThumbnailBitmaps = new Bitmap[5];
            for ( int i = 0; i < _friendsThumbnailBitmaps.length; i++ ) {
                Drawable drawable = resources.getDrawable(R.drawable.thumbnail_default);
                _friendsThumbnailBitmaps[i] = ((BitmapDrawable) drawable).getBitmap();
            }


            mTime = new Time();
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
            Log.d("", "" + centerX + " " + centerY);

            DrawFriends(canvas, centerX, centerY);
            DrawClock(canvas, centerX, centerY);
        }

        void DrawFriends(Canvas canvas, float cx, float cy) {
            for( int i = 0; i < 5; i++ ) {
                _friendsOrbitAngle[i] += 0.6f * i;
            }

            for (int i = 0; i < 5; i++) {
                float radius = FRIENDS_ORBIT_RADIUS * i;
                canvas.drawCircle(cx, cy, radius, _circleOutlinePaint);

                float angle = (float) Math.toRadians(_friendsOrbitAngle[i]);
                float dx = (float) (radius * Math.cos(angle));
                float dy = (float) (radius * Math.sin(angle));
                Bitmap bitmap = _friendsThumbnailBitmaps[i];
                canvas.drawBitmap(bitmap, cx + dx - bitmap.getWidth() / 2, cy + dy - bitmap.getHeight() / 2, null);
            }
        }

        void DrawClock(Canvas canvas, float centerX, float centerY) {
            /* 각 침들의 각도와 길이를 계산합니다. */
            float secRot = mTime.second / 30f * (float) Math.PI;
            int minutes = mTime.minute;
            float minRot = minutes / 30f * (float) Math.PI;
            float hrRot = ((mTime.hour + (minutes / 60f)) / 6f) * (float) Math.PI;

            float secLength = centerX - 20;
            float minLength = centerX - 40;
            float hrLength = centerX - 80;

            /* Interactive 모드일 때에는, 초침을 그립니다. */
            if (!isInAmbientMode()) {
                float secX = (float) Math.sin(secRot) * secLength;
                float secY = (float) -Math.cos(secRot) * secLength;
                canvas.drawLine(centerX, centerY, centerX + secX, centerY +
                        secY, mSecondPaint);
            }

            // Draw the minute and hour hands.
            float minX = (float) Math.sin(minRot) * minLength;
            float minY = (float) -Math.cos(minRot) * minLength;
            canvas.drawLine(centerX, centerY, centerX + minX, centerY + minY,
                    mMinutePaint);
            float hrX = (float) Math.sin(hrRot) * hrLength;
            float hrY = (float) -Math.cos(hrRot) * hrLength;
            canvas.drawLine(centerX, centerY, centerX + hrX, centerY + hrY,
                    mHourPaint);
        }
    }
}