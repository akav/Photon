//	The MIT License (MIT)
//	
//	Copyright (c) 2016 Tzu-Chieh Chang (as known as D01phiN)
//	
//	Permission is hereby granted, free of charge, to any person obtaining a copy
//	of this software and associated documentation files (the "Software"), to deal
//	in the Software without restriction, including without limitation the rights
//	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//	copies of the Software, and to permit persons to whom the Software is
//	furnished to do so, subject to the following conditions:
//	
//	The above copyright notice and this permission notice shall be included in all
//	copies or substantial portions of the Software.
//	
//	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//	SOFTWARE.

package core;

import java.util.stream.IntStream;

import math.Rand;
import math.Vector3f;
import math.material.BRDF;
import math.material.Material;
import model.primitive.Intersection;
import scene.Scene;
import util.Debug;
import util.Func;

public class PathTracer
{
    public PathTracer()
    {
        
    }
    
    public void trace(Scene scene, HdrFrame sampleResult)
    {
        int widthPx  = sampleResult.getWidthPx();
        int heightPx = sampleResult.getHeightPx();

        Camera camera;
        synchronized(scene.getCamera())
        {
            camera = scene.getCamera();
        }

        // Parallelize over rows (y)
        IntStream.range(0, heightPx).parallel().forEach(y -> {
            Ray ray = new Ray();
            Intersection intersection = new Intersection();
            for(int x = 0; x < widthPx; x++)
            {
                ray.reset();
                camera.calcRayThroughPixelDistributed(ray, widthPx, heightPx, x, y);
                // pathTraceIterative(scene, ray, intersection, 3);
                pathTraceIterative(scene, ray, intersection, Integer.MAX_VALUE);
                sampleResult.setPixelRgb(x, y, ray.getRadiance().x, ray.getRadiance().y, ray.getRadiance().z);
            }
        });

        Statistics.addNumRays(widthPx * heightPx);
    }
    
    private void pathTraceIterative(Scene scene, Ray ray, Intersection intersection, int numBounces)
    {
        for(int nBounce = 0; nBounce <= numBounces; nBounce++)
        {
            intersection.clear();
            
            if(scene.findClosestIntersection(ray, intersection))
            {
                if(!intersection.interact(ray))
                    return;
                
                // offset a little to prevent self intersection artefact
                ray.getOrigin().set(ray.getDir().mul(0.001f).addLocal(intersection.getHitPoint()));
            }
            else
            {
                return;
            }
        }
    }
}
