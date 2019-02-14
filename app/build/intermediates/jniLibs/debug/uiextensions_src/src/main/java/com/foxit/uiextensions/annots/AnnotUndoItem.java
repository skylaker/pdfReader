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
package com.foxit.uiextensions.annots;

import android.graphics.RectF;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.DateTime;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.BorderInfo;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.uiextensions.IUndoItem;


public abstract class AnnotUndoItem implements IUndoItem {
    protected PDFViewCtrl mPdfViewCtrl;

    public int			mPageIndex;
    public int		    mType;
    public String		mNM;
    // current properties
    public RectF        mBBox;
    public long		    mColor;
    public float		mOpacity;
    public float		mLineWidth;
    public long 		mFlags;
    public String		mSubject;
    public String		mAuthor;
    public DateTime		mCreationDate;
    public DateTime     mModifiedDate;
    public String		mContents;
    public String		mIntent;
    public int  		mBorderStyle;
    public float[]      mDashes;

    // old properties
    public RectF		mOldBBox;
    public long 		mOldColor;
    public float		mOldOpacity;
    public float		mOldLineWidth;
    public long 		mOldFlags;
    public String		mOldSubject;
    public String		mOldAuthor;
    public DateTime		mOldCreationDate;
    public DateTime		mOldModifiedDate;
    public String		mOldContents;
    public String		mOldIntent;
    public int		    mOldBorderStyle;
    public float[]  	mOldDashes;


    public void setCurrentValue(Annot annot) {
        if (annot == null)
            return;
        try {
            if (annot.getPage() != null)
                mPageIndex = annot.getPage().getIndex();
            mType = annot.getType();
            mNM = annot.getUniqueID();
            mBBox = new RectF(annot.getRect());
            mColor = annot.getBorderColor();
            mFlags = annot.getFlags();
            mModifiedDate = annot.getModifiedDateTime();
            mContents = annot.getContent();
            if (annot.isMarkup()) {
                mOpacity = ((Markup)annot).getOpacity();
                mSubject = ((Markup)annot).getSubject();
                mAuthor = ((Markup)annot).getTitle();
                mCreationDate = ((Markup)annot).getCreationDateTime();
                mIntent = ((Markup)annot).getIntent();
            }

            BorderInfo borderInfo = annot.getBorderInfo();
            if (borderInfo != null) {
                mLineWidth = borderInfo.getWidth();
                mBorderStyle = borderInfo.getStyle();
                float[] dashes = borderInfo.getDashes();
                if (dashes != null) {
                    mDashes = new float[dashes.length];
                    System.arraycopy(borderInfo.getDashes(), 0, mDashes, 0, dashes.length);
                }

            }

        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    public void setOldValue(Annot annot) {
        if (annot == null) return;
        try {
            if (annot.getPage() != null)
                mPageIndex = annot.getPage().getIndex();
            mType = annot.getType();
            mNM = annot.getUniqueID();
            mOldBBox = new RectF(annot.getRect());
            mOldColor = annot.getBorderColor();
            mOldFlags = annot.getFlags();
            mOldModifiedDate = annot.getModifiedDateTime();
            mOldContents = annot.getContent();
            if (annot.isMarkup()) {
                mOldOpacity = ((Markup)annot).getOpacity();
                mOldSubject = ((Markup)annot).getSubject();
                mOldAuthor = ((Markup)annot).getTitle();
                mOldCreationDate = ((Markup)annot).getCreationDateTime();
                mOldIntent = ((Markup)annot).getIntent();
            }

            BorderInfo borderInfo = annot.getBorderInfo();
            if (borderInfo != null) {
                mOldLineWidth = borderInfo.getWidth();
                mOldBorderStyle = borderInfo.getStyle();
                float[] dashes = borderInfo.getDashes();
                if (dashes != null) {
                    mOldDashes = new float[dashes.length];
                    System.arraycopy(borderInfo.getDashes(), 0, mOldDashes, 0, dashes.length);
                }

            }

        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    public void setCurrentValue(AnnotContent content) {
        mPageIndex = content.getPageIndex();
        mType = content.getType();
        mNM = content.getNM();
        if (content.getBBox() != null) {
            mBBox = new RectF(content.getBBox());
        }

        mColor = content.getColor();
        mOpacity = content.getOpacity() / 255f;
        if (content.getLineWidth() != 0)
            mLineWidth = content.getLineWidth();
        if (content.getSubject() != null)
            mSubject = content.getSubject();
        if (content.getModifiedDate() != null)
            mModifiedDate = content.getModifiedDate();
        if (content.getContents() != null) {
            mContents = content.getContents();
        }

        if (content.getIntent() != null) {
            mIntent = content.getIntent();
        }

    }

    public void setOldValue(AnnotContent content) {
        mPageIndex = content.getPageIndex();
        mType = content.getType();
        mNM = content.getNM();
        if (content.getBBox() != null)
            mOldBBox = new RectF(content.getBBox());
            mOldColor = content.getColor();
            mOldOpacity = content.getOpacity() / 255f;
        if (content.getLineWidth() != 0)
            mOldLineWidth = content.getLineWidth();
        if (content.getSubject() != null)
            mOldSubject = content.getSubject();
        if (content.getModifiedDate() != null)
            mOldModifiedDate = content.getModifiedDate();
        if (content.getContents() != null)
            mOldContents = content.getContents();
        if (content.getIntent() != null)
            mOldIntent = content.getIntent();
    }
}
