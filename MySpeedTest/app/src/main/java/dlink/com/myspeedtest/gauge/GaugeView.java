package dlink.com.myspeedtest.gauge;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import dlink.com.myspeedtest.R;

public class GaugeView extends View {

    public static final int SIZE = 300;
    public static final float CENTER = 0.5f;
    public static final boolean SHOW_SCALE = false;

    public static final float OUTER_RIM_WIDTH = 0.05f;

    public static final float NEEDLE_WIDTH = 0.025f;
    public static final float NEEDLE_HEIGHT = 0.32f;

    public static final float SCALE_POSITION = 0.015f;
    public static final float SCALE_START_VALUE = 0.0f;
    public static final float SCALE_END_VALUE = 100.0f;
    public static final float SCALE_START_ANGLE = 60.0f;
    public static final int SCALE_DIVISIONS = 10;

    public static final int[] RANGE_COLORS = {Color.WHITE};

    public static final int TEXT_SHADOW_COLOR = Color.argb(0, 0, 0, 0);
    public static final float TEXT_VALUE_SIZE = 0.3f;
    public static final float TEXT_UNIT_SIZE = 0.1f;
    private static final float[] DEFAULT_RANGE_VALUES = new float[]{0.0f, 1f, 5f, 10f, 20f, 30f, 50f, 75f, 100f};

    private boolean mShowScale;

    private float mOuterRimWidth;
    private float mInnerRimWidth;
    private float mNeedleWidth;
    private float mNeedleHeight;

    private float mScalePosition;
    private float mScaleStartValue;
    private float mScaleEndValue;
    private float mScaleStartAngle;
    private float[] mRangeValues;

    private int[] mRangeColors;
    private int mDivisions;
    private int mSubdivisions;

    private RectF mOuterRimRect;
    private RectF mInnerRimRect;
    private RectF mFaceRect;
    private RectF mScaleRect;

    private Bitmap mBackground;
    private Paint mBackgroundPaint;
    private Paint[] mRangePaints;
    private Paint mNeedleLeftPaint;
    private Paint mTextValuePaint;
    private Paint mTextUnitPaint;

    private String mTextValue;
    private String mTextUnit;
    private int mTextShadowColor;
    private float mTextValueSize;
    private float mTextUnitSize;


    private Path mNeedleLeftPath;

    private float mScaleRotation;
    private float mDivisionValue;
    private float mSubdivisionAngle;

    private float mTargetValue;
    private float mCurrentValue;

    private float mNeedleVelocity;
    private float mNeedleAcceleration;
    private long mNeedleLastMoved = -1;
    private boolean mNeedleInitialized;

    public GaugeView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        readAttrs(context, attrs, defStyle);
        init();
    }

    public GaugeView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GaugeView(final Context context) {
        this(context, null, 0);
    }

    private void readAttrs(final Context context, final AttributeSet attrs, final int defStyle) {
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GaugeView, defStyle, 0);
        mShowScale = a.getBoolean(R.styleable.GaugeView_showScale, SHOW_SCALE);
        mOuterRimWidth = a.getFloat(R.styleable.GaugeView_outerRimWidth, OUTER_RIM_WIDTH);
        mInnerRimWidth = 0.0f;
        mNeedleWidth = a.getFloat(R.styleable.GaugeView_needleWidth, NEEDLE_WIDTH);
        mNeedleHeight = a.getFloat(R.styleable.GaugeView_needleHeight, NEEDLE_HEIGHT);
        mScalePosition = (mShowScale) ? a.getFloat(R.styleable.GaugeView_scalePosition, SCALE_POSITION) : 0.0f;
        mScaleStartValue = a.getFloat(R.styleable.GaugeView_scaleStartValue, SCALE_START_VALUE);
        mScaleEndValue = a.getFloat(R.styleable.GaugeView_scaleEndValue, SCALE_END_VALUE);
        mScaleStartAngle = a.getFloat(R.styleable.GaugeView_scaleStartAngle, SCALE_START_ANGLE);
        mDivisions = a.getInteger(R.styleable.GaugeView_divisions, SCALE_DIVISIONS);
        mSubdivisions = 1;
        mTextShadowColor = a.getColor(R.styleable.GaugeView_textShadowColor, TEXT_SHADOW_COLOR);

        final CharSequence[] rangeValues = a.getTextArray(R.styleable.GaugeView_rangeValues);
        final CharSequence[] rangeColors = a.getTextArray(R.styleable.GaugeView_rangeColors);
        readRanges(rangeValues, rangeColors);
        final int textValueId = a.getResourceId(R.styleable.GaugeView_textValue, 0);
        final String textValue = a.getString(R.styleable.GaugeView_textValue);
        mTextValue = (0 < textValueId) ? context.getString(textValueId) : (null != textValue) ? textValue : "";
        final int textUnitId = a.getResourceId(R.styleable.GaugeView_textUnit, 0);
        final String textUnit = a.getString(R.styleable.GaugeView_textUnit);
        mTextUnit = (0 < textUnitId) ? context.getString(textUnitId) : (null != textUnit) ? textUnit : "";
        mTextShadowColor = a.getColor(R.styleable.GaugeView_textShadowColor, TEXT_SHADOW_COLOR);
        mTextValueSize = a.getFloat(R.styleable.GaugeView_textValueSize, TEXT_VALUE_SIZE);
        mTextUnitSize = a.getFloat(R.styleable.GaugeView_textUnitSize, TEXT_UNIT_SIZE);
        a.recycle();
    }

    private void readRanges(final CharSequence[] rangeValues, final CharSequence[] rangeColors) {
        int rangeValuesLength;
        if (rangeValues == null) {
            rangeValuesLength = DEFAULT_RANGE_VALUES.length;
        } else {
            rangeValuesLength = rangeValues.length;
        }
        final int length = rangeValuesLength;
        if (rangeValues != null) {
            mRangeValues = new float[length];
            for (int i = 0; i < length; i++) {
                mRangeValues[i] = Float.parseFloat(rangeValues[i].toString());
            }
        } else {
            mRangeValues = DEFAULT_RANGE_VALUES;
        }
        if (rangeColors != null) {
            mRangeColors = new int[length];
            for (int i = 0; i < length; i++) {
                mRangeColors[i] = Color.parseColor(rangeColors[i].toString());
            }
        } else {
            mRangeColors = RANGE_COLORS;
        }
    }

    @TargetApi(11)
    private void init() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        initDrawingRects();
        initDrawingTools();
        initScale();
    }

    public void initDrawingRects() {
        final float add = 0.015f;
        mInnerRimRect = new RectF(mOuterRimRect.left + mOuterRimWidth, mOuterRimRect.top + mOuterRimWidth, mOuterRimRect.right
                - mOuterRimWidth, mOuterRimRect.bottom - mOuterRimWidth);
        mFaceRect = new RectF(mInnerRimRect.left + mInnerRimWidth - add, mInnerRimRect.top + mInnerRimWidth - add,
                mInnerRimRect.right - mInnerRimWidth + add, mInnerRimRect.bottom - mInnerRimWidth + add);
        mScaleRect = new RectF(mFaceRect.left + mScalePosition, mFaceRect.top + mScalePosition, mFaceRect.right - mScalePosition,
                mFaceRect.bottom - mScalePosition);
    }

    private void initDrawingTools() {
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setFilterBitmap(true);
        setDefaultScaleRangePaints();
        setDefaultNeedlePaths();
        mNeedleLeftPaint = getDefaultNeedleLeftPaint();
        mTextValuePaint = getDefaultTextValuePaint();
        mTextUnitPaint = getDefaultTextUnitPaint();
   }

    public void setDefaultNeedlePaths() {
        final float x = 0.5f, y = 0.5f;
        mNeedleLeftPath = new Path();
        mNeedleLeftPath.moveTo(x, y);
        mNeedleLeftPath.lineTo(x - mNeedleWidth, y);
        mNeedleLeftPath.lineTo(x, y - mNeedleHeight);
        mNeedleLeftPath.lineTo(x+0.03f, y );
        mNeedleLeftPath.lineTo(x-0.03f, y );
    }

    // Нижная половинка указки
    public Paint getDefaultNeedleLeftPaint() {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(getResources().getColor(R.color.light_blue));
        return paint;
    }

    public void setDefaultScaleRangePaints() {
        final int length = mRangeColors.length;
        mRangePaints = new Paint[length];
        for (int i = 0; i < length; i++) {
            mRangePaints[i] = new Paint(Paint.LINEAR_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
            mRangePaints[i].setColor(mRangeColors[i]);
            mRangePaints[i].setStyle(Paint.Style.STROKE);
            mRangePaints[i].setStrokeWidth(0.005f);
            mRangePaints[i].setTextSize(0.05f);
            mRangePaints[i].setTypeface(Typeface.SANS_SERIF);
            mRangePaints[i].setTextAlign(Paint.Align.CENTER);
            mRangePaints[i].setShadowLayer(0.005f, 0.002f, 0.002f, mTextShadowColor);
        }
    }

    // скорость
    public Paint getDefaultTextValuePaint() {
        final Paint paint = new Paint(Paint.LINEAR_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
        paint.setColor(getResources().getColor(R.color.light_blue));
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(0.005f);
        paint.setTextSize(mTextValueSize);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setShadowLayer(0.01f, 0.002f, 0.002f, mTextShadowColor);
        return paint;
    }

    //MBPS
    public Paint getDefaultTextUnitPaint() {
        final Paint paint = new Paint(Paint.LINEAR_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
        paint.setColor(getResources().getColor(R.color.light_blue));
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(0.005f);
        paint.setTextSize(mTextUnitSize);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setShadowLayer(0.01f, 0.002f, 0.002f, mTextShadowColor);
        return paint;
    }

    @Override
    protected void onRestoreInstanceState(final Parcelable state) {
        final Bundle bundle = (Bundle) state;
        final Parcelable superState = bundle.getParcelable("superState");
        super.onRestoreInstanceState(superState);

        mNeedleInitialized = bundle.getBoolean("needleInitialized");
        mNeedleVelocity = bundle.getFloat("needleVelocity");
        mNeedleAcceleration = bundle.getFloat("needleAcceleration");
        mNeedleLastMoved = bundle.getLong("needleLastMoved");
        mCurrentValue = bundle.getFloat("currentValue");
        mTargetValue = bundle.getFloat("targetValue");
    }

    private void initScale() {
        mScaleRotation = (mScaleStartAngle + 180) % 360;
        mDivisionValue = (mScaleEndValue - mScaleStartValue) / mDivisions;
        mSubdivisionAngle = (360 - 2 * mScaleStartAngle) / (mDivisions * mSubdivisions);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        final Bundle state = new Bundle();
        state.putParcelable("superState", superState);
        state.putBoolean("needleInitialized", mNeedleInitialized);
        state.putFloat("needleVelocity", mNeedleVelocity);
        state.putFloat("needleAcceleration", mNeedleAcceleration);
        state.putLong("needleLastMoved", mNeedleLastMoved);
        state.putFloat("currentValue", mCurrentValue);
        state.putFloat("targetValue", mTargetValue);
        return state;
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        final int chosenWidth = chooseDimension(widthMode, widthSize);
        final int chosenHeight = chooseDimension(heightMode, heightSize);
        setMeasuredDimension(chosenWidth, chosenHeight);
    }

    private int chooseDimension(final int mode, final int size) {
        switch (mode) {
            case View.MeasureSpec.AT_MOST:
            case View.MeasureSpec.EXACTLY:
                return size;
            case View.MeasureSpec.UNSPECIFIED:
            default:
                return getDefaultDimension();
        }
    }

    private int getDefaultDimension() {
        return SIZE;
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        drawGauge();
    }

    private void drawGauge() {
        if (null != mBackground) {
            mBackground.recycle();
        }
        mBackground = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(mBackground);
        final float scale = Math.min(getWidth(), getHeight());
        canvas.scale(scale, scale);
        canvas.translate((scale == getHeight()) ? ((getWidth() - scale) / 2) / scale : 0
                , (scale == getWidth()) ? ((getHeight() - scale) / 2) / scale : 0);
        drawFace(canvas);
        drawScale(canvas);
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        drawBackground(canvas);
        final float scale = Math.min(getWidth(), getHeight());
        canvas.scale(scale, scale);
        canvas.translate((scale == getHeight()) ? ((getWidth() - scale) / 2) / scale : 0
                , (scale == getWidth()) ? ((getHeight() - scale) / 2) / scale : 0);
        drawNeedle(canvas);
        drawText(canvas);
        computeCurrentValue();
    }

    private void drawBackground(final Canvas canvas) {
        if (null != mBackground) {
            canvas.drawBitmap(mBackground, 0, 0, mBackgroundPaint);
        }
    }

    private void drawFace(final Canvas canvas) {
        //148
        //244
        float width = 0.5f;
        float height = 0.5f;
        float center_x, center_y;
        float radius;
        radius = 0.41f;
        Paint paint = new Paint();
        paint.setColor(getResources().getColor(R.color.speedometr_grey));
        paint.setStrokeWidth(0.1F);
        paint.setStyle(Paint.Style.FILL);
        final RectF oval = new RectF();
        paint.setStyle(Paint.Style.STROKE);
        center_x = 0.5f;
        center_y = 0.5f;
        oval.set(center_x - radius,
                center_y - radius,
                center_x + radius,
                center_y + radius);
        canvas.drawArc(oval, 140, 260, false, paint);
    }


    private void drawText(final Canvas canvas) {
        final String textValue = !TextUtils.isEmpty(mTextValue) ? mTextValue : valueString(mCurrentValue);
        final float textValueWidth = mTextValuePaint.measureText(textValue);
        final float textUnitWidth = !TextUtils.isEmpty(mTextUnit) ? mTextUnitPaint.measureText(mTextUnit) : 0;
        final float startX = CENTER;
        final float startY = CENTER + 0.2f;
        drawText(canvas, -1, textValue, startX, startY, mTextValuePaint);
        if (!TextUtils.isEmpty(mTextUnit)) {
            drawText(canvas, -1, mTextUnit, CENTER, CENTER + 0.3f, mTextUnitPaint);
        }
    }

    private void drawScale(final Canvas canvas) {
        canvas.save();
        canvas.rotate(mScaleRotation, 0.5f, 0.5f);
        final int totalTicks = mDivisions * mSubdivisions + 1;
        for (int i = 0; i < totalTicks; i++) {
            final float y1 = mScaleRect.top + 0.065f;
            final float y3 = y1 + 0.090f;
            final float value = getValueForTick(i);
            final Paint paint = getRangePaint(value);
            float div = mScaleEndValue / (float) mDivisions;
            float mod = value % div;
            paint.setStrokeWidth(0.01f);
            //цвет цифров скорости
            paint.setColor(getResources().getColor(R.color.speedometr_text_grey));
            canvas.drawLine(0.5f, y1 - 0.015f, 0.5f, y3 - 0.07f, paint);
            paint.setStyle(Paint.Style.FILL);
            drawText(canvas, i, valueString(value), 0.5f, y3, paint);
            canvas.rotate(mSubdivisionAngle, 0.5f, 0.5f);
        }
        canvas.restore();
    }

    private void drawText(Canvas canvas, int tick, String value, float x, float y, Paint paint) {
        float originalTextSize = paint.getTextSize();
        final float magnifier = 100f;
        canvas.save();
        canvas.scale(1f / magnifier, 1f / magnifier);
        float textWidth = 0;
        float textHeight = 0;
        if (tick != -1) {
            final int middleValue = mRangeValues.length / 2;
            if (tick == middleValue) {
                textHeight = -1;
                textWidth = 0;
            } else {
                textHeight = 1.5f;
                textWidth = -1;
            }
            canvas.rotate(120 - tick * mSubdivisionAngle, x * magnifier, y * magnifier);
        }
        paint.setTextSize(originalTextSize * magnifier);
        canvas.drawText(value, x * magnifier + textWidth, y * magnifier + textHeight, paint);
        canvas.restore();
        paint.setTextSize(originalTextSize);
    }

    private String valueString(final float value) {
        return String.format("%d", (int) value);
    }

    private float getValueForTick(final int tick) {
        return mRangeValues[tick];
    }

    private Paint getRangePaint(final float value) {
        return mRangePaints[0];
    }

    private void drawNeedle(final Canvas canvas) {
        if (mNeedleInitialized) {
            final float angle = getAngleForValue(mCurrentValue);
            final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(getResources().getColor(R.color.light_blue));
            paint.setStrokeWidth(0.1f);
            float sweepAngle;
            if (angle != 0 && angle != 240) {
                if (angle < 240) {
                    sweepAngle = angle + 120;
                } else {
                    sweepAngle = angle - 240;
                }
                canvas.drawArc(mFaceRect, 140, sweepAngle, false, paint);
            }
            canvas.save();
            canvas.rotate(angle, 0.5f, 0.5f);
            canvas.drawPath(mNeedleLeftPath, mNeedleLeftPaint);
            canvas.restore();
        }
    }

    private float getAngleForValue(final float value) {
        int range = -1;
        for (int i = 0; i < mRangeValues.length - 1; ++i) {
            range++;
            if (value == mRangeValues[i]) {
                range = i;
                break;
            }
            if (value == mRangeValues[i + 1]) {
                range = i + 1;
                break;
            }
            if (value > mRangeValues[i] && value < mRangeValues[i + 1]) {
                break;
            }
        }
        if (range == -1)
            return 0;
        float angle = range * mSubdivisionAngle + (value - mRangeValues[range]) * mSubdivisionAngle / (mRangeValues[range + 1] - mRangeValues[range]);
        return (mScaleRotation + angle) % 360;
    }

    private void computeCurrentValue() {
        if (!(Math.abs(mCurrentValue - mTargetValue) > 0.01f)) {
            return;
        }
        if (-1 != mNeedleLastMoved) {
            final float time = (System.currentTimeMillis() - mNeedleLastMoved) / 1000.0f;
            final float direction = Math.signum(mNeedleVelocity);
            if (Math.abs(mNeedleVelocity) < 90.0f) {
                mNeedleAcceleration = 5.0f * (mTargetValue - mCurrentValue);
            } else {
                mNeedleAcceleration = 0.0f;
            }
            mNeedleAcceleration = 5.0f * (mTargetValue - mCurrentValue);
            mCurrentValue += mNeedleVelocity * time;
            mNeedleVelocity += mNeedleAcceleration * time;
            if ((mTargetValue - mCurrentValue) * direction < 0.01f * direction) {
                mCurrentValue = mTargetValue;
                mNeedleVelocity = 0.0f;
                mNeedleAcceleration = 0.0f;
                mNeedleLastMoved = -1L;
            } else {
                mNeedleLastMoved = System.currentTimeMillis();
            }
            invalidate();
        } else {
            mNeedleLastMoved = System.currentTimeMillis();
            computeCurrentValue();
        }
    }

    public void setTargetValue(final float value) {
        if (mShowScale) {
            if (value < mScaleStartValue) {
                mTargetValue = mScaleStartValue;
            } else if (value > mScaleEndValue) {
                mTargetValue = mScaleEndValue;
            } else {
                mTargetValue = value;
            }
        } else {
            mTargetValue = value;
        }
        mNeedleInitialized = true;
        invalidate();
    }

    public void setShowRangeValues(boolean mShowRangeValues) {
        mNeedleInitialized = true;
        invalidate();
    }
}
