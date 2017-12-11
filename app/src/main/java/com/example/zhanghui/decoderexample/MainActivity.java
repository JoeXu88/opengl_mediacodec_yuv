/**
 * simplest mediacodec decode_video_to_yuvfile example
 * Given any input media file, this app will decode it and you can choose to output yuv files or not
 * You can also use this app to test your decoder performance, but remember to remove redundant logs at first
 *
 * author: zhanghui
 * email: zhanghuicuc@gmail.com
 * blog: http://blog.csdn.net/nonmarking
 *
 */
package com.example.zhanghui.decoderexample;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.Trace;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private EditText mUrlEditText;
    private EditText mToastEditText;
    private Button mDecodeButton;
    private static final MediaCodecList sMCL = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
    private String decoder = null;
    private long costTime = 0;
    private int frameNum = 0;
    private GLSurface mglsuface = null;
    private GLSurfaceView mglview = null;
    private MyGLRender mrender = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mUrlEditText = (EditText) findViewById(R.id.input_url_editText);
        mToastEditText = (EditText) findViewById(R.id.toast_editText);
        mDecodeButton = (Button) findViewById(R.id.decode_button);

        /*** seting gl render option ***/
        //mglview = (GLSurfaceView)  findViewById(R.id.glv);
        //mglview.setEGLContextClientVersion(2);
        // Render the view only when there is a change in the drawing data
        //mglview.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mglsuface = new GLSurface(this);
        mrender = new MyGLRender(mglsuface);
        mglsuface.setRenderer(mrender);
        /******************************/

        mDecodeButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                String fileUrl = mUrlEditText.getText().toString().trim();
                if (fileUrl == null) {
                    Toast.makeText(MainActivity.this, "file url wrong", Toast.LENGTH_SHORT).show();
                } else {
                    new DecodeTask().execute(fileUrl);
                }
            }
        });
    }

    private Handler mainHandler = new Handler() {
         public void handleMessage(Message msg) {
             if (msg.what == 0) {
                 mToastEditText.setText("frame No." + msg.obj + " decoded");
             } else if (msg.what == 1) {
                 mToastEditText.setText(frameNum + " frames Decode finished in " + msg.obj + " ms");
             }
         }
    };

    public class DecodeTask extends AsyncTask<String, Integer, Long> {

        @Override
        protected Long doInBackground(String... url) {
            DecodeVideo(url[0]);
            return costTime;
        }

        @Override
        protected void onPreExecute() {
            setContentView(mglsuface);
        }

        @Override
        protected void onPostExecute(Long t) {
            setContentView(R.layout.activity_main);
            //mToastEditText.setText(frameNum + " frames Decode finished in " + t + " ms");
            if (t > 0) {
                Message msg = new Message();
                msg.what = 1;
                msg.obj = t;
                mainHandler.sendMessage(msg);
            } else {
                Toast.makeText(MainActivity.this, "decode cost time wrong", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void DecodeVideo(String fileUrl){
        int width = -1;
        int height = -1;
        int ylen = 0;
        int srcW = -1;
        int srcH = -1;

        MediaExtractor extractor = new MediaExtractor();
        MediaCodec codec = null;
        if (!canDecodeVideo(fileUrl, extractor, codec)) {
            Log.i(TAG, "no supported decoder found ");
            return; //skip
        }
        int trackIndex = extractor.getSampleTrackIndex();
        MediaFormat format = extractor.getTrackFormat(trackIndex);
        srcH = format.getInteger(MediaFormat.KEY_HEIGHT);
        srcW = format.getInteger(MediaFormat.KEY_WIDTH);
        Log.d(TAG,"reso from source file=> w:"+srcW+", h:"+srcH);
        try {
            codec = MediaCodec.createByCodecName(decoder);
        } catch (IOException e) {
            Log.e(TAG, "failed to create decoder");
            return;
        }
        Log.i("@@@@", "using codec: " + codec.getName());
        codec.configure(format, null, null /* crypto */, 0 /* flags */);
        codec.start();
        long decodeStartTime = SystemClock.elapsedRealtime();
        long last_renderTime = decodeStartTime;
        // start decode loop
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        final long kTimeOutUs = 5000; // 5ms timeout
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int deadDecoderCounter = 0;
        int samplenum = 0;
        int numframes = 0;
        String fileName = "/sdcard/Pictures/output_dec.yuv";
        FileOutputStream outStream;
        try {
            Log.v(TAG, "output will be saved as " + fileName);
            outStream = new FileOutputStream(fileName);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to create debug output file " + fileName, ioe);
        }
        while (!sawOutputEOS && deadDecoderCounter < 100) {
            // handle input
            Trace.beginSection("DecodeVideo handleinput");
            if (!sawInputEOS) {
                int inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);

                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = codec.getInputBuffer(inputBufIndex);

                    int sampleSize =
                            extractor.readSampleData(dstBuf, 0 /* offset */);
                    long presentationTimeUs = extractor.getSampleTime();
                    Log.i("@@@@", "read sample " + samplenum + ":" + extractor.getSampleFlags()
                     + " @ " + extractor.getSampleTime() + " size " + sampleSize);

                    if (sampleSize < 0) {
                        Log.d(TAG, "saw input EOS.");
                        sawInputEOS = true;
                        sampleSize = 0; // required otherwise queueInputBuffer returns invalid.
                    } else {
                        samplenum++; // increment before comparing with stopAtSample
                        if (samplenum == -1) {
                            Log.d(TAG, "saw input EOS (stop at sample).");
                            sawInputEOS = true; // tag this sample as EOS
                        }
                    }
                    codec.queueInputBuffer(
                            inputBufIndex,
                            0 /* offset */,
                            sampleSize,
                            presentationTimeUs,
                            sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                    if (!sawInputEOS) {
                        extractor.advance();
                    }
                }
            }
            Trace.endSection();

            // handle output
            int outputBufIndex = codec.dequeueOutputBuffer(info, kTimeOutUs);

            deadDecoderCounter++;
            Trace.beginSection("DecodeVideo handleoutput");
            if (outputBufIndex >= 0) {
                if (info.size > 0) { // Disregard 0-sized buffers at the end.
                    deadDecoderCounter = 0;
                    numframes++;
                    Log.d(TAG, "got frame, size " + info.size + "/" + info.presentationTimeUs +
                            "/" + numframes + "/" + info.flags);
                    Message msg = new Message();
                    msg.what = 0;
                    msg.obj = numframes;
                    mainHandler.sendMessage(msg);
                    //String fileName = "/sdcard/Pictures/output_" + numframes + ".yuv";
                    Image image = codec.getOutputImage(outputBufIndex);
                    byte[] data = getDataFromImage(image);
                    if (data != null && data.length > 0 && ylen > 0) {

                        mrender.update(data);
                        Log.d(TAG,"finish one frame cost:"+(SystemClock.elapsedRealtime()-last_renderTime));
                        last_renderTime = SystemClock.elapsedRealtime();

                        /*
                        try {
                            outStream.write(data);
                            //outStream.close();
                        } catch (IOException ioe) {
                            throw new RuntimeException("failed writing data to file " + fileName, ioe);
                        }
                        */
                    }
                    if (image != null) {
                        image.close();
                    }
                }
                codec.releaseOutputBuffer(outputBufIndex, false /* render */);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "saw output EOS.");
                    sawOutputEOS = true;
                }
            }  else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat oformat = codec.getOutputFormat();
                if (oformat.containsKey(MediaFormat.KEY_COLOR_FORMAT) &&
                        oformat.containsKey(MediaFormat.KEY_WIDTH) &&
                        oformat.containsKey(MediaFormat.KEY_HEIGHT)) {
                    int colorFormat = oformat.getInteger(MediaFormat.KEY_COLOR_FORMAT);
                    width = oformat.getInteger(MediaFormat.KEY_WIDTH);
                    height = oformat.getInteger(MediaFormat.KEY_HEIGHT);

                    if(width > srcW) width = srcW;
                    if(height > srcH) height = srcH;
                    Log.d(TAG, "output fmt: " + colorFormat + " dim " + width + "x" + height);
                    mrender.update(width, height);
                    ylen = width * height;
                }
            } else if (outputBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                Log.d(TAG, "no output frame available yet");
            }
            Trace.endSection();
        }
        try {
            outStream.close();
        } catch (IOException ioe) {
            throw new RuntimeException("failed close file " + fileName, ioe);
        }

        long decodeEndTime = SystemClock.elapsedRealtime();
        costTime = decodeEndTime - decodeStartTime;
        frameNum = numframes;
        codec.stop();
        codec.release();
        extractor.release();
        return;
    }

    private boolean canDecodeVideo(String fileUrl, MediaExtractor ex, MediaCodec codec) {
        try {
            ex.setDataSource(fileUrl);
            for (int i = 0; i < ex.getTrackCount(); ++i) {
                MediaFormat format = ex.getTrackFormat(i); //ex. call MPEG4Extractor.getTrackMetaData
                // only check for video codecs
                String mime = format.getString(MediaFormat.KEY_MIME).toLowerCase();
                if (!mime.startsWith("video/")) {
                    continue;
                }
                decoder = sMCL.findDecoderForFormat(format);
                if (decoder != null) {
                    ex.selectTrack(i);
                    return true;
                }
            }
        } catch (IOException e) {
            Log.i(TAG, "could not open path " + fileUrl);
        }
        return false;
    }

    /**
     * Get a byte array image data from an Image object.
     * <p>
     * Read data from all planes of an Image into a contiguous unpadded,
     * unpacked 1-D linear byte array, such that it can be write into disk, or
     * accessed by software conveniently. It supports YUV_420_888/NV21/YV12
     * input Image format.
     * </p>
     * <p>
     * For YUV_420_888/NV21/YV12/Y8/Y16, it returns a byte array that contains
     * the Y plane data first, followed by U(Cb), V(Cr) planes if there is any
     * (xstride = width, ystride = height for chroma and luma components).
     * </p>
     */
    private static byte[] getDataFromImage(Image image) {
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        int rowStride, pixelStride;
        byte[] data = null;

        // Read image data
        Image.Plane[] planes = image.getPlanes();

        // Check image validity
        switch (format) {
            case ImageFormat.YUV_420_888:
                Log.d(TAG, "image fmt: YUV_420_888");
                break;
            case ImageFormat.NV21:
                Log.d(TAG, "image fmt: NV21");
                break;
            case ImageFormat.YV12:
                Log.d(TAG, "image fmt: YV12");
                break;
            default:
                Log.e(TAG, "Unsupported Image Format: " + format);
                return null;
        }
        if (((format == ImageFormat.YUV_420_888) || (format == ImageFormat.NV21)
                ||(format == ImageFormat.YV12)) && (planes.length != 3)) {
            Log.e(TAG, "YUV420 format Images should have 3 planes");
            return null;
        }

        ByteBuffer buffer = null;

        int offset = 0;
        data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        //Log.d(TAG,"deocde image w:"+width+", h:"+height+", bitppxl:"+ImageFormat.getBitsPerPixel(format));
        byte[] rowData = new byte[planes[0].getRowStride()];
        for (int i = 0; i < planes.length; i++) {
            int shift = (i == 0) ? 0 : 1;
            buffer = planes[i].getBuffer();
            rowStride = planes[i].getRowStride();
            pixelStride = planes[i].getPixelStride();
            // For multi-planar yuv images, assuming yuv420 with 2x2 chroma subsampling.
            int w = crop.width() >> shift;
            int h = crop.height() >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int bytesPerPixel = ImageFormat.getBitsPerPixel(format) / 8;
                int length;
                if (pixelStride == bytesPerPixel) {
                    // Special case: optimized read of the entire row
                    length = w * bytesPerPixel;
                    buffer.get(data, offset, length);
                    offset += length;
                } else {
                    // Generic case: should work for any pixelStride but slower.
                    // Use intermediate buffer to avoid read byte-by-byte from
                    // DirectByteBuffer, which is very bad for performance
                    length = (w - 1) * pixelStride + bytesPerPixel;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[offset++] = rowData[col * pixelStride];
                    }
                }
                // Advance buffer the remainder of the row stride
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }
        return data;
    }
}
