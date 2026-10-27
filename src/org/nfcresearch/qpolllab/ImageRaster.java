package org.nfcresearch.qpolllab;

import java.util.Arrays;
import java.util.Random;

/** Quantization and preview decoding share the panel's four code values. */
public final class ImageRaster {
    private static final int[] COLORS={0xff000000,0xffffffff,0xfff5cd00,0xffc80000};
    public static int color(int code){if(code<0||code>3)throw new IllegalArgumentException("颜色码错误");return COLORS[code];}

    private static final float[] BAYER_2X2 = {
        0f/4f, 2f/4f,
        3f/4f, 1f/4f
    };

    private static final float[] BAYER_4X4 = {
         0f/16f,  8f/16f,  2f/16f, 10f/16f,
        12f/16f,  4f/16f, 14f/16f,  6f/16f,
         3f/16f, 11f/16f,  1f/16f,  9f/16f,
        15f/16f,  7f/16f, 13f/16f,  5f/16f
    };

    private static final float[] BAYER_8X8 = {
         0f/64f, 32f/64f,  8f/64f, 40f/64f,  2f/64f, 34f/64f, 10f/64f, 42f/64f,
        48f/64f, 16f/64f, 56f/64f, 24f/64f, 50f/64f, 18f/64f, 58f/64f, 26f/64f,
        12f/64f, 44f/64f,  4f/64f, 36f/64f, 14f/64f, 46f/64f,  6f/64f, 38f/64f,
        60f/64f, 28f/64f, 52f/64f, 20f/64f, 62f/64f, 30f/64f, 54f/64f, 22f/64f,
         3f/64f, 35f/64f, 11f/64f, 43f/64f,  1f/64f, 33f/64f,  9f/64f, 41f/64f,
        51f/64f, 19f/64f, 59f/64f, 27f/64f, 49f/64f, 17f/64f, 57f/64f, 25f/64f,
        15f/64f, 47f/64f,  7f/64f, 39f/64f, 13f/64f, 45f/64f,  5f/64f, 37f/64f,
        63f/64f, 31f/64f, 55f/64f, 23f/64f, 61f/64f, 29f/64f, 53f/64f, 21f/64f
    };

    private static final int[] BLUE_NOISE_16 = {
        244,  96, 179,  22, 126,  49, 172, 237,  33, 114, 219,  41, 153, 209,  67, 137,
         14, 212,  57, 142, 227,  88, 198,  11, 146, 190,  77, 165,  18, 110, 240,  85,
        186,  40, 167, 107,  28, 161,  72, 132, 252,  53, 122, 233,  92, 175,  30, 157,
         75, 128, 248,  68, 203, 118,   2, 216,  99,  17, 183,  64, 139, 223, 103,  45,
        220,  20,  84, 150,  37, 193,  82, 149,  39, 169, 247, 106,  25,  61, 194, 129,
        111, 176, 234, 117,  56, 170,  24, 119, 206,  60, 135,  15, 201, 145, 250,   8,
         48,  89,  34, 197, 138, 225,  66, 187,  86, 229,  31, 162,  79, 113,  51, 182,
        164, 208, 148,  62,  12, 102, 154,  26, 246, 109, 178, 100, 226,  36, 173,  93,
         69,  16, 241,  98, 180,  46, 214,  74, 134,  44, 210,  10, 131, 195,  81, 232,
        144, 123,  42, 160,  80, 238, 124, 166,   5, 155,  83, 147, 253,  55, 121,   3,
        230, 192,  87, 217,  27,  63, 190,  43, 200,  65, 188,  50, 115, 171, 211,  70,
         38, 108,   7, 136, 174, 112, 141,  94, 239, 120,  21, 221,  90,  19, 156,  29,
        152,  59, 184,  73,  52, 205,  13, 181,  35, 163,  97, 133,  47, 207,  76, 243,
         95, 222,  23, 236, 127,  32, 168,  78, 213,  71, 249,  61, 177, 101, 140, 125,
        185, 130,  91, 158,  86, 242, 105, 124,  16, 137,  32, 191,   9, 235,  40, 189,
          5,  47, 202,  36, 175,  54, 218,  58, 196,  91, 224, 104, 151,  83, 215,  64
    };

    public static byte[] quantize(int[] pixels,int width,int height,boolean binary,int threshold,boolean dither,int block) {
        return quantize(pixels, width, height, binary, threshold, dither ? Dither.FLOYD_STEINBERG : Dither.NONE, block);
    }

    public static byte[] quantize(int[] pixels,int width,int height,boolean binary,int threshold,Dither dither,int block) {
        if(width<=0||height<=0||(long)width*height!=pixels.length||threshold<0||threshold>255)
            throw new IllegalArgumentException("图片参数错误");
        if(dither==null)dither=Dither.NONE;
        if(block>1) {
            // Binary threshold must be applied after averaging, not to four-color codes.
            if(!binary)return ImageBlocks.quantize(pixels,width,height,block,COLORS);
            if(block!=2&&block!=4&&block!=8&&block!=16)throw new IllegalArgumentException("色块参数错误");
            byte[] out=new byte[pixels.length];
            for(int y=0;y<height;y+=block)for(int x=0;x<width;x+=block){
                int right=Math.min(width,x+block),bottom=Math.min(height,y+block);float sum=0;
                for(int yy=y;yy<bottom;yy++)for(int xx=x;xx<right;xx++)sum+=luma(pixels[yy*width+xx]);
                byte code=(byte)(sum/((right-x)*(bottom-y))>=threshold?1:0);
                for(int yy=y;yy<bottom;yy++)Arrays.fill(out,yy*width+x,yy*width+right,code);
            }
            return out;
        }
        if(block!=1)throw new IllegalArgumentException("色块参数错误");

        byte[] codes=new byte[pixels.length];
        boolean errorDiff = (dither == Dither.FLOYD_STEINBERG || dither == Dither.ATKINSON ||
            dither == Dither.BURKES || dither == Dither.SIERRA3 || dither == Dither.SIERRA2 ||
            dither == Dither.SIERRA_LITE || dither == Dither.STUCKI || dither == Dither.JJN);

        Random rnd = (dither == Dither.RANDOM) ? new Random(1337) : null;

        // [channel 0..2][line 0..2][x padded with 2 on left and 2 on right]
        float[][][] lines = new float[3][3][width + 4];

        for(int y=0;y<height;y++) {
            for(int x=0;x<width;x++) {
                int index=y*width+x,p=pixels[index],best=0;
                if(binary) {
                    float offset = errorDiff ? lines[0][0][x + 2] : getOffset(dither, x, y, true, rnd);
                    float value = clamp(luma(p) + offset);
                    best = value >= threshold ? 1 : 0;
                    if(errorDiff) {
                        float err = value - (best == 1 ? 255f : 0f);
                        diffuse(dither, lines[0][0], lines[0][1], lines[0][2], x, err);
                    }
                } else {
                    float r, g, b;
                    if (errorDiff) {
                        r = clamp(((p >>> 16) & 255) + lines[0][0][x + 2]);
                        g = clamp(((p >>> 8) & 255) + lines[1][0][x + 2]);
                        b = clamp((p & 255) + lines[2][0][x + 2]);
                    } else {
                        float off = getOffset(dither, x, y, false, rnd);
                        r = clamp(((p >>> 16) & 255) + off);
                        g = clamp(((p >>> 8) & 255) + off);
                        b = clamp((p & 255) + off);
                    }
                    float score=Float.MAX_VALUE;
                    for(int k=0;k<4;k++){
                        int c=COLORS[k];
                        float dr=r-((c>>>16)&255),dg=g-((c>>>8)&255),db=b-(c&255),s=dr*dr+dg*dg+db*db;
                        if(s<score){score=s;best=k;}
                    }
                    if(errorDiff) {
                        int c = COLORS[best];
                        diffuse(dither, lines[0][0], lines[0][1], lines[0][2], x, r - ((c >>> 16) & 255));
                        diffuse(dither, lines[1][0], lines[1][1], lines[1][2], x, g - ((c >>> 8) & 255));
                        diffuse(dither, lines[2][0], lines[2][1], lines[2][2], x, b - (c & 255));
                    }
                }
                codes[index]=(byte)best;
            }
            if(errorDiff) {
                for(int c=0;c<3;c++) {
                    float[] t = lines[c][0];
                    lines[c][0] = lines[c][1];
                    lines[c][1] = lines[c][2];
                    lines[c][2] = t;
                    Arrays.fill(lines[c][2], 0f);
                }
            }
        }
        return codes;
    }

    private static float getOffset(Dither dither, int x, int y, boolean binary, Random rnd) {
        float val;
        switch (dither) {
            case BAYER_2X2:
                val = BAYER_2X2[(y % 2) * 2 + (x % 2)];
                break;
            case BAYER_4X4:
                val = BAYER_4X4[(y % 4) * 4 + (x % 4)];
                break;
            case BAYER_8X8:
                val = BAYER_8X8[(y % 8) * 8 + (x % 8)];
                break;
            case BLUE_NOISE:
                val = BLUE_NOISE_16[(y % 16) * 16 + (x % 16)] / 255f;
                break;
            case RANDOM:
                val = rnd.nextFloat();
                break;
            default:
                return 0f;
        }
        return (val - 0.5f) * (binary ? 128f : 56f);
    }

    private static void diffuse(Dither dither, float[] l0, float[] l1, float[] l2, int x, float err) {
        switch (dither) {
            case FLOYD_STEINBERG:
                l0[x + 3] += err * (7f / 16f);
                l1[x + 1] += err * (3f / 16f);
                l1[x + 2] += err * (5f / 16f);
                l1[x + 3] += err * (1f / 16f);
                break;
            case ATKINSON:
                // Atkinson distributes 6/8 of error, leaving 2/8 (high contrast).
                l0[x + 3] += err * (1f / 8f);
                l0[x + 4] += err * (1f / 8f);
                l1[x + 1] += err * (1f / 8f);
                l1[x + 2] += err * (1f / 8f);
                l1[x + 3] += err * (1f / 8f);
                l2[x + 2] += err * (1f / 8f);
                break;
            case BURKES:
                l0[x + 3] += err * (8f / 32f);
                l0[x + 4] += err * (4f / 32f);
                l1[x]     += err * (2f / 32f);
                l1[x + 1] += err * (4f / 32f);
                l1[x + 2] += err * (8f / 32f);
                l1[x + 3] += err * (4f / 32f);
                l1[x + 4] += err * (2f / 32f);
                break;
            case SIERRA3:
                l0[x + 3] += err * (5f / 32f);
                l0[x + 4] += err * (3f / 32f);
                l1[x]     += err * (2f / 32f);
                l1[x + 1] += err * (4f / 32f);
                l1[x + 2] += err * (5f / 32f);
                l1[x + 3] += err * (4f / 32f);
                l1[x + 4] += err * (2f / 32f);
                l2[x + 1] += err * (1f / 32f);
                l2[x + 2] += err * (2f / 32f);
                l2[x + 3] += err * (1f / 32f);
                break;
            case SIERRA2:
                l0[x + 3] += err * (4f / 16f);
                l0[x + 4] += err * (3f / 16f);
                l1[x]     += err * (1f / 16f);
                l1[x + 1] += err * (2f / 16f);
                l1[x + 2] += err * (3f / 16f);
                l1[x + 3] += err * (2f / 16f);
                l1[x + 4] += err * (1f / 16f);
                break;
            case SIERRA_LITE:
                l0[x + 3] += err * (2f / 4f);
                l1[x + 1] += err * (1f / 4f);
                l1[x + 2] += err * (1f / 4f);
                break;
            case STUCKI:
                l0[x + 3] += err * (8f / 42f);
                l0[x + 4] += err * (4f / 42f);
                l1[x]     += err * (2f / 42f);
                l1[x + 1] += err * (4f / 42f);
                l1[x + 2] += err * (8f / 42f);
                l1[x + 3] += err * (4f / 42f);
                l1[x + 4] += err * (2f / 42f);
                l2[x]     += err * (1f / 42f);
                l2[x + 1] += err * (2f / 42f);
                l2[x + 2] += err * (4f / 42f);
                l2[x + 3] += err * (2f / 42f);
                l2[x + 4] += err * (1f / 42f);
                break;
            case JJN:
                l0[x + 3] += err * (7f / 48f);
                l0[x + 4] += err * (5f / 48f);
                l1[x]     += err * (3f / 48f);
                l1[x + 1] += err * (5f / 48f);
                l1[x + 2] += err * (7f / 48f);
                l1[x + 3] += err * (5f / 48f);
                l1[x + 4] += err * (3f / 48f);
                l2[x]     += err * (1f / 48f);
                l2[x + 1] += err * (3f / 48f);
                l2[x + 2] += err * (5f / 48f);
                l2[x + 3] += err * (3f / 48f);
                l2[x + 4] += err * (1f / 48f);
                break;
            default:
                break;
        }
    }

    private static float luma(int p){return (299*((p>>>16)&255)+587*((p>>>8)&255)+114*(p&255))/1000f;}
    private static float clamp(float value){return Math.max(0,Math.min(255,value));}

    public static int[] preview(byte[] packed,int width,int height) {
        if(width<=0||height<=0||height%4!=0||(long)width*height/4!=packed.length)throw new IllegalArgumentException("预览尺寸错误");
        int[] pixels=new int[width*height];
        for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
            int fromBottom=height-1-y,index=(width-1-x)*(height/4)+fromBottom/4;
            pixels[y*width+x]=COLORS[((packed[index]&255)>>(6-2*(fromBottom%4)))&3];
        }
        return pixels;
    }
}
