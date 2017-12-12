# opengl_mediacodec_yuv
use opengl es to render yuv frame output from mediacodec

media codec simple decode project please refer to https://github.com/zhanghuicuc/simplest_mediacodec_decode
opengl render yuv please refer to http://blog.csdn.net/ueryueryuery/article/details/17608185


opengl render vertex data:
(-1,1)                                          (1,1)
        c__________________________d
         |            |           |
         |            |           |
  _____________________________________________
         |            |           |
         |            |           | 
         |            |           |
        a__________________________b
(-1,-1)                                         (1,-1)


We have four limited postion for full size window of 2D picture, so we should make vertex of our window inside the limited vertex data.
e.g : window of left top, vertex data = {-1.0f, 0.0f, 0.0f, 0.0f, -1.0f, 1.0f, 0.0f, 1.0f}, please consider the order of vertice.
                                         |        |   |        |  |        |   |        |
                                         -----a----   -----b----  ----c-----   ------d---
