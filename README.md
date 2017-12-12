# opengl_mediacodec_yuv
use opengl es to render yuv frame output from mediacodec<br>

media codec simple decode project please refer to https://github.com/zhanghuicuc/simplest_mediacodec_decode <br>
opengl render yuv please refer to http://blog.csdn.net/ueryueryuery/article/details/17608185 <br>
<br>

#### opengl render vertex data
                (-1,1)                                      (1,1)
                        c____________|____________d
                        |            |           |
                        |            |           |
                _____________________|________________________
                        |            |           |
                        |            |           | 
                        |            |           |
                        a____________|___________|b
                (-1,-1)              |                        (1,-1)


We have four limited postion for full size window of 2D picture, so we should make vertex of our window inside the limited vertex data.
e.g : window of left top, vertex data = {-1.0f, 0.0f, 0.0f, 0.0f, -1.0f, 1.0f, 0.0f, 1.0f}, please consider the order of vertice.<br>
We need to follow the order: a->b->c->d.
