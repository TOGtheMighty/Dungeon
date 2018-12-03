package hourglass.dungeon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.os.Handler;
import android.app.Activity;
import android.os.Message;
import android.view.WindowManager;
import android.widget.TextView;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import java.util.HashMap;
import java.util.LinkedList;

import static java.lang.Math.min;
import static java.lang.Math.floor;

public class GameHolder extends SurfaceView implements SurfaceHolder.Callback {
    public Context save_Context;
    public int framecnt = 0;
    public GameThread GThread;
    private TextView mStatusText;
    private Paint titleTextPaint;
    private Paint fpsTextPaint;
    int animation_per_frame = 6;
    Bitmap bobj;
    Bitmap bobj2;
    java.util.Queue q;

    public GameHolder(Context context) {
        super(context);
        save_Context = context;
        setFocusable(true); //not yet necessary, but you never know what the future brings
        titleTextPaint = new Paint();
        titleTextPaint.setColor(0xFF000000);
        titleTextPaint.setStyle(Paint.Style.FILL);
        titleTextPaint.setTextSize(60);
        fpsTextPaint = new Paint();
        fpsTextPaint.setColor(0xFF000000);
        fpsTextPaint.setStyle(Paint.Style.FILL);
        fpsTextPaint.setTextSize(60);
        q = new java.util.LinkedList();
        bobj = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        bobj2 = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        for (int xcnt = 0; xcnt < 100; xcnt++) {
            for (int ycnt = 0; ycnt < 100; ycnt++) {
                bobj.setPixel(xcnt, ycnt, 0x00000000);
                bobj2.setPixel(xcnt, ycnt, 0x00000000);
            }
        }
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        // create thread only; it's started in surfaceCreated()
        GThread = new GameThread(holder, context, new Handler() {
            @Override
            public void handleMessage(Message m) {
                mStatusText.setVisibility(m.getData().getInt("viz"));
                mStatusText.setText(m.getData().getString("text"));
            }
        });
        setFocusable(true); // make sure we get key events
        holder.addCallback(this);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        GThread.setSurfaceSize(width, height);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
        GThread.setRunning(true);
        GThread.start();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        GThread.setRunning(false);
        while (retry) {
            try {
                try {
                    GThread.mGT.join();
                } catch (InterruptedException e) {
                }
                GThread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }

    public boolean onTouchEvent(MotionEvent event)
    {
        GThread.onTouchEvent(event);
        return true;
    }

/*    protected void USEDTOBEonDraw(Canvas canvas) {
        for (int xcnt = 0; xcnt < 100; xcnt++) {
            for (int ycnt = 0; ycnt < 100; ycnt++) {
                bobj.setPixel(xcnt, ycnt, 0x00000011 +0x100*ycnt + 0x10000 * xcnt + 0x1000000*(int)(min(255.0,(double)framecnt*.255)));
                int redval = (int)((double)(xcnt)*min(1.0,(double)framecnt / 1000.0));
                int greenval = (int)((double)(ycnt)*min(1.0,(double)framecnt / 1000.0));
                int blueval = (int)((double)(11.0)*min(1.0,(double)framecnt / 1000.0));
                bobj2.setPixel(xcnt, ycnt, 0xFF000000 +0x10000*redval + 0x100 * greenval + blueval);
            }
        }
        long ntime = System.nanoTime();
        q.add(ntime);
        Bitmap tbmp = SBHObj.GetBGScreen(framecnt/animation_per_frame);
        framecnt++;

        canvas.scale((float)1.0,(float)1.0);
        canvas.drawBitmap(bobj, 25, 50, null);
        canvas.drawBitmap(bobj2, 25, 150, null);
        canvas.drawText(Integer.toString(framecnt),10,1275,titleTextPaint);
        if (framecnt > 100) {
            long ttime = (long)q.remove();
            canvas.drawText(Double.toString(100.0*Math.pow(10.0,9.0)/((double)ntime-(double)ttime)),10,1475,fpsTextPaint);
        }
        TileDirt01 tdobj = new TileDirt01(save_Context);
        for(int xcnt = 0; xcnt < 100; xcnt++) {
            for(int ycnt = 0; ycnt < 100; ycnt++) {
//            tdobj.PasteImage(SBHObj, tbmp, cnt*tdobj.img_width, 0, framecnt/animation_per_frame);
//            canvas.drawBitmap(tdobj.ImageBMP, -50+xcnt*tdobj.img_width/2+ycnt*tdobj.img_width/2, 50-xcnt*(tdobj.img_height-1)/2+ycnt*(tdobj.img_height-1)/2, null);
                int tx = (int) (-50 + xcnt * tdobj.img_width / 2 + ycnt * tdobj.img_width / 2);
                int ty = (int) (50 - xcnt * (tdobj.img_height - 1) / 2 + ycnt * (tdobj.img_height - 1) / 2);
                canvas.drawBitmap(tdobj.ImageBMP, tx,ty,null);
            }
        }
        canvas.drawBitmap(tbmp, 0, 0, null);
        invalidate();
    }*/
}

class GameThread extends Thread {
    private final Object mRunLock = new Object();
    private boolean mRun = false;
    private SurfaceHolder mSurfaceHolder;
    private Handler mHandler;
    public Context mContext;
    private int mCanvasHeight = 1;
    private int mCanvasWidth = 1;
    private Paint fpsTextPaint;
    private Paint memoryPaint;
    private java.util.Queue time_q;
    private java.util.Queue bitmap_q;
    private long framecnt;
    private long CurrentX = 0;
    private long CurrentY = 0;
    TileDirt01 tdobj;
    Bitmap [] bmp_array;
    GraphicsThread mGT;

    public GameThread(SurfaceHolder surfaceHolder, Context context, Handler handler) {
        mSurfaceHolder = surfaceHolder;
        mHandler = handler;
        mContext = context;
        tdobj = new TileDirt01(mContext);
        fpsTextPaint = new Paint();
        fpsTextPaint.setColor(0xFF0000FF);
        fpsTextPaint.setStyle(Paint.Style.FILL);
        fpsTextPaint.setTextSize(60);
        memoryPaint = new Paint();
        memoryPaint.setColor(0xFF0000FF);
        memoryPaint.setStyle(Paint.Style.FILL);
        memoryPaint.setTextSize(60);
        time_q = new java.util.LinkedList();
        bitmap_q = new java.util.LinkedList();
        bmp_array = new Bitmap[1000000];
        mGT = new GraphicsThread(this);
        mGT.setRunning(true);
        mGT.start();
//        mGT.AddToLoadQ(mGT.GetCoordID(15000, 28000));
//        mGT.AddToLoadQ(mGT.GetCoordID(16000, 28000));
//        mGT.AddToLoadQ(mGT.GetCoordID(15000, 29000));
//        mGT.AddToLoadQ(mGT.GetCoordID(16000, 29000));
        int tile = mGT.GetCoordID(15000, 28000);
        this.CurrentX = 15000;
        this.CurrentY = 28000;
    }

    @Override
    public void run() {
        while (mRun) {
//            bmp_array[(int)framecnt] = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888);
//            bmp_array[(int)framecnt].eraseColor(0xFFFF00FF);
//            bitmap_q.add(bmp_array[(int)framecnt]);
            Canvas c = null;
            try {
                c = mSurfaceHolder.lockCanvas(null);
                synchronized (mSurfaceHolder) {
                    synchronized (mRunLock) {
                        if (mRun) doDraw(c);
                    }
                }
            } finally {
                // do this in a finally so that if an exception is thrown
                // during the above, we don't leave the Surface in an
                // inconsistent state
                if (c != null) {
                    mSurfaceHolder.unlockCanvasAndPost(c);
                }
            }
        }
    }

    public void setSurfaceSize(int width, int height) {
        // synchronized to make sure these all change atomically
        synchronized (mSurfaceHolder) {
            mCanvasWidth = width;
            mCanvasHeight = height;
            // don't forget to resize the background image
        }
    }

    public void setRunning(boolean b) {
        // Do not allow mRun to be modified while any canvas operations
        // are potentially in-flight. See doDraw().
        synchronized (mRunLock) {
            mRun = b;
        }
    }

    private void doDraw(Canvas canvas) {
        // Draw the background image. Operations on the Canvas accumulate
        // so this is like clearing the screen.
        long ntime = System.nanoTime();
        time_q.add(ntime);
//        mGT.DrawCanvas(canvas,(int)framecnt/6,15000,28000);
        Paint paint2 = new Paint();
        paint2.setColor(Color.WHITE);
        paint2.setStyle(Paint.Style.FILL);
        canvas.drawPaint(paint2);
        mGT.DrawCanvas(canvas,(int)framecnt/6,this.CurrentX,this.CurrentY);
//        canvas.drawBitmap(SBHObj.GetBGScreen((int)framecnt/10),0,0,null);
        if (framecnt > 100) {
            long ttime = (long) time_q.remove();
            canvas.drawText(Double.toString(100.0 * Math.pow(10.0, 9.0) / ((double) ntime - (double) ttime)), 10, (long)((double)mCanvasHeight*.75), fpsTextPaint);
        }
        framecnt++;
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Activity.ACTIVITY_SERVICE);
        MemoryInfo mi = new MemoryInfo();
        activityManager.getMemoryInfo(mi);
        Runtime runtime = Runtime.getRuntime();
        //String s = String.format("free:%s%% %sKB total:%sKB max:%sKB ", runtime.freeMemory() * 100f / runtime.totalMemory(), runtime.freeMemory(), runtime.totalMemory() / 1024,
//                runtime.maxMemory() / 1024);
        String s = Long.toString((long)((float)mi.availMem/1024.0/1024.0));
        canvas.drawText(s,10,(long)((double)mCanvasHeight*.8), fpsTextPaint);//Long.toString(availHeapSizeInMB), 10, (long)((double)mCanvasHeight*.8), fpsTextPaint);

        canvas.save();
        canvas.restore();
    }

    public void onTouchEvent(MotionEvent event)
    {
        if((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP){
            double tX = event.getX();
            double tY = event.getY();
            this.CurrentX += (long)(tX - mGT.screenWidth/2);
            this.CurrentY += (long)(tY - mGT.screenHeight/2);
        }
    }
};

class GraphicsThread extends Thread {
    GameThread mGT;
    ResourceHolder mResourceHolder;
    int mMapWidth = 1;
    int mMapHeight = 1;
    int mTileWidth = 28;
    int mTileHeight = 15;
    int mTileTilt = 2;
    int mbgBitmapWidth = 1000;
    int mbgBitmapHeight = 1000;
    int bg_animation_frames = 6;
    int screenWidth = 1100;
    int screenHeight = 1100;
    long mTotalWidth = 1;
    long mTotalHeight = 1;
    long mbgOffsetX = 0;
    long mbgOffsetY = 0;
    Bitmap mMapBMap = null;
    HashMap<Integer, ScreenBitmapHolder> backgroundHMap;
    private boolean mRun = false;
    private java.util.Queue bg_load_q;
    int [][]tileGrid;

    public GraphicsThread(GameThread sGT){
        mGT = sGT;
        mResourceHolder = new ResourceHolder(mGT.mContext);
        backgroundHMap = new HashMap<Integer,ScreenBitmapHolder>();
        mMapBMap = BitmapFactory.decodeResource(mGT.mContext.getResources(), R.drawable.maze);
        mMapWidth = mMapBMap.getWidth();
        mMapHeight = mMapBMap.getHeight();
        this.mTotalHeight = this.GetYCoord(0, mMapHeight-1)+(long)mTileHeight;
        this.mTotalWidth = this.GetYCoord(mMapWidth-1, mMapHeight-1)+(long)mTileWidth;
        bg_load_q = new LinkedList();
        mbgOffsetY = (long)mMapWidth*(mTileHeight-1)/2;
        mbgOffsetX = (long)0;
        tileGrid = new int[mTileWidth][mTileHeight];
        WindowManager wm = (WindowManager) mGT.mContext.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displaymetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(displaymetrics);
        screenWidth = displaymetrics.widthPixels;
        screenHeight = displaymetrics.heightPixels;
        for(int cntx = 0; cntx < mTileWidth; cntx++){
            for(int cnty = 0; cnty < mTileHeight; cnty++){
                if ((cntx+1)+cnty*mTileTilt < mTileWidth/2){
                    tileGrid[cntx][cnty] = 1;
                }
                else if (cntx-cnty*mTileTilt > mTileWidth/2){
                    tileGrid[cntx][cnty] = 2;
                }
                else if (cntx+cnty*mTileTilt > mTileWidth/2 + (mTileHeight-1)*2){
                    tileGrid[cntx][cnty] = 3;
                }
                else if (cnty*mTileTilt-(cntx+1) > mTileWidth/2){
                    tileGrid[cntx][cnty] = 4;
                }
                else {
                    tileGrid[cntx][cnty] = 0;
                }
            }
        }
    }

    @Override
    public void run() {
        while (mRun) {
            if (bg_load_q.peek() != null){
                int coordToLoad = (int) bg_load_q.poll();
                long fy_pos = this.GetYFromCoord(coordToLoad);
                long fx_pos = this.GetXFromCoord(coordToLoad);
                ScreenBitmapHolder tSBHObj = new ScreenBitmapHolder(mbgBitmapWidth,mbgBitmapHeight,bg_animation_frames,fx_pos,fy_pos);
                // Load tiles onto SBH
                int xStartTile = this.GetXTile(fx_pos, fy_pos);
                int yStartTile = this.GetYTile(fx_pos, fy_pos);
                int xTile = xStartTile;
                int yTile = yStartTile;
                int rowcnt = 0;
                while(this.GetYCoord(xTile,yTile) <= tSBHObj.y_pos + this.mbgBitmapHeight){
                    while(this.GetXCoord(xTile,yTile) <= tSBHObj.x_pos + this.mbgBitmapWidth) {
                        // Get tile information
                        if (xTile < 0 | xTile >= this.mMapWidth | yTile < 0 | yTile >= this.mMapHeight){

                        }
                        else {
                            int tMapPixel = mMapBMap.getPixel(xTile, yTile);
                            ImageHolder tIH = null;
                            if (tMapPixel == 0xFF000000) {
                                tIH = null;
                            } else if (tMapPixel == 0xFFFFFFFF) {
                                tIH = mResourceHolder.GetTileIH(tMapPixel);
                            }
                            if (tIH != null) {
                                for (int framecnt = 0; framecnt < this.bg_animation_frames; framecnt++) {
                                    int x = 5;
                                    long t_x = this.GetXCoord(xTile, yTile) + tIH.GetImageXOffset(0, 0, framecnt);
                                    long t_y = this.GetYCoord(xTile, yTile) + tIH.GetImageYOffset(0, 0, framecnt);
                                    tSBHObj.pasteBitmap(tIH.GetBitmap(0, 0, framecnt), t_x, t_y, framecnt);
                                }
                            }
                        }
                        xTile += 1;
                        yTile += 1;
                    }

                        xTile = xStartTile - (int)((2 + rowcnt) / 2);
                        yTile = yStartTile + (int)((1 + rowcnt) / 2);
                        rowcnt++;
                }
                backgroundHMap.put(coordToLoad,tSBHObj);
                int x = 5;
            }
        }
    }

    public void AddToLoadQ(int coordToAdd){
        if (bg_load_q.contains(coordToAdd)){
            return;
        }
        else {
            bg_load_q.add(coordToAdd);
        }
    }

    public void setRunning(boolean b) {
        mRun = b;
    }

    public void CenterScreen(double x, double y){

    }

    public int GetCoordIDFromTile(int x_pos, int y_pos){

        long tx = GetXCoord(x_pos,y_pos);
        long ty = GetYCoord(x_pos,y_pos);
        return(int)((ty / mbgBitmapHeight)*(mTotalWidth/mbgBitmapWidth)+tx/mbgBitmapWidth);
    }

    public int GetCoordID(long x_pos, long y_pos){
        return(int)((y_pos / mbgBitmapHeight)*((mTotalWidth-1)/mbgBitmapWidth+1))+(int)(x_pos/mbgBitmapWidth);
    }

    public int GetXTile(long x_pos, long y_pos) {
        int xTile = (int) floor(((double) x_pos - (double) mbgOffsetX) / (double) mTileWidth - ((double) y_pos - (double) mbgOffsetY) / ((double) mTileHeight - 1.0));
        int yTile = (int) floor(((double) x_pos - (double) mbgOffsetX) / (double) mTileWidth + ((double) y_pos - (double) mbgOffsetY) / ((double) mTileHeight - 1.0));
        int xExtra = (int) (x_pos - GetXCoord(xTile, yTile));
        int yExtra = (int) (y_pos - GetYCoord(xTile, yTile));
        int tGridVal = tileGrid[xExtra][yExtra];
        if (tGridVal == 2) {
            return xTile + 1;
        } else if (tGridVal == 4) {
            return xTile - 1;
        } else {
            return xTile;
        }
    }

    public int GetYTile(long x_pos, long y_pos) {
        int xTile = (int) floor(((double) x_pos - (double) mbgOffsetX) / (double) mTileWidth - ((double) y_pos - (double) mbgOffsetY) / ((double) mTileHeight - 1.0));
        int yTile = (int) floor(((double) x_pos - (double) mbgOffsetX) / (double) mTileWidth + ((double) y_pos - (double) mbgOffsetY) / ((double) mTileHeight - 1.0));
        int xExtra = (int) (x_pos - GetXCoord(xTile, yTile));
        int yExtra = (int) (y_pos - GetYCoord(xTile, yTile));
        int tGridVal = tileGrid[xExtra][yExtra];
        if (tGridVal == 1) {
            return yTile - 1;
        } else if (tGridVal == 3) {
            return yTile + 1;
        } else {
            return yTile;
        }
    }

    public void DrawCanvas(Canvas canvas,int framecnt,long x_pos,long y_pos){
        int FirstInRowCoordNum = this.GetCoordID(x_pos, y_pos);
        int tCoorNum = FirstInRowCoordNum;
        while((this.GetXFromCoord(tCoorNum) <= x_pos + this.screenWidth) & (this.GetYFromCoord(tCoorNum) <= y_pos + this.screenHeight)){
            ScreenBitmapHolder tSBH = backgroundHMap.get(tCoorNum);
            if (tSBH == null) {
                this.AddToLoadQ(tCoorNum);
            }
            else {
                canvas.drawBitmap(tSBH.GetBGScreen(framecnt), tSBH.x_pos - x_pos, tSBH.y_pos - y_pos, null);
            }
            tCoorNum += 1;
            if(this.GetXFromCoord(tCoorNum) > x_pos + this.screenWidth){
                tCoorNum = FirstInRowCoordNum + (int)((mTotalWidth-1)/mbgBitmapWidth + 1);
                FirstInRowCoordNum = tCoorNum;
            }
            long new_x = this.GetXFromCoord(tCoorNum);
            long new_y = this.GetYFromCoord(tCoorNum);
            new_y = new_y + 0;
        }
    }

    public long GetXFromCoord(int coordNum){
        long coordsWide = (mTotalWidth-1)/mbgBitmapWidth+1;
        return(long)mbgBitmapWidth*(coordNum%coordsWide);
    }

    public long GetYFromCoord(int coordNum){
        long coordsWide = (mTotalWidth-1)/mbgBitmapWidth+1;
        return(long)mbgBitmapHeight*(coordNum/coordsWide);
    }

    public long GetXCoord(int x_pos, int y_pos){
        return (long) (x_pos * mTileWidth / 2 + y_pos * mTileWidth / 2);
    }

    public long GetYCoord(int x_pos, int y_pos){
        return (long) ((mMapWidth-1)*((mTileHeight - 1) / 2) - x_pos * (mTileHeight - 1) / 2 + y_pos * (mTileHeight - 1) / 2);
    }
};

class ResourceHolder{
    HashMap<Integer, ImageHolder> TileHMap;
    public ResourceHolder(Context context){
        TileHMap = new HashMap<Integer, ImageHolder>();
        TileDirt01 tObj = new TileDirt01(context);
        TileHMap.put(0xFFFFFFFF,tObj);
    }
    public ImageHolder GetTileIH(int TileNum){
        if (TileHMap.containsKey(TileNum)){
            return TileHMap.get(TileNum);
        }
        else {
            return null;
        }
    }
}

class ScreenBitmapHolder {
    int mHeight = 1;
    int mWidth = 1;
    long x_pos = 0;
    long y_pos = 0;
    int number_of_bgs = 6;
    Bitmap [] background_array;

    public ScreenBitmapHolder(int set_width,int set_height,int set_numbers_of_bgs, long set_x_pos,long set_y_pos) {
        mHeight = set_height;
        mWidth = set_width;
        number_of_bgs = set_numbers_of_bgs;
        background_array = new Bitmap[number_of_bgs];
        for (int cnt = 0; cnt < number_of_bgs; cnt++) {
            background_array[cnt] = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
/*            for (int xcnt = 0; xcnt < mWidth; xcnt++) {
                for (int ycnt = 0; ycnt < mHeight; ycnt++) {
                    background_array[cnt].setPixel(xcnt, ycnt, mm(xcnt,ycnt,cnt));
                }
            }*/
        }
        x_pos = set_x_pos;
        y_pos = set_y_pos;
    }

    public void pasteBitmap(Bitmap srcBitmap, long tar_x_pos, long tar_y_pos,int animationCnt){
        Bitmap tBitmap = background_array[animationCnt];
        Canvas tCanvas = new Canvas(tBitmap);
        tCanvas.drawBitmap(srcBitmap,tar_x_pos - x_pos,tar_y_pos-y_pos,null);
        if ((tar_x_pos - x_pos > 0)&(tar_y_pos-y_pos>0)){
            int x = 5;
        }
    }

    public int mm(int xcnt, int ycnt, int scnt){
        return 0xFF000000 + 0x100*(int)((float)ycnt/(float)mHeight*(float)0xFF) + 0x10000 * (int)((float)xcnt/(float)mWidth*(float)0xFF) + 0x000001*(int)((float)scnt/(float)number_of_bgs*(float)0xFF);
    }

    public Bitmap GetBGScreen(int framecount){
        int mframecount = framecount % number_of_bgs;
        return background_array[mframecount];
    }
}

class ImageHolder
{
    Bitmap ImageBMP;
    int img_width,img_height,x_offset = 0, y_offset = 0;

    public ImageHolder(Context context)
    {
    }

    public Bitmap GetBitmap(int action, int direction, int framecnt){
        return ImageBMP;
    }

    public int GetImageWidth(int action, int direction, int framecnt){
        return img_width;
    }

    public int GetImageHeight(int action, int direction, int framecnt){
        return img_height;
    }

    public int GetImageXOffset(int action, int direction, int framecnt){
        return x_offset;
    }

    public int GetImageYOffset(int action, int direction, int framecnt){
        return y_offset;
    }
}

class TileDirt01 extends ImageHolder
{
    public TileDirt01(Context context)
    {
        super(context);
        ImageBMP = BitmapFactory.decodeResource(context.getResources(), R.drawable.dirt01);
        img_width = (int)ImageBMP.getWidth();// 28;
        img_height = (int)ImageBMP.getHeight();//15;
    }
}