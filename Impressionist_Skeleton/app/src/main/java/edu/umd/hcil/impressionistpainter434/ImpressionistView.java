package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.text.MessageFormat;
import java.util.Random;


/**
 * Skeleton created by jon on 3/20/2016.
 *
 * Project completed by Thomas McHale
 */
public class ImpressionistView extends View {

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();

    private int _alpha = 150;
    private int _defaultRadius = 25;
    private Point _lastPoint = null;
    private long _lastPointTime = -1;
    private boolean _useMotionSpeedForBrushStrokeSize = true;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Square;
    private float _minBrushRadius = 5;
    private Random generator = new Random();

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(_defaultRadius);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
        _imageView.buildDrawingCache();
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    public void setDynamicMode(boolean dynamic){
        _useMotionSpeedForBrushStrokeSize = dynamic;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        _offScreenCanvas.drawColor(Color.WHITE);
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
    }


    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){

        switch(motionEvent.getAction()){

            case MotionEvent.ACTION_DOWN:
                // Pick up our initial information so we can calculate velocity later.
                _lastPointTime = SystemClock.elapsedRealtime();
                _lastPoint = new Point((int)motionEvent.getX(), (int)motionEvent.getY());
            case MotionEvent.ACTION_MOVE:


                Bitmap imageViewBitmap = _imageView.getDrawingCache();
                if (imageViewBitmap == null ) break; // No image loaded yet.

                Rect drawBounds = new Rect(0,0,imageViewBitmap.getWidth(), imageViewBitmap.getHeight());


                int historySize = motionEvent.getHistorySize();

                // Paint current x/y
                float touchX = motionEvent.getX();
                float touchY = motionEvent.getY();


                // Velocity update
                Point currentPoint = new Point((int) touchX, (int) touchY);
                float velocity = (float) Math.sqrt(Math.pow(_lastPoint.x - currentPoint.x,2) + Math.pow(_lastPoint.y - currentPoint.y,2))/(SystemClock.elapsedRealtime() - _lastPointTime);
                _lastPoint = currentPoint;
                _lastPointTime = SystemClock.elapsedRealtime();

                int velocityScale = 25; // Default velocity scale.

                // We need to draw both the historical points and the current x/y point.
                // NOTE: The first pass through this uses the current X/Y, subsequent passes are through the historical points.
                for (int i = 0; i < historySize+1; i++){
                    if (drawBounds.contains((int) touchX,(int) touchY) && getBitmapPositionInsideImageView(_imageView).contains((int) touchX,(int) touchY)) {
                        _paint.setColor(imageViewBitmap.getPixel((int) touchX, (int) touchY));
                        _paint.setAlpha(_alpha);

                        switch (_brushType){
                            case Circle:
                                if (_useMotionSpeedForBrushStrokeSize){
                                    // Set stroke width to velocity * 5 or min brush radius if stroke width would be too small.
                                    _paint.setStrokeWidth(velocity * velocityScale > _minBrushRadius? velocity * velocityScale : _minBrushRadius );
                                    _offScreenCanvas.drawCircle(touchX, touchY, velocity * velocityScale, _paint);
                                } else {
                                    _offScreenCanvas.drawCircle(touchX, touchY, _defaultRadius, _paint);
                                }
                                break;
                            case Square:
                                if (_useMotionSpeedForBrushStrokeSize){
                                    // Set stroke width to velocity * 5 or min brush radius if stroke width would be too small.
                                    _paint.setStrokeWidth(velocity * velocityScale > _minBrushRadius ? velocity * velocityScale : _minBrushRadius);
                                }
                                _offScreenCanvas.drawPoint(touchX, touchY, _paint);
                                break;
                            case Hatch:
                                velocityScale = 15;
                                if (!_useMotionSpeedForBrushStrokeSize){
                                    velocity = 1;
                                }
                                for (int j = 0; j < 3; j ++){
                                    int xDir = generator.nextBoolean()? 1 : -1;
                                    float sx = touchX + xDir * (generator.nextInt(5) * velocity * velocityScale);
                                    float ex = touchX - xDir * (generator.nextInt(5) * velocity * velocityScale);
                                    int yDir = generator.nextBoolean()? 1 : -1;
                                    float sy = touchY + yDir * (generator.nextInt(5) * velocity * velocityScale);
                                    float ey = touchY - yDir * (generator.nextInt(5) * velocity * velocityScale);
                                    _paint.setStrokeWidth(3);
                                    _offScreenCanvas.drawLine(sx,sy,ex,ey,_paint);
                                }
                                break;


                        }
                    }
                    if (i < historySize) {
                        touchX = motionEvent.getHistoricalX(i);
                        touchY = motionEvent.getHistoricalY(i);
                    }
                }



                // TODO invalidate only rectangle we changed
                invalidate();


                // TODO do I need to reupdate velocity??
                break;
        }



        return true;
    }


    /**
     * Returns the offscreen bitmap, for purposes of saving the bitmap.
     * @return
     */
    public Bitmap getOffScreenBitmap(){
        return _offScreenBitmap;
    }



    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }


}

