/**
 * Copyright (C) 2003-2017, Foxit Software Inc..
 * All Rights Reserved.
 * <p>
 * http://www.foxitsoftware.com
 * <p>
 * The following code is copyrighted and is the proprietary of Foxit Software Inc.. It is not allowed to
 * distribute any parts of Foxit Mobile PDF SDK to third party or public without permission unless an agreement
 * is signed between Foxit Software Inc. and customers to explicitly grant customers permissions.
 * Review legal.txt for additional license and legal information.
 */
package com.foxit.uiextensions.modules.thumbnail;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.NonNull;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;

public class ThumbnailItem implements Comparable<ThumbnailItem> {
    private boolean isSelected;
    private final PDFViewCtrl mPDFView;
    private Point mThumbnailSize;
    private final Point mBackgroundSize;
    private PDFPage mPDFPage;
    private boolean mbNeedCompute;
    private Rect mThumbnailRect;
    private Bitmap mBitmap;

    public final static int EDIT_NO_VIEW = 0;
    public final static int EDIT_LEFT_VIEW = 1;
    public final static int EDIT_RIGHT_VIEW = 2;

    public int editViewFlag = 0;
    private boolean isRendering = false;

    public ThumbnailItem(int pageIndex, Point backgroundSize, PDFViewCtrl pdfViewCtrl) {
        try {
            mPDFPage = pdfViewCtrl.getDoc().getPage(pageIndex);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        mPDFView = pdfViewCtrl;
        mBackgroundSize = backgroundSize;
        isSelected = false;
        mbNeedCompute = true;
    }

    public int getIndex() {
        try {
            return mPDFPage.getIndex();
        } catch (PDFException e) {
            return -1;
        }
    }

    public boolean isRendering() {
        return isRendering;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public void resetRending(boolean rendering) {
        isRendering = rendering;
    }

    public boolean needRecompute() {
        return mbNeedCompute;
    }

    public PDFPage getPage() {
        return mPDFPage;
    }

    public void closePage() {
        try {
            if (mPDFPage != null) {
                mPDFPage.getDocument().closePage(getIndex());
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    public boolean isSelected() {
        return this.isSelected;
    }

    public boolean setRotation(int rotation) {
        boolean success = true;
        int[] pageIndexes = new int[1];
        pageIndexes[0] = getIndex();
        if (mPDFView.rotatePages(pageIndexes, rotation)) {
        }
        mbNeedCompute = true;
        return success;
    }

    public int getRotation() {
        int rotation = 0;
        try {
            rotation = getPage() != null ? getPage().getRotation() : 0;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return rotation;
    }

    private void compute() {
        if (mThumbnailRect == null) {
            mThumbnailRect = new Rect();
        }
        if (mThumbnailSize == null) {
            mThumbnailSize = new Point();
        }
        try {
            PDFPage page = getPage();
            if (page == null)
                return;
            float psWidth = page.getWidth();
            float psHeight = page.getHeight();

            float scale = Math.min(mBackgroundSize.x / psWidth, mBackgroundSize.y / psHeight);
            psWidth *= scale;
            psHeight *= scale;
            int left = (int) (mBackgroundSize.x / 2.0f - psWidth / 2.0f);
            int top = (int) (mBackgroundSize.y / 2.0f - psHeight / 2.0f);
            final int right = mBackgroundSize.x - left;
            final int bottom = mBackgroundSize.y - top;
            mThumbnailRect.set(left, top, right, bottom);
            mThumbnailSize.set((int) psWidth, (int) psHeight);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        mbNeedCompute = false;
    }

    public Point getSize() {
        if (mbNeedCompute)
            compute();
        return new Point(mThumbnailSize);
    }

    public Rect getRect() {
        if (mbNeedCompute)
            compute();
        return new Rect(mThumbnailRect);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof ThumbnailItem)) return false;
        return this == o || this.getIndex() == ((ThumbnailItem) o).getIndex();
    }

    @Override
    public int compareTo(@NonNull ThumbnailItem another) {
        return getIndex() - another.getIndex();
    }
}
