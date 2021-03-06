/* Copyright 2014 The jeo project. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jeo.data.mem;

import com.vividsolutions.jts.geom.Envelope;
import org.jeo.data.Driver;
import org.jeo.data.RasterDataset;
import org.jeo.data.RasterQuery;
import org.jeo.raster.*;
import org.jeo.util.Dimension;
import org.jeo.util.Key;
import org.jeo.util.Rect;
import org.osgeo.proj4j.CoordinateReferenceSystem;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MemRaster implements RasterDataset {

    String name;
    Envelope bounds;
    CoordinateReferenceSystem crs;

    List<MemBand> bands = new ArrayList<MemBand>();

    public MemRaster(String name, Envelope bounds, CoordinateReferenceSystem crs) {
        this.name = name;
        this.bounds = bounds;
        this.crs = crs;
    }

    public void addBand(String name, Band.Color color, DataType datatype, Object data) {
        if (data == null || !data.getClass().isArray()) {
            throw new IllegalArgumentException("data must be a non-null array");
        }

        if (Array.getLength(data) == 0 || !Array.get(data, 0).getClass().isArray()
            || Array.getLength(Array.get(data,0)) == 0) {
            throw new IllegalArgumentException("data must be two dimensional array of non-zero size");
        }

        Array2D<Number> array = new Array2D<Number>(data);
        if (!bands.isEmpty()) {
            Array2D first = bands.get(0).data;
            if (first.length() != array.length() && first.length(0) != array.length(0)) {
                throw new IllegalArgumentException(String.format(
                    "data dimensions must match existing band: (%d,%d), was (%d,%d)", first.length(0), first.length(),
                    array.length(0), array.length()));
            }
        }

        bands.add(new MemBand(name, color, datatype, array));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public Driver<?> getDriver() {
        return new Memory();
    }

    @Override
    public Map<Key<?>, Object> getDriverOptions() {
        return Collections.EMPTY_MAP;
    }

    @Override
    public CoordinateReferenceSystem crs() throws IOException {
        return crs;
    }

    @Override
    public Envelope bounds() throws IOException {
        return bounds;
    }

    @Override
    public void close() {
    }

    @Override
    public Dimension size() {
        if (bands.isEmpty()) {
            return new Dimension(0,0);
        }

        Array2D buf = bands.get(0).data;
        return new Dimension(buf.length(0), buf.length());
    }

    @Override
    public List<Band> bands() throws IOException {
        return (List) bands;
    }

    @Override
    public Raster read(RasterQuery query) throws IOException {
        Raster raster = new Raster();
        raster.bounds(bounds()).crs(crs());

        Rect r = rect();
        if (query.getBounds() != null) {
            r = r.map(query.getBounds(), raster.bounds());
            raster.bounds(raster.bounds().intersection(query.getBounds()));
        }

        Dimension size = query.getSize();
        if (size == null) {
            size = size();
        }
        raster.size(size);

        List<Band> bands = null;
        if (query.getBands() != null) {
            int[] b = query.getBands();
            bands = new ArrayList<Band>(b.length);
            for (int i : b) {
                bands.add(bands().get(i));
            }
        }
        else {
            bands = bands();
        }
        raster.bands(bands);

        DataType dataType = query.getDataType();
        if (dataType == null) {
            dataType = bands.get(0).datatype();
        }

        DataBuffer buf = DataBuffer.create(r.width() * r.height(), dataType);
        buf.buffer().order(ByteOrder.LITTLE_ENDIAN);
        for (int y = r.top; y < r.bottom; y++) {
            for (int x = r.left; x < r.right; x++) {
                for (Band band : bands) {
                    MemBand mb = (MemBand)band;
                    buf.put(mb.data.get(y,x));
                }
                buf.word();
            }
        }

        if (!size.equals(r.size())) {
            buf = DataBuffer.resample(buf, r.size(), size);
        }

        return raster.data(buf.rewind());
    }

    Rect rect() {
        return new Rect(0,0, size());
    }

    static class Array2D<T> {

        Object array;

        Array2D(Object array) {
            this.array = array;
        }

        int length() {
            return Array.getLength(array);
        }

        int length(int dim) {
            return Array.getLength(Array.get(array, dim));
        }

        T get(int i, int j) {
            return (T) Array.get(Array.get(array, i), j);
        }
    }

    static class MemBand implements Band {

        Array2D<Number> data;
        Color color;
        DataType datatype;
        String name;

        MemBand(String name, Color color, DataType datatype, Array2D<Number> data) {
            this.name = name;
            this.color = color;
            this.datatype = datatype;
            this.data = data;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Color color() {
            return color;
        }

        @Override
        public DataType datatype() {
            return datatype;
        }

        @Override
        public Double nodata() {
            return null;
        }

        @Override
        public Stats stats() throws IOException {
            Stats stats = new Stats(Double.MAX_VALUE, -Double.MAX_VALUE, 0d, 0d);

            int n = data.length() * data.length(0);

            for (int i = 0; i < data.length(); i++) {
                for (int j = 0; j < data.length(i); j++) {
                    double val = data.get(i,j).doubleValue();
                    stats.max(Math.max(val, stats.max()));
                    stats.min(Math.min(val, stats.min()));
                    stats.mean(stats.mean()+val);
                }
            }

            //TODO: use a streaming algorithm to calculate stdev
            double mean = stats.mean() / ((double)n);
            stats.mean(mean);

            double stdev = 0;
            for (int i = 0; i < data.length(); i++) {
                for (int j = 0; j < data.length(i); j++) {
                    double val = data.get(i,j).doubleValue();
                    double diff = mean - val;
                    stdev += diff * diff;
                }
            }

            stats.stdev(Math.sqrt(stdev / ((double)n)));

            return stats;
        }
    }
}
