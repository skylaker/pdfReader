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

import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.uiextensions.utils.Event;

public interface AnnotHandler extends PDFViewCtrl.IDrawEventListener{

    int getType();

    public boolean annotCanAnswer(Annot annot);

    public RectF getAnnotBBox(Annot annot);

    public boolean isHitAnnot(Annot annot, PointF point);

    public void onAnnotSelected(Annot annot, boolean reRender);

    public void onAnnotDeselected(Annot annot, boolean reRender);

    public void addAnnot(int pageIndex, AnnotContent content, boolean addUndo, Event.Callback result);

    public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result);

    public void removeAnnot(Annot annot, boolean addUndo, Event.Callback result);

    boolean onTouchEvent(int pageIndex, MotionEvent motionEvent, Annot annot);

    boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot);

    boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, Annot annot);
}
