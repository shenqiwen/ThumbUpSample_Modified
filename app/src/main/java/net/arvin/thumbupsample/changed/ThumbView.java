package net.arvin.thumbupsample.changed;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import net.arvin.thumbupsample.R;

/**
 * Created by arvinljw on 17/10/25 14:52
 * Function：
 * Desc： 左边部分 点赞与未点赞 动效处理
 *
 * 防止重复点击逻辑混乱
 * 其余 冗余逻辑较多
 */
public class ThumbView extends View {
    //圆圈颜色
    private static final int START_COLOR = Color.parseColor("#00e24d3d");
    private static final int END_COLOR = Color.parseColor("#88e24d3d");
    //缩放动画的时间
    private static final int SCALE_DURING = 150;
    //圆圈扩散动画的时间
    private static final int RADIUS_DURING = 300;

    private static final float SCALE_MIN = 0.9f;
    private static final float SCALE_MAX = 1f;

    private Bitmap mThumbUp;
    private Bitmap mShining;
    private Bitmap mThumbNormal;
    private Paint mBitmapPaint;

    private float mThumbWidth;
    private float mThumbHeight;
    private float mShiningWidth;
    private float mShiningHeight;

    private TuvPoint mShiningPoint;
    private TuvPoint mThumbPoint;
    private TuvPoint mCirclePoint;

    private float mRadiusMax;
    private float mRadiusMin;
    private float mRadius;
    private Path mClipPath;
    private Paint mCirclePaint;

    // 点赞标记
    private boolean mIsThumbUp;
    private long mLastStartTime;
    //点击的回调
    private ThumbUpClickListener mThumbUpClickListener;

    //被点击的次数，未点击时，未点赞是0，点赞是1，所以点完之后的次数是偶数则就是未点赞，奇数就是点赞
 //   private int mClickCount;
    // 点击后动画完成的次数
  //  private int mEndCount;
    private AnimatorSet mThumbUpAnim;

    public ThumbView(Context context) {
        this(context, null);
    }

    public ThumbView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThumbView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {
        // 初始化 图片相关信息
        initBitmapInfo();
        // 设置 圆圈 颜料
        mCirclePaint = new Paint();
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCirclePaint.setStrokeWidth(TuvUtils.dip2px(getContext(), 2));
        // 圆圈相对父View ThumbUpView的坐标
        mCirclePoint = new TuvPoint();
        mCirclePoint.x = mThumbPoint.x + mThumbWidth / 2;
        mCirclePoint.y = mThumbPoint.y + mThumbHeight / 2;
        // 圆圈 最大半径 最小半径
        mRadiusMax = Math.max(mCirclePoint.x - getPaddingLeft(), mCirclePoint.y - getPaddingTop());
        mRadiusMin = TuvUtils.dip2px(getContext(), 8);//这个值是根据点击效果调整得到的
        mClipPath = new Path();
        mClipPath.addCircle(mCirclePoint.x, mCirclePoint.y, mRadiusMax, Path.Direction.CW);
    }

    private void initBitmapInfo() {
        mBitmapPaint = new Paint();
        mBitmapPaint.setAntiAlias(true);
        // 获取 Bitmap 视图对象
        resetBitmap();
        // 点赞图的宽高
        mThumbWidth = mThumbUp.getWidth();
        mThumbHeight = mThumbUp.getHeight();
        // 三点 图 的宽高
        mShiningWidth = mShining.getWidth();
        mShiningHeight = mShining.getHeight();
        // 三点图 与 点赞图的 相对父View ThumbUpView的坐标
        mShiningPoint = new TuvPoint();
        mThumbPoint = new TuvPoint();
        //这个相对位置是在布局中试出来的
        // 三点 与 点赞图 的坐标位置
        mShiningPoint.x = getPaddingLeft() + TuvUtils.dip2px(getContext(), 2);
        mShiningPoint.y = getPaddingTop();
        mThumbPoint.x = getPaddingLeft();
        mThumbPoint.y = getPaddingTop() + TuvUtils.dip2px(getContext(), 8);
    }

    private void resetBitmap() {
        mThumbUp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_messages_like_selected);
        mThumbNormal = BitmapFactory.decodeResource(getResources(), R.drawable.ic_messages_like_unselected);
        mShining = BitmapFactory.decodeResource(getResources(), R.drawable.ic_messages_like_selected_shining);
    }

    //  当前视图部分 是否点赞 的属性 设置 逻辑处理
    public void setIsThumbUp(boolean isThumbUp) {
        this.mIsThumbUp = isThumbUp;
        // 点赞了 计数1 未点赞 计数0
    //    mClickCount = mIsThumbUp ? 1 : 0;
     //   mEndCount = mClickCount;
        // 刷新视图
        postInvalidate();
    }

    public boolean isThumbUp() {
        return mIsThumbUp;
    }
    // 当前视图部分  设置 点赞监听的 回调
    public void setThumbUpClickListener(ThumbUpClickListener thumbUpClickListener) {
        this.mThumbUpClickListener = thumbUpClickListener;
    }

    public TuvPoint getCirclePoint() {
        return mCirclePoint;
    }

    // 测量
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(TuvUtils.getDefaultSize(widthMeasureSpec, getContentWidth() + getPaddingLeft() + getPaddingRight()),
                TuvUtils.getDefaultSize(heightMeasureSpec, getContentHeight() + getPaddingTop() + getPaddingBottom()));
    }

    private int getContentWidth() {
        // 由上边代码可知 此行代码没用  点赞图的x坐标 必定比 三点图x坐标 要小
        float minLeft = Math.min(mShiningPoint.x, mThumbPoint.x);
        float maxRight = Math.max(mShiningPoint.x + mShiningWidth, mThumbPoint.x + mThumbWidth);
        return (int) (maxRight - minLeft);
    }

    private int getContentHeight() {
        float minTop = Math.min(mShiningPoint.y, mThumbPoint.y);
        float maxBottom = Math.max(mShiningPoint.y + mShiningHeight, mThumbPoint.y + mThumbHeight);
        return (int) (maxBottom - minTop);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle data = new Bundle();
        data.putParcelable("superData", super.onSaveInstanceState());
        data.putBoolean("isThumbUp", mIsThumbUp);
        return data;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle data = (Bundle) state;
        Parcelable superData = data.getParcelable("superData");
        super.onRestoreInstanceState(superData);

        mIsThumbUp = data.getBoolean("isThumbUp", false);

        init();
    }

    // 绘制
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mIsThumbUp) {  // 已点赞
            if (mClipPath != null) {
                canvas.save();
                canvas.clipPath(mClipPath);
                // 画 三点 图
                canvas.drawBitmap(mShining, mShiningPoint.x, mShiningPoint.y, mBitmapPaint);
                canvas.restore();
                // 画 圆圈
                canvas.drawCircle(mCirclePoint.x, mCirclePoint.y, mRadius, mCirclePaint);
            }
            // 画 点赞图
            canvas.drawBitmap(mThumbUp, mThumbPoint.x, mThumbPoint.y, mBitmapPaint);
        } else { // 未点赞
            // 画 未点赞图
            canvas.drawBitmap(mThumbNormal, mThumbPoint.x, mThumbPoint.y, mBitmapPaint);
        }
    }
    // 动画 开始方法
    public void startAnim() {
 //       mClickCount++;
//        boolean isFastAnim = false;
//        long currentTimeMillis = System.currentTimeMillis();
//        if (currentTimeMillis - mLastStartTime < 300) {
//            isFastAnim = true;
//        }
//        mLastStartTime = currentTimeMillis;

        if (mIsThumbUp) { // 已点赞 状态
//            if (isFastAnim) {  // 连续点击
//                startFastAnim();
//                return;
//            }
            // 取消点赞动效
            startThumbDownAnim();
 //           mClickCount = 0;
        } else {  // 未点赞 状态
          //  if (mThumbUpAnim != null) {  // 点赞动画已执行过 即 已经点赞过
          //      mClickCount = 0; // ?
         //   } else {  // 开启 点赞动效
                startThumbUpAnim();
           //     mClickCount = 1;
          //  }
        }
     //   mEndCount = mClickCount;  // ?
    }

//    /**
//     * 快速点击的 动效
//     */
//    private void startFastAnim() {
//        /**
//         * 点赞图标 放大动效
//         */
//        ObjectAnimator thumbUpScale = ObjectAnimator.ofFloat(this, "thumbUpScale", SCALE_MIN, SCALE_MAX);
//        thumbUpScale.setDuration(SCALE_DURING);
//        thumbUpScale.setInterpolator(new OvershootInterpolator());
//        /**
//         * 圆圈 放大动效
//         */
//        ObjectAnimator circleScale = ObjectAnimator.ofFloat(this, "circleScale", mRadiusMin, mRadiusMax);
//        circleScale.setDuration(RADIUS_DURING);
//
//        AnimatorSet set = new AnimatorSet();
//        set.play(thumbUpScale).with(circleScale);
//        set.addListener(new AnimatorListenerAdapter() {
//            @Override
//            public void onAnimationEnd(Animator animation) {
//                super.onAnimationEnd(animation);
//                mEndCount++;
//                if (mClickCount != mEndCount) {  // 如果动画 点击次数 与 点击后动画完成点击数 不相等 则表示 连续点击 不做动效处理
//                    return;
//                }
//                if (mClickCount % 2 == 0) { // 点击次数 偶数  即 开始 取消点赞 动效
//                    startThumbDownAnim();
//                } else {   // 点击次数 奇数 即 开始 点赞动效
//                    if (mThumbUpClickListener != null) {
//                        // 点赞回调
//                        mThumbUpClickListener.thumbUpFinish();
//                    }
//                }
//            }
//        });
//        set.start();
//    }

    /**
     * 取消点赞的 动效
     */
    private void startThumbDownAnim() {
        /**
         * 点赞图标的 放大动画
         */
        ObjectAnimator thumbUpScale = ObjectAnimator.ofFloat(this, "thumbUpScale", SCALE_MIN, SCALE_MAX);
        thumbUpScale.setDuration(SCALE_DURING);
        thumbUpScale.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mIsThumbUp = false;
                setNotThumbUpScale(SCALE_MAX);
                if (mThumbUpClickListener != null) {
                    // 取消点赞的回调
                    mThumbUpClickListener.thumbDownFinish();
                }
            }
        });
        thumbUpScale.start();
    }

    /**
     *点赞的动效
     */
    private void startThumbUpAnim() {
        /**
         * 未点赞 图标的缩小动画
         */
        ObjectAnimator notThumbUpScale = ObjectAnimator.ofFloat(this, "notThumbUpScale", SCALE_MAX, SCALE_MIN);
        notThumbUpScale.setDuration(SCALE_DURING);
        notThumbUpScale.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mIsThumbUp = true;
            }
        });
        /**
         * 点赞图标的放大动画
         */
        ObjectAnimator thumbUpScale = ObjectAnimator.ofFloat(this, "thumbUpScale", SCALE_MIN, SCALE_MAX);
        thumbUpScale.setDuration(SCALE_DURING);
        thumbUpScale.setInterpolator(new OvershootInterpolator());
        /**
         * 圆圈的放大动画
         */
        ObjectAnimator circleScale = ObjectAnimator.ofFloat(this, "circleScale", mRadiusMin, mRadiusMax);
        circleScale.setDuration(RADIUS_DURING);
        /**
         * 动画的集合
         */
        mThumbUpAnim = new AnimatorSet();
        mThumbUpAnim.play(thumbUpScale).with(circleScale);
        mThumbUpAnim.play(thumbUpScale).after(notThumbUpScale);
        mThumbUpAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mThumbUpAnim = null;
                if (mThumbUpClickListener != null) {
                    // 点赞的 回调
                    mThumbUpClickListener.thumbUpFinish();
                }
            }
        });
        // 动画开始
        mThumbUpAnim.start();
    }

    private void setNotThumbUpScale(float scale) {
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        mThumbNormal = BitmapFactory.decodeResource(getResources(), R.drawable.ic_messages_like_unselected);
        mThumbNormal = Bitmap.createBitmap(mThumbNormal, 0, 0, mThumbNormal.getWidth(), mThumbNormal.getHeight(),
                matrix, true);
        postInvalidate();
    }

    private void setThumbUpScale(float scale) {
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        mThumbUp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_messages_like_selected);
        mThumbUp = Bitmap.createBitmap(mThumbUp, 0, 0, mThumbUp.getWidth(), mThumbUp.getHeight(),
                matrix, true);
        postInvalidate();
    }

    private void setShiningScale(float scale) {
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        mShining = BitmapFactory.decodeResource(getResources(), R.drawable.ic_messages_like_selected_shining);
        mShining = Bitmap.createBitmap(mShining, 0, 0, mShining.getWidth(), mShining.getHeight(),
                matrix, true);
        postInvalidate();
    }

    public void setCircleScale(float radius) {
        mRadius = radius;
        mClipPath = new Path();
        mClipPath.addCircle(mCirclePoint.x, mCirclePoint.y, mRadius, Path.Direction.CW);
        float fraction = (mRadiusMax - radius) / (mRadiusMax - mRadiusMin);
        mCirclePaint.setColor((int) TuvUtils.evaluate(fraction, START_COLOR, END_COLOR));
        postInvalidate();
    }

    // 点赞回调监听接口
    public interface ThumbUpClickListener {
        //点赞回调
        void thumbUpFinish();
        //取消回调
        void thumbDownFinish();
    }

}
