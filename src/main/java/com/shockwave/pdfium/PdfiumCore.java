package com.shockwave.pdfium;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Surface;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class PdfiumCore {
    private static final String TAG = PdfiumCore.class.getName();

    static {
        System.loadLibrary("modpdfium");
        System.loadLibrary("jniPdfium");
    }

    private native long nativeOpenDocument(int fd, String password);

    private native long nativeOpenMemDocument(byte[] data, String password);

    private native void nativeCloseDocument(long docPtr);

    private native int nativeGetPageCount(long docPtr);

    private native long nativeLoadPage(long docPtr, int pageIndex);

    private native long[] nativeLoadPages(long docPtr, int fromIndex, int toIndex);

    private native void nativeClosePage(long pagePtr);

    private native void nativeClosePages(long[] pagesPtr);

    private native int nativeGetPageWidthPixel(long pagePtr, int dpi);

    private native int nativeGetPageHeightPixel(long pagePtr, int dpi);

    private native int nativeGetPageWidthPoint(long pagePtr);

    private native int nativeGetPageHeightPoint(long pagePtr);

    //private native long nativeGetNativeWindow(Surface surface);
    //private native void nativeRenderPage(long pagePtr, long nativeWindowPtr);
    private native void nativeRenderPage(long pagePtr, Surface surface, int dpi,
                                         int startX, int startY,
                                         int drawSizeHor, int drawSizeVer,
                                         boolean renderAnnot);

    private native void nativeRenderPageBitmap(long pagePtr, Bitmap bitmap, int dpi,
                                               int startX, int startY,
                                               int drawSizeHor, int drawSizeVer,
                                               boolean renderAnnot);

    private native String nativeGetDocumentMetaText(long docPtr, String tag);

    private native Long nativeGetFirstChildBookmark(long docPtr, Long bookmarkPtr);

    private native Long nativeGetSiblingBookmark(long docPtr, long bookmarkPtr);

    private native String nativeGetBookmarkTitle(long bookmarkPtr);

    private native long nativeGetBookmarkDestIndex(long docPtr, long bookmarkPtr);

    private native Long nativeGetLinkAtPoint(long pagePtr, double x, double y);

    private native Long nativeGetLinkDest(long docPtr, long linkPtr);

    private native int nativeGetDestPageIndex(long docPtr, long destPtr);

    private native Long nativeGetLinkAction(long linkPtr);

    private native int nativeGetActionType(long actionPtr);

    private native byte[] nativeGetActionURIPath(long docPtr, long actionPtr);

    private native double[] nativeDeviceToPage(long pagePtr, int startX, int startY, int width, int height, int rotate, int deviceX, int deviceY);

    private static final Class FD_CLASS = FileDescriptor.class;
    private static final String FD_FIELD_NAME = "descriptor";
    private static Field mFdField = null;

    private int mCurrentDpi;

    /* synchronize native methods */
    private static final Object lock = new Object();

    public PdfiumCore(Context ctx) {
        mCurrentDpi = ctx.getResources().getDisplayMetrics().densityDpi;
    }

    public static int getNumFd(ParcelFileDescriptor fdObj) {
        try {
            if (mFdField == null) {
                mFdField = FD_CLASS.getDeclaredField(FD_FIELD_NAME);
                mFdField.setAccessible(true);
            }

            return mFdField.getInt(fdObj.getFileDescriptor());
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return -1;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public PdfDocument newDocument(ParcelFileDescriptor fd) throws IOException {
        return newDocument(fd, null);
    }

    public PdfDocument newDocument(ParcelFileDescriptor fd, String password) throws IOException {
        PdfDocument document = new PdfDocument();
        document.parcelFileDescriptor = fd;
        synchronized (lock) {
            document.mNativeDocPtr = nativeOpenDocument(getNumFd(fd), password);
        }

        return document;
    }

    public PdfDocument newDocument(byte[] data) throws IOException {
        return newDocument(data, null);
    }

    public PdfDocument newDocument(byte[] data, String password) throws IOException {
        PdfDocument document = new PdfDocument();
        synchronized (lock) {
            document.mNativeDocPtr = nativeOpenMemDocument(data, password);
        }
        return document;
    }

    public int getPageCount(PdfDocument doc) {
        synchronized (lock) {
            return nativeGetPageCount(doc.mNativeDocPtr);
        }
    }

    public long openPage(PdfDocument doc, int pageIndex) {
        long pagePtr;
        synchronized (lock) {
            pagePtr = nativeLoadPage(doc.mNativeDocPtr, pageIndex);
            doc.mNativePagesPtr.put(pageIndex, pagePtr);
            return pagePtr;
        }

    }

    public long[] openPage(PdfDocument doc, int fromIndex, int toIndex) {
        long[] pagesPtr;
        synchronized (lock) {
            pagesPtr = nativeLoadPages(doc.mNativeDocPtr, fromIndex, toIndex);
            int pageIndex = fromIndex;
            for (long page : pagesPtr) {
                if (pageIndex > toIndex) break;
                doc.mNativePagesPtr.put(pageIndex, page);
                pageIndex++;
            }

            return pagesPtr;
        }
    }

    public int getPageWidth(PdfDocument doc, int index) {
        synchronized (lock) {
            Long pagePtr;
            if ((pagePtr = doc.mNativePagesPtr.get(index)) != null) {
                return nativeGetPageWidthPixel(pagePtr, mCurrentDpi);
            }
            return 0;
        }
    }

    public int getPageHeight(PdfDocument doc, int index) {
        synchronized (lock) {
            Long pagePtr;
            if ((pagePtr = doc.mNativePagesPtr.get(index)) != null) {
                return nativeGetPageHeightPixel(pagePtr, mCurrentDpi);
            }
            return 0;
        }
    }

    public int getPageWidthPoint(PdfDocument doc, int index) {
        synchronized (lock) {
            Long pagePtr;
            if ((pagePtr = doc.mNativePagesPtr.get(index)) != null) {
                return nativeGetPageWidthPoint(pagePtr);
            }
            return 0;
        }
    }

    public int getPageHeightPoint(PdfDocument doc, int index) {
        synchronized (lock) {
            Long pagePtr;
            if ((pagePtr = doc.mNativePagesPtr.get(index)) != null) {
                return nativeGetPageHeightPoint(pagePtr);
            }
            return 0;
        }
    }

    public void renderPage(PdfDocument doc, Surface surface, int pageIndex,
                           int startX, int startY, int drawSizeX, int drawSizeY) {
        renderPage(doc, surface, pageIndex, startX, startY, drawSizeX, drawSizeY, false);
    }

    public void renderPage(PdfDocument doc, Surface surface, int pageIndex,
                           int startX, int startY, int drawSizeX, int drawSizeY,
                           boolean renderAnnot) {
        synchronized (lock) {
            try {
                //nativeRenderPage(doc.mNativePagesPtr.get(pageIndex), surface, mCurrentDpi);
                nativeRenderPage(doc.mNativePagesPtr.get(pageIndex), surface, mCurrentDpi,
                        startX, startY, drawSizeX, drawSizeY, renderAnnot);
            } catch (NullPointerException e) {
                Log.e(TAG, "mContext may be null");
                e.printStackTrace();
            } catch (Exception e) {
                Log.e(TAG, "Exception throw from native");
                e.printStackTrace();
            }
        }
    }

    public void renderPageBitmap(PdfDocument doc, Bitmap bitmap, int pageIndex,
                                 int startX, int startY, int drawSizeX, int drawSizeY) {
        renderPageBitmap(doc, bitmap, pageIndex, startX, startY, drawSizeX, drawSizeY, false);
    }

    public void renderPageBitmap(PdfDocument doc, Bitmap bitmap, int pageIndex,
                                 int startX, int startY, int drawSizeX, int drawSizeY,
                                 boolean renderAnnot) {
        synchronized (lock) {
            try {
                nativeRenderPageBitmap(doc.mNativePagesPtr.get(pageIndex), bitmap, mCurrentDpi,
                        startX, startY, drawSizeX, drawSizeY, renderAnnot);
            } catch (NullPointerException e) {
                Log.e(TAG, "mContext may be null");
                e.printStackTrace();
            } catch (Exception e) {
                Log.e(TAG, "Exception throw from native");
                e.printStackTrace();
            }
        }
    }

    public void closeDocument(PdfDocument doc) {
        synchronized (lock) {
            for (Integer index : doc.mNativePagesPtr.keySet()) {
                nativeClosePage(doc.mNativePagesPtr.get(index));
            }
            doc.mNativePagesPtr.clear();

            nativeCloseDocument(doc.mNativeDocPtr);

            if (doc.parcelFileDescriptor != null) { //if document was loaded from file
                try {
                    doc.parcelFileDescriptor.close();
                } catch (IOException e) {
                /* ignore */
                }
                doc.parcelFileDescriptor = null;
            }
        }
    }

    public PdfDocument.Meta getDocumentMeta(PdfDocument doc) {
        synchronized (lock) {
            PdfDocument.Meta meta = new PdfDocument.Meta();
            meta.title = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Title");
            meta.author = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Author");
            meta.subject = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Subject");
            meta.keywords = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Keywords");
            meta.creator = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Creator");
            meta.producer = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Producer");
            meta.creationDate = nativeGetDocumentMetaText(doc.mNativeDocPtr, "CreationDate");
            meta.modDate = nativeGetDocumentMetaText(doc.mNativeDocPtr, "ModDate");

            return meta;
        }
    }

    public PdfDocument.Position deviceToPage(PdfDocument doc, int pageIndex, int startX, int startY, int width, int height, int rotate, int deviceX, int deviceY) {
        synchronized (lock) {
            Long pagePtr;
            PdfDocument.Position pos = new PdfDocument.Position();

            if ((pagePtr = doc.mNativePagesPtr.get(pageIndex)) != null) {
                double[] posXY = nativeDeviceToPage(pagePtr, startX, startY, width, height, rotate, deviceX, deviceY);
                pos.x = posXY[0];
                pos.y = posXY[1];
            }

            return pos;
        }
    }

    public PdfDocument.Link getLinkAtPoint(PdfDocument doc, int pageIndex, double x, double y) {
        synchronized (lock) {
            Long pagePtr;
            if ((pagePtr = doc.mNativePagesPtr.get(pageIndex)) != null) {
                Long linkPtr = nativeGetLinkAtPoint(pagePtr, x, y);
                if (linkPtr != null) {
                    PdfDocument.Link link = new PdfDocument.Link();
                    link.mNativePtr = linkPtr;
                    link.dest = this.getLinkDest(doc, link);
                    if (link.dest == null) {
                        link.action = this.getLinkAction(doc, link);
                    }
                    return link;
                }
            }

            return null;
        }
    }

    private PdfDocument.Dest getLinkDest(PdfDocument doc, PdfDocument.Link link) {
        synchronized (lock) {
            Long destPtr = nativeGetLinkDest(doc.mNativeDocPtr, link.mNativePtr);
            if (destPtr != null) {
                PdfDocument.Dest dest = new PdfDocument.Dest();
                dest.mNativePtr = destPtr;
                dest.pageIndex = nativeGetDestPageIndex(doc.mNativeDocPtr, destPtr);
                return dest;
            }

            return null;
        }
    }

    private PdfDocument.Action getLinkAction(PdfDocument doc, PdfDocument.Link link) {
        synchronized (lock) {
            Long actionPtr = nativeGetLinkAction(link.mNativePtr);
            if (actionPtr != null) {
                PdfDocument.Action action = new PdfDocument.Action();
                action.mNativePtr = actionPtr;
                action.type = nativeGetActionType(actionPtr);

                if (action.type == PdfDocument.Action.TYPE_URI) {
                    action.uri = getActionURI(doc, action);
                }
                return action;
            }

            return null;
        }
    }

    private Uri getActionURI(PdfDocument doc, PdfDocument.Action action) {
        synchronized (lock) {
            byte[] uriBytes = nativeGetActionURIPath(doc.mNativeDocPtr, action.mNativePtr);
            try {
                return Uri.parse(new String(uriBytes, "US-ASCII"));
            } catch(UnsupportedEncodingException e) {
                Log.e("PdfiumCore", "Unsupported URI encoding: " + e.getMessage());
                return null;
            }
        }
    }

    public List<PdfDocument.Bookmark> getTableOfContents(PdfDocument doc) {
        synchronized (lock) {
            List<PdfDocument.Bookmark> topLevel = new ArrayList<>();
            Long first = nativeGetFirstChildBookmark(doc.mNativeDocPtr, null);
            if (first != null) {
                recursiveGetBookmark(topLevel, doc, first);
            }
            return topLevel;
        }
    }

    private void recursiveGetBookmark(List<PdfDocument.Bookmark> tree, PdfDocument doc, long bookmarkPtr) {
        PdfDocument.Bookmark bookmark = new PdfDocument.Bookmark();
        bookmark.mNativePtr = bookmarkPtr;
        bookmark.title = nativeGetBookmarkTitle(bookmarkPtr);
        bookmark.pageIdx = nativeGetBookmarkDestIndex(doc.mNativeDocPtr, bookmarkPtr);
        tree.add(bookmark);

        Long child = nativeGetFirstChildBookmark(doc.mNativeDocPtr, bookmarkPtr);
        if (child != null) {
            recursiveGetBookmark(bookmark.getChildren(), doc, child);
        }

        Long sibling = nativeGetSiblingBookmark(doc.mNativeDocPtr, bookmarkPtr);
        if (sibling != null) {
            recursiveGetBookmark(tree, doc, sibling);
        }
    }
}
