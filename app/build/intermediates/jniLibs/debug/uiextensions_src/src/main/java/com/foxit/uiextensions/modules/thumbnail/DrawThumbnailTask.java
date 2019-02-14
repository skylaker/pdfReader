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
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;

import com.foxit.sdk.Task;
import com.foxit.sdk.common.CommonDefines;
import com.foxit.sdk.common.PDFError;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.Renderer;

public class DrawThumbnailTask extends Task {
    private final Rect mBmpArea;
    private final Point mViewSize;
    private Bitmap mBmp;
    private final PDFPage mPDFPage;
    private DrawThumbnailCallback mCallback;
    private ThumbnailItem mThumbnailItem;

    public DrawThumbnailTask(final ThumbnailItem item, final DrawThumbnailCallback callback) {
        super(new CallBack() {
            @Override
            public void result(Task task) {
                item.resetRending(false);
                DrawThumbnailTask task1 = (DrawThumbnailTask) task;
                if (task1.mStatus == STATUS_FINISHED) {
                    if (task1.mCallback != null) {
                        task1.mCallback.result(item, task1, ((DrawThumbnailTask) task).mBmp);
                    }
                }
            }

        });
        mCallback = callback;
        mPDFPage = item.getPage();
        mBmpArea = new Rect(0, 0, item.getSize().x, item.getSize().y);
        mViewSize = item.getSize();
        mPriority = PRIORITY_PATCH;
        mThumbnailItem = item;
        mThumbnailItem.resetRending(true);
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    protected void prepare() {
        if (mBmp == null) {
            mBmp = Bitmap.createBitmap(mBmpArea.width(), mBmpArea.height(),
                    Bitmap.Config.ARGB_8888);
        }
    }

    @Override
    protected void execute() {
        if (mStatus != STATUS_REDAY)
            return;
        mStatus = STATUS_RUNNING;

        if (mBmpArea.width() == 0 || mBmpArea.height() == 0) {
            mStatus = STATUS_ERROR;
            return;
        }

        if (mBmp != null) {
            renderPage();
        } else {
            mErr = PDFError.UNKNOWN_ERROR.getCode();
            mStatus = STATUS_ERROR;
        }
    }

    private void renderPage() {
        try {
            if (!mPDFPage.isParsed()) {
                int nRet = mPDFPage.startParse(PDFPage.e_parsePageNormal, null, false);
                while (nRet == CommonDefines.e_progressToBeContinued) {
                    nRet = mPDFPage.continueParse();
                }
            }

            Matrix matrix = mPDFPage.getDisplayMatrix(-mBmpArea.left, -mBmpArea.top, mViewSize.x, mViewSize.y, 0);

            int colorMode = Renderer.e_colorModeNormal;
            if (mPDFPage.hasTransparency()) {
                mBmp.eraseColor(Color.TRANSPARENT);
            } else {
                mBmp.eraseColor(Color.WHITE);
            }

            Renderer render = Renderer.create(mBmp);
            if (render == null) {
                mErr = PDFError.UNKNOWN_ERROR.getCode();
                mStatus = STATUS_ERROR;
                return;
            }
            render.setColorMode(colorMode);
            render.setRenderContent(Renderer.e_renderPage | Renderer.e_renderAnnot);
            int progress = render.startRender(mPDFPage, matrix, null);
            while (progress == CommonDefines.e_progressToBeContinued) {
                progress = render.continueRender();
            }
            render.release();
            mErr = PDFError.NO_ERROR.getCode();
            mStatus = STATUS_FINISHED;
        } catch (PDFException e) {
            mErr = e.getLastError();
            mStatus = STATUS_ERROR;
        }
    }

    public ThumbnailItem getThumbnailItem() {
        return mThumbnailItem;
    }
}
