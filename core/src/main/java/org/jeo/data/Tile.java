package org.jeo.data;

import java.util.Arrays;

public class Tile {

    Integer zoom, column, row;
    byte[] data;
    String mimeType;

    public Tile() {
    }

    public Tile(Integer zoom, Integer column, Integer row, byte[] data, String mimeType) {
        super();
        this.zoom = zoom;
        this.column = column;
        this.row = row;
        this.data = data;
        this.mimeType = mimeType;
    }

    public Integer getZoom() {
        return zoom;
    }

    public void setZoom(Integer zoom) {
        this.zoom = zoom;
    }

    public Integer getColumn() {
        return column;
    }

    public void setColumn(Integer column) {
        this.column = column;
    }

    public Integer getRow() {
        return row;
    }

    public void setRow(Integer row) {
        this.row = row;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((column == null) ? 0 : column.hashCode());
        result = prime * result + Arrays.hashCode(data);
        result = prime * result + ((row == null) ? 0 : row.hashCode());
        result = prime * result + ((zoom == null) ? 0 : zoom.hashCode());
        result = prime * result + ((mimeType == null) ? 0 : mimeType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Tile other = (Tile) obj;
        if (column == null) {
            if (other.column != null)
                return false;
        } else if (!column.equals(other.column))
            return false;
        if (!Arrays.equals(data, other.data))
            return false;
        if (row == null) {
            if (other.row != null)
                return false;
        } else if (!row.equals(other.row))
            return false;
        if (zoom == null) {
            if (other.zoom != null)
                return false;
        } else if (!zoom.equals(other.zoom))
            return false;
        if (mimeType == null) {
            if (other.mimeType != null)
                return false;
        } else if (!mimeType.equals(other.mimeType))
            return false;
        return true;
    }
}
