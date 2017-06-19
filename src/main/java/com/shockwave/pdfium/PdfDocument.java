package com.shockwave.pdfium;

import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v4.util.ArrayMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PdfDocument {

    public static class Meta {
        String title;
        String author;
        String subject;
        String keywords;
        String creator;
        String producer;
        String creationDate;
        String modDate;

        public String getTitle() {
            return title;
        }

        public String getAuthor() {
            return author;
        }

        public String getSubject() {
            return subject;
        }

        public String getKeywords() {
            return keywords;
        }

        public String getCreator() {
            return creator;
        }

        public String getProducer() {
            return producer;
        }

        public String getCreationDate() {
            return creationDate;
        }

        public String getModDate() {
            return modDate;
        }
    }

    /**
     * Position in X Y Coordinate system
     */
    public static class Position {
        double x;
        double y;

        public double getX() {return x;}
        public double getY() {return y;}
    }

    /**
     * PDF links, used with annotations
     */
    public static class Link {
        long mNativePtr;
        PdfDocument.Dest dest;
        PdfDocument.Action action;

        public PdfDocument.Dest getDest() {return dest;}
        public PdfDocument.Action getAction() {return action;}
    }

    /**
     * PDF destionation, used with bookmarks and annotation links
     */
    public static class Dest {
        long mNativePtr;
        int pageIndex = -1;

        public int getPageIndex() {return pageIndex;};
    }

    /**
     * PDF action, used with bookmarks and annotation links
     */
    public static class Action {
        public static final int TYPE_UNSUPPORTED = 0;
        public static final int TYPE_DEST_INTERNAL = 1;
        public static final int TYPE_DEST_EXTERNAL = 2;
        public static final int TYPE_URI = 3;
        public static final int TYPE_LAUNCH_APP = 4;

        long mNativePtr;

        /**
         * Action types are:
         * 0 Unsupported action type.
         * 1 Go to a destination within current document.
         * 2 Go to a destination within another document.
         * 3 Universal Resource Identifier, including web pages and other Internet based
         * resources.
         * 4 Launch an application or open a file.
         */
        int type = TYPE_UNSUPPORTED;
        Uri uri;

        public Uri getUri() {return uri;}
    }

    public static class Bookmark {
        private List<Bookmark> children = new ArrayList<>();
        String title;
        Uri uri;
        long pageIdx;
        long mNativePtr;

        public List<Bookmark> getChildren() {
            return children;
        }

        public boolean hasChildren() {
            return !children.isEmpty();
        }

        public String getTitle() {
            return title;
        }

        public long getPageIdx() {
            return pageIdx;
        }

        public Uri getUri() { return uri; }
    }

    /*package*/ PdfDocument() {
    }

    /*package*/ long mNativeDocPtr;
    /*package*/ ParcelFileDescriptor parcelFileDescriptor;

    /*package*/ final Map<Integer, Long> mNativePagesPtr = new ArrayMap<>();

    public boolean hasPage(int index) {
        return mNativePagesPtr.containsKey(index);
    }
}
